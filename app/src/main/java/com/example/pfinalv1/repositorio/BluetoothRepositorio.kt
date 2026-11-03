package com.example.pfinalv1.repositorio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * UUID del perfil SPP (Serial Port Profile): Bluetooth clásico "como si fuera un cable serie".
 * Es el que usa BluetoothSerial en el ESP32 (sketch EBESP32_LED_Bt.ino del profe).
 */
private val UUID_SPP: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
private const val TAG = "BluetoothRepositorio"

/** Estado del adaptador Bluetooth del teléfono. */
enum class EstadoAdaptador { NO_SOPORTADO, APAGADO, ENCENDIDO }

/** Estados posibles de la conexión con el ESP32; la IU hace un when sobre ellos. */
sealed class EstadoConexion {
    data object Inactivo : EstadoConexion()
    data class Conectando(val nombreDispositivo: String) : EstadoConexion()
    data class Conectado(val nombreDispositivo: String) : EstadoConexion()
    data class Error(val mensaje: String) : EstadoConexion()
    data object Desconectado : EstadoConexion()
}

/**
 * Patrón de repositorio (como BluetoothRepository de ConectaESP32): abre el socket RFCOMM,
 * lee lo que manda el ESP32 en un ciclo de corrutina, escribe comandos y cierra todo.
 *
 * El ViewModel observa dos flujos:
 *  - [estadoConexion]  → StateFlow con el estado actual (siempre tiene un valor)
 *  - [datosRecibidos]  → SharedFlow con cada pedazo de texto que llega (eventos)
 *
 * Los permisos (BLUETOOTH_CONNECT en Android 12+) se piden en la IU antes de llamar aquí.
 */
class BluetoothRepositorio(context: Context) {

    // "as?" por si el dispositivo no tiene Bluetooth (p. ej. el emulador): el adaptador queda en null
    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val adaptador: BluetoothAdapter? = bluetoothManager?.adapter

    private var socket: BluetoothSocket? = null
    private var salida: OutputStream? = null
    private var entrada: InputStream? = null
    private var jobLectura: Job? = null

    private val _estadoConexion = MutableStateFlow<EstadoConexion>(EstadoConexion.Inactivo)
    val estadoConexion: StateFlow<EstadoConexion> = _estadoConexion.asStateFlow()

    // extraBufferCapacity: si llegan datos antes de que alguien los lea, no se pierden ni bloquean
    private val _datosRecibidos = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val datosRecibidos: SharedFlow<String> = _datosRecibidos.asSharedFlow()

    fun estadoAdaptador(): EstadoAdaptador = when {
        adaptador == null -> EstadoAdaptador.NO_SOPORTADO
        !adaptador.isEnabled -> EstadoAdaptador.APAGADO
        else -> EstadoAdaptador.ENCENDIDO
    }

    /**
     * Conecta con el dispositivo emparejado de dirección MAC [direccion].
     * La conexión bloquea el hilo, por eso corre en Dispatchers.IO dentro del [scope]
     * (el viewModelScope del ViewModel).
     */
    @SuppressLint("MissingPermission") // la IU ya verificó los permisos antes de llamar
    fun conectar(direccion: String, nombre: String?, scope: CoroutineScope) {
        val adaptadorLocal = adaptador
        if (adaptadorLocal == null || !adaptadorLocal.isEnabled) {
            _estadoConexion.value = EstadoConexion.Error("El Bluetooth está apagado o no está disponible")
            return
        }

        val dispositivo: BluetoothDevice = try {
            adaptadorLocal.getRemoteDevice(direccion)
        } catch (e: IllegalArgumentException) {
            _estadoConexion.value = EstadoConexion.Error("Dirección MAC inválida: $direccion")
            return
        }

        val nombreMostrado = nombre?.takeIf { it.isNotBlank() } ?: direccion
        _estadoConexion.value = EstadoConexion.Conectando(nombreMostrado)

        scope.launch(Dispatchers.IO) {
            try {
                cerrarConexionInterna() // por si había una conexión anterior
                // Android recomienda cancelar el descubrimiento antes de conectar: lo hace más lento
                adaptadorLocal.cancelDiscovery()
                val nuevoSocket = dispositivo.createRfcommSocketToServiceRecord(UUID_SPP)
                socket = nuevoSocket
                nuevoSocket.connect() // bloquea hasta conectar o fallar
                salida = nuevoSocket.outputStream
                entrada = nuevoSocket.inputStream
                _estadoConexion.value = EstadoConexion.Conectado(nombreMostrado)
                Log.d(TAG, "Conectado a $nombreMostrado ($direccion)")
                iniciarLectura(scope)
            } catch (e: IOException) {
                Log.e(TAG, "Error al conectar: ${e.message}", e)
                _estadoConexion.value = EstadoConexion.Error(
                    "No se pudo conectar con $nombreMostrado. ¿Está encendido el ESP32?"
                )
                cerrarConexionInterna()
            } catch (e: SecurityException) {
                Log.e(TAG, "Error de seguridad al conectar: ${e.message}", e)
                _estadoConexion.value = EstadoConexion.Error("Falta el permiso de Bluetooth")
                cerrarConexionInterna()
            }
        }
    }

    /**
     * Ciclo de lectura: espera bytes del ESP32 (read bloquea) y los publica como texto.
     * Los bytes no traen formato; el ViewModel los junta y separa las tramas ("24.36C").
     */
    private fun iniciarLectura(scope: CoroutineScope) {
        jobLectura?.cancel()
        jobLectura = scope.launch(Dispatchers.IO) {
            val buffer = ByteArray(1024)
            try {
                while (isActive && socket?.isConnected == true) {
                    val flujo = entrada ?: break
                    val bytes = flujo.read(buffer)
                    if (bytes > 0) {
                        val texto = String(buffer, 0, bytes)
                        Log.d(TAG, "Recibido: $texto")
                        _datosRecibidos.emit(texto)
                    } else if (bytes < 0) {
                        Log.d(TAG, "El dispositivo remoto cerró la conexión (EOF)")
                        break
                    }
                }
            } catch (e: IOException) {
                // Si seguimos activos, la conexión se cayó sola; si no, fue un desconectar() nuestro
                if (isActive) Log.w(TAG, "Lectura interrumpida: ${e.message}")
            } finally {
                if (_estadoConexion.value is EstadoConexion.Conectado) {
                    _estadoConexion.value = EstadoConexion.Desconectado
                    cerrarConexionInterna()
                }
            }
        }
    }

    /** Escribe [texto] en el socket (por ejemplo "1", "0" o "t"). */
    fun enviar(texto: String, scope: CoroutineScope) {
        val flujo = salida
        if (flujo == null || socket?.isConnected != true) {
            _estadoConexion.value = EstadoConexion.Error("No hay conexión para enviar")
            return
        }
        scope.launch(Dispatchers.IO) {
            try {
                flujo.write(texto.toByteArray())
                flujo.flush()
                Log.d(TAG, "Enviado: $texto")
            } catch (e: IOException) {
                Log.e(TAG, "Error al enviar: ${e.message}")
                _estadoConexion.value = EstadoConexion.Error("Se perdió la conexión al enviar")
                cerrarConexionInterna()
            }
        }
    }

    /** Cierra la conexión a petición del usuario. */
    fun desconectar(scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            cerrarConexionInterna()
            _estadoConexion.value = EstadoConexion.Desconectado
        }
    }

    /**
     * Versión síncrona para onCleared() del ViewModel: en ese momento viewModelScope ya
     * está cancelado y un launch no correría, así que se cierra directo.
     */
    fun liberar() {
        cerrarConexionInterna()
        _estadoConexion.value = EstadoConexion.Desconectado
    }

    private fun cerrarConexionInterna() {
        jobLectura?.cancel()
        jobLectura = null
        try {
            salida?.close()
            entrada?.close()
            socket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error al cerrar el socket: ${e.message}")
        } finally {
            salida = null
            entrada = null
            socket = null
        }
    }
}

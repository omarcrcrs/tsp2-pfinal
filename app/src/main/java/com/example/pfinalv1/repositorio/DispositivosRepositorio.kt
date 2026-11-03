package com.example.pfinalv1.repositorio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import com.example.pfinalv1.model.DispositivoBluetooth

private const val TAG = "DispositivosRepositorio"

/**
 * Lista los dispositivos ya emparejados con el teléfono (como PairedDevicesRepository
 * del repo ConectaESP32). La app no "descubre" dispositivos nuevos: el ESP32 se empareja
 * una sola vez desde Ajustes → Bluetooth del sistema y aquí solo se elige.
 */
class DispositivosRepositorio(context: Context) {

    // "as?" por si el dispositivo no tiene Bluetooth (p. ej. el emulador): el adaptador queda en null
    private val adaptador: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    fun estadoAdaptador(): EstadoAdaptador = when {
        adaptador == null -> EstadoAdaptador.NO_SOPORTADO
        !adaptador.isEnabled -> EstadoAdaptador.APAGADO
        else -> EstadoAdaptador.ENCENDIDO
    }

    /** Dispositivos emparejados, con los ESP32 primero. Lista vacía si no hay Bluetooth o permisos. */
    @SuppressLint("MissingPermission") // la IU pide BLUETOOTH_CONNECT antes de llamar
    fun obtenerEmparejados(): List<DispositivoBluetooth> {
        val adaptadorLocal = adaptador
        if (adaptadorLocal == null || !adaptadorLocal.isEnabled) {
            Log.w(TAG, "Bluetooth no disponible o apagado")
            return emptyList()
        }
        return try {
            adaptadorLocal.bondedDevices
                .orEmpty()
                .map { dispositivo -> dispositivo.aDispositivoBluetooth() }
                .sortedByDescending { it.esEsp32 }
        } catch (e: SecurityException) {
            Log.e(TAG, "Sin permiso para leer los dispositivos emparejados", e)
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.aDispositivoBluetooth(): DispositivoBluetooth {
        val nombreDispositivo = this.name ?: "Dispositivo sin nombre"
        return DispositivoBluetooth(
            nombre = nombreDispositivo,
            direccion = this.address,
            tipo = when (this.type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Clásico"
                BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                else -> "Desconocido"
            },
            esEsp32 = nombreDispositivo.contains("ESP32", ignoreCase = true) ||
                nombreDispositivo.contains("ESP", ignoreCase = true) ||
                nombreDispositivo.contains("HC-", ignoreCase = true)
        )
    }
}

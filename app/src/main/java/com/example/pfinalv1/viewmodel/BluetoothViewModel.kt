package com.example.pfinalv1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pfinalv1.data.SimuladorEsp32
import com.example.pfinalv1.domain.OBJETIVO_MAXIMO
import com.example.pfinalv1.domain.OBJETIVO_MINIMO
import com.example.pfinalv1.domain.OBJETIVO_POR_DEFECTO
import com.example.pfinalv1.domain.PASO_OBJETIVO
import com.example.pfinalv1.domain.extraerTemperatura
import com.example.pfinalv1.domain.formatearHora
import com.example.pfinalv1.domain.formatearTemperatura
import com.example.pfinalv1.domain.ledDeberiaEncender
import com.example.pfinalv1.domain.separarTramas
import com.example.pfinalv1.model.DispositivoBluetooth
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.model.Origenes
import com.example.pfinalv1.repositorio.BluetoothRepositorio
import com.example.pfinalv1.repositorio.EstadoAdaptador
import com.example.pfinalv1.repositorio.EstadoConexion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Todo lo que la pantalla Cocina necesita dibujar, en un solo objeto inmutable.
 * La vista recibe este estado y manda eventos; nunca toca el repositorio ni el socket.
 */
data class EstadoCocina(
    val conexion: EstadoConexion = EstadoConexion.Inactivo,
    val estadoAdaptador: EstadoAdaptador = EstadoAdaptador.ENCENDIDO,
    val temperaturaC: Double? = null,          // null = todavía no hay lectura
    val horaUltimaLectura: Long? = null,       // milisegundos de la última lectura
    val ledEncendido: Boolean = false,
    val monitoreando: Boolean = false,         // corrutina de lectura periódica activa
    val modoSimulacion: Boolean = false,       // usa SimuladorEsp32 en vez del Bluetooth
    val objetivoC: Double = OBJETIVO_POR_DEFECTO, // temperatura que debe alcanzar el platillo
    val nombreObjetivo: String = "",           // "Pollo y aves", "Hervir (CDMX)"… ("" si es manual)
    val controlAutomatico: Boolean = false,    // LED ON al llegar al objetivo (termostato)
    val recetaEnCurso: String = "",            // receta del recetario que se está cocinando
    val nombreDispositivo: String = "",        // último ESP32 al que se conectó
    val bitacora: List<String> = emptyList()   // últimos mensajes enviados/recibidos
) {
    val conectado: Boolean get() = conexion is EstadoConexion.Conectado

    /** Hay de dónde leer: ESP32 conectado o simulador activo. */
    val puedeLeer: Boolean get() = conectado || modoSimulacion

    /** La última lectura ya llegó al objetivo de cocción. */
    val objetivoAlcanzado: Boolean get() = temperaturaC != null && temperaturaC >= objetivoC
}

/**
 * ViewModel de la comunicación con el ESP32 (intermediario entre BluetoothRepositorio y la IU,
 * como BluetoothViewModel de ConectaESP32). Es AndroidViewModel porque el repositorio
 * necesita el Context de la aplicación para obtener el adaptador Bluetooth.
 *
 * Protocolo de un carácter del sketch EBESP32_LED_Bt.ino:
 *   '1' enciende el LED (GPIO5) · '0' lo apaga · 't' responde la temperatura del LM35 como "24.36C"
 */
class BluetoothViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = BluetoothRepositorio(application.applicationContext)
    private val simulador = SimuladorEsp32()

    private val _estado = MutableStateFlow(EstadoCocina(estadoAdaptador = repositorio.estadoAdaptador()))
    val estado: StateFlow<EstadoCocina> = _estado.asStateFlow()

    /** Avisos de una sola vez para el Snackbar (conectado, error, "activa la simulación"…). */
    private val _mensajes = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val mensajes: SharedFlow<String> = _mensajes.asSharedFlow()

    private var jobMonitoreo: Job? = null

    /** Texto que llegó del ESP32 y todavía no termina en "C" (tramas partidas). */
    private var bufferEntrada = ""

    init {
        // Observar el estado de la conexión del repositorio
        repositorio.estadoConexion
            .onEach { conexion ->
                _estado.update { it.copy(conexion = conexion) }
                when (conexion) {
                    is EstadoConexion.Conectado -> {
                        registrarEnBitacora("Conectado a ${conexion.nombreDispositivo}")
                        _mensajes.tryEmit("Conectado a ${conexion.nombreDispositivo}")
                    }
                    is EstadoConexion.Error -> {
                        if (!_estado.value.modoSimulacion) detenerMonitoreo()
                        registrarEnBitacora("Error: ${conexion.mensaje}")
                        _mensajes.tryEmit(conexion.mensaje)
                    }
                    is EstadoConexion.Desconectado -> {
                        if (!_estado.value.modoSimulacion) detenerMonitoreo()
                        registrarEnBitacora("Desconectado")
                    }
                    else -> Unit
                }
            }
            .launchIn(viewModelScope)

        // Observar los bytes que manda el ESP32
        repositorio.datosRecibidos
            .onEach { trozo -> procesarDatos(trozo) }
            .launchIn(viewModelScope)
    }

    // ───────────────────────── Conexión ─────────────────────────

    fun conectar(dispositivo: DispositivoBluetooth) {
        bufferEntrada = ""
        _estado.update {
            it.copy(
                nombreDispositivo = dispositivo.nombre,
                estadoAdaptador = repositorio.estadoAdaptador()
            )
        }
        repositorio.conectar(dispositivo.direccion, dispositivo.nombre, viewModelScope)
    }

    fun desconectar() {
        detenerMonitoreo()
        repositorio.desconectar(viewModelScope)
    }

    /** Vuelve a leer si el Bluetooth del teléfono está encendido (al regresar a la pantalla). */
    fun actualizarEstadoAdaptador() {
        _estado.update { it.copy(estadoAdaptador = repositorio.estadoAdaptador()) }
    }

    // ───────────────────────── Temperatura ─────────────────────────

    /** Pide una lectura: manda 't' al ESP32 o, en simulación, toma el siguiente valor del simulador. */
    fun solicitarTemperatura() {
        val actual = _estado.value
        when {
            actual.modoSimulacion -> registrarLectura(simulador.siguienteLectura(), simulada = true)
            actual.conectado -> {
                repositorio.enviar(COMANDO_TEMPERATURA, viewModelScope)
                registrarEnBitacora("→ $COMANDO_TEMPERATURA")
            }
            else -> _mensajes.tryEmit("Conecta el ESP32 o activa el modo simulación")
        }
    }

    /** Lectura periódica con una corrutina cancelable (tema de Corrutinas: launch, delay, cancel). */
    fun iniciarMonitoreo() {
        if (!_estado.value.puedeLeer) {
            _mensajes.tryEmit("Conecta el ESP32 o activa el modo simulación")
            return
        }
        jobMonitoreo?.cancel()
        jobMonitoreo = viewModelScope.launch {
            _estado.update { it.copy(monitoreando = true) }
            while (isActive && _estado.value.puedeLeer) {
                solicitarTemperatura()
                delay(INTERVALO_MONITOREO_MS)
            }
            // Se sale solo si ya no hay de dónde leer (se cayó la conexión)
            _estado.update { it.copy(monitoreando = false) }
        }
    }

    fun detenerMonitoreo() {
        jobMonitoreo?.cancel()
        jobMonitoreo = null
        _estado.update { it.copy(monitoreando = false) }
    }

    fun alternarMonitoreo() {
        if (_estado.value.monitoreando) detenerMonitoreo() else iniciarMonitoreo()
    }

    // ───────────────────────── LED y objetivo de cocción ─────────────────────────

    /** Manda '1' u '0' al ESP32 (en simulación solo cambia el estado en pantalla). */
    fun cambiarLed(encender: Boolean) {
        val actual = _estado.value
        if (!actual.puedeLeer) {
            _mensajes.tryEmit("Conecta el ESP32 o activa el modo simulación")
            return
        }
        if (!actual.modoSimulacion) {
            repositorio.enviar(if (encender) COMANDO_LED_ON else COMANDO_LED_OFF, viewModelScope)
        }
        _estado.update { it.copy(ledEncendido = encender) }
        registrarEnBitacora(if (encender) "→ $COMANDO_LED_ON (LED encendido)" else "→ $COMANDO_LED_OFF (LED apagado)")
    }

    /** Objetivo predefinido (Pollo 74 °C, Hervir 92 °C…) o sugerido por la receta. */
    fun fijarObjetivo(gradosC: Double, nombre: String = "") {
        _estado.update {
            it.copy(objetivoC = gradosC.coerceIn(OBJETIVO_MINIMO, OBJETIVO_MAXIMO), nombreObjetivo = nombre)
        }
        aplicarControlAutomatico()
    }

    fun subirObjetivo() = ajustarObjetivo(PASO_OBJETIVO)

    fun bajarObjetivo() = ajustarObjetivo(-PASO_OBJETIVO)

    private fun ajustarObjetivo(cambio: Double) {
        _estado.update {
            it.copy(
                objetivoC = (it.objetivoC + cambio).coerceIn(OBJETIVO_MINIMO, OBJETIVO_MAXIMO),
                nombreObjetivo = "" // al ajustar a mano deja de ser un preset
            )
        }
        aplicarControlAutomatico()
    }

    /** Termostato: con esto activo, el LED se enciende al llegar al objetivo y se apaga al bajar. */
    fun cambiarControlAutomatico(activar: Boolean) {
        _estado.update { it.copy(controlAutomatico = activar) }
        if (activar) aplicarControlAutomatico()
    }

    // ───────────────────────── Receta en curso ─────────────────────────

    /** Desde el detalle de una receta: "Cocinar con el termómetro" fija la receta y su objetivo sugerido. */
    fun iniciarCoccion(nombreReceta: String, objetivoSugeridoC: Double) {
        _estado.update { it.copy(recetaEnCurso = nombreReceta) }
        fijarObjetivo(objetivoSugeridoC, nombre = nombreReceta)
        registrarEnBitacora("Cocinando: $nombreReceta (objetivo ${formatearTemperatura(objetivoSugeridoC)})")
    }

    fun terminarCoccion() {
        _estado.update { it.copy(recetaEnCurso = "") }
    }

    // ───────────────────────── Simulación ─────────────────────────

    fun cambiarModoSimulacion(activar: Boolean) {
        detenerMonitoreo()
        _estado.update { it.copy(modoSimulacion = activar) }
        registrarEnBitacora(if (activar) "Modo simulación activado" else "Modo simulación desactivado")
    }

    /** Simula poner la olla al fuego (+10 °C) para ver la alerta y el LED sin esperar. */
    fun simularCalor() {
        if (!_estado.value.modoSimulacion) return
        simulador.calentar()
        solicitarTemperatura()
    }

    fun simularFrio() {
        if (!_estado.value.modoSimulacion) return
        simulador.enfriar()
        solicitarTemperatura()
    }

    // ───────────────────────── Historial ─────────────────────────

    /** Lectura lista para guardarse en Room con el estado actual de la cocina (null si aún no hay lectura). */
    fun lecturaActual(): Lectura? {
        val actual = _estado.value
        val temperatura = actual.temperaturaC ?: return null
        return Lectura(
            temperaturaC = temperatura,
            objetivoC = actual.objetivoC,
            fecha = actual.horaUltimaLectura ?: System.currentTimeMillis(),
            receta = actual.recetaEnCurso,
            origen = if (actual.modoSimulacion) Origenes.SIMULACION else Origenes.ESP32,
            dispositivo = if (actual.modoSimulacion) "Simulador" else actual.nombreDispositivo,
            ledEncendido = actual.ledEncendido
        )
    }

    // ───────────────────────── Internos ─────────────────────────

    /** Junta los pedazos que llegan por Bluetooth y procesa cada trama completa ("24.36C"). */
    private fun procesarDatos(trozo: String) {
        val (tramas, pendiente) = separarTramas(bufferEntrada + trozo)
        bufferEntrada = pendiente
        tramas.forEach { trama ->
            val temperatura = extraerTemperatura(trama)
            if (temperatura != null) {
                registrarEnBitacora("← ${trama}C")
                registrarLectura(temperatura, simulada = false)
            } else {
                registrarEnBitacora("← trama no reconocida: $trama")
            }
        }
    }

    private fun registrarLectura(temperatura: Double, simulada: Boolean) {
        val yaHabiaLlegado = _estado.value.objetivoAlcanzado
        _estado.update {
            it.copy(temperaturaC = temperatura, horaUltimaLectura = System.currentTimeMillis())
        }
        if (simulada) registrarEnBitacora("← ${formatearTemperatura(temperatura)} (simulada)")
        // Aviso de una sola vez cuando se cruza el objetivo
        if (!yaHabiaLlegado && _estado.value.objetivoAlcanzado) {
            _mensajes.tryEmit("¡Listo! Se alcanzó el objetivo de ${formatearTemperatura(_estado.value.objetivoC)}")
        }
        aplicarControlAutomatico()
    }

    private fun aplicarControlAutomatico() {
        val actual = _estado.value
        val temperatura = actual.temperaturaC ?: return
        if (!actual.controlAutomatico || !actual.puedeLeer) return
        val debeEncender = ledDeberiaEncender(temperatura, actual.objetivoC, actual.ledEncendido)
        if (debeEncender != actual.ledEncendido) cambiarLed(debeEncender)
    }

    private fun registrarEnBitacora(texto: String) {
        val linea = "${formatearHora(System.currentTimeMillis())}  $texto"
        _estado.update { it.copy(bitacora = (it.bitacora + linea).takeLast(MAX_BITACORA)) }
    }

    override fun onCleared() {
        detenerMonitoreo()
        repositorio.liberar()
        super.onCleared()
    }

    companion object {
        const val COMANDO_LED_ON = "1"
        const val COMANDO_LED_OFF = "0"
        const val COMANDO_TEMPERATURA = "t"
        const val INTERVALO_MONITOREO_MS = 2_000L
        private const val MAX_BITACORA = 8
    }
}

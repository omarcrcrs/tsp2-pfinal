package com.example.pfinalv1.data

import com.example.pfinalv1.domain.TEMPERATURA_AMBIENTE_REFERENCIA
import kotlin.math.roundToInt
import kotlin.random.Random

/**
 * "ESP32 de mentiras" para probar la app sin la tarjeta conectada (demos, pruebas de
 * instrumentación y para desarrollar en el emulador, que no tiene Bluetooth clásico).
 *
 * Imita al LM35 en la cocina: la temperatura cambia poco a poco (caminata aleatoria de
 * ±0.6 °C) y se mantiene entre 0 y 120 °C. Con [calentar] se simula poner la olla al fuego
 * y con [enfriar] retirarla, para ver la alerta y el control automático del LED sin esperar.
 *
 * El profe aceptó datos dummy para la segunda etapa; en la final el origen real es el ESP32.
 */
class SimuladorEsp32(semilla: Long? = null) {

    private val aleatorio: Random = if (semilla != null) Random(semilla) else Random.Default

    private var temperatura: Double = TEMPERATURA_AMBIENTE_REFERENCIA

    /** Responde como lo haría el ESP32 al comando 't': la temperatura actual con 2 decimales. */
    fun siguienteLectura(): Double {
        temperatura += aleatorio.nextDouble(-0.6, 0.6)
        temperatura = temperatura.coerceIn(MINIMA, MAXIMA)
        return redondear2(temperatura)
    }

    /** Sube de golpe la temperatura simulada (olla al fuego). */
    fun calentar(grados: Double = PASO_SIMULACION) {
        temperatura = (temperatura + grados).coerceAtMost(MAXIMA)
    }

    /** Baja de golpe la temperatura simulada (olla fuera del fuego). */
    fun enfriar(grados: Double = PASO_SIMULACION) {
        temperatura = (temperatura - grados).coerceAtLeast(MINIMA)
    }

    /** Misma cadena que manda el sketch real ("24.36C"); útil para probar el separador de tramas. */
    fun siguienteTrama(): String = String.format(java.util.Locale.US, "%.2fC", siguienteLectura())

    private fun redondear2(valor: Double): Double = (valor * 100.0).roundToInt() / 100.0

    companion object {
        const val MINIMA = 0.0
        const val MAXIMA = 120.0
        const val PASO_SIMULACION = 10.0
    }
}

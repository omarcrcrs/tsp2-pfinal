package com.example.pfinalv1.domain

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ───────────────────────── Lógica pura (sin Compose, sin Room, sin Bluetooth) ─────────────────────────
// Todo lo que se puede probar con una prueba unitaria local vive aquí (capa "domain",
// como el CalcularMontoUseCase del profe y el CalculoGasolina del Ejercicio 10).

/** Formato de México para fechas y números. Fijar el Locale evita sorpresas en el Preview y en las pruebas. */
private val LOCALE_MX: Locale = Locale.forLanguageTag("es-MX")

/** Temperatura típica de una cocina; es el valor inicial del simulador. */
const val TEMPERATURA_AMBIENTE_REFERENCIA = 24.0

/** Objetivo de cocción por defecto (°C): temperatura interna segura para pollo. */
const val OBJETIVO_POR_DEFECTO = 74.0

/** Paso con el que suben/bajan los botones − / + del objetivo. */
const val PASO_OBJETIVO = 1.0

/** Rango permitido para el objetivo (refrigerador … aceite para freír). */
const val OBJETIVO_MINIMO = 0.0
const val OBJETIVO_MAXIMO = 120.0

/** Histéresis del control automático del LED (°C), para que no parpadee alrededor del objetivo. */
const val HISTERESIS_LED = 0.5

/**
 * Separa las tramas completas del texto que va llegando por el puerto serie Bluetooth.
 *
 * El sketch del profe (EBESP32_LED_Bt.ino) responde a 't' con la temperatura seguida de
 * una "C", por ejemplo "24.36C", pero el socket puede entregarla en pedazos ("24." y
 * luego "36C") o varias juntas ("24.36C24.41C"). La letra C (o un salto de línea) marca
 * el final de cada trama; lo que quede sin terminador se regresa como "pendiente" para
 * pegarlo con el siguiente pedazo.
 *
 * @return par (tramas completas, texto pendiente)
 */
fun separarTramas(acumulado: String): Pair<List<String>, String> {
    val completas = mutableListOf<String>()
    val pendiente = StringBuilder()
    for (caracter in acumulado) {
        if (caracter == 'C' || caracter == 'c' || caracter == '\n' || caracter == '\r') {
            if (pendiente.isNotBlank()) completas.add(pendiente.toString().trim())
            pendiente.clear()
        } else {
            pendiente.append(caracter)
        }
    }
    return Pair(completas, pendiente.toString())
}

/**
 * "24.36" → 24.36 · "24,36 C" → 24.36 · " -3.5" → -3.5 · "hola" → null
 * Se quitan los caracteres que no sean dígitos, punto o signo (por si el ESP32 manda espacios).
 */
fun extraerTemperatura(trama: String): Double? {
    val limpio = trama
        .replace(',', '.')
        .filter { it.isDigit() || it == '.' || it == '-' }
    return limpio.toDoubleOrNull()
}

/** 25 °C → 77 °F */
fun celsiusAFahrenheit(celsius: Double): Double = celsius * 9.0 / 5.0 + 32.0

/** 25 °C → 298.15 K */
fun celsiusAKelvin(celsius: Double): Double = celsius + 273.15

/** Qué tan cerca está la lectura del objetivo de cocción; sirve para elegir color y texto en la IU. */
enum class NivelCoccion { LEJOS, CALENTANDO, CASI, ALCANZADO }

/**
 * Clasifica una temperatura respecto al objetivo:
 *  - ALCANZADO  → ya llegó (o pasó) el objetivo
 *  - CASI       → faltan 3 °C o menos
 *  - CALENTANDO → faltan entre 3 y 15 °C
 *  - LEJOS      → faltan más de 15 °C
 */
fun clasificarCoccion(celsius: Double, objetivo: Double): NivelCoccion {
    val faltan = objetivo - celsius
    return when {
        faltan <= 0.0 -> NivelCoccion.ALCANZADO
        faltan <= 3.0 -> NivelCoccion.CASI
        faltan <= 15.0 -> NivelCoccion.CALENTANDO
        else -> NivelCoccion.LEJOS
    }
}

/**
 * Control automático del LED (ON/OFF) con histéresis, como un termostato sencillo:
 *  - al llegar al objetivo el LED se enciende (aviso de "¡listo!");
 *  - solo se apaga cuando la temperatura baja al objetivo menos la histéresis;
 *  - en la franja intermedia se conserva el estado actual (así no parpadea).
 */
fun ledDeberiaEncender(
    temperatura: Double,
    objetivo: Double,
    ledActual: Boolean,
    histeresis: Double = HISTERESIS_LED
): Boolean = when {
    temperatura >= objetivo -> true
    temperatura <= objetivo - histeresis -> false
    else -> ledActual
}

/** Grados que faltan para el objetivo (negativo si ya se pasó). */
fun gradosFaltantes(temperatura: Double, objetivo: Double): Double = objetivo - temperatura

// ───────────────────────── Formato para mostrar en pantalla ─────────────────────────

/** 24.36 → "24.4 °C" */
fun formatearTemperatura(celsius: Double): String = String.format(LOCALE_MX, "%.1f °C", celsius)

/** 24.36 → "75.8 °F" */
fun formatearFahrenheit(celsius: Double): String =
    String.format(LOCALE_MX, "%.1f °F", celsiusAFahrenheit(celsius))

/** +9.1 → "+9.1 °C" · −2.0 → "-2.0 °C" (siempre con signo) */
fun formatearDiferencia(diferenciaC: Double): String = String.format(LOCALE_MX, "%+.1f °C", diferenciaC)

/** Milisegundos desde 1970 → "02/11/2026 21:05" */
fun formatearFecha(milisegundos: Long): String =
    SimpleDateFormat("dd/MM/yyyy HH:mm", LOCALE_MX).format(Date(milisegundos))

/** Milisegundos desde 1970 → "21:05:33" (para la última lectura y la bitácora) */
fun formatearHora(milisegundos: Long): String =
    SimpleDateFormat("HH:mm:ss", LOCALE_MX).format(Date(milisegundos))

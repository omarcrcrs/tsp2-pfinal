package com.example.pfinalv1

import com.example.pfinalv1.domain.NivelCoccion
import com.example.pfinalv1.domain.celsiusAFahrenheit
import com.example.pfinalv1.domain.celsiusAKelvin
import com.example.pfinalv1.domain.clasificarCoccion
import com.example.pfinalv1.domain.extraerTemperatura
import com.example.pfinalv1.domain.formatearDiferencia
import com.example.pfinalv1.domain.formatearTemperatura
import com.example.pfinalv1.domain.gradosFaltantes
import com.example.pfinalv1.domain.ledDeberiaEncender
import com.example.pfinalv1.domain.separarTramas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pruebas locales (corren en la computadora, sin emulador) de la capa domain:
 * el protocolo del ESP32 y la lógica del termómetro de cocina no dependen de Android.
 */
class ConversionTemperaturaTest {

    private val delta = 0.0001

    // ── Tramas del puerto serie Bluetooth ──

    @Test
    fun separarTramas_tramaCompleta() {
        val (tramas, pendiente) = separarTramas("24.36C")

        assertEquals(listOf("24.36"), tramas)
        assertEquals("", pendiente)
    }

    @Test
    fun separarTramas_tramaPartidaEnDosPedazos() {
        // El socket entrega "24." y después "36C25.1": la primera llega completa, la segunda queda pendiente
        val (primeras, pendiente1) = separarTramas("24.")
        assertTrue(primeras.isEmpty())
        assertEquals("24.", pendiente1)

        val (segundas, pendiente2) = separarTramas(pendiente1 + "36C25.1")
        assertEquals(listOf("24.36"), segundas)
        assertEquals("25.1", pendiente2)
    }

    @Test
    fun separarTramas_variasTramasJuntas() {
        val (tramas, pendiente) = separarTramas("24.36C24.41C\n")

        assertEquals(listOf("24.36", "24.41"), tramas)
        assertEquals("", pendiente)
    }

    @Test
    fun extraerTemperatura_aceptaPuntoComaYEspacios() {
        assertEquals(24.36, extraerTemperatura("24.36")!!, delta)
        assertEquals(24.36, extraerTemperatura(" 24,36 ")!!, delta)
        assertEquals(-3.5, extraerTemperatura("-3.5")!!, delta)
        assertNull(extraerTemperatura("hola"))
    }

    // ── Conversiones ──

    @Test
    fun celsiusAFahrenheit_25gradosSon77() {
        assertEquals(77.0, celsiusAFahrenheit(25.0), delta)
    }

    @Test
    fun celsiusAKelvin_0gradosSon273_15() {
        assertEquals(273.15, celsiusAKelvin(0.0), delta)
    }

    @Test
    fun gradosFaltantes_positivoSiFaltaYNegativoSiSePaso() {
        assertEquals(5.6, gradosFaltantes(temperatura = 68.4, objetivo = 74.0), delta)
        assertEquals(-1.3, gradosFaltantes(temperatura = 75.3, objetivo = 74.0), delta)
    }

    // ── Clasificación respecto al objetivo y termostato ──

    @Test
    fun clasificarCoccion_nivelesRespectoAlObjetivo() {
        assertEquals(NivelCoccion.LEJOS, clasificarCoccion(celsius = 40.0, objetivo = 74.0))        // faltan 34
        assertEquals(NivelCoccion.CALENTANDO, clasificarCoccion(celsius = 65.0, objetivo = 74.0))   // faltan 9
        assertEquals(NivelCoccion.CASI, clasificarCoccion(celsius = 72.0, objetivo = 74.0))         // faltan 2
        assertEquals(NivelCoccion.ALCANZADO, clasificarCoccion(celsius = 74.0, objetivo = 74.0))    // llegó
        assertEquals(NivelCoccion.ALCANZADO, clasificarCoccion(celsius = 80.0, objetivo = 74.0))    // se pasó
        // Funciona igual con un objetivo bajo (refrigerador a 5 °C)
        assertEquals(NivelCoccion.ALCANZADO, clasificarCoccion(celsius = 6.0, objetivo = 5.0))
    }

    @Test
    fun ledDeberiaEncender_conHisteresisNoParpadea() {
        // Llega al objetivo → enciende
        assertTrue(ledDeberiaEncender(temperatura = 74.0, objetivo = 74.0, ledActual = false))
        // Baja un poco pero sigue dentro de la franja de histéresis → se queda encendido
        assertTrue(ledDeberiaEncender(temperatura = 73.8, objetivo = 74.0, ledActual = true))
        // Baja más que la histéresis (0.5 °C) → apaga
        assertFalse(ledDeberiaEncender(temperatura = 73.4, objetivo = 74.0, ledActual = true))
        // En la franja intermedia, si estaba apagado sigue apagado
        assertFalse(ledDeberiaEncender(temperatura = 73.8, objetivo = 74.0, ledActual = false))
    }

    // ── Formato ──

    @Test
    fun formatearTemperatura_unDecimalConUnidad() {
        assertEquals("24.4 °C", formatearTemperatura(24.36))
    }

    @Test
    fun formatearDiferencia_siempreConSigno() {
        assertEquals("+9.1 °C", formatearDiferencia(9.06))
        assertEquals("-2.0 °C", formatearDiferencia(-2.0))
    }
}

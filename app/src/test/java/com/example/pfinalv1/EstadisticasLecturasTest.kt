package com.example.pfinalv1

import com.example.pfinalv1.data.SimuladorEsp32
import com.example.pfinalv1.data.lecturasDeMuestra
import com.example.pfinalv1.domain.calcularEstadisticas
import com.example.pfinalv1.domain.contarConReceta
import com.example.pfinalv1.domain.contarObjetivosAlcanzados
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Pruebas locales del resumen del historial y del simulador. */
class EstadisticasLecturasTest {

    private val delta = 0.0001

    @Test
    fun calcularEstadisticas_listaVaciaDevuelveNull() {
        assertNull(calcularEstadisticas(emptyList()))
    }

    @Test
    fun calcularEstadisticas_promedioMinimaMaximaYUltima() {
        // lecturasDeMuestra: 75.3, 68.1, 43.4, 91.6, 4.2 (de la más reciente a la más antigua)
        val estadisticas = calcularEstadisticas(lecturasDeMuestra)

        assertNotNull(estadisticas)
        assertEquals(5, estadisticas!!.cantidad)
        assertEquals(56.52, estadisticas.promedio, delta)
        assertEquals(4.2, estadisticas.minima, delta)
        assertEquals(91.6, estadisticas.maxima, delta)
        assertEquals(75.3, estadisticas.ultima, delta)
    }

    @Test
    fun contarObjetivosAlcanzados_comparaCadaLecturaConSuPropioObjetivo() {
        // 75.3 ≥ 74 ✓ · 68.1 < 74 ✗ · 43.4 ≥ 43 ✓ · 91.6 < 92 ✗ · 4.2 < 5 ✗
        assertEquals(2, contarObjetivosAlcanzados(lecturasDeMuestra))
        assertEquals(0, contarObjetivosAlcanzados(emptyList()))
    }

    @Test
    fun contarConReceta_soloLasQueSeTomaronCocinandoUnaReceta() {
        assertEquals(3, contarConReceta(lecturasDeMuestra))
    }

    @Test
    fun simulador_lecturasDentroDelRangoYConFormatoDelSketch() {
        val simulador = SimuladorEsp32(semilla = 42L)

        repeat(200) {
            val lectura = simulador.siguienteLectura()
            assertTrue("Lectura fuera de rango: $lectura", lectura in SimuladorEsp32.MINIMA..SimuladorEsp32.MAXIMA)
        }
        // Misma cadena que manda el ESP32 real: dos decimales y la letra C
        assertTrue(simulador.siguienteTrama().matches(Regex("""\d+\.\d{2}C""")))
    }

    @Test
    fun simulador_calentarSubeLaTemperatura() {
        val simulador = SimuladorEsp32(semilla = 7L)
        val antes = simulador.siguienteLectura()
        simulador.calentar() // +10 °C
        val despues = simulador.siguienteLectura()

        // Subió 10 °C y la caminata aleatoria solo mueve ±0.6 °C
        assertTrue("$antes → $despues", despues > antes + 8.0)
    }
}

package com.example.pfinalv1

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.pfinalv1.domain.formatearTemperatura
import com.example.pfinalv1.ui.theme.Pfinalv1Theme
import com.example.pfinalv1.ui.views.AccionesCocina
import com.example.pfinalv1.ui.views.CocinaContent
import com.example.pfinalv1.viewmodel.EstadoCocina
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Prueba de instrumentación (corre en el teléfono o emulador) de la pantalla Cocina.
 * Se dibuja CocinaContent con un estado fijo, sin ViewModel ni Bluetooth, y se verifica
 * lo que ve el usuario: la lectura, el nivel respecto al objetivo, el aviso de "listo" y
 * que los botones disparen su evento.
 */
@RunWith(AndroidJUnit4::class)
class CocinaViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val contexto = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun muestraLaLecturaYElNivelCalentando() {
        val estado = EstadoCocina(
            modoSimulacion = true,
            temperaturaC = 68.36,
            objetivoC = 74.0,
            nombreObjetivo = "Pollo y aves",
            horaUltimaLectura = System.currentTimeMillis()
        )

        composeTestRule.setContent {
            Pfinalv1Theme {
                CocinaContent(estado = estado, acciones = AccionesCocina())
            }
        }

        // 68.36 se muestra redondeado a un decimal: "68.4 °C"; faltan 5.6 °C → nivel "Calentando".
        // performScrollTo: la pantalla es desplazable, así la prueba no depende del tamaño del teléfono.
        composeTestRule.onNodeWithText(formatearTemperatura(68.36)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(contexto.getString(R.string.nivel_calentando)).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(contexto.getString(R.string.etiqueta_simulacion)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun avisoDeListoAparece_cuandoLaLecturaLlegaAlObjetivo() {
        val estado = EstadoCocina(modoSimulacion = true, temperaturaC = 75.0, objetivoC = 74.0)

        composeTestRule.setContent {
            Pfinalv1Theme {
                CocinaContent(estado = estado, acciones = AccionesCocina())
            }
        }

        composeTestRule.onNodeWithText(contexto.getString(R.string.objetivo_alcanzado))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule.onNodeWithText(contexto.getString(R.string.nivel_alcanzado)).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun botonLeerTemperatura_disparaElEvento() {
        var vecesLeido = 0

        composeTestRule.setContent {
            Pfinalv1Theme {
                CocinaContent(
                    estado = EstadoCocina(modoSimulacion = true),
                    acciones = AccionesCocina(onLeerTemperatura = { vecesLeido++ })
                )
            }
        }

        composeTestRule.onNodeWithText(contexto.getString(R.string.leer_temperatura))
            .performScrollTo()
            .performClick()

        assertEquals(1, vecesLeido)
    }

    @Test
    fun botonLeerTemperatura_estaDeshabilitadoSinConexionNiSimulacion() {
        var vecesLeido = 0

        composeTestRule.setContent {
            Pfinalv1Theme {
                CocinaContent(
                    estado = EstadoCocina(modoSimulacion = false),
                    acciones = AccionesCocina(onLeerTemperatura = { vecesLeido++ })
                )
            }
        }

        // El botón existe pero está deshabilitado: el clic no debe llegar al evento
        composeTestRule.onNodeWithText(contexto.getString(R.string.leer_temperatura))
            .performScrollTo()
            .performClick()

        assertEquals(0, vecesLeido)
    }
}

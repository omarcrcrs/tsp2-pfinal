package com.example.pfinalv1.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pfinalv1.R
import com.example.pfinalv1.data.lecturasDeMuestra
import com.example.pfinalv1.domain.calcularEstadisticas
import com.example.pfinalv1.domain.contarConReceta
import com.example.pfinalv1.domain.contarObjetivosAlcanzados
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.ui.components.EstadoVacio
import com.example.pfinalv1.ui.components.IndicadorCargando
import com.example.pfinalv1.ui.components.TarjetaLectura
import com.example.pfinalv1.ui.components.TarjetaResumen
import com.example.pfinalv1.ui.theme.Pfinalv1Theme
import com.example.pfinalv1.viewmodel.HistorialViewModel

/** Pantalla Historial: todas las temperaturas guardadas en Room (operación "Mostrar"). */
@Composable
fun HistorialView(
    viewModel: HistorialViewModel,
    onAbrirDetalle: (Int) -> Unit,
    onMensaje: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Escucha la tabla: cada insert, update o delete llega aquí como una lista nueva
    val lecturas by viewModel.lecturas.collectAsStateWithLifecycle()
    val textoEliminada = stringResource(R.string.lectura_eliminada)
    val textoVaciado = stringResource(R.string.historial_vaciado)

    HistorialContent(
        lecturas = lecturas,
        onAbrirDetalle = onAbrirDetalle,
        onEliminar = { lectura ->
            viewModel.eliminar(lectura)
            onMensaje(textoEliminada)
        },
        onVaciar = {
            viewModel.eliminarTodas()
            onMensaje(textoVaciado)
        },
        modifier = modifier
    )
}

/** Contenido sin ViewModel para previsualizarlo. [lecturas] = null mientras Room responde. */
@Composable
fun HistorialContent(
    lecturas: List<Lectura>?,
    onAbrirDetalle: (Int) -> Unit,
    onEliminar: (Lectura) -> Unit,
    onVaciar: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirmarVaciado by rememberSaveable { mutableStateOf(false) }

    when {
        lecturas == null -> IndicadorCargando(modifier = modifier.padding(top = 48.dp))

        lecturas.isEmpty() -> EstadoVacio(
            icono = R.drawable.ic_historial,
            titulo = stringResource(R.string.sin_lecturas),
            detalle = stringResource(R.string.sin_lecturas_detalle),
            modifier = modifier
        )

        else -> LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "resumen") {
                val estadisticas = calcularEstadisticas(lecturas)
                if (estadisticas != null) {
                    TarjetaResumen(
                        estadisticas = estadisticas,
                        objetivosAlcanzados = contarObjetivosAlcanzados(lecturas),
                        conReceta = contarConReceta(lecturas)
                    )
                }
            }
            item(key = "acciones") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.toca_para_detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(onClick = { confirmarVaciado = true }) {
                        Text(
                            text = stringResource(R.string.vaciar_historial),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            items(items = lecturas, key = { lectura -> lectura.id }) { lectura ->
                TarjetaLectura(
                    lectura = lectura,
                    onClick = { onAbrirDetalle(lectura.id) },
                    onEliminar = { onEliminar(lectura) },
                    modifier = Modifier.animateItem()
                )
            }
        }
    }

    if (confirmarVaciado) {
        AlertDialog(
            onDismissRequest = { confirmarVaciado = false },
            title = { Text(text = stringResource(R.string.vaciar_historial)) },
            text = { Text(text = stringResource(R.string.vaciar_historial_confirmacion, lecturas?.size ?: 0)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarVaciado = false
                        onVaciar()
                    }
                ) {
                    Text(text = stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarVaciado = false }) {
                    Text(text = stringResource(R.string.cancelar))
                }
            }
        )
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HistorialContentPreview() {
    Pfinalv1Theme {
        HistorialContent(
            lecturas = lecturasDeMuestra,
            onAbrirDetalle = {},
            onEliminar = {},
            onVaciar = {}
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Sin registros")
@Composable
fun HistorialContentVacioPreview() {
    Pfinalv1Theme {
        HistorialContent(
            lecturas = emptyList(),
            onAbrirDetalle = {},
            onEliminar = {},
            onVaciar = {}
        )
    }
}

package com.example.pfinalv1.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.example.pfinalv1.domain.celsiusAKelvin
import com.example.pfinalv1.domain.clasificarCoccion
import com.example.pfinalv1.domain.formatearDiferencia
import com.example.pfinalv1.domain.formatearFahrenheit
import com.example.pfinalv1.domain.formatearFecha
import com.example.pfinalv1.domain.formatearTemperatura
import com.example.pfinalv1.domain.gradosFaltantes
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.ui.components.EtiquetaEstado
import com.example.pfinalv1.ui.components.EtiquetaOrigen
import com.example.pfinalv1.ui.components.FilaDato
import com.example.pfinalv1.ui.components.IndicadorCargando
import com.example.pfinalv1.ui.components.MensajeCentrado
import com.example.pfinalv1.ui.components.PuntoLed
import com.example.pfinalv1.ui.components.colorNivel
import com.example.pfinalv1.ui.components.textoNivel
import com.example.pfinalv1.ui.theme.Pfinalv1Theme
import com.example.pfinalv1.viewmodel.HistorialViewModel
import kotlinx.coroutines.flow.map
import java.util.Locale

/** Envoltura para distinguir "Room todavía no responde" (null) de "el registro no existe" (lectura = null). */
private data class ResultadoLectura(val lectura: Lectura?)

/**
 * Detalle de una temperatura guardada: consulta un registro por id (SELECT ... WHERE id = :id),
 * muestra cada columna con su tipo de dato y permite modificar la nota (UPDATE) o eliminarlo (DELETE).
 */
@Composable
fun DetalleLecturaView(
    idLectura: Int,
    viewModel: HistorialViewModel,
    onRegresar: () -> Unit,
    onMensaje: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val resultado by remember(idLectura) {
        viewModel.observarPorId(idLectura).map { lectura -> ResultadoLectura(lectura) }
    }.collectAsStateWithLifecycle(initialValue = null)

    val textoNotaGuardada = stringResource(R.string.nota_guardada)
    val textoEliminada = stringResource(R.string.lectura_eliminada)

    when (val actual = resultado) {
        null -> IndicadorCargando(modifier = modifier.padding(top = 48.dp))

        else -> {
            val lectura = actual.lectura
            if (lectura == null) {
                MensajeCentrado(
                    texto = stringResource(R.string.registro_no_existe, idLectura),
                    modifier = modifier
                )
            } else {
                DetalleLecturaContent(
                    lectura = lectura,
                    onGuardarNota = { nota ->
                        viewModel.actualizar(lectura.copy(nota = nota))
                        onMensaje(textoNotaGuardada)
                    },
                    onEliminar = {
                        viewModel.eliminar(lectura)
                        onMensaje(textoEliminada)
                        onRegresar()
                    },
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
fun DetalleLecturaContent(
    lectura: Lectura,
    onGuardarNota: (String) -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    // La nota se edita localmente; se escribe en Room hasta tocar "Guardar nota"
    var nota by rememberSaveable { mutableStateOf(lectura.nota) }
    var notaInicializada by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(lectura.id) {
        if (!notaInicializada) {
            nota = lectura.nota
            notaInicializada = true
        }
    }
    val nivel = clasificarCoccion(lectura.temperaturaC, lectura.objetivoC)

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Encabezado con la temperatura grande
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = formatearTemperatura(lectura.temperaturaC),
                    style = MaterialTheme.typography.displayMedium,
                    color = colorNivel(nivel)
                )
                Text(
                    text = if (lectura.receta.isBlank()) stringResource(R.string.lectura_sin_receta) else lectura.receta,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = formatearFecha(lectura.fecha),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    EtiquetaEstado(texto = textoNivel(nivel), color = colorNivel(nivel))
                    Spacer(modifier = Modifier.width(8.dp))
                    EtiquetaOrigen(origen = lectura.origen)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(
                        R.string.objetivo_y_diferencia,
                        formatearTemperatura(lectura.objetivoC),
                        formatearDiferencia(gradosFaltantes(lectura.temperaturaC, lectura.objetivoC))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Cada columna de la tabla con su tipo de dato (Int, Double, Long, String, Boolean)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.datos_registro),
                    style = MaterialTheme.typography.titleMedium
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                FilaDato(etiqueta = "id · Int", valor = lectura.id.toString())
                FilaDato(etiqueta = "temperatura_c · Double", valor = String.format(Locale.US, "%.2f", lectura.temperaturaC))
                FilaDato(etiqueta = "objetivo_c · Double", valor = String.format(Locale.US, "%.2f", lectura.objetivoC))
                FilaDato(etiqueta = "fecha · Long", valor = lectura.fecha.toString())
                FilaDato(
                    etiqueta = "receta · String",
                    valor = lectura.receta.ifBlank { stringResource(R.string.sin_receta) }
                )
                FilaDato(etiqueta = "origen · String", valor = lectura.origen)
                FilaDato(
                    etiqueta = "dispositivo · String",
                    valor = lectura.dispositivo.ifBlank { stringResource(R.string.sin_dispositivo) }
                )
                Row(
                    modifier = Modifier.padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "led_encendido · Boolean",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    PuntoLed(encendido = lectura.ledEncendido)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = lectura.ledEncendido.toString(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                FilaDato(etiqueta = stringResource(R.string.en_fahrenheit), valor = formatearFahrenheit(lectura.temperaturaC))
                FilaDato(
                    etiqueta = stringResource(R.string.en_kelvin),
                    valor = String.format(Locale.US, "%.2f K", celsiusAKelvin(lectura.temperaturaC))
                )
            }
        }

        // Nota editable (UPDATE)
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text(text = stringResource(R.string.nota)) },
                    placeholder = { Text(text = stringResource(R.string.nota_sugerencia)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { onGuardarNota(nota.trim()) },
                    enabled = nota.trim() != lectura.nota,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.guardar_nota))
                }
            }
        }

        OutlinedButton(
            onClick = onEliminar,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.eliminar_lectura),
                color = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DetalleLecturaContentPreview() {
    Pfinalv1Theme {
        DetalleLecturaContent(
            lectura = lecturasDeMuestra.first(),
            onGuardarNota = {},
            onEliminar = {}
        )
    }
}

@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.pfinalv1.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.pfinalv1.R
import com.example.pfinalv1.domain.formatearFecha
import com.example.pfinalv1.domain.formatearTemperatura
import com.example.pfinalv1.domain.listaIngredientes
import com.example.pfinalv1.domain.objetivoSugerido
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.model.RecetaGuardada
import com.example.pfinalv1.ui.components.Estrellas
import com.example.pfinalv1.ui.components.IndicadorCargando
import com.example.pfinalv1.ui.components.MensajeCentrado
import com.example.pfinalv1.viewmodel.ConsultaEstado
import com.example.pfinalv1.viewmodel.HistorialViewModel
import com.example.pfinalv1.viewmodel.RecetarioViewModel

/**
 * Detalle de una receta del recetario: SELECT … WHERE id = :id mostrando cada columna con su
 * tipo de dato; desde aquí se modifica (UPDATE: calificación, favorita, veces cocinada, notas),
 * se elimina (DELETE) y se manda a cocinar con el termómetro ESP32 (viene de la Práctica 3).
 */
@Composable
fun DetalleRecetaView(
    idRegistro: Int,
    viewModel: RecetarioViewModel,
    historialViewModel: HistorialViewModel,
    onCocinar: (nombreReceta: String, objetivoC: Double) -> Unit,
    onMensaje: (String) -> Unit,
    onRegresar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by remember(idRegistro) { viewModel.consultarReceta(idRegistro) }
        .collectAsStateWithLifecycle(initialValue = ConsultaEstado.Cargando)

    var confirmarEliminar by remember { mutableStateOf(false) }
    val textoEliminada = stringResource(R.string.receta_eliminada)

    when (val consulta = estado) {
        is ConsultaEstado.Cargando -> IndicadorCargando(
            modifier = modifier.padding(top = 48.dp),
            texto = stringResource(R.string.consultando_registro)
        )

        is ConsultaEstado.NoExiste -> MensajeCentrado(
            texto = stringResource(R.string.receta_no_existe, idRegistro),
            detalle = stringResource(R.string.receta_no_existe_detalle),
            modifier = modifier
        )

        is ConsultaEstado.Encontrada -> {
            // Últimas temperaturas registradas cocinando esta receta (tabla "lecturas")
            val lecturasReceta by remember(consulta.receta.nombre) {
                historialViewModel.observarPorReceta(consulta.receta.nombre)
            }.collectAsStateWithLifecycle(initialValue = emptyList())

            ContenidoReceta(
                receta = consulta.receta,
                lecturasReceta = lecturasReceta,
                viewModel = viewModel,
                onCocinar = { onCocinar(consulta.receta.nombre, objetivoSugerido(consulta.receta.categoria)) },
                onEliminar = { confirmarEliminar = true },
                avisar = onMensaje,
                modifier = modifier
            )
        }
    }

    val recetaActual = (estado as? ConsultaEstado.Encontrada)?.receta
    if (confirmarEliminar && recetaActual != null) {
        AlertDialog(
            onDismissRequest = { confirmarEliminar = false },
            title = { Text(text = stringResource(R.string.eliminar_receta_titulo)) },
            text = { Text(text = stringResource(R.string.eliminar_receta_confirmacion, recetaActual.nombre)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmarEliminar = false
                    viewModel.eliminar(recetaActual)
                    onMensaje(textoEliminada)
                    onRegresar()
                }) { Text(text = stringResource(R.string.eliminar), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmarEliminar = false }) { Text(text = stringResource(R.string.cancelar)) }
            }
        )
    }
}

@Composable
private fun ContenidoReceta(
    receta: RecetaGuardada,
    lecturasReceta: List<Lectura>,
    viewModel: RecetarioViewModel,
    onCocinar: () -> Unit,
    onEliminar: () -> Unit,
    avisar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // Borrador de las notas; se escribe en la BD solo al tocar «Guardar notas»
    var notas by remember(receta.id) { mutableStateOf(receta.notas) }
    val objetivo = objetivoSugerido(receta.categoria)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            AsyncImage(
                model = receta.imagenUrl,
                contentDescription = "Foto de ${receta.nombre}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
            )
        }

        item {
            Text(receta.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Row {
                AssistChip(onClick = {}, label = { Text(receta.categoria) })
                Spacer(Modifier.width(8.dp))
                AssistChip(onClick = {}, label = { Text(receta.area) })
            }
        }

        // Mecatrónica ↔ recetario: cocinar esta receta con el termómetro ESP32
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_termometro),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.cocinar_con_termometro),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.objetivo_sugerido, receta.categoria, formatearTemperatura(objetivo)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onCocinar, modifier = Modifier.fillMaxWidth()) {
                        Text(text = stringResource(R.string.ir_a_cocinar))
                    }
                    if (lecturasReceta.isNotEmpty()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                        Text(
                            text = stringResource(R.string.ultimas_temperaturas),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        lecturasReceta.forEach { lectura ->
                            Text(
                                text = "${formatearFecha(lectura.fecha)} · ${formatearTemperatura(lectura.temperaturaC)}" +
                                    " (objetivo ${formatearTemperatura(lectura.objetivoC)})",
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }

        // Columnas del registro con el tipo de dato de cada una
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.datos_registro),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    FilaColumna("id", "Int", "${receta.id}")
                    FilaColumna("id_meal", "String", receta.idMeal)
                    FilaColumna("nombre", "String", receta.nombre)
                    FilaColumna("calificacion", "Float", "${receta.calificacion} / 5")
                    FilaColumna("favorita", "Boolean", "${receta.favorita}")
                    FilaColumna("veces_cocinada", "Int", "${receta.vecesCocinada}")
                    FilaColumna("fecha_guardado", "Long", "${receta.fechaGuardado}\n${formatearFecha(receta.fechaGuardado)}")
                }
            }
        }

        // Cambios a los registros (UPDATE)
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.modificar_registro),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.calificacion), modifier = Modifier.weight(1f))
                        Estrellas(
                            calificacion = receta.calificacion,
                            tamano = 28.dp,
                            onCalificar = { estrellas ->
                                viewModel.calificar(receta, estrellas)
                                avisar("Calificación actualizada a ${estrellas.toInt()} estrellas")
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(text = stringResource(R.string.favorita), modifier = Modifier.weight(1f))
                        Switch(
                            checked = receta.favorita,
                            onCheckedChange = {
                                viewModel.alternarFavorita(receta)
                                avisar(if (receta.favorita) "Quitada de favoritas" else "Marcada como favorita")
                            }
                        )
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.veces_cocinada, receta.vecesCocinada),
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = {
                            viewModel.registrarCocinada(receta.id)
                            avisar("Se registró una preparación más")
                        }) { Text(text = stringResource(R.string.mas_uno_hoy)) }
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    OutlinedTextField(
                        value = notas,
                        onValueChange = { notas = it },
                        label = { Text(text = stringResource(R.string.notas_personales)) },
                        minLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            viewModel.guardarNotas(receta, notas)
                            avisar("Notas guardadas")
                        },
                        enabled = notas.trim() != receta.notas,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(text = stringResource(R.string.guardar_notas)) }
                }
            }
        }

        item {
            Text(
                text = stringResource(R.string.ingredientes_cantidad, receta.listaIngredientes().size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        items(receta.listaIngredientes()) { ingrediente ->
            Text("• $ingrediente", style = MaterialTheme.typography.bodyMedium)
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.instrucciones),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(receta.instrucciones, style = MaterialTheme.typography.bodyMedium)
        }

        item {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onEliminar, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.eliminar_receta), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

/** Una columna del registro: nombre en la BD, tipo de dato y valor. */
@Composable
private fun FilaColumna(columna: String, tipo: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(columna, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace)
            Text(tipo, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
        }
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1.4f)
        )
    }
}

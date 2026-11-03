@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.pfinalv1.ui.views

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.pfinalv1.R
import com.example.pfinalv1.model.Ingrediente
import com.example.pfinalv1.model.Receta
import com.example.pfinalv1.ui.components.EstadoVacio
import com.example.pfinalv1.ui.components.IndicadorCargando
import com.example.pfinalv1.ui.components.MensajeCentrado
import com.example.pfinalv1.ui.components.TarjetaRecetaResumen
import com.example.pfinalv1.viewmodel.BusquedaUiState
import com.example.pfinalv1.viewmodel.BusquedaViewModel
import com.example.pfinalv1.viewmodel.RecetaResumen

/**
 * Pantalla Recetas (API TheMealDB, Práctica 2): buscar por nombre, pedir una aleatoria o explorar
 * una categoría; cada receta se puede guardar en el recetario (INSERT en Room, Práctica 3).
 */
@Composable
fun RecetasView(
    viewModel: BusquedaViewModel,
    onGuardar: (Receta) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var consulta by rememberSaveable { mutableStateOf("") }
    var categoriaSeleccionada by rememberSaveable { mutableStateOf("") }

    RecetasContent(
        uiState = uiState,
        consulta = consulta,
        categoriaSeleccionada = categoriaSeleccionada,
        hayListaPrevia = viewModel.hayListaPrevia,
        onConsultaChange = { consulta = it },
        onBuscar = {
            categoriaSeleccionada = ""
            viewModel.buscar(consulta)
        },
        onAleatoria = {
            categoriaSeleccionada = ""
            viewModel.recetaAleatoria()
        },
        onCategoria = { categoria ->
            categoriaSeleccionada = categoria
            viewModel.explorarCategoria(categoria)
        },
        onAbrirResumen = { resumen -> viewModel.abrir(resumen.idMeal) },
        onVolverALista = { viewModel.volverALista() },
        onGuardar = onGuardar,
        modifier = modifier
    )
}

@Composable
fun RecetasContent(
    uiState: BusquedaUiState,
    consulta: String,
    categoriaSeleccionada: String,
    hayListaPrevia: Boolean,
    onConsultaChange: (String) -> Unit,
    onBuscar: () -> Unit,
    onAleatoria: () -> Unit,
    onCategoria: (String) -> Unit,
    onAbrirResumen: (RecetaResumen) -> Unit,
    onVolverALista: () -> Unit,
    onGuardar: (Receta) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        // Búsqueda por nombre
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = consulta,
                onValueChange = onConsultaChange,
                label = { Text(text = stringResource(R.string.buscar_receta_etiqueta)) },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onBuscar) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text(text = stringResource(R.string.buscar))
            }
        }

        Spacer(Modifier.height(8.dp))

        // Categorías para "comer mejor" + receta aleatoria
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AssistChip(
                onClick = onAleatoria,
                label = { Text(text = stringResource(R.string.receta_aleatoria)) },
                leadingIcon = { Icon(Icons.Default.Refresh, contentDescription = null) }
            )
            BusquedaViewModel.CATEGORIAS.forEach { categoria ->
                FilterChip(
                    selected = categoria == categoriaSeleccionada,
                    onClick = { onCategoria(categoria) },
                    label = { Text(text = categoria) }
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.weight(1f)) {
            when (val estado = uiState) {
                is BusquedaUiState.Inicial -> EstadoVacio(
                    icono = R.drawable.ic_recetas,
                    titulo = stringResource(R.string.recetas_inicial),
                    detalle = stringResource(R.string.recetas_inicial_detalle)
                )

                is BusquedaUiState.Cargando -> IndicadorCargando(texto = stringResource(R.string.consultando_api))

                is BusquedaUiState.Error -> MensajeCentrado(
                    texto = estado.mensaje,
                    detalle = stringResource(R.string.recetas_error_detalle),
                    esError = true
                )

                is BusquedaUiState.ExitoLista -> CuadriculaCategoria(
                    categoria = estado.categoria,
                    recetas = estado.recetas,
                    onAbrir = onAbrirResumen
                )

                is BusquedaUiState.Exito -> RecetaEncontrada(
                    receta = estado.receta,
                    hayListaPrevia = hayListaPrevia,
                    onVolverALista = onVolverALista,
                    onGuardar = { onGuardar(estado.receta) }
                )
            }
        }
    }
}

/** Cuadrícula de recetas de una categoría (≥ 10 imágenes asíncronas con Coil, como en el Ejercicio 7). */
@Composable
private fun CuadriculaCategoria(
    categoria: String,
    recetas: List<RecetaResumen>,
    onAbrir: (RecetaResumen) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.recetas_en_categoria, recetas.size, categoria),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 150.dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(items = recetas, key = { it.idMeal }) { resumen ->
                TarjetaRecetaResumen(resumen = resumen, onClick = { onAbrir(resumen) })
            }
        }
    }
}

@Composable
private fun RecetaEncontrada(
    receta: Receta,
    hayListaPrevia: Boolean,
    onVolverALista: () -> Unit,
    onGuardar: () -> Unit
) {
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        if (hayListaPrevia) {
            item {
                TextButton(onClick = onVolverALista) {
                    Text(text = stringResource(R.string.volver_a_la_lista))
                }
            }
        }

        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                AsyncImage(
                    model = receta.imagenUrl,
                    contentDescription = "Foto de ${receta.nombre}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                )
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(receta.nombre, style = MaterialTheme.typography.titleLarge)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        if (receta.categoria.isNotBlank()) {
                            AssistChip(onClick = {}, label = { Text(receta.categoria) })
                            Spacer(Modifier.width(8.dp))
                        }
                        if (receta.area.isNotBlank()) {
                            AssistChip(onClick = {}, label = { Text(receta.area) })
                        }
                    }
                }
            }
        }

        item {
            Spacer(Modifier.height(12.dp))
            // La receta de la API se convierte en un registro de Room
            Button(onClick = onGuardar, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(text = stringResource(R.string.guardar_en_recetario))
            }
        }

        if (receta.ingredientes.isNotEmpty()) {
            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.ingredientes_cantidad, receta.ingredientes.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
            }
            items(receta.ingredientes) { ingrediente ->
                FilaIngrediente(ingrediente)
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.instrucciones),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text(receta.instrucciones, style = MaterialTheme.typography.bodyMedium)
        }

        receta.youtubeUrl?.let { url ->
            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: ActivityNotFoundException) {
                            // Sin navegador ni app de YouTube (p. ej. un emulador mínimo): no hay nada que abrir
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = stringResource(R.string.ver_video))
                }
            }
        }
    }
}

@Composable
private fun FilaIngrediente(ingrediente: Ingrediente) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(ingrediente.nombre, modifier = Modifier.weight(1f))
            Text(ingrediente.medida, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

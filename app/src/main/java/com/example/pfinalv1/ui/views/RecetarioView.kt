package com.example.pfinalv1.ui.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pfinalv1.R
import com.example.pfinalv1.data.recetasDeMuestra
import com.example.pfinalv1.model.RecetaGuardada
import com.example.pfinalv1.ui.components.EstadoVacio
import com.example.pfinalv1.ui.components.IndicadorCargando
import com.example.pfinalv1.ui.components.TarjetaReceta
import com.example.pfinalv1.ui.theme.Pfinalv1Theme
import com.example.pfinalv1.viewmodel.RecetarioViewModel

/**
 * Pantalla Recetario (pantalla de inicio): la tabla "recetas_guardadas" (SELECT), consulta de un
 * registro por id y acceso a la API para agregar más recetas (INSERT). Viene de la Práctica 3.
 */
@Composable
fun RecetarioView(
    viewModel: RecetarioViewModel,
    onBuscar: () -> Unit,
    onAbrir: (id: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val recetas by viewModel.recetas.collectAsStateWithLifecycle()

    RecetarioContent(
        recetas = recetas,
        onBuscar = onBuscar,
        onAbrir = onAbrir,
        modifier = modifier
    )
}

/** Contenido sin ViewModel para previsualizarlo. [recetas] = null mientras Room responde. */
@Composable
fun RecetarioContent(
    recetas: List<RecetaGuardada>?,
    onBuscar: () -> Unit,
    onAbrir: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var idConsulta by rememberSaveable { mutableStateOf("") }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            ConsultaPorId(
                idTexto = idConsulta,
                onIdChange = { idConsulta = it },
                onConsultar = { idConsulta.toIntOrNull()?.let(onAbrir) }
            )

            when {
                recetas == null -> IndicadorCargando(texto = stringResource(R.string.leyendo_bd))

                recetas.isEmpty() -> EstadoVacio(
                    icono = R.drawable.ic_recetario,
                    titulo = stringResource(R.string.recetario_vacio),
                    detalle = stringResource(R.string.recetario_vacio_detalle)
                )

                else -> ListaRecetas(recetas = recetas, onAbrir = onAbrir)
            }
        }

        ExtendedFloatingActionButton(
            onClick = onBuscar,
            icon = { Icon(Icons.Default.Search, contentDescription = null) },
            text = { Text(text = stringResource(R.string.buscar_recetas)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}

/** El usuario escribe el id (llave primaria) y la app consulta ese registro (SELECT … WHERE id). */
@Composable
private fun ConsultaPorId(idTexto: String, onIdChange: (String) -> Unit, onConsultar: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = idTexto,
                onValueChange = { nuevo -> if (nuevo.all { it.isDigit() }) onIdChange(nuevo) },
                label = { Text(text = stringResource(R.string.consultar_por_id)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = onConsultar, enabled = idTexto.toIntOrNull() != null) {
                Text(text = stringResource(R.string.consultar))
            }
        }
    }
}

@Composable
private fun ListaRecetas(recetas: List<RecetaGuardada>, onAbrir: (Int) -> Unit) {
    val favoritas = recetas.count { it.favorita }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        // Espacio extra abajo para que el botón flotante no tape la última tarjeta
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.resumen_recetario, recetas.size, favoritas),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(recetas, key = { it.id }) { receta ->
            TarjetaReceta(receta = receta, onClick = { onAbrir(receta.id) })
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RecetarioContentPreview() {
    Pfinalv1Theme {
        RecetarioContent(
            recetas = recetasDeMuestra.mapIndexed { indice, receta -> receta.copy(id = indice + 1) },
            onBuscar = {},
            onAbrir = {}
        )
    }
}

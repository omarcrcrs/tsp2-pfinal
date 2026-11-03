package com.example.pfinalv1.ui.views

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pfinalv1.R
import com.example.pfinalv1.model.DispositivoBluetooth
import com.example.pfinalv1.repositorio.EstadoAdaptador
import com.example.pfinalv1.ui.components.EstadoVacio
import com.example.pfinalv1.ui.components.IndicadorCargando
import com.example.pfinalv1.ui.components.TarjetaDispositivo
import com.example.pfinalv1.ui.components.TarjetaError
import com.example.pfinalv1.ui.components.permisosBluetoothRequeridos
import com.example.pfinalv1.ui.components.textoEstadoAdaptador
import com.example.pfinalv1.ui.components.tienePermisosBluetooth
import com.example.pfinalv1.ui.theme.Pfinalv1Theme
import com.example.pfinalv1.viewmodel.DispositivosViewModel

/**
 * Pantalla Dispositivos: pide los permisos de Bluetooth, lista los emparejados y
 * deja elegir el ESP32. Conectar es tarea del BluetoothViewModel (lo hace el NavManager).
 */
@Composable
fun DispositivosView(
    viewModel: DispositivosViewModel,
    onConectar: (DispositivoBluetooth) -> Unit,
    onMensaje: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dispositivos by viewModel.dispositivos.collectAsStateWithLifecycle()
    val seleccionado by viewModel.seleccionado.collectAsStateWithLifecycle()
    val cargando by viewModel.cargando.collectAsStateWithLifecycle()
    val estadoAdaptador by viewModel.estadoAdaptador.collectAsStateWithLifecycle()

    // ── Gestión de permisos (Android 12+ los pide en tiempo de ejecución) ──
    var tienePermisos by remember { mutableStateOf(tienePermisosBluetooth(context)) }
    val textoPermisosNegados = stringResource(R.string.permisos_negados)

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val todosOtorgados = resultados.values.all { otorgado -> otorgado }
        tienePermisos = todosOtorgados
        if (todosOtorgados) viewModel.refrescar() else onMensaje(textoPermisosNegados)
    }

    LaunchedEffect(Unit) {
        if (tienePermisos) viewModel.refrescar() else lanzadorPermisos.launch(permisosBluetoothRequeridos())
    }

    DispositivosContent(
        dispositivos = dispositivos,
        seleccionado = seleccionado,
        cargando = cargando,
        estadoAdaptador = estadoAdaptador,
        tienePermisos = tienePermisos,
        onPedirPermisos = { lanzadorPermisos.launch(permisosBluetoothRequeridos()) },
        onActualizar = { viewModel.refrescar() },
        onSeleccionar = { dispositivo -> viewModel.seleccionar(dispositivo) },
        onConectar = onConectar,
        modifier = modifier
    )
}

@Composable
fun DispositivosContent(
    dispositivos: List<DispositivoBluetooth>,
    seleccionado: DispositivoBluetooth?,
    cargando: Boolean,
    estadoAdaptador: EstadoAdaptador,
    tienePermisos: Boolean,
    onPedirPermisos: () -> Unit,
    onActualizar: () -> Unit,
    onSeleccionar: (DispositivoBluetooth) -> Unit,
    onConectar: (DispositivoBluetooth) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "estado") {
                Text(
                    text = textoEstadoAdaptador(estadoAdaptador),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (estadoAdaptador == EstadoAdaptador.ENCENDIDO) MaterialTheme.colorScheme.onBackground
                    else MaterialTheme.colorScheme.error
                )
            }

            when {
                !tienePermisos -> item(key = "permisos") {
                    TarjetaError(
                        titulo = stringResource(R.string.permisos_titulo),
                        detalle = stringResource(R.string.permisos_detalle),
                        textoBoton = stringResource(R.string.conceder_permisos),
                        onReintentar = onPedirPermisos
                    )
                }

                estadoAdaptador != EstadoAdaptador.ENCENDIDO -> item(key = "bt_apagado") {
                    TarjetaError(
                        titulo = stringResource(R.string.bt_no_disponible_titulo),
                        detalle = stringResource(R.string.bt_no_disponible_detalle),
                        textoBoton = stringResource(R.string.actualizar_lista),
                        onReintentar = onActualizar
                    )
                }

                cargando -> item(key = "cargando") {
                    IndicadorCargando(texto = stringResource(R.string.buscando_emparejados))
                }

                dispositivos.isEmpty() -> item(key = "vacio") {
                    EstadoVacio(
                        icono = R.drawable.ic_bluetooth,
                        titulo = stringResource(R.string.sin_dispositivos),
                        detalle = stringResource(R.string.sin_dispositivos_detalle)
                    )
                }

                else -> {
                    item(key = "titulo_lista") {
                        Text(
                            text = stringResource(R.string.dispositivos_emparejados, dispositivos.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(items = dispositivos, key = { it.direccion }) { dispositivo ->
                        TarjetaDispositivo(
                            dispositivo = dispositivo,
                            seleccionado = seleccionado?.direccion == dispositivo.direccion,
                            onClick = { onSeleccionar(dispositivo) }
                        )
                    }
                }
            }

            item(key = "ayuda") {
                TarjetaAyudaEsp32()
            }
        }

        // Botones fijos abajo
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { if (tienePermisos) onActualizar() else onPedirPermisos() },
                enabled = !cargando,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.actualizar_lista))
            }
            Button(
                onClick = { seleccionado?.let(onConectar) },
                enabled = seleccionado != null && tienePermisos,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.conectar))
            }
        }
    }
}

/** Recordatorio del kit y del protocolo del sketch (sirve en la expo y para quien no conoce el proyecto). */
@Composable
private fun TarjetaAyudaEsp32() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.ayuda_titulo),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.ayuda_pasos),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                Text(
                    text = stringResource(R.string.ayuda_protocolo_etiqueta),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.ayuda_protocolo),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

// ───────────────────────── Preview ─────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DispositivosContentPreview() {
    val ejemplo = listOf(
        DispositivoBluetooth("ESP32_BT_Serial", "A4:CF:12:9B:3E:7A", "Clásico", esEsp32 = true),
        DispositivoBluetooth("Audífonos", "10:94:BB:01:22:33", "Dual", esEsp32 = false)
    )
    Pfinalv1Theme {
        DispositivosContent(
            dispositivos = ejemplo,
            seleccionado = ejemplo.first(),
            cargando = false,
            estadoAdaptador = EstadoAdaptador.ENCENDIDO,
            tienePermisos = true,
            onPedirPermisos = {},
            onActualizar = {},
            onSeleccionar = {},
            onConectar = {}
        )
    }
}

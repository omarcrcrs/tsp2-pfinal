@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.pfinalv1.ui.views

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.pfinalv1.R
import com.example.pfinalv1.data.ObjetivoCoccion
import com.example.pfinalv1.data.ObjetivosCoccion
import com.example.pfinalv1.domain.formatearTemperatura
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.repositorio.EstadoAdaptador
import com.example.pfinalv1.repositorio.EstadoConexion
import com.example.pfinalv1.ui.components.EtiquetaEstado
import com.example.pfinalv1.ui.components.PuntoLed
import com.example.pfinalv1.ui.components.TarjetaBitacora
import com.example.pfinalv1.ui.components.TarjetaTemperatura
import com.example.pfinalv1.ui.components.colorEstadoConexion
import com.example.pfinalv1.ui.components.textoEstadoAdaptador
import com.example.pfinalv1.ui.components.textoEstadoConexion
import com.example.pfinalv1.ui.theme.ColorLedEncendido
import com.example.pfinalv1.ui.theme.Pfinalv1Theme
import com.example.pfinalv1.viewmodel.BluetoothViewModel
import com.example.pfinalv1.viewmodel.EstadoCocina

/**
 * Eventos que la pantalla Cocina puede disparar. Van juntos en una clase para que
 * CocinaContent no tenga 14 lambdas sueltas; en Previews y pruebas se usan los valores
 * por defecto (no hacen nada).
 */
data class AccionesCocina(
    val onLeerTemperatura: () -> Unit = {},
    val onAlternarMonitoreo: () -> Unit = {},
    val onCambiarLed: (Boolean) -> Unit = {},
    val onCambiarControlAutomatico: (Boolean) -> Unit = {},
    val onFijarObjetivo: (ObjetivoCoccion) -> Unit = {},
    val onSubirObjetivo: () -> Unit = {},
    val onBajarObjetivo: () -> Unit = {},
    val onTerminarCoccion: () -> Unit = {},
    val onCambiarSimulacion: (Boolean) -> Unit = {},
    val onSimularCalor: () -> Unit = {},
    val onSimularFrio: () -> Unit = {},
    val onGuardarLectura: () -> Unit = {},
    val onDesconectar: () -> Unit = {},
    val onIrADispositivos: () -> Unit = {}
)

/** Pantalla Cocina (termómetro ESP32): conecta el estado del BluetoothViewModel con el contenido. */
@Composable
fun CocinaView(
    viewModel: BluetoothViewModel,
    onGuardarLectura: (Lectura) -> Unit,
    onIrADispositivos: () -> Unit,
    onMensaje: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val estado by viewModel.estado.collectAsStateWithLifecycle()

    // (Los avisos de una sola vez del ViewModel —viewModel.mensajes— los escucha el NavManager,
    //  que siempre está en pantalla, y los muestra en el Snackbar.)

    // Al entrar a la pantalla se vuelve a revisar si el Bluetooth del teléfono está encendido
    LaunchedEffect(Unit) {
        viewModel.actualizarEstadoAdaptador()
    }

    val textoGuardada = stringResource(R.string.lectura_guardada)
    val textoSinLectura = stringResource(R.string.primero_toma_lectura)

    CocinaContent(
        estado = estado,
        acciones = AccionesCocina(
            onLeerTemperatura = viewModel::solicitarTemperatura,
            onAlternarMonitoreo = viewModel::alternarMonitoreo,
            onCambiarLed = viewModel::cambiarLed,
            onCambiarControlAutomatico = viewModel::cambiarControlAutomatico,
            onFijarObjetivo = { objetivo -> viewModel.fijarObjetivo(objetivo.gradosC, objetivo.nombre) },
            onSubirObjetivo = viewModel::subirObjetivo,
            onBajarObjetivo = viewModel::bajarObjetivo,
            onTerminarCoccion = viewModel::terminarCoccion,
            onCambiarSimulacion = viewModel::cambiarModoSimulacion,
            onSimularCalor = viewModel::simularCalor,
            onSimularFrio = viewModel::simularFrio,
            onGuardarLectura = {
                val lectura = viewModel.lecturaActual()
                if (lectura != null) {
                    onGuardarLectura(lectura)
                    onMensaje(textoGuardada)
                } else {
                    onMensaje(textoSinLectura)
                }
            },
            onDesconectar = viewModel::desconectar,
            onIrADispositivos = onIrADispositivos
        ),
        modifier = modifier
    )
}

/** Contenido sin ViewModel (estado y eventos como parámetros): se puede previsualizar y probar. */
@Composable
fun CocinaContent(
    estado: EstadoCocina,
    acciones: AccionesCocina,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        TarjetaConexion(estado = estado, acciones = acciones)

        // Receta del recetario que se está cocinando (se fija desde su detalle)
        AnimatedVisibility(visible = estado.recetaEnCurso.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_recetario),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.cocinando),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = estado.recetaEnCurso,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    TextButton(onClick = acciones.onTerminarCoccion) {
                        Text(text = stringResource(R.string.terminar_coccion))
                    }
                }
            }
        }

        TarjetaTemperatura(
            temperaturaC = estado.temperaturaC,
            horaUltimaLectura = estado.horaUltimaLectura,
            objetivoC = estado.objetivoC,
            nombreObjetivo = estado.nombreObjetivo,
            modoSimulacion = estado.modoSimulacion
        )

        // Aviso animado: aparece solo cuando la lectura llega al objetivo
        AnimatedVisibility(visible = estado.objetivoAlcanzado) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_alerta),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.objetivo_alcanzado),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            text = stringResource(R.string.objetivo_alcanzado_detalle, formatearTemperatura(estado.objetivoC)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }

        // Botones de lectura
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = acciones.onLeerTemperatura,
                enabled = estado.puedeLeer && !estado.monitoreando,
                modifier = Modifier.weight(1f)
            ) {
                Text(text = stringResource(R.string.leer_temperatura))
            }
            Button(
                onClick = acciones.onAlternarMonitoreo,
                enabled = estado.puedeLeer,
                modifier = Modifier.weight(1f),
                colors = if (estado.monitoreando) {
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                } else {
                    ButtonDefaults.buttonColors()
                }
            ) {
                Text(
                    text = if (estado.monitoreando) stringResource(R.string.detener_monitoreo)
                    else stringResource(R.string.iniciar_monitoreo)
                )
            }
        }

        TarjetaObjetivo(estado = estado, acciones = acciones)

        TarjetaLed(estado = estado, acciones = acciones)

        TarjetaSimulacion(estado = estado, acciones = acciones)

        Button(
            onClick = acciones.onGuardarLectura,
            enabled = estado.temperaturaC != null,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_historial),
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = stringResource(R.string.guardar_lectura))
        }

        TarjetaBitacora(lineas = estado.bitacora)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

/** Estado del Bluetooth del teléfono y de la conexión con el ESP32. */
@Composable
private fun TarjetaConexion(estado: EstadoCocina, acciones: AccionesCocina) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_bluetooth),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.termometro_esp32),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                EtiquetaEstado(
                    texto = textoEstadoConexion(estado.conexion),
                    color = colorEstadoConexion(estado.conexion)
                )
            }

            if (estado.estadoAdaptador != EstadoAdaptador.ENCENDIDO) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = textoEstadoAdaptador(estado.estadoAdaptador),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = acciones.onIrADispositivos,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.elegir_dispositivo))
                }
                TextButton(
                    onClick = acciones.onDesconectar,
                    enabled = estado.conexion is EstadoConexion.Conectado ||
                        estado.conexion is EstadoConexion.Conectando,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = stringResource(R.string.desconectar))
                }
            }
        }
    }
}

/** Objetivo de cocción: temperaturas seguras predefinidas (chips) y ajuste fino con − / +. */
@Composable
private fun TarjetaObjetivo(estado: EstadoCocina, acciones: AccionesCocina) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.objetivo_coccion),
                    style = MaterialTheme.typography.titleMedium
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = acciones.onBajarObjetivo) { Text(text = "−") }
                    Text(
                        text = formatearTemperatura(estado.objetivoC),
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    OutlinedButton(onClick = acciones.onSubirObjetivo) { Text(text = "+") }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ObjetivosCoccion.todos.forEach { objetivo ->
                    FilterChip(
                        selected = objetivo.nombre == estado.nombreObjetivo,
                        onClick = { acciones.onFijarObjetivo(objetivo) },
                        label = { Text(text = "${objetivo.nombre} · ${objetivo.gradosC.toInt()}°") }
                    )
                }
            }
            val descripcion = ObjetivosCoccion.todos.firstOrNull { it.nombre == estado.nombreObjetivo }?.descripcion
            if (descripcion != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Control del LED (GPIO5): manual con el Switch o automático al llegar al objetivo. */
@Composable
private fun TarjetaLed(estado: EstadoCocina, acciones: AccionesCocina) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PuntoLed(encendido = estado.ledEncendido)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.led_esp32),
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = if (estado.ledEncendido) stringResource(R.string.led_encendido)
                            else stringResource(R.string.led_apagado),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Switch(
                    checked = estado.ledEncendido,
                    onCheckedChange = acciones.onCambiarLed,
                    enabled = estado.puedeLeer && !estado.controlAutomatico,
                    colors = SwitchDefaults.colors(checkedTrackColor = ColorLedEncendido)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.control_automatico),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.control_automatico_detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = estado.controlAutomatico,
                    onCheckedChange = acciones.onCambiarControlAutomatico,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

/** Modo simulación: permite probar toda la app sin la tarjeta (emulador, demos). */
@Composable
private fun TarjetaSimulacion(estado: EstadoCocina, acciones: AccionesCocina) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (estado.modoSimulacion) MaterialTheme.colorScheme.tertiaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.modo_simulacion),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = stringResource(R.string.modo_simulacion_detalle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = estado.modoSimulacion,
                    onCheckedChange = acciones.onCambiarSimulacion,
                    colors = SwitchDefaults.colors(checkedTrackColor = MaterialTheme.colorScheme.tertiary)
                )
            }
            AnimatedVisibility(visible = estado.modoSimulacion) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(onClick = acciones.onSimularFrio, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.simular_frio))
                    }
                    OutlinedButton(onClick = acciones.onSimularCalor, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(R.string.simular_calor))
                    }
                }
            }
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CocinaContentPreview() {
    Pfinalv1Theme {
        CocinaContent(
            estado = EstadoCocina(
                conexion = EstadoConexion.Conectado("ESP32_BT_Serial"),
                temperaturaC = 75.3,
                horaUltimaLectura = System.currentTimeMillis(),
                ledEncendido = true,
                monitoreando = true,
                objetivoC = 74.0,
                nombreObjetivo = "Pollo y aves",
                controlAutomatico = true,
                recetaEnCurso = "Teriyaki Chicken Casserole",
                nombreDispositivo = "ESP32_BT_Serial",
                bitacora = listOf("21:05:31  → t", "21:05:31  ← 75.30C", "21:05:31  → 1 (LED encendido)")
            ),
            acciones = AccionesCocina()
        )
    }
}

@Preview(showBackground = true, showSystemUi = true, name = "Sin conexión")
@Composable
fun CocinaContentSinConexionPreview() {
    Pfinalv1Theme {
        CocinaContent(estado = EstadoCocina(), acciones = AccionesCocina())
    }
}

package com.example.pfinalv1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.pfinalv1.R
import com.example.pfinalv1.data.lecturasDeMuestra
import com.example.pfinalv1.domain.Estadisticas
import com.example.pfinalv1.domain.clasificarCoccion
import com.example.pfinalv1.domain.formatearDiferencia
import com.example.pfinalv1.domain.formatearFahrenheit
import com.example.pfinalv1.domain.formatearFecha
import com.example.pfinalv1.domain.formatearHora
import com.example.pfinalv1.domain.formatearTemperatura
import com.example.pfinalv1.domain.gradosFaltantes
import com.example.pfinalv1.model.DispositivoBluetooth
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.ui.theme.Pfinalv1Theme

/**
 * Tarjeta grande de la pantalla Cocina: la lectura actual como el display de un termómetro
 * de cocina, con el objetivo de cocción y cuánto falta. Si todavía no hay lectura muestra "-- °C".
 */
@Composable
fun TarjetaTemperatura(
    temperaturaC: Double?,
    horaUltimaLectura: Long?,
    objetivoC: Double,
    nombreObjetivo: String,
    modoSimulacion: Boolean,
    modifier: Modifier = Modifier
) {
    val nivel = temperaturaC?.let { clasificarCoccion(it, objetivoC) }
    val colorValor = nivel?.let { colorNivel(it) } ?: MaterialTheme.colorScheme.onSurfaceVariant

    ElevatedCard(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_termometro),
                        contentDescription = null,
                        tint = colorValor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.sensor_lm35),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                if (modoSimulacion) {
                    EtiquetaEstado(
                        texto = stringResource(R.string.etiqueta_simulacion),
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = temperaturaC?.let { formatearTemperatura(it) } ?: stringResource(R.string.sin_lectura),
                style = MaterialTheme.typography.displayMedium,
                color = colorValor
            )
            Text(
                text = temperaturaC?.let { formatearFahrenheit(it) } ?: stringResource(R.string.sin_lectura_detalle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(10.dp))

            // Objetivo de cocción y cuánto falta para llegar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.objetivo_coccion),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (nombreObjetivo.isBlank()) formatearTemperatura(objetivoC)
                        else "${formatearTemperatura(objetivoC)} · $nombreObjetivo",
                        style = MaterialTheme.typography.titleMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
                if (nivel != null && temperaturaC != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        EtiquetaEstado(texto = textoNivel(nivel), color = colorNivel(nivel))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.faltan_grados,
                                formatearDiferencia(gradosFaltantes(temperaturaC, objetivoC))
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (horaUltimaLectura != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ultima_lectura, formatearHora(horaUltimaLectura)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** Tarjeta de una lectura guardada en el historial. Al tocarla se abre el detalle. */
@Composable
fun TarjetaLectura(
    lectura: Lectura,
    onClick: () -> Unit,
    onEliminar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val nivel = clasificarCoccion(lectura.temperaturaC, lectura.objetivoC)

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, end = 4.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Franja de color del nivel de cocción
            Box(
                modifier = Modifier
                    .size(width = 6.dp, height = 56.dp)
                    .clip(CircleShape)
                    .background(colorNivel(nivel))
            )
            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = formatearTemperatura(lectura.temperaturaC),
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorNivel(nivel)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    EtiquetaOrigen(origen = lectura.origen)
                }
                Text(
                    text = if (lectura.receta.isBlank()) stringResource(R.string.lectura_sin_receta)
                    else lectura.receta,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(
                        R.string.registro_numero_fecha_objetivo,
                        lectura.id,
                        formatearFecha(lectura.fecha),
                        formatearTemperatura(lectura.objetivoC)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (lectura.nota.isNotBlank()) {
                    Text(
                        text = lectura.nota,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                PuntoLed(encendido = lectura.ledEncendido)
                Text(
                    text = stringResource(R.string.led),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onEliminar) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.eliminar),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/** Resumen numérico del historial (promedio, mínima, máxima) calculado en domain. */
@Composable
fun TarjetaResumen(
    estadisticas: Estadisticas,
    objetivosAlcanzados: Int,
    conReceta: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.resumen_lecturas, estadisticas.cantidad),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DatoResumen(etiqueta = stringResource(R.string.promedio), valor = formatearTemperatura(estadisticas.promedio))
                DatoResumen(etiqueta = stringResource(R.string.minima), valor = formatearTemperatura(estadisticas.minima))
                DatoResumen(etiqueta = stringResource(R.string.maxima), valor = formatearTemperatura(estadisticas.maxima))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.resumen_objetivos, objetivosAlcanzados, conReceta),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun DatoResumen(etiqueta: String, valor: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = valor,
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

/** Tarjeta de un dispositivo emparejado (los ESP32 se resaltan). */
@Composable
fun TarjetaDispositivo(
    dispositivo: DispositivoBluetooth,
    seleccionado: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = when {
                seleccionado -> MaterialTheme.colorScheme.primaryContainer
                dispositivo.esEsp32 -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (seleccionado) 6.dp else 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (dispositivo.esEsp32) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.primary
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = if (dispositivo.esEsp32) R.drawable.ic_chip else R.drawable.ic_bluetooth),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = dispositivo.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (dispositivo.esEsp32) {
                        Spacer(modifier = Modifier.width(8.dp))
                        EtiquetaEstado(texto = "ESP32", color = MaterialTheme.colorScheme.secondary)
                    }
                }
                Text(
                    text = dispositivo.direccion,
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = stringResource(R.string.tipo_dispositivo, dispositivo.tipo),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (seleccionado) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = stringResource(R.string.seleccionado),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Últimos mensajes enviados (→) y recibidos (←) por Bluetooth, en monoespaciada. */
@Composable
fun TarjetaBitacora(lineas: List<String>, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.bitacora_comunicacion),
                style = MaterialTheme.typography.titleMedium
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (lineas.isEmpty()) {
                Text(
                    text = stringResource(R.string.bitacora_vacia),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                lineas.forEach { linea ->
                    Text(
                        text = linea,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ───────────────────────── Previews ─────────────────────────

@Preview(showBackground = true)
@Composable
fun TarjetaTemperaturaPreview() {
    Pfinalv1Theme {
        TarjetaTemperatura(
            temperaturaC = 68.4,
            horaUltimaLectura = System.currentTimeMillis(),
            objetivoC = 74.0,
            nombreObjetivo = "Pollo y aves",
            modoSimulacion = true,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TarjetaLecturaPreview() {
    Pfinalv1Theme {
        TarjetaLectura(
            lectura = lecturasDeMuestra.first(),
            onClick = {},
            onEliminar = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

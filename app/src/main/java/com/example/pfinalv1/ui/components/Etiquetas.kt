package com.example.pfinalv1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pfinalv1.R
import com.example.pfinalv1.domain.NivelCoccion
import com.example.pfinalv1.model.Origenes
import com.example.pfinalv1.repositorio.EstadoAdaptador
import com.example.pfinalv1.repositorio.EstadoConexion
import com.example.pfinalv1.ui.theme.ColorAlcanzado
import com.example.pfinalv1.ui.theme.ColorCalentando
import com.example.pfinalv1.ui.theme.ColorCasi
import com.example.pfinalv1.ui.theme.ColorLedApagado
import com.example.pfinalv1.ui.theme.ColorLedEncendido
import com.example.pfinalv1.ui.theme.ColorLejos

// ───────────────────────── Traducción de estados del dominio a colores y textos ─────────────────────────

fun colorNivel(nivel: NivelCoccion): Color = when (nivel) {
    NivelCoccion.LEJOS -> ColorLejos
    NivelCoccion.CALENTANDO -> ColorCalentando
    NivelCoccion.CASI -> ColorCasi
    NivelCoccion.ALCANZADO -> ColorAlcanzado
}

@Composable
fun textoNivel(nivel: NivelCoccion): String = when (nivel) {
    NivelCoccion.LEJOS -> stringResource(R.string.nivel_lejos)
    NivelCoccion.CALENTANDO -> stringResource(R.string.nivel_calentando)
    NivelCoccion.CASI -> stringResource(R.string.nivel_casi)
    NivelCoccion.ALCANZADO -> stringResource(R.string.nivel_alcanzado)
}

@Composable
fun textoEstadoConexion(estado: EstadoConexion): String = when (estado) {
    is EstadoConexion.Inactivo -> stringResource(R.string.conexion_inactiva)
    is EstadoConexion.Conectando -> stringResource(R.string.conexion_conectando, estado.nombreDispositivo)
    is EstadoConexion.Conectado -> stringResource(R.string.conexion_conectado, estado.nombreDispositivo)
    is EstadoConexion.Error -> stringResource(R.string.conexion_error, estado.mensaje)
    is EstadoConexion.Desconectado -> stringResource(R.string.conexion_desconectado)
}

@Composable
fun colorEstadoConexion(estado: EstadoConexion): Color = when (estado) {
    is EstadoConexion.Conectado -> ColorLedEncendido
    is EstadoConexion.Conectando -> MaterialTheme.colorScheme.tertiary
    is EstadoConexion.Error -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
fun textoEstadoAdaptador(estado: EstadoAdaptador): String = when (estado) {
    EstadoAdaptador.NO_SOPORTADO -> stringResource(R.string.bt_no_soportado)
    EstadoAdaptador.APAGADO -> stringResource(R.string.bt_apagado)
    EstadoAdaptador.ENCENDIDO -> stringResource(R.string.bt_encendido)
}

// ───────────────────────── Piezas pequeñas reutilizables ─────────────────────────

/** Etiqueta tipo "pastilla" con un punto de color (Conectado, Simulación, ESP32…). */
@Composable
fun EtiquetaEstado(
    texto: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/** Círculo que imita el LED del ESP32: verde brillante encendido, gris apagado. */
@Composable
fun PuntoLed(encendido: Boolean, modifier: Modifier = Modifier) {
    val color = if (encendido) ColorLedEncendido else ColorLedApagado
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
            .border(width = 3.dp, color = color.copy(alpha = 0.35f), shape = CircleShape)
    )
}

/** Etiqueta del origen de una lectura: ESP32 (verde), Simulación (ámbar) o Manual (gris). */
@Composable
fun EtiquetaOrigen(origen: String, modifier: Modifier = Modifier) {
    val color = when (origen) {
        Origenes.ESP32 -> MaterialTheme.colorScheme.secondary
        Origenes.SIMULACION -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    EtiquetaEstado(texto = origen, color = color, modifier = modifier)
}

/** Renglón "etiqueta ............ valor" para las tarjetas de datos. */
@Composable
fun FilaDato(etiqueta: String, valor: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = valor,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

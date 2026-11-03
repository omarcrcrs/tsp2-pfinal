package com.example.pfinalv1.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Calificación de 0 a 5 estrellas (columna REAL "calificacion" del recetario; viene de la Práctica 3).
 * Si recibe [onCalificar], tocar una estrella dispara el UPDATE con ese valor.
 */
@Composable
fun Estrellas(
    calificacion: Float,
    modifier: Modifier = Modifier,
    tamano: Dp = 20.dp,
    onCalificar: ((Float) -> Unit)? = null
) {
    Row(modifier = modifier) {
        for (estrella in 1..5) {
            val llena = estrella <= calificacion
            val clic = if (onCalificar != null) {
                Modifier.clickable { onCalificar(estrella.toFloat()) }
            } else {
                Modifier
            }
            Icon(
                imageVector = Icons.Default.Star,
                contentDescription = "$estrella de 5 estrellas",
                tint = if (llena) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outlineVariant,
                modifier = Modifier
                    .size(tamano)
                    .then(clic)
            )
        }
    }
}

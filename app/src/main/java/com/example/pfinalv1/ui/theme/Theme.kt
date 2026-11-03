package com.example.pfinalv1.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = AzulAcero80,
    onPrimary = AzulAcero10,
    primaryContainer = AzulAcero20,
    onPrimaryContainer = AzulAcero90,
    secondary = Verde80,
    onSecondary = Verde20,
    secondaryContainer = Verde20,
    onSecondaryContainer = Verde80,
    tertiary = Ambar80,
    onTertiary = Ambar20,
    tertiaryContainer = Ambar20,
    onTertiaryContainer = Ambar80,
    background = FondoOscuro,
    onBackground = AzulAcero90,
    surface = SuperficieOscura,
    onSurface = AzulAcero90,
    surfaceVariant = AzulAcero20,
    onSurfaceVariant = AzulAcero80
)

private val LightColorScheme = lightColorScheme(
    primary = AzulAcero40,
    onPrimary = Color.White,
    primaryContainer = AzulAcero90,
    onPrimaryContainer = AzulAcero10,
    secondary = Verde40,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Verde20,
    tertiary = Ambar40,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE3B3),
    onTertiaryContainer = Ambar20,
    background = FondoClaro,
    onBackground = AzulAcero10,
    surface = SuperficieClara,
    onSurface = AzulAcero10,
    surfaceVariant = Color(0xFFE1E8ED),
    onSurfaceVariant = AzulAcero40
)

/**
 * Tema Material personalizado de ComeMejor.
 * Material You (dynamicColor) queda apagado a propósito: la paleta azul acero / verde / ámbar
 * es parte de la identidad de la app y debe verse igual en cualquier teléfono.
 */
@Composable
fun Pfinalv1Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

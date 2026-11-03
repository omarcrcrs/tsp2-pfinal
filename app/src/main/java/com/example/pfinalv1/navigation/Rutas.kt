package com.example.pfinalv1.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.example.pfinalv1.R

/** Rutas de navegación en un solo lugar (evita errores de dedo al escribirlas). */
object Rutas {
    // Pestañas de la barra inferior
    const val RECETARIO = "Recetario"
    const val RECETAS = "Recetas"
    const val COCINA = "Cocina"
    const val HISTORIAL = "Historial"

    // Pantallas secundarias (con flecha de regresar)
    const val DISPOSITIVOS = "Dispositivos"
    const val DETALLE_RECETA = "DetalleReceta/{id}"
    const val DETALLE_LECTURA = "DetalleLectura/{id}"

    /** Nombre del argumento que viaja en las rutas de detalle: el id del registro en la BD. */
    const val ARG_ID = "id"

    fun detalleReceta(id: Int) = "DetalleReceta/$id"
    fun detalleLectura(id: Int) = "DetalleLectura/$id"
}

/** Pestañas de la barra inferior: ruta, etiqueta e ícono propio. */
data class DestinoPrincipal(
    val ruta: String,
    @StringRes val etiqueta: Int,
    @DrawableRes val icono: Int
)

val destinosPrincipales: List<DestinoPrincipal> = listOf(
    DestinoPrincipal(Rutas.RECETARIO, R.string.pestana_recetario, R.drawable.ic_recetario),
    DestinoPrincipal(Rutas.RECETAS, R.string.pestana_recetas, R.drawable.ic_recetas),
    DestinoPrincipal(Rutas.COCINA, R.string.pestana_cocina, R.drawable.ic_termometro),
    DestinoPrincipal(Rutas.HISTORIAL, R.string.pestana_historial, R.drawable.ic_historial)
)

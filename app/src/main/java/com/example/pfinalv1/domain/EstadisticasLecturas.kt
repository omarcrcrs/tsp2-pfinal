package com.example.pfinalv1.domain

import com.example.pfinalv1.model.Lectura

/** Resumen numérico del historial que se muestra arriba de la lista. */
data class Estadisticas(
    val cantidad: Int,
    val promedio: Double,
    val minima: Double,
    val maxima: Double,
    val ultima: Double
)

/**
 * Calcula promedio, mínima, máxima y última lectura del historial.
 * Devuelve null si la lista está vacía (no hay nada que resumir).
 * Se asume la lista como llega de Room: ordenada de la más reciente a la más antigua.
 */
fun calcularEstadisticas(lecturas: List<Lectura>): Estadisticas? {
    if (lecturas.isEmpty()) return null
    val temperaturas = lecturas.map { it.temperaturaC }
    return Estadisticas(
        cantidad = lecturas.size,
        promedio = temperaturas.average(),
        minima = temperaturas.min(),
        maxima = temperaturas.max(),
        ultima = temperaturas.first()
    )
}

/** Cuántas lecturas guardadas alcanzaron el objetivo de cocción que tenían en ese momento. */
fun contarObjetivosAlcanzados(lecturas: List<Lectura>): Int =
    lecturas.count { it.objetivoC > 0.0 && it.temperaturaC >= it.objetivoC }

/** Cuántas lecturas se tomaron cocinando una receta del recetario. */
fun contarConReceta(lecturas: List<Lectura>): Int =
    lecturas.count { it.receta.isNotBlank() }

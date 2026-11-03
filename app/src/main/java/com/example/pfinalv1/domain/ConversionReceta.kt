package com.example.pfinalv1.domain

import com.example.pfinalv1.model.Receta
import com.example.pfinalv1.model.RecetaGuardada

// ───────────────────────── Recetas: API (Práctica 2) ↔ Room (Práctica 3) ─────────────────────────

/** Receta de la API → registro de Room. La lista de ingredientes se guarda como texto, una línea por ingrediente. */
fun Receta.aRecetaGuardada(): RecetaGuardada = RecetaGuardada(
    idMeal = idMeal,
    nombre = nombre,
    categoria = categoria,
    area = area,
    imagenUrl = imagenUrl,
    ingredientes = ingredientes.joinToString("\n") { ingrediente ->
        if (ingrediente.medida.isBlank()) ingrediente.nombre else "${ingrediente.nombre} · ${ingrediente.medida}"
    },
    instrucciones = instrucciones
)

/** Operación inversa: la columna TEXT vuelve a ser una lista para la IU. */
fun RecetaGuardada.listaIngredientes(): List<String> =
    ingredientes.lines().map { it.trim() }.filter { it.isNotEmpty() }

/**
 * Sugiere un objetivo de cocción según la categoría de TheMealDB (Chicken, Beef, Seafood…).
 * Son las temperaturas internas mínimas seguras de la FDA/USDA; si la categoría no se
 * reconoce se usa la más exigente (74 °C) por seguridad.
 */
fun objetivoSugerido(categoria: String): Double = when (categoria.trim().lowercase()) {
    "chicken" -> 74.0
    "beef", "lamb", "pork", "goat" -> 63.0
    "seafood" -> 63.0
    "pasta", "vegetarian", "vegan", "side", "starter", "miscellaneous" -> 92.0 // hervir en la CDMX
    "dessert", "breakfast" -> 71.0
    else -> 74.0
}

package com.example.pfinalv1.model

/** Un ingrediente con su medida ("penne rigate" · "1 pound"). */
data class Ingrediente(val nombre: String, val medida: String)

/** Receta tal como la entrega TheMealDB (Práctica 2), ya limpia para la IU. */
data class Receta(
    val idMeal: String,
    val nombre: String,
    val categoria: String,
    val area: String,
    val instrucciones: String,
    val imagenUrl: String,
    val youtubeUrl: String?,
    val ingredientes: List<Ingrediente>
)

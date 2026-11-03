package com.example.pfinalv1.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Endpoints de TheMealDB (https://www.themealdb.com/api.php): API REST gratuita de recetas,
 * con la key pública de pruebas "1" en la URL base. Es la misma API de las prácticas 2 y 3.
 * Al ser suspend, Retrofit los ejecuta fuera del hilo principal.
 */
interface MealApiService {

    // GET https://www.themealdb.com/api/json/v1/1/search.php?s={nombre}
    @GET("search.php")
    suspend fun buscarPorNombre(@Query("s") nombre: String): MealResponseDto

    // GET https://www.themealdb.com/api/json/v1/1/random.php
    @GET("random.php")
    suspend fun recetaAleatoria(): MealResponseDto

    // GET https://www.themealdb.com/api/json/v1/1/filter.php?c={categoria}  (solo id, nombre y foto)
    @GET("filter.php")
    suspend fun porCategoria(@Query("c") categoria: String): MealResumenResponseDto

    // GET https://www.themealdb.com/api/json/v1/1/lookup.php?i={idMeal}  (receta completa)
    @GET("lookup.php")
    suspend fun porId(@Query("i") idMeal: String): MealResponseDto
}

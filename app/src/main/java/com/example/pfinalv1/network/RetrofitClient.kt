package com.example.pfinalv1.network

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Punto único de configuración de la API (mismo patrón que CatApi del profe y que el
 * RetrofitClient de las prácticas 2 y 3). El "1" final es la key pública de pruebas de TheMealDB.
 * "by lazy": se construye la primera vez que se usa, no al arrancar la app.
 */
object RetrofitClient {

    private const val BASE_URL = "https://www.themealdb.com/api/json/v1/1/"

    // El interceptor imprime en Logcat cada petición y su código de respuesta (nivel BASIC).
    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        .build()

    val api: MealApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MealApiService::class.java)
    }
}

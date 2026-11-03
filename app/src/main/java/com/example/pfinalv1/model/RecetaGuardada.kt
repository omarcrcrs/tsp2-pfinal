package com.example.pfinalv1.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla "recetas_guardadas" (viene de la Práctica 3): el recetario personal del usuario.
 * Cada propiedad es una columna; Room traduce el tipo de Kotlin al de SQLite:
 *   Int, Long y Boolean → INTEGER · Float → REAL · String → TEXT
 */
@Entity(tableName = "recetas_guardadas")
data class RecetaGuardada(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,                                   // INTEGER, llave primaria
    @ColumnInfo(name = "id_meal") val idMeal: String,  // TEXT: id de la receta en TheMealDB
    val nombre: String,                                // TEXT
    val categoria: String,                             // TEXT
    val area: String,                                  // TEXT: cocina de origen
    @ColumnInfo(name = "imagen_url") val imagenUrl: String,
    val ingredientes: String,                          // TEXT: un ingrediente por línea
    val instrucciones: String,                         // TEXT
    val calificacion: Float = 0f,                      // REAL: 0 a 5 estrellas
    val favorita: Boolean = false,                     // INTEGER (0/1)
    @ColumnInfo(name = "veces_cocinada") val vecesCocinada: Int = 0,
    val notas: String = "",                            // TEXT: notas personales
    @ColumnInfo(name = "fecha_guardado")
    val fechaGuardado: Long = System.currentTimeMillis() // INTEGER: fecha en milisegundos
)

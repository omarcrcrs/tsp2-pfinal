package com.example.pfinalv1.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.model.RecetaGuardada

/**
 * Base de datos local de la app (archivo "comemejor_db") con dos tablas:
 *  - recetas_guardadas → el recetario personal (Práctica 3)
 *  - lecturas          → temperaturas medidas con el ESP32 al cocinar
 *
 * Room es un ORM sobre SQLite: cada entidad se vuelve una tabla y cada DAO una clase
 * generada en compilación (KSP).
 *
 * ¿Por qué Room y no Firebase? El recetario y las lecturas deben funcionar en la cocina
 * sin internet y son personales; Firestore (NoSQL en la nube) tendría sentido para
 * compartir recetas entre varios teléfonos. Se puede agregar después.
 *
 * Si el esquema cambia (un campo nuevo en una entidad) hay que subir "version" y escribir
 * una Migration; si no, Room no abre la BD y en una app publicada se perderían los datos.
 */
@Database(entities = [RecetaGuardada::class, Lectura::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun recetaDao(): RecetaDao

    abstract fun lecturaDao(): LecturaDao

    companion object {
        // Singleton: una sola instancia para toda la app. @Volatile = todos los hilos ven el valor más reciente.
        @Volatile
        private var instancia: AppDatabase? = null

        fun obtener(context: Context): AppDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "comemejor_db"
                ).build().also { instancia = it }
            }
    }
}

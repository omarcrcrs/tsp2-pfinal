package com.example.pfinalv1.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pfinalv1.model.RecetaGuardada
import kotlinx.coroutines.flow.Flow

/**
 * DAO del recetario (viene de la Práctica 3). Solo se declaran las operaciones;
 * Room genera la implementación con KSP y valida el SQL al compilar.
 */
@Dao
interface RecetaDao {

    // INSERT: una receta nueva o varias (registros iniciales)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(receta: RecetaGuardada)

    @Insert
    suspend fun insertarTodas(recetas: List<RecetaGuardada>)

    // SELECT: toda la tabla. Al ser Flow, cada cambio en la tabla emite la lista actualizada.
    @Query("SELECT * FROM recetas_guardadas ORDER BY fecha_guardado DESC")
    fun observarTodas(): Flow<List<RecetaGuardada>>

    // SELECT: un registro por su llave primaria (null si no existe)
    @Query("SELECT * FROM recetas_guardadas WHERE id = :id")
    fun observarPorId(id: Int): Flow<RecetaGuardada?>

    @Query("SELECT COUNT(*) FROM recetas_guardadas")
    suspend fun contar(): Int

    @Query("SELECT EXISTS(SELECT 1 FROM recetas_guardadas WHERE id_meal = :idMeal)")
    suspend fun existe(idMeal: String): Boolean

    // UPDATE: Room localiza la fila por el id y reemplaza sus columnas
    @Update
    suspend fun actualizar(receta: RecetaGuardada)

    // UPDATE escrito en SQL: incrementa un solo campo sin tocar el resto
    @Query("UPDATE recetas_guardadas SET veces_cocinada = veces_cocinada + 1 WHERE id = :id")
    suspend fun registrarCocinada(id: Int)

    // DELETE
    @Delete
    suspend fun eliminar(receta: RecetaGuardada)
}

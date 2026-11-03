package com.example.pfinalv1.room

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.pfinalv1.model.Lectura
import kotlinx.coroutines.flow.Flow

/**
 * DAO (Data Access Object): las operaciones CRUD sobre la tabla "lecturas" del termómetro.
 * Solo se declaran; Room (con KSP) genera la implementación al compilar y valida el SQL.
 * Las funciones suspend corren en una corrutina, nunca en el hilo principal.
 */
@Dao
interface LecturaDao {

    // C — Create: guarda una lectura nueva (id = 0 → Room lo autogenera)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(lectura: Lectura)

    // R — Read: todas las lecturas, de la más reciente a la más antigua.
    // Devuelve un Flow: cada vez que la tabla cambia, Room emite la lista actualizada.
    @Query("SELECT * FROM lecturas ORDER BY fecha DESC")
    fun observarTodas(): Flow<List<Lectura>>

    // R — Read: una sola lectura por su id; emite null si se eliminó
    @Query("SELECT * FROM lecturas WHERE id = :id")
    fun observarPorId(id: Int): Flow<Lectura?>

    // R — Read: las últimas lecturas tomadas cocinando una receta (se muestran en su detalle)
    @Query("SELECT * FROM lecturas WHERE receta = :receta ORDER BY fecha DESC LIMIT 5")
    fun observarPorReceta(receta: String): Flow<List<Lectura>>

    // R — Read: cuántas filas hay
    @Query("SELECT COUNT(*) FROM lecturas")
    suspend fun contar(): Int

    // U — Update: Room busca la fila con el mismo id y la reemplaza
    @Update
    suspend fun actualizar(lectura: Lectura)

    // D — Delete: borra la fila cuyo id coincide
    @Delete
    suspend fun eliminar(lectura: Lectura)

    // D — Delete: vacía la tabla (botón "Vaciar historial")
    @Query("DELETE FROM lecturas")
    suspend fun eliminarTodas()
}

package com.example.pfinalv1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.room.LecturaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel del historial (Room). Las vistas nunca hablan con el DAO: le piden cada
 * operación a este ViewModel, que la ejecuta en viewModelScope para no congelar la IU.
 * Recibe el DAO por constructor (se crea con viewModelFactory en el NavManager).
 */
class HistorialViewModel(private val dao: LecturaDao) : ViewModel() {

    /**
     * Lista que observa la pantalla Historial.
     *  - null        → Room todavía no responde (indicador de carga)
     *  - lista vacía → la tabla no tiene registros
     */
    val lecturas: StateFlow<List<Lectura>?> = dao.observarTodas()
        .stateIn<List<Lectura>?>(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    /** Create: guarda una lectura nueva (id = 0 para que Room lo autogenere). */
    fun guardar(lectura: Lectura) {
        viewModelScope.launch {
            dao.insertar(lectura.copy(id = 0))
        }
    }

    /** Read: una lectura por id, observable (emite null si la borran mientras se ve el detalle). */
    fun observarPorId(id: Int): Flow<Lectura?> = dao.observarPorId(id)

    /** Read: últimas lecturas tomadas cocinando una receta (se muestran en el detalle de la receta). */
    fun observarPorReceta(nombreReceta: String): Flow<List<Lectura>> = dao.observarPorReceta(nombreReceta)

    /** Update: reemplaza la lectura (por ejemplo, con una nota nueva). */
    fun actualizar(lectura: Lectura) {
        viewModelScope.launch {
            dao.actualizar(lectura)
        }
    }

    /** Delete: una lectura. */
    fun eliminar(lectura: Lectura) {
        viewModelScope.launch {
            dao.eliminar(lectura)
        }
    }

    /** Delete: todo el historial. */
    fun eliminarTodas() {
        viewModelScope.launch {
            dao.eliminarTodas()
        }
    }
}

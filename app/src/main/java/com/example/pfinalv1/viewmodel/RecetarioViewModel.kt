package com.example.pfinalv1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pfinalv1.data.recetasDeMuestra
import com.example.pfinalv1.domain.aRecetaGuardada
import com.example.pfinalv1.model.Receta
import com.example.pfinalv1.model.RecetaGuardada
import com.example.pfinalv1.room.RecetaDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Resultado de consultar un registro del recetario por id. */
sealed interface ConsultaEstado {
    data object Cargando : ConsultaEstado
    data object NoExiste : ConsultaEstado
    data class Encontrada(val receta: RecetaGuardada) : ConsultaEstado
}

/**
 * ViewModel del recetario (tabla recetas_guardadas, Práctica 3). Las vistas nunca tocan Room:
 * piden cada operación aquí y se ejecuta en una corrutina (viewModelScope) para no congelar la IU.
 */
class RecetarioViewModel(private val dao: RecetaDao) : ViewModel() {

    /** Toda la tabla. null = Room aún no responde; lista vacía = sin registros. */
    val recetas: StateFlow<List<RecetaGuardada>?> = dao.observarTodas()
        .stateIn<List<RecetaGuardada>?>(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    init {
        // La primera vez que se abre la app se insertan las recetas de muestra para no arrancar en blanco
        viewModelScope.launch {
            if (dao.contar() == 0) dao.insertarTodas(recetasDeMuestra)
        }
    }

    /** SELECT por llave primaria; se vuelve a emitir cada vez que el registro cambia. */
    fun consultarReceta(id: Int): Flow<ConsultaEstado> = dao.observarPorId(id).map { receta ->
        if (receta == null) ConsultaEstado.NoExiste else ConsultaEstado.Encontrada(receta)
    }

    /** INSERT: guarda una receta traída de la API; avisa si ya estaba en la tabla. */
    fun guardar(receta: Receta, alTerminar: (guardada: Boolean) -> Unit) {
        viewModelScope.launch {
            if (dao.existe(receta.idMeal)) {
                alTerminar(false)
            } else {
                dao.insertar(receta.aRecetaGuardada())
                alTerminar(true)
            }
        }
    }

    // UPDATE: cambios a los registros
    fun calificar(receta: RecetaGuardada, estrellas: Float) = actualizar(receta.copy(calificacion = estrellas))

    fun alternarFavorita(receta: RecetaGuardada) = actualizar(receta.copy(favorita = !receta.favorita))

    fun guardarNotas(receta: RecetaGuardada, notas: String) = actualizar(receta.copy(notas = notas.trim()))

    fun registrarCocinada(id: Int) {
        viewModelScope.launch { dao.registrarCocinada(id) }
    }

    private fun actualizar(receta: RecetaGuardada) {
        viewModelScope.launch { dao.actualizar(receta) }
    }

    // DELETE
    fun eliminar(receta: RecetaGuardada) {
        viewModelScope.launch { dao.eliminar(receta) }
    }
}

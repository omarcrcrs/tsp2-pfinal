package com.example.pfinalv1.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.pfinalv1.model.Receta
import com.example.pfinalv1.network.MealResponseDto
import com.example.pfinalv1.network.RetrofitClient
import com.example.pfinalv1.network.aReceta
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException

/** Una receta de la lista de una categoría (solo id, nombre y foto). */
data class RecetaResumen(val idMeal: String, val nombre: String, val imagenUrl: String)

/** Un solo estado posible a la vez para la pantalla Recetas (misma idea que CatUiState del profe). */
sealed interface BusquedaUiState {
    data object Inicial : BusquedaUiState
    data object Cargando : BusquedaUiState
    data class Exito(val receta: Receta) : BusquedaUiState
    data class ExitoLista(val categoria: String, val recetas: List<RecetaResumen>) : BusquedaUiState
    data class Error(val mensaje: String) : BusquedaUiState
}

/**
 * ViewModel de la API (TheMealDB). Separado del de la BD: una tarea distinta, un ViewModel distinto.
 * Busca por nombre, pide una receta aleatoria o explora una categoría (Vegetarian, Seafood…).
 */
class BusquedaViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<BusquedaUiState>(BusquedaUiState.Inicial)
    val uiState: StateFlow<BusquedaUiState> = _uiState.asStateFlow()

    /** Última lista de categoría consultada, para poder regresar a ella desde una receta. */
    private var ultimaLista: BusquedaUiState.ExitoLista? = null

    fun buscar(nombre: String) {
        val texto = nombre.trim()
        if (texto.isBlank()) {
            _uiState.value = BusquedaUiState.Error("Escribe el nombre de una receta para buscar")
            return
        }
        pedirReceta(sinResultado = "No se encontró ninguna receta con \"$texto\"") {
            RetrofitClient.api.buscarPorNombre(texto)
        }
    }

    fun recetaAleatoria() {
        pedirReceta(sinResultado = "No se pudo obtener una receta aleatoria") {
            RetrofitClient.api.recetaAleatoria()
        }
    }

    /** Receta completa a partir de su id (al tocar una tarjeta de la lista de categoría). */
    fun abrir(idMeal: String) {
        pedirReceta(sinResultado = "No se encontró la receta $idMeal") {
            RetrofitClient.api.porId(idMeal)
        }
    }

    /** Lista de recetas de una categoría de TheMealDB (Vegetarian, Vegan, Seafood, Chicken…). */
    fun explorarCategoria(categoria: String) {
        viewModelScope.launch {
            _uiState.value = BusquedaUiState.Cargando
            _uiState.value = try {
                val resumenes = RetrofitClient.api.porCategoria(categoria).meals.orEmpty().mapNotNull { dto ->
                    val id = dto.idMeal ?: return@mapNotNull null
                    RecetaResumen(
                        idMeal = id,
                        nombre = dto.strMeal.orEmpty(),
                        imagenUrl = dto.strMealThumb.orEmpty()
                    )
                }
                if (resumenes.isEmpty()) {
                    BusquedaUiState.Error("La categoría $categoria no tiene recetas")
                } else {
                    BusquedaUiState.ExitoLista(categoria, resumenes).also { ultimaLista = it }
                }
            } catch (e: IOException) {
                BusquedaUiState.Error(SIN_INTERNET)
            } catch (e: HttpException) {
                BusquedaUiState.Error("El servidor respondió con un error (código ${e.code()}).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BusquedaUiState.Error("No se pudo leer la respuesta: ${e.message ?: "error desconocido"}")
            }
        }
    }

    /** Regresa a la última lista de categoría (si la hay) o al estado inicial. */
    fun volverALista() {
        _uiState.value = ultimaLista ?: BusquedaUiState.Inicial
    }

    val hayListaPrevia: Boolean
        get() = ultimaLista != null

    private fun pedirReceta(sinResultado: String, peticion: suspend () -> MealResponseDto) {
        viewModelScope.launch {
            _uiState.value = BusquedaUiState.Cargando
            _uiState.value = try {
                val dto = peticion().meals?.firstOrNull()
                if (dto != null) BusquedaUiState.Exito(dto.aReceta()) else BusquedaUiState.Error(sinResultado)
            } catch (e: IOException) {
                BusquedaUiState.Error(SIN_INTERNET)
            } catch (e: HttpException) {
                BusquedaUiState.Error("El servidor respondió con un error (código ${e.code()}).")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BusquedaUiState.Error("No se pudo leer la respuesta: ${e.message ?: "error desconocido"}")
            }
        }
    }

    companion object {
        private const val SIN_INTERNET = "No hay conexión a internet. Revisa tu red e intenta de nuevo."

        /** Categorías de TheMealDB que se ofrecen como chips ("comer mejor": verduras, pescado, pollo…). */
        val CATEGORIAS = listOf("Vegetarian", "Vegan", "Seafood", "Chicken", "Pasta", "Breakfast", "Side", "Starter")
    }
}

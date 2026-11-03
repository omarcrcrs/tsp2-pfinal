package com.example.pfinalv1

import com.example.pfinalv1.domain.aRecetaGuardada
import com.example.pfinalv1.domain.listaIngredientes
import com.example.pfinalv1.domain.objetivoSugerido
import com.example.pfinalv1.model.Ingrediente
import com.example.pfinalv1.model.Receta
import com.example.pfinalv1.network.MealDto
import com.example.pfinalv1.network.aReceta
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pruebas locales: la receta de la API se convierte en registro de Room y los ingredientes se recuperan íntegros. */
class ConversionRecetaTest {

    private val receta = Receta(
        idMeal = "52771",
        nombre = "Spicy Arrabiata Penne",
        categoria = "Vegetarian",
        area = "Italian",
        instrucciones = "Cocer la pasta…",
        imagenUrl = "https://www.themealdb.com/images/media/meals/ustsqw1468250014.jpg",
        youtubeUrl = null,
        ingredientes = listOf(
            Ingrediente("penne rigate", "1 pound"),
            Ingrediente("garlic", "3 cloves"),
            Ingrediente("basil", "")
        )
    )

    @Test
    fun aRecetaGuardada_conservaDatosYUneIngredientes() {
        val registro = receta.aRecetaGuardada()

        assertEquals(0, registro.id)                 // Room autogenera la llave primaria
        assertEquals("52771", registro.idMeal)
        assertEquals("penne rigate · 1 pound\ngarlic · 3 cloves\nbasil", registro.ingredientes)
        assertEquals(0f, registro.calificacion, 0.0001f)
        assertEquals(false, registro.favorita)
    }

    @Test
    fun listaIngredientes_recuperaTresLineas() {
        val lineas = receta.aRecetaGuardada().listaIngredientes()

        assertEquals(listOf("penne rigate · 1 pound", "garlic · 3 cloves", "basil"), lineas)
    }

    @Test
    fun objetivoSugerido_porCategoriaDeTheMealDB() {
        assertEquals(74.0, objetivoSugerido("Chicken"), 0.0001)
        assertEquals(63.0, objetivoSugerido("Beef"), 0.0001)
        assertEquals(63.0, objetivoSugerido("seafood"), 0.0001)   // sin importar mayúsculas
        assertEquals(92.0, objetivoSugerido("Vegetarian"), 0.0001)
        assertEquals(74.0, objetivoSugerido("Categoría rara"), 0.0001) // por seguridad, la más exigente
    }

    @Test
    fun mealDto_aReceta_ignoraIngredientesVaciosYNulos() {
        val dto = MealDto(
            idMeal = "1", strMeal = "Prueba", strCategory = "Seafood", strArea = "Mexican",
            strInstructions = "Mezclar", strMealThumb = "https://x/y.jpg", strTags = null, strYoutube = "  ",
            strIngredient1 = "Camarón", strIngredient2 = " ", strIngredient3 = null, strIngredient4 = "Limón",
            strIngredient5 = null, strIngredient6 = null, strIngredient7 = null, strIngredient8 = null,
            strIngredient9 = null, strIngredient10 = null, strIngredient11 = null, strIngredient12 = null,
            strIngredient13 = null, strIngredient14 = null, strIngredient15 = null, strIngredient16 = null,
            strIngredient17 = null, strIngredient18 = null, strIngredient19 = null, strIngredient20 = null,
            strMeasure1 = "500 g", strMeasure2 = null, strMeasure3 = null, strMeasure4 = null,
            strMeasure5 = null, strMeasure6 = null, strMeasure7 = null, strMeasure8 = null,
            strMeasure9 = null, strMeasure10 = null, strMeasure11 = null, strMeasure12 = null,
            strMeasure13 = null, strMeasure14 = null, strMeasure15 = null, strMeasure16 = null,
            strMeasure17 = null, strMeasure18 = null, strMeasure19 = null, strMeasure20 = null
        )

        val convertida = dto.aReceta()

        assertEquals(listOf(Ingrediente("Camarón", "500 g"), Ingrediente("Limón", "")), convertida.ingredientes)
        assertNull(convertida.youtubeUrl) // un enlace en blanco se trata como ausente
        assertEquals("Seafood", convertida.categoria)
    }
}

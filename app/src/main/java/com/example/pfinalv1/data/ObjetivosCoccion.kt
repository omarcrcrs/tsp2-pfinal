package com.example.pfinalv1.data

/**
 * Temperatura que debe alcanzar un alimento o preparación. Al llegar a ella la app avisa
 * (alerta en pantalla) y, con el control automático activado, enciende el LED del ESP32.
 */
data class ObjetivoCoccion(
    val nombre: String,
    val gradosC: Double,
    val descripcion: String
)

/**
 * Objetivos predefinidos para la pantalla Cocina. Las temperaturas internas mínimas de
 * seguridad son las que recomiendan la FDA/USDA ("Safe Minimum Internal Temperatures");
 * la de hervor se ajustó a la altitud de la Ciudad de México (≈ 2 240 m), donde el agua
 * hierve cerca de 92 °C y no a 100 °C.
 */
object ObjetivosCoccion {
    val POLLO = ObjetivoCoccion("Pollo y aves", 74.0, "Temperatura interna mínima segura para aves (74 °C / 165 °F).")

    val todos: List<ObjetivoCoccion> = listOf(
        POLLO,
        ObjetivoCoccion("Carne molida", 71.0, "Hamburguesas, albóndigas y picadillo (71 °C / 160 °F)."),
        ObjetivoCoccion("Res, cerdo y pescado", 63.0, "Cortes enteros y pescado (63 °C / 145 °F), con 3 min de reposo."),
        ObjetivoCoccion("Recalentar sobras", 74.0, "Las sobras deben recalentarse hasta 74 °C en todo el platillo."),
        ObjetivoCoccion("Hervir (CDMX)", 92.0, "A la altitud de la CDMX el agua hierve cerca de 92 °C."),
        ObjetivoCoccion("Té verde", 80.0, "Más caliente quema las hojas y amarga la infusión."),
        ObjetivoCoccion("Leche para yogur", 43.0, "Temperatura de incubación de los cultivos (42–45 °C)."),
        ObjetivoCoccion("Mantener caliente", 60.0, "Por debajo de 60 °C la comida caliente entra a la zona de peligro."),
        ObjetivoCoccion("Refrigerador", 5.0, "Por arriba de 5 °C los alimentos fríos entran a la zona de peligro (5–60 °C).")
    )
}

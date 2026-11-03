package com.example.pfinalv1.ui.theme

import androidx.compose.ui.graphics.Color

// ───────────────────────── Paleta de ComeMejor ─────────────────────────
// primary   → azul acero: la app es un instrumento de cocina (termómetro, registros); serio y legible
// secondary → verde hoja: alimentación saludable, el recetario y los datos "en vivo" del ESP32
// tertiary  → ámbar: calor, cocción y alertas de temperatura (contrasta con azules y verdes)
// Los niveles de cocción y el LED tienen colores propios para que se entiendan de un vistazo.

val AzulAcero90 = Color(0xFFCFDCE4)
val AzulAcero80 = Color(0xFFA9BECB)
val AzulAcero60 = Color(0xFF5B7C92)
val AzulAcero40 = Color(0xFF2F4F63)
val AzulAcero20 = Color(0xFF17303F)
val AzulAcero10 = Color(0xFF0B1A23)

val Verde80 = Color(0xFFA5D6A7)
val Verde40 = Color(0xFF2E7D32)
val Verde20 = Color(0xFF1B4D1E)

val Ambar80 = Color(0xFFFFD28A)
val Ambar40 = Color(0xFFC77800)
val Ambar20 = Color(0xFF6B3F00)

val FondoClaro = Color(0xFFF4F7F5)
val FondoOscuro = Color(0xFF0E171D)
val SuperficieClara = Color(0xFFFFFFFF)
val SuperficieOscura = Color(0xFF17262F)

// Niveles de cocción (domain.NivelCoccion) y estado del LED
val ColorLejos = Color(0xFF1E88E5)        // azul: falta mucho
val ColorCalentando = Color(0xFF43A047)   // verde: va bien
val ColorCasi = Color(0xFFFB8C00)         // naranja: faltan 3 °C o menos
val ColorAlcanzado = Color(0xFFE53935)    // rojo: ya llegó al objetivo
val ColorLedEncendido = Color(0xFF4CAF50)
val ColorLedApagado = Color(0xFF9E9E9E)

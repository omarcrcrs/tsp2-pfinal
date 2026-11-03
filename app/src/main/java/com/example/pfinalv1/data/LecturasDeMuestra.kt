package com.example.pfinalv1.data

import com.example.pfinalv1.model.Lectura
import com.example.pfinalv1.model.Origenes

/**
 * Lecturas de ejemplo para los @Preview de Android Studio y las pruebas.
 * NO se insertan en la base de datos: el historial empieza vacío y se llena con lecturas reales.
 */
private const val UNA_HORA = 60 * 60 * 1000L

val lecturasDeMuestra: List<Lectura> = listOf(
    Lectura(id = 5, temperaturaC = 75.3, objetivoC = 74.0, fecha = 1_793_152_800_000L, receta = "Teriyaki Chicken Casserole", origen = Origenes.ESP32, dispositivo = "ESP32_BT_Serial", ledEncendido = true, nota = "Pollo listo, jugoso"),
    Lectura(id = 4, temperaturaC = 68.1, objetivoC = 74.0, fecha = 1_793_152_800_000L - UNA_HORA, receta = "Teriyaki Chicken Casserole", origen = Origenes.ESP32, dispositivo = "ESP32_BT_Serial", ledEncendido = false, nota = ""),
    Lectura(id = 3, temperaturaC = 43.4, objetivoC = 43.0, fecha = 1_793_152_800_000L - 2 * UNA_HORA, receta = "", origen = Origenes.SIMULACION, dispositivo = "Simulador", ledEncendido = true, nota = "Prueba sin tarjeta"),
    Lectura(id = 2, temperaturaC = 91.6, objetivoC = 92.0, fecha = 1_793_152_800_000L - 3 * UNA_HORA, receta = "Spicy Arrabiata Penne", origen = Origenes.ESP32, dispositivo = "ESP32_BT_Serial", ledEncendido = false, nota = "Agua casi hirviendo"),
    Lectura(id = 1, temperaturaC = 4.2, objetivoC = 5.0, fecha = 1_793_152_800_000L - 4 * UNA_HORA, receta = "", origen = Origenes.MANUAL, dispositivo = "", ledEncendido = false, nota = "Refrigerador")
)

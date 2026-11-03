package com.example.pfinalv1.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Tabla "lecturas": cada fila es una temperatura medida con el termómetro de cocina
 * (ESP32 + LM35) que el usuario decidió guardar desde la pantalla Cocina.
 *
 * Tipos de dato que guarda la tabla (5 distintos):
 *  - Int     → id (llave primaria que Room autogenera)
 *  - Double  → temperatura en °C que respondió el ESP32 y el objetivo de cocción vigente
 *  - Long    → fecha y hora en milisegundos; un número se ordena bien con ORDER BY,
 *              un texto "dd/MM/yyyy" no
 *  - String  → receta que se estaba cocinando, origen de la lectura (ESP32, Simulación o
 *              Manual), nombre del dispositivo Bluetooth y una nota libre
 *  - Boolean → si el LED estaba encendido en ese momento (SQLite lo guarda como 0/1)
 *
 * Todos los campos tienen valor por defecto para poder crear una lectura con
 * Lectura() y completarla con copy(...), igual que en el Ejercicio 10.
 */
@Entity(tableName = "lecturas")
data class Lectura(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "temperatura_c")
    val temperaturaC: Double = 0.0,

    @ColumnInfo(name = "objetivo_c")
    val objetivoC: Double = 0.0,

    @ColumnInfo(name = "fecha")
    val fecha: Long = System.currentTimeMillis(),

    /** Nombre de la receta en curso ("" si solo se midió). Se guarda el nombre, no el id, para que
     *  la lectura sobreviva aunque la receta se borre del recetario. */
    @ColumnInfo(name = "receta")
    val receta: String = "",

    @ColumnInfo(name = "origen")
    val origen: String = Origenes.ESP32,

    @ColumnInfo(name = "dispositivo")
    val dispositivo: String = "",

    @ColumnInfo(name = "led_encendido")
    val ledEncendido: Boolean = false,

    @ColumnInfo(name = "nota")
    val nota: String = ""
)

/** De dónde salió la lectura. Se guarda como texto para que se lea directo en el Database Inspector. */
object Origenes {
    const val ESP32 = "ESP32"
    const val SIMULACION = "Simulación"
    const val MANUAL = "Manual"
}

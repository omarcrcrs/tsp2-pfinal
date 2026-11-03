package com.example.pfinalv1.model

/**
 * Datos mínimos de un dispositivo Bluetooth emparejado con el teléfono.
 * Es una copia "plana" de android.bluetooth.BluetoothDevice para que las vistas
 * y el ViewModel no dependan de clases del sistema (idea del repo ConectaESP32 del profe).
 */
data class DispositivoBluetooth(
    val nombre: String,
    val direccion: String,   // MAC, por ejemplo "A4:CF:12:9B:3E:7A"
    val tipo: String,        // Clásico, BLE, Dual o Desconocido
    val esEsp32: Boolean     // el nombre contiene "ESP32", "ESP" o "HC-" (módulos HC-05/HC-06)
)

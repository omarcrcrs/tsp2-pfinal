package com.example.pfinalv1.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/**
 * Permisos de Bluetooth según la versión de Android (mismo criterio que ConectaESP32 del profe):
 *  - Android 12 (API 31 "S") en adelante: BLUETOOTH_SCAN y BLUETOOTH_CONNECT, que son
 *    "peligrosos" y se piden en tiempo de ejecución con un cuadro de diálogo.
 *  - Antes de Android 12: BLUETOOTH y BLUETOOTH_ADMIN, que se otorgan solos al instalar
 *    por estar declarados en el AndroidManifest.
 */
fun permisosBluetoothRequeridos(): Array<String> =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    } else {
        arrayOf(Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
    }

/** true si el usuario ya otorgó todos los permisos de la lista anterior. */
fun tienePermisosBluetooth(context: Context): Boolean =
    permisosBluetoothRequeridos().all { permiso ->
        ContextCompat.checkSelfPermission(context, permiso) == PackageManager.PERMISSION_GRANTED
    }

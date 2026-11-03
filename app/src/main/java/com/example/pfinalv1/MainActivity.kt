package com.example.pfinalv1

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.pfinalv1.navigation.NavManager
import com.example.pfinalv1.room.AppDatabase
import com.example.pfinalv1.ui.theme.Pfinalv1Theme

/**
 * ComeMejor — Proyecto final de Temas Selectos de Programación II (2027-I).
 *
 * App para comer mejor: recetas de la API TheMealDB (Práctica 2), recetario personal en Room
 * (Práctica 3) y un termómetro de cocina con ESP32 + LM35 por Bluetooth clásico (SPP) que avisa
 * y enciende el LED cuando el platillo llega a su temperatura segura.
 *
 * Capas (MVVM): model · domain · data · network · room · repositorio · viewmodel · navigation · ui
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Una sola instancia de la base de datos para toda la app (singleton).
        // Los DAO viajan al NavManager, que se los entrega a los ViewModel.
        val baseDeDatos = AppDatabase.obtener(applicationContext)

        setContent {
            Pfinalv1Theme {
                NavManager(
                    recetaDao = baseDeDatos.recetaDao(),
                    lecturaDao = baseDeDatos.lecturaDao()
                )
            }
        }
    }
}

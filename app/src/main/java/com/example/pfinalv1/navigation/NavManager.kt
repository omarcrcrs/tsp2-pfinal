package com.example.pfinalv1.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pfinalv1.R
import com.example.pfinalv1.room.LecturaDao
import com.example.pfinalv1.room.RecetaDao
import com.example.pfinalv1.ui.components.BarraSuperior
import com.example.pfinalv1.ui.views.CocinaView
import com.example.pfinalv1.ui.views.DetalleLecturaView
import com.example.pfinalv1.ui.views.DetalleRecetaView
import com.example.pfinalv1.ui.views.DispositivosView
import com.example.pfinalv1.ui.views.HistorialView
import com.example.pfinalv1.ui.views.RecetarioView
import com.example.pfinalv1.ui.views.RecetasView
import com.example.pfinalv1.viewmodel.BluetoothViewModel
import com.example.pfinalv1.viewmodel.BusquedaViewModel
import com.example.pfinalv1.viewmodel.DispositivosViewModel
import com.example.pfinalv1.viewmodel.HistorialViewModel
import com.example.pfinalv1.viewmodel.RecetarioViewModel
import kotlinx.coroutines.launch

/**
 * Administra la navegación de ComeMejor: 4 pestañas en la barra inferior
 * (Recetario · Recetas · Cocina · Historial) y tres pantallas secundarias con flecha de regresar
 * (Dispositivos Bluetooth, DetalleReceta/{id} y DetalleLectura/{id}).
 *
 * Un solo Scaffold para toda la app: barra superior, barra inferior y Snackbar viven aquí;
 * las vistas solo reciben estado y lambdas (nunca el NavController), como en el Ejercicio 10.
 *
 * Un ViewModel por tarea distinta (consejo del profe):
 *  - RecetarioViewModel    → tabla recetas_guardadas (Room, Práctica 3)
 *  - BusquedaViewModel     → API TheMealDB (Práctica 2)
 *  - BluetoothViewModel    → comunicación con el ESP32 (compartido por Cocina, Dispositivos y detalles)
 *  - DispositivosViewModel → lista de emparejados
 *  - HistorialViewModel    → tabla lecturas (Room)
 */
@Composable
fun NavManager(recetaDao: RecetaDao, lecturaDao: LecturaDao) {
    val navController = rememberNavController()

    val recetarioViewModel: RecetarioViewModel = viewModel(
        factory = viewModelFactory {
            initializer { RecetarioViewModel(recetaDao) }
        }
    )
    val historialViewModel: HistorialViewModel = viewModel(
        factory = viewModelFactory {
            initializer { HistorialViewModel(lecturaDao) }
        }
    )
    val busquedaViewModel: BusquedaViewModel = viewModel()
    val bluetoothViewModel: BluetoothViewModel = viewModel()
    val dispositivosViewModel: DispositivosViewModel = viewModel()

    // Un solo SnackbarHostState para toda la app. Antes de mostrar un aviso se descarta el
    // anterior para que no se formen en cola (detalle que quedó pendiente en el Ejercicio 10).
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val mostrarMensaje: (String) -> Unit = { texto ->
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(texto)
        }
    }

    // Los avisos del Bluetooth (conectado, error, objetivo alcanzado…) se escuchan aquí, que
    // siempre está en pantalla, para que no se pierdan cuando el usuario está en otra pestaña.
    LaunchedEffect(bluetoothViewModel) {
        bluetoothViewModel.mensajes.collect { mensaje -> mostrarMensaje(mensaje) }
    }

    // Ruta actual → título de la barra superior, flecha de regresar y pestaña seleccionada
    val entradaActual by navController.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val idActual = entradaActual?.arguments?.getInt(Rutas.ARG_ID) ?: 0
    val esSecundaria = rutaActual == Rutas.DISPOSITIVOS ||
        rutaActual == Rutas.DETALLE_RECETA ||
        rutaActual == Rutas.DETALLE_LECTURA
    val titulo = when (rutaActual) {
        Rutas.RECETAS -> stringResource(R.string.titulo_recetas)
        Rutas.COCINA -> stringResource(R.string.titulo_cocina)
        Rutas.HISTORIAL -> stringResource(R.string.titulo_historial)
        Rutas.DISPOSITIVOS -> stringResource(R.string.titulo_dispositivos)
        Rutas.DETALLE_RECETA -> stringResource(R.string.titulo_detalle_receta, idActual)
        Rutas.DETALLE_LECTURA -> stringResource(R.string.titulo_detalle_lectura, idActual)
        else -> stringResource(R.string.titulo_recetario)
    }
    // Pestaña que se resalta: la propia o, en una pantalla secundaria, la pestaña de la que depende
    val pestanaActiva = when (rutaActual) {
        Rutas.DISPOSITIVOS -> Rutas.COCINA
        Rutas.DETALLE_RECETA -> Rutas.RECETARIO
        Rutas.DETALLE_LECTURA -> Rutas.HISTORIAL
        else -> rutaActual
    }
    val regresar: () -> Unit = { navController.popBackStack() }

    Scaffold(
        topBar = {
            BarraSuperior(
                titulo = titulo,
                onRegresar = if (esSecundaria) regresar else null
            )
        },
        bottomBar = {
            NavigationBar {
                destinosPrincipales.forEach { destino ->
                    NavigationBarItem(
                        selected = pestanaActiva == destino.ruta,
                        onClick = { navegarAPestana(navController, destino.ruta) },
                        icon = {
                            Icon(
                                painter = painterResource(id = destino.icono),
                                contentDescription = null
                            )
                        },
                        label = { Text(text = stringResource(id = destino.etiqueta)) }
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.RECETARIO,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ── Pestañas ──
            composable(Rutas.RECETARIO) {
                RecetarioView(
                    viewModel = recetarioViewModel,
                    onBuscar = { navegarAPestana(navController, Rutas.RECETAS) },
                    onAbrir = { id -> navController.navigate(Rutas.detalleReceta(id)) }
                )
            }

            composable(Rutas.RECETAS) {
                RecetasView(
                    viewModel = busquedaViewModel,
                    onGuardar = { receta ->
                        recetarioViewModel.guardar(receta) { guardada ->
                            mostrarMensaje(
                                if (guardada) "«${receta.nombre}» se guardó en tu recetario"
                                else "«${receta.nombre}» ya estaba en tu recetario"
                            )
                        }
                    }
                )
            }

            composable(Rutas.COCINA) {
                CocinaView(
                    viewModel = bluetoothViewModel,
                    onGuardarLectura = { lectura -> historialViewModel.guardar(lectura) },
                    onIrADispositivos = { navController.navigate(Rutas.DISPOSITIVOS) { launchSingleTop = true } },
                    onMensaje = mostrarMensaje
                )
            }

            composable(Rutas.HISTORIAL) {
                HistorialView(
                    viewModel = historialViewModel,
                    onAbrirDetalle = { id -> navController.navigate(Rutas.detalleLectura(id)) },
                    onMensaje = mostrarMensaje
                )
            }

            // ── Pantallas secundarias ──
            composable(Rutas.DISPOSITIVOS) {
                DispositivosView(
                    viewModel = dispositivosViewModel,
                    onConectar = { dispositivo ->
                        bluetoothViewModel.conectar(dispositivo)
                        navController.popBackStack() // regresa a Cocina, donde se ve el estado de la conexión
                    },
                    onMensaje = mostrarMensaje
                )
            }

            // Rutas con argumento: el {id} del registro viaja como Int
            composable(
                route = Rutas.DETALLE_RECETA,
                arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.IntType })
            ) { entrada ->
                DetalleRecetaView(
                    idRegistro = entrada.arguments?.getInt(Rutas.ARG_ID) ?: 0,
                    viewModel = recetarioViewModel,
                    historialViewModel = historialViewModel,
                    onCocinar = { nombreReceta, objetivoC ->
                        bluetoothViewModel.iniciarCoccion(nombreReceta, objetivoC)
                        navegarAPestana(navController, Rutas.COCINA)
                    },
                    onMensaje = mostrarMensaje,
                    onRegresar = regresar
                )
            }

            composable(
                route = Rutas.DETALLE_LECTURA,
                arguments = listOf(navArgument(Rutas.ARG_ID) { type = NavType.IntType })
            ) { entrada ->
                DetalleLecturaView(
                    idLectura = entrada.arguments?.getInt(Rutas.ARG_ID) ?: 0,
                    viewModel = historialViewModel,
                    onRegresar = regresar,
                    onMensaje = mostrarMensaje
                )
            }
        }
    }
}

/**
 * Cambia de pestaña sin apilar pantallas: regresa hasta el Recetario guardando el estado de
 * la pestaña que se deja y restaura el de la que se abre (patrón recomendado por Android).
 */
private fun navegarAPestana(navController: NavHostController, ruta: String) {
    navController.navigate(ruta) {
        popUpTo(Rutas.RECETARIO) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

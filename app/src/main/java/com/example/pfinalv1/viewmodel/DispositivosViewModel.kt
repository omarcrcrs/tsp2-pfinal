package com.example.pfinalv1.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.pfinalv1.model.DispositivoBluetooth
import com.example.pfinalv1.repositorio.DispositivosRepositorio
import com.example.pfinalv1.repositorio.EstadoAdaptador
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de la pantalla Dispositivos: lista los emparejados y guarda cuál se eligió.
 * Separado del BluetoothViewModel porque es otra tarea (elegir vs. comunicarse),
 * igual que el profe separó PairedDevicesViewModel de BluetoothViewModel.
 */
class DispositivosViewModel(application: Application) : AndroidViewModel(application) {

    private val repositorio = DispositivosRepositorio(application.applicationContext)

    private val _dispositivos = MutableStateFlow<List<DispositivoBluetooth>>(emptyList())
    val dispositivos: StateFlow<List<DispositivoBluetooth>> = _dispositivos.asStateFlow()

    private val _seleccionado = MutableStateFlow<DispositivoBluetooth?>(null)
    val seleccionado: StateFlow<DispositivoBluetooth?> = _seleccionado.asStateFlow()

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando.asStateFlow()

    private val _estadoAdaptador = MutableStateFlow(repositorio.estadoAdaptador())
    val estadoAdaptador: StateFlow<EstadoAdaptador> = _estadoAdaptador.asStateFlow()

    /** Vuelve a leer la lista de emparejados (se llama al entrar con permisos y con el botón Actualizar). */
    fun refrescar() {
        viewModelScope.launch {
            _cargando.value = true
            _estadoAdaptador.value = repositorio.estadoAdaptador()
            val lista = repositorio.obtenerEmparejados()
            _dispositivos.value = lista
            // Si el seleccionado ya no está emparejado, se olvida
            if (_seleccionado.value != null && lista.none { it.direccion == _seleccionado.value?.direccion }) {
                _seleccionado.value = null
            }
            _cargando.value = false
        }
    }

    fun seleccionar(dispositivo: DispositivoBluetooth) {
        _seleccionado.value = dispositivo
    }
}

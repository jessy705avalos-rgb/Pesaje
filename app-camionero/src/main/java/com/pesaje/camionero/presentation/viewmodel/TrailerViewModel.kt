package com.pesaje.camionero.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.camionero.domain.repository.TrailerRepository
import com.pesaje.core.data.local.RegistroPesajeTrailer
import com.pesaje.core.domain.model.WeightReading
import com.pesaje.core.domain.repository.WeightRepository
import com.pesaje.core.domain.usecase.PrintTrailerEntradaUseCase
import com.pesaje.core.domain.usecase.PrintTrailerSalidaUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "TRAILER_VM"

class TrailerViewModel(
    private val repository: WeightRepository,
    private val trailerRepository: TrailerRepository,
    private val printTrailerEntradaUseCase: PrintTrailerEntradaUseCase,
    private val printTrailerSalidaUseCase: PrintTrailerSalidaUseCase
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentWeight = MutableStateFlow<WeightReading?>(null)
    val currentWeight: StateFlow<WeightReading?> = _currentWeight.asStateFlow()

    private var connectionJob: Job? = null
    private var observeJob: Job? = null

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()


    val vehiculosAbiertos: StateFlow<List<RegistroPesajeTrailer>> =
        trailerRepository.obtenerAbiertos()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000),
                emptyList()
            )

    private val _vehiculoSeleccionado = MutableStateFlow<RegistroPesajeTrailer?>(null)
    val vehiculoSeleccionado: StateFlow<RegistroPesajeTrailer?> =
        _vehiculoSeleccionado.asStateFlow()

    fun connect() {
        connectionJob?.cancel()
        observeJob?.cancel()
        connectionJob = viewModelScope.launch {
            repository.connect().collect { _isConnected.value = it }
        }
        observeJob = viewModelScope.launch {
            repository.observeWeight().collect { _currentWeight.value = it }
        }
    }

    fun readWeight() {
        viewModelScope.launch { repository.requestWeight() }
    }

    fun setZero() {
        viewModelScope.launch { repository.setZero() }
    }

    fun setTare() {
        viewModelScope.launch { repository.setTare() }
    }

    fun registrarEntrada(
        placas: String, conductor: String, carga: String,
        imprimirDespues: Boolean = false, printerName: String = "Printer001"
    ) {
        val pesoActual = currentWeight.value?.kilograms
        if (pesoActual == null || placas.isBlank() || conductor.isBlank()) {
            _mensaje.value = "Faltan datos o no hay peso capturado"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val yaExiste = trailerRepository.buscarAbiertoPorPlacas(placas.trim())
            if (yaExiste != null) {
                _mensaje.value = "Ya existe una entrada abierta para las placas $placas"
                return@launch
            }
            val fechaActual = obtenerFechaActual()
            val registro = RegistroPesajeTrailer(
                placas = placas.trim(), conductor = conductor.trim(), carga = carga.trim(),
                pesoEntrada = pesoActual, fechaEntrada = fechaActual, estaAbierto = true
            )
            trailerRepository.registrarEntrada(registro)
            Log.d(TAG, "✅ Entrada registrada: $registro")

            if (imprimirDespues) {
                _mensaje.value = "Imprimiendo ticket..."
                val exito = printTrailerEntradaUseCase(
                    printerName,
                    placas.trim(),
                    conductor.trim(),
                    carga.trim(),
                    pesoActual,
                    fechaActual
                )
                _mensaje.value =
                    if (exito) "¡Ticket entrada impreso con éxito!" else "Entrada registrada, pero falló la impresión"
            } else {
                _mensaje.value = "Entrada registrada correctamente"
            }
        }
    }

    fun seleccionarVehiculoParaSalida(placas: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _vehiculoSeleccionado.value = trailerRepository.buscarAbiertoPorPlacas(placas)
        }
    }

    fun registrarSalida(imprimirDespues: Boolean = false, printerName: String = "Printer001") {
        val vehiculo = vehiculoSeleccionado.value
        val pesoSalidaActual = currentWeight.value?.kilograms
        if (vehiculo == null || pesoSalidaActual == null) {
            _mensaje.value = "Selecciona un vehículo y captura el peso de salida"
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val pesoNeto = abs(vehiculo.pesoEntrada - pesoSalidaActual)
            val fechaSalidaActual = obtenerFechaActual()
            val registroActualizado = vehiculo.copy(
                pesoSalida = pesoSalidaActual, fechaSalida = fechaSalidaActual, estaAbierto = false
            )
            trailerRepository.registrarSalida(registroActualizado)
            Log.d(TAG, "✅ Salida registrada. Neto: $pesoNeto kg")

            if (imprimirDespues) {
                val exito = printTrailerSalidaUseCase(
                    printerName,
                    vehiculo.placas,
                    vehiculo.conductor,
                    vehiculo.carga,
                    vehiculo.pesoEntrada,
                    vehiculo.fechaEntrada,
                    pesoSalidaActual,
                    fechaSalidaActual,
                    pesoNeto
                )
                _mensaje.value =
                    if (exito) "Salida registrada e impresa. Neto: $pesoNeto kg" else "Salida registrada, pero falló la impresión"
            } else {
                _mensaje.value = "Salida registrada. Peso neto: $pesoNeto kg"
            }
            _vehiculoSeleccionado.value = null
        }
    }

    fun clearMensaje() {
        _mensaje.value = null
    }

    private fun obtenerFechaActual(): String {
        val formato =
            java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return formato.format(java.util.Date())
    }

    override fun onCleared() {
        connectionJob?.cancel(); observeJob?.cancel(); repository.disconnect()
        super.onCleared()
    }
}
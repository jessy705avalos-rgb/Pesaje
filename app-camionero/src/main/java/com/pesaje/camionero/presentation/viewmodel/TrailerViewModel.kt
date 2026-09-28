package com.pesaje.camionero.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.camionero.domain.repository.TrailerRepository
import com.pesaje.core.data.local.RegistroPesajeTrailer
import com.pesaje.core.data.local.SettingsDataStore
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val TAG = "TRAILER_VM"

class TrailerViewModel(
    private val repository: WeightRepository,
    private val trailerRepository: TrailerRepository,
    private val printTrailerEntradaUseCase: PrintTrailerEntradaUseCase,
    private val printTrailerSalidaUseCase: PrintTrailerSalidaUseCase,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentWeight = MutableStateFlow<WeightReading?>(null)
    val currentWeight: StateFlow<WeightReading?> = _currentWeight.asStateFlow()

    private var connectionJob: Job? = null
    private var observeJob: Job? = null

    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    // --- Configuración de Tickets desde DataStore ---
    val entradaHeader: StateFlow<String> = settingsDataStore.trailerEntradaHeaderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "ENTRADA TRAILER")

    val entradaFooter: StateFlow<String> = settingsDataStore.trailerEntradaFooterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Conserve su ticket")

    val salidaHeader: StateFlow<String> = settingsDataStore.trailerSalidaHeaderFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "SALIDA TRAILER")

    val salidaFooter: StateFlow<String> = settingsDataStore.trailerSalidaFooterFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Regrese pronto")

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

    fun guardarConfiguracionTickets(
        eHeader: String,
        eFooter: String,
        sHeader: String,
        sFooter: String
    ) {
        viewModelScope.launch {
            settingsDataStore.saveTrailerTicketSettings(eHeader, eFooter, sHeader, sFooter)
        }
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
            _currentWeight.value = null // limpiamos el peso

            if (imprimirDespues) {
                _mensaje.value = "Imprimiendo ticket..."
                val titulo = settingsDataStore.trailerEntradaHeaderFlow.first()
                val piePagina = settingsDataStore.trailerEntradaFooterFlow.first()

                val exito = printTrailerEntradaUseCase(
                    printerName = printerName,
                    placas = placas.trim(),
                    conductor = conductor.trim(),
                    carga = carga.trim(),
                    pesoEntrada = pesoActual,
                    fechaEntrada = fechaActual,
                    titulo = titulo,
                    piePagina = piePagina
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
            _currentWeight.value = null

            if (imprimirDespues) {
                val titulo = settingsDataStore.trailerSalidaHeaderFlow.first()
                val piePagina = settingsDataStore.trailerSalidaFooterFlow.first()

                val exito = printTrailerSalidaUseCase(
                    printerName = printerName,
                    placas = vehiculo.placas,
                    conductor = vehiculo.conductor,
                    carga = vehiculo.carga,
                    pesoEntrada = vehiculo.pesoEntrada,
                    fechaEntrada = vehiculo.fechaEntrada,
                    pesoSalida = pesoSalidaActual,
                    fechaSalida = fechaSalidaActual,
                    pesoNeto = pesoNeto,
                    titulo = titulo,
                    piePagina = piePagina
                )
                _mensaje.value =
                    if (exito) "Salida registrada e impresa. Neto: ${pesoNeto.toInt()} kg" else "Salida registrada, pero falló la impresión"
            } else {
                _mensaje.value = "Salida registrada. Peso neto: ${pesoNeto.toInt()} kg"
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
        connectionJob?.cancel()
        observeJob?.cancel()
        repository.disconnect()
        super.onCleared()
    }

    fun reimprimirUltimoTicketEntrada(printerName: String = "Printer001") {
        viewModelScope.launch(Dispatchers.IO) {
            val ultimo = trailerRepository.obtenerUltimoEntrada()
            if (ultimo != null) {
                val titulo = settingsDataStore.trailerEntradaHeaderFlow.first()
                val piePagina = settingsDataStore.trailerEntradaFooterFlow.first()

                val exito = printTrailerEntradaUseCase(
                    printerName = printerName,
                    placas = ultimo.placas,
                    conductor = ultimo.conductor,
                    carga = ultimo.carga,
                    pesoEntrada = ultimo.pesoEntrada,
                    fechaEntrada = ultimo.fechaEntrada,
                    titulo = titulo,
                    piePagina = piePagina
                )
                _mensaje.value = if (exito) "Reimprimiendo último ticket de entrada..." else "Error al imprimir el ticket."
            } else {
                _mensaje.value = "No hay registros de entrada para reimprimir."
            }
        }
    }

    fun reimprimirUltimoTicketSalida(printerName: String = "Printer001") {
        viewModelScope.launch(Dispatchers.IO) {
            val ultimo = trailerRepository.obtenerUltimoSalida()
            if (ultimo != null) {
                val pesoSalidaLocal = ultimo.pesoSalida
                val fechaSalidaLocal = ultimo.fechaSalida

                if (pesoSalidaLocal != null && fechaSalidaLocal != null) {
                    val pesoNeto = kotlin.math.abs(ultimo.pesoEntrada - pesoSalidaLocal)
                    val titulo = settingsDataStore.trailerSalidaHeaderFlow.first()
                    val piePagina = settingsDataStore.trailerSalidaFooterFlow.first()

                    val exito = printTrailerSalidaUseCase(
                        printerName = printerName,
                        placas = ultimo.placas,
                        conductor = ultimo.conductor,
                        carga = ultimo.carga,
                        pesoEntrada = ultimo.pesoEntrada,
                        fechaEntrada = ultimo.fechaEntrada,
                        pesoSalida = pesoSalidaLocal,
                        fechaSalida = fechaSalidaLocal,
                        pesoNeto = pesoNeto,
                        titulo = titulo,
                        piePagina = piePagina
                    )
                    _mensaje.value = if (exito) "Reimprimiendo último ticket de salida..." else "Error al imprimir el ticket."
                } else {
                    _mensaje.value = "El último registro de salida está incompleto."
                }
            } else {
                _mensaje.value = "No hay registros de salida para reimprimir."
            }
        }
    }
}
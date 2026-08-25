package com.pesaje.camionero.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.core.data.local.RegistroPesajeTrailer
import com.pesaje.core.data.local.RegistroPesajeTrailerDao
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
    private val registroDao: RegistroPesajeTrailerDao,
    private val printTrailerEntradaUseCase: PrintTrailerEntradaUseCase,
    private val printTrailerSalidaUseCase: PrintTrailerSalidaUseCase
) : ViewModel() {

    // ---------- Estado de conexión Bluetooth con la báscula ----------
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentWeight = MutableStateFlow<WeightReading?>(null)
    val currentWeight: StateFlow<WeightReading?> = _currentWeight.asStateFlow()

    private var connectionJob: Job? = null
    private var observeJob: Job? = null

    // ---------- Mensajes para mostrar al usuario (Toast) ----------
    private val _mensaje = MutableStateFlow<String?>(null)
    val mensaje: StateFlow<String?> = _mensaje.asStateFlow()

    // ---------- Lista de vehículos con entrada abierta (para el dropdown de Salidas) ----------
    val vehiculosAbiertos: StateFlow<List<RegistroPesajeTrailer>> =
        registroDao.obtenerAbiertos()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // ---------- Vehículo elegido en el dropdown de Salidas, con sus datos completos ----------
    private val _vehiculoSeleccionado = MutableStateFlow<RegistroPesajeTrailer?>(null)
    val vehiculoSeleccionado: StateFlow<RegistroPesajeTrailer?> = _vehiculoSeleccionado.asStateFlow()

    // ==================== CONEXIÓN BLUETOOTH (BÁSCULA) ====================

    fun connect() {
        Log.d(TAG, "connect() llamado — cancelando escuchas anteriores si existían")
        connectionJob?.cancel()
        observeJob?.cancel()

        connectionJob = viewModelScope.launch {
            repository.connect().collect { connected ->
                Log.d(TAG, "Estado de conexión actualizado: $connected")
                _isConnected.value = connected
            }
        }

        observeJob = viewModelScope.launch {
            repository.observeWeight().collect { reading ->
                Log.d(TAG, "Nuevo peso recibido: ${reading.kilograms} kg")
                _currentWeight.value = reading
            }
        }
    }

    fun readWeight() {
        viewModelScope.launch {
            repository.requestWeight()
        }
    }

    fun setZero() {
        viewModelScope.launch {
            repository.setZero()
        }
    }

    fun setTare() {
        viewModelScope.launch {
            repository.setTare()
        }
    }

    // ==================== REGISTRAR ENTRADA ====================

    fun registrarEntrada(
        placas: String,
        conductor: String,
        carga: String,
        imprimirDespues: Boolean = false,
        printerName: String = "Printer001"
    ) {
        val pesoActual = currentWeight.value?.kilograms

        if (pesoActual == null || placas.isBlank() || conductor.isBlank()) {
            _mensaje.value = "Faltan datos o no hay peso capturado"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Regla de negocio: solo puede haber un registro abierto por placas
            val yaExiste = registroDao.buscarAbiertoPorPlacas(placas.trim())

            if (yaExiste != null) {
                _mensaje.value = "Ya existe una entrada abierta para las placas $placas"
                return@launch
            }

            val fechaActual = obtenerFechaActual()
            val registro = RegistroPesajeTrailer(
                placas = placas.trim(),
                conductor = conductor.trim(),
                carga = carga.trim(),
                pesoEntrada = pesoActual,
                fechaEntrada = fechaActual,
                estaAbierto = true
            )

            registroDao.insertar(registro)
            Log.d(TAG, "✅ Entrada registrada: $registro")

            if (imprimirDespues) {
                val exito = printTrailerEntradaUseCase(
                    printerName = printerName,
                    placas = placas.trim(),
                    conductor = conductor.trim(),
                    carga = carga.trim(),
                    pesoEntrada = pesoActual,
                    fechaEntrada = fechaActual
                )
                _mensaje.value = if (exito) {
                    "Entrada registrada e impresa"
                } else {
                    "Entrada registrada, pero falló la impresión"
                }
            } else {
                _mensaje.value = "Entrada registrada correctamente"
            }
        }
    }

    // ==================== REGISTRAR SALIDA ====================

    fun seleccionarVehiculoParaSalida(placas: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val vehiculo = registroDao.buscarAbiertoPorPlacas(placas)
            _vehiculoSeleccionado.value = vehiculo
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
            // Valor absoluto: garantiza que el peso neto siempre sea positivo,
            // sin importar si el camión entró vacío/lleno o salió vacío/lleno
            val pesoNeto = abs(vehiculo.pesoEntrada - pesoSalidaActual)
            val fechaSalidaActual = obtenerFechaActual()

            val registroActualizado = vehiculo.copy(
                pesoSalida = pesoSalidaActual,
                fechaSalida = fechaSalidaActual,
                estaAbierto = false
            )

            registroDao.actualizar(registroActualizado)
            Log.d(TAG, "✅ Salida registrada. Peso neto: $pesoNeto kg")

            if (imprimirDespues) {
                val exito = printTrailerSalidaUseCase(
                    printerName = printerName,
                    placas = vehiculo.placas,
                    conductor = vehiculo.conductor,
                    carga = vehiculo.carga,
                    pesoEntrada = vehiculo.pesoEntrada,
                    fechaEntrada = vehiculo.fechaEntrada,
                    pesoSalida = pesoSalidaActual,
                    fechaSalida = fechaSalidaActual,
                    pesoNeto = pesoNeto
                )
                _mensaje.value = if (exito) {
                    "Salida registrada e impresa. Neto: $pesoNeto kg"
                } else {
                    "Salida registrada, pero falló la impresión"
                }
            } else {
                _mensaje.value = "Salida registrada. Peso neto: $pesoNeto kg"
            }

            _vehiculoSeleccionado.value = null
        }
    }

    // ==================== UTILIDADES ====================

    fun clearMensaje() {
        _mensaje.value = null
    }

    private fun obtenerFechaActual(): String {
        val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return formato.format(java.util.Date())
    }

    override fun onCleared() {
        Log.d(TAG, "onCleared() — desconectando")
        connectionJob?.cancel()
        observeJob?.cancel()
        repository.disconnect()
        super.onCleared()
    }
}
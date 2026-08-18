package com.pesaje.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.core.data.local.RegistroPesajeGanado
import com.pesaje.core.data.local.RegistroPesajeGanadoDao
import com.pesaje.domain.model.CattleWeighingState
import com.pesaje.core.domain.model.WeightReading
import com.pesaje.core.domain.repository.WeightRepository
import com.pesaje.core.domain.usecase.PrintCattleTicketUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PESAJE_VM"

class WeightViewModel(
    private val repository: WeightRepository,
    private val printCattleTicketUseCase: PrintCattleTicketUseCase,
    private val registroDao: RegistroPesajeGanadoDao,
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentWeight = MutableStateFlow<WeightReading?>(null)
    val currentWeight: StateFlow<WeightReading?> = _currentWeight.asStateFlow()

    // Estado del proceso de pesaje de ganado
    private val _cattleState = MutableStateFlow(CattleWeighingState.WAITING_FOR_ANIMAL)
    val cattleState: StateFlow<CattleWeighingState> = _cattleState.asStateFlow()

    // Peso retenido/congelado en pantalla (Hold)
    private val _lockedWeight = MutableStateFlow<Double?>(null)
    val lockedWeight: StateFlow<Double?> = _lockedWeight.asStateFlow()

    // Umbral mínimo de peso para detectar un animal (en kg)
    private val ANIMAL_THRESHOLD = 20.0

    // Referencias a los trabajos activos
    private var connectionJob: Job? = null
    private var observeJob: Job? = null

    // Estado de impresión
    private val _printStatus = MutableStateFlow<String?>(null)
    val printStatus: StateFlow<String?> = _printStatus.asStateFlow()

    fun connect() {
        Log.d(TAG, "ViewModel.connect() llamado — cancelando escuchas anteriores si existían")

        connectionJob?.cancel()
        observeJob?.cancel()

        connectionJob = viewModelScope.launch {
            repository.connect().collect { connected ->
                Log.d(TAG, "Estado de conexión actualizado: $connected")
                _isConnected.value = connected
            }
        }

        observeJob = viewModelScope.launch {
            Log.d(TAG, "Empezando a escuchar observeWeight()...")
            repository.observeWeight().collect { reading ->
                Log.d(TAG, "Nuevo peso recibido en ViewModel: ${reading.kilograms} kg")
                _currentWeight.value = reading
                processWeightForCattle(reading)
            }
        }
    }

    // Lógica para la autocaptura en Modo Ganado
    private fun processWeightForCattle(reading: WeightReading) {
        val weight = reading.kilograms
        val isStable = reading.isStable

        when (_cattleState.value) {
            CattleWeighingState.WAITING_FOR_ANIMAL -> {
                if (weight >= ANIMAL_THRESHOLD) {
                    Log.d(TAG, "🐄 Animal detectado ($weight kg). Estabilizando...")
                    _cattleState.value = CattleWeighingState.STABILIZING
                }
            }

            CattleWeighingState.STABILIZING -> {
                if (weight < ANIMAL_THRESHOLD) {
                    _cattleState.value = CattleWeighingState.WAITING_FOR_ANIMAL
                } else if (isStable) {
                    Log.d(TAG, "🎯 ¡Peso estable capturado!: $weight kg")
                    _lockedWeight.value = weight
                    _cattleState.value = CattleWeighingState.LOCKED
                }
            }

            CattleWeighingState.LOCKED -> {
                _cattleState.value = CattleWeighingState.WAITING_FOR_EXIT
            }

            CattleWeighingState.WAITING_FOR_EXIT -> {
                if (weight < ANIMAL_THRESHOLD) {
                    Log.d(TAG, "🔄 Animal bajó de la báscula. Reiniciando ciclo.")
                    resetCattleProcess()
                }
            }
        }
    }

    fun readWeight() {
        Log.d(TAG, "ViewModel.readWeight() llamado")
        viewModelScope.launch {
            repository.requestWeight()
        }
    }

    fun setTare() {
        Log.d(TAG, "ViewModel.setTare() llamado")
        viewModelScope.launch {
            repository.setTare()
        }
    }

    fun setZero() {
        Log.d(TAG, "ViewModel.setZero() llamado")
        viewModelScope.launch {
            repository.setZero()
        }
    }

    fun resetCattleProcess() {
        _cattleState.value = CattleWeighingState.WAITING_FOR_ANIMAL
        _lockedWeight.value = null
    }

    fun printTicket(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _printStatus.value = "Imprimiendo ticket..."

            val exito = printCattleTicketUseCase(
                printerName = printerName,
                areteId = areteId,
                sexo = sexo,
                pesoKg = pesoKg
            )

            if (exito) {
                _printStatus.value = "¡Ticket impreso con éxito!"
            } else {
                _printStatus.value =
                    "Error: No se pudo conectar a '$printerName' o falló la impresión."
            }
        }
    }

    fun guardarRegistro(areteId: String, sexo: String) {
        val pesoActual = currentWeight.value

        if (pesoActual == null || areteId.isBlank()) {
            Log.e(TAG, "❌ No se puede guardar: falta el peso o el arete")
            return
        }
        viewModelScope.launch (Dispatchers.IO){
            val registro = RegistroPesajeGanado(
                arete = areteId,
                sexo= sexo,
                peso = pesoActual.kilograms,
                fecha = obtenerFechaActual()
            )
            registroDao.insertar(registro)
            Log.d(TAG, "✅ Registro guardado: $registro")
        }
    }

    private fun obtenerFechaActual(): String {
        val formato = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return formato.format(java.util.Date())
    }


    fun clearPrintStatus() {
        _printStatus.value = null
    }

    override fun onCleared() {
        Log.d(TAG, "ViewModel.onCleared() — desconectando")
        connectionJob?.cancel()
        observeJob?.cancel()
        repository.disconnect()
        super.onCleared()
    }
}
package com.pesaje.presentation.viewmodel

import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.data.remote.PrinterBluetoothManager
import com.pesaje.data.remote.TicketPrinterHelper
import com.pesaje.data.repositoryImpl.PrinterRepositoryImpl
import com.pesaje.domain.model.CattleWeighingState
import com.pesaje.domain.model.WeighingMode
import com.pesaje.domain.model.WeightReading
import com.pesaje.domain.repository.PrinterRepository
import com.pesaje.domain.repository.WeightRepository
import com.pesaje.domain.usecase.PrintTicketUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PESAJE_VM"

class WeightViewModel(
    private val repository: WeightRepository,

    ) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentWeight = MutableStateFlow<WeightReading?>(null)
    val currentWeight: StateFlow<WeightReading?> = _currentWeight.asStateFlow()

    // Modo de pesaje seleccionado (Ganado por defecto)
    private val _weighingMode = MutableStateFlow(WeighingMode.CATTLE)
    val weighingMode: StateFlow<WeighingMode> = _weighingMode.asStateFlow()

    // Estado del proceso de pesaje de ganado
    private val _cattleState = MutableStateFlow(CattleWeighingState.WAITING_FOR_ANIMAL)
    val cattleState: StateFlow<CattleWeighingState> = _cattleState.asStateFlow()

    // Peso retenido/congelado en pantalla (Hold)
    private val _lockedWeight = MutableStateFlow<Double?>(null)
    val lockedWeight: StateFlow<Double?> = _lockedWeight.asStateFlow()

    // Umbral mínimo de peso para detectar un animal (en kg)
    private val ANIMAL_THRESHOLD = 20.0

    // Guardamos referencia a los "trabajos" activos para poder cancelarlos
    private var connectionJob: Job? = null
    private var observeJob: Job? = null

    //para la impresión
    private val printerHelper = TicketPrinterHelper()
    private val printerRepository = PrinterRepositoryImpl(printerHelper)
    private val printTicketUseCase = PrintTicketUseCase(printerRepository)

    private val _printStatus = MutableStateFlow<String?>(null)
    val printStatus: StateFlow<String?> = _printStatus.asStateFlow()
    fun connect() {
        Log.d(TAG, "ViewModel.connect() llamado — cancelando escuchas anteriores si existían")

        // Cancela cualquier escucha anterior antes de crear una nueva
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

                // AQUÍ SE PROCESA EL PESO EN CADA LECTURA QUE LLEGA
                processWeightForCattle(reading)
            }
        }
    }

    // Lógica para la autocaptura en Modo Ganado
    private fun processWeightForCattle(reading: WeightReading) {
        if (_weighingMode.value == WeighingMode.STANDARD) return

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

    fun setWeighingMode(mode: WeighingMode) {
        Log.d(TAG, "Cambiando modo de pesaje a: $mode")
        _weighingMode.value = mode
        resetCattleProcess()
    }

    fun resetCattleProcess() {
        _cattleState.value = CattleWeighingState.WAITING_FOR_ANIMAL
        _lockedWeight.value = null
    }

    fun printTicket(
        context: Context,
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            _printStatus.value = "Conectando a la impresora..."

            val printerBluetoothManager = PrinterBluetoothManager(context)
            val socket = printerBluetoothManager.connectToPrinter(printerName)

            if (socket == null || !socket.isConnected) {
                _printStatus.value =
                    "Error: No se pudo conectar a '$printerName'. Revisa que esté encendida y emparejada."
                return@launch
            }

            val exito = printTicketUseCase(socket, areteId, sexo, pesoKg)

            if (exito) {
                _printStatus.value = "¡Ticket impreso con éxito!"
            } else {
                _printStatus.value = "Error al imprimir el ticket."
            }
        }
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
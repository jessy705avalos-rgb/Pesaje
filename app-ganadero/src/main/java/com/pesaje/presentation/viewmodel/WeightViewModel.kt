package com.pesaje.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.core.data.local.RegistroPesajeGanado
import com.pesaje.core.data.local.RegistroPesajeGanadoDao
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

    private var connectionJob: Job? = null
    private var observeJob: Job? = null

    private val _printStatus = MutableStateFlow<String?>(null)
    val printStatus: StateFlow<String?> = _printStatus.asStateFlow()

    fun connect() {
        Log.d(TAG, "ViewModel.connect() llamado")

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
                _currentWeight.value = reading
            }
        }
    }

    fun readWeight() {
        Log.d(TAG, "ViewModel.readWeight() llamado - Solicitando trama al indicador")
        viewModelScope.launch {
            repository.requestWeight()
        }
    }

    fun setTare() {
        viewModelScope.launch { repository.setTare() }
    }

    fun setZero() {
        viewModelScope.launch { repository.setZero() }
    }

    fun guardarEImprimir(
        areteId: String,
        sexo: String,
        printerName: String = "Printer001"
    ) {
        val pesoAGuardar = currentWeight.value?.kilograms

        if (pesoAGuardar == null || areteId.isBlank()) {
            Log.e(TAG, "❌ No se puede guardar: falta el peso o el arete")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val registro = RegistroPesajeGanado(
                arete = areteId,
                sexo = sexo,
                peso = pesoAGuardar,
                fecha = obtenerFechaActual()
            )
            registroDao.insertar(registro)
            Log.d(TAG, "✅ Registro guardado: $registro")
            limpiarPeso()

            _printStatus.value = "Imprimiendo ticket..."
            val exito = printCattleTicketUseCase(
                printerName = printerName,
                areteId = areteId,
                sexo = sexo,
                pesoKg = pesoAGuardar
            )
            _printStatus.value = if (exito) {
                "¡Ticket impreso y guardado con éxito!"
            } else {
                "Guardado. Error al conectar a '$printerName'."
            }
        }
    }

    fun reimprimirUltimoTicket(printerName: String = "Printer001") {
        viewModelScope.launch(Dispatchers.IO) {
            val ultimoRegistro = registroDao.obtenerUltimo()

            if (ultimoRegistro == null) {
                _printStatus.value = "No hay tickets previos para reimprimir."
                return@launch
            }

            _printStatus.value = "Reimprimiendo último ticket..."
            val exito = printCattleTicketUseCase(
                printerName = printerName,
                areteId = ultimoRegistro.arete,
                sexo = ultimoRegistro.sexo,
                pesoKg = ultimoRegistro.peso
            )
            _printStatus.value = if (exito) {
                "¡Reimpresión exitosa!"
            } else {
                "Error al reimprimir en '$printerName'."
            }
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
        connectionJob?.cancel()
        observeJob?.cancel()
        repository.disconnect()
        super.onCleared()
    }

    fun limpiarPeso() {
        _currentWeight.value = null
    }
}
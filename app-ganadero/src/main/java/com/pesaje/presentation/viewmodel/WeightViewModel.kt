package com.pesaje.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.core.data.local.RegistroPesajeGanado
import com.pesaje.core.data.local.RegistroPesajeGanadoDao
import com.pesaje.core.data.local.SettingsDataStore
import com.pesaje.core.domain.model.WeightReading
import com.pesaje.core.domain.repository.WeightRepository
import com.pesaje.core.domain.usecase.PrintCattleTicketUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "PESAJE_VM"

class WeightViewModel(
    private val repository: WeightRepository,
    private val printCattleTicketUseCase: PrintCattleTicketUseCase,
    private val registroDao: RegistroPesajeGanadoDao,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentWeight = MutableStateFlow<WeightReading?>(null)
    val currentWeight: StateFlow<WeightReading?> = _currentWeight.asStateFlow()

    private var connectionJob: Job? = null
    private var observeJob: Job? = null

    private val _printStatus = MutableStateFlow<String?>(null)
    val printStatus: StateFlow<String?> = _printStatus.asStateFlow()

    val ticketHeader = settingsDataStore.headerFlow
    val ticketFooter = settingsDataStore.footerFlow

    // Configuración de dispositivos
    val indicatorDevice = settingsDataStore.indicatorDeviceFlow
    val printerDevice = settingsDataStore.printerDeviceFlow
    val indicatorFormat = settingsDataStore.indicatorFormatFlow

    fun connect() {
        connectionJob?.cancel()
        observeJob?.cancel()

        connectionJob = viewModelScope.launch {
            repository.connect().collect { connected ->
                _isConnected.value = connected
            }
        }

        observeJob = viewModelScope.launch {
            repository.observeWeight().collect { reading ->
                _currentWeight.value = reading
            }
        }
    }

    fun readWeight() {
        viewModelScope.launch { repository.requestWeight() }
    }

    fun setTare() {
        viewModelScope.launch { repository.setTare() }
    }

    fun setZero() {
        viewModelScope.launch { repository.setZero() }
    }

    fun limpiarPeso() {
        _currentWeight.value = null
    }

    fun guardarEImprimir(
        areteId: String,
        sexo: String,
        header: String,
        footer: String
    ) {
        val pesoAGuardar = currentWeight.value?.kilograms
        val decimalesAGuardar = currentWeight.value?.decimals ?: 1

        if (pesoAGuardar == null || areteId.isBlank()) {
            Log.e(TAG, "❌ No se puede guardar: falta el peso o el arete")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            // Misma fecha para guardar e imprimir
            val fechaRegistro = obtenerFechaActual()

            val registro = RegistroPesajeGanado(
                arete = areteId,
                sexo = sexo,
                peso = pesoAGuardar,
                fecha = fechaRegistro
            )
            registroDao.insertar(registro)

            limpiarPeso()

            val printerName = settingsDataStore.printerDeviceFlow.first()
            if (printerName.isBlank()) {
                _printStatus.value = "Guardado. Selecciona una impresora en Configuración."
                return@launch
            }

            _printStatus.value = "Imprimiendo ticket..."
            val exito = printCattleTicketUseCase(
                printerName = printerName,
                areteId = areteId,
                sexo = sexo,
                pesoKg = pesoAGuardar,
                titulo = header,
                piePagina = footer,
                fecha = fechaRegistro,
                decimales = decimalesAGuardar
            )
            _printStatus.value = if (exito) {
                "¡Ticket impreso y guardado con éxito!"
            } else {
                "Guardado. Error al conectar a '$printerName'."
            }
        }
    }

    fun reimprimirUltimoTicket(
        header: String,
        footer: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val ultimoRegistro = registroDao.obtenerUltimo()

            if (ultimoRegistro == null) {
                _printStatus.value = "No hay tickets previos para reimprimir."
                return@launch
            }

            val printerName = settingsDataStore.printerDeviceFlow.first()
            if (printerName.isBlank()) {
                _printStatus.value = "Selecciona una impresora en Configuración."
                return@launch
            }

            // CAMBIO 1: calcula los decimales a partir del peso guardado
            val decimalesReimpresion = java.math.BigDecimal.valueOf(ultimoRegistro.peso)
                .stripTrailingZeros()
                .scale()
                .coerceAtLeast(0)

            _printStatus.value = "Reimprimiendo último ticket..."
            val exito = printCattleTicketUseCase(
                printerName = printerName,
                areteId = ultimoRegistro.arete,
                sexo = ultimoRegistro.sexo,
                pesoKg = ultimoRegistro.peso,
                titulo = header,
                piePagina = footer,
                fecha = ultimoRegistro.fecha,   // fecha/hora ORIGINAL del registro
                decimales = decimalesReimpresion   // CAMBIO 2: pásalo aquí
            )
            _printStatus.value = if (exito) {
                "¡Reimpresión exitosa!"
            } else {
                "Error al reimprimir en '$printerName'."
            }
        }
    }

    fun guardarConfiguracion(
        header: String,
        footer: String,
        indicatorFormat: String,
        indicatorDevice: String,
        printerDevice: String
    ) {
        viewModelScope.launch {
            val deviceAnterior = settingsDataStore.indicatorDeviceFlow.first()
            val formatoAnterior = settingsDataStore.indicatorFormatFlow.first()

            settingsDataStore.saveTicketSettings(header, footer)
            settingsDataStore.saveDeviceSettings(indicatorFormat, indicatorDevice, printerDevice)

            // Si cambió el indicador o su formato, reconecta con la nueva selección
            if (indicatorDevice != deviceAnterior || indicatorFormat != formatoAnterior) {
                limpiarPeso()
                connect()
            }

            _printStatus.value = "Configuración guardada"
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
}
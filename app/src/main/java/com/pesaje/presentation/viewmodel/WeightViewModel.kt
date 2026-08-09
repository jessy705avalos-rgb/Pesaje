package com.pesaje.presentation.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.domain.model.WeightReading
import com.pesaje.domain.repository.WeightRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

private const val TAG = "PESAJE_VM"

class WeightViewModel(
    private val repository: WeightRepository
) : ViewModel() {

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _currentWeight = MutableStateFlow<WeightReading?>(null)
    val currentWeight: StateFlow<WeightReading?> = _currentWeight.asStateFlow()

    //  Guardamos referencia a los "trabajos" activos para poder cancelarlos
    private var connectionJob: Job? = null
    private var observeJob: Job? = null

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
            }
        }
    }

    fun readWeight() {
        Log.d(TAG, "ViewModel.readWeight() llamado")
        viewModelScope.launch {
            repository.requestWeight()
        }
    }

    fun setTare(){
        Log.d(TAG, "ViewModel.setTare() llamado")
        viewModelScope.launch {
            repository.setTare()
        }
    }

    fun setZero(){
        Log.d(TAG, "ViewModel.setZero() llamado")
        viewModelScope.launch {
            repository.setZero()
        }

    }

    override fun onCleared() {
        Log.d(TAG, "ViewModel.onCleared() — desconectando")
        connectionJob?.cancel()
        observeJob?.cancel()
        repository.disconnect()
        super.onCleared()
    }
}
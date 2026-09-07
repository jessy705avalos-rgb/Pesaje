package com.pesaje.camionero.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.camionero.domain.repository.CsvExportTrailerRepository
import com.pesaje.camionero.domain.repository.TrailerRepository
import com.pesaje.core.data.local.RegistroPesajeTrailer
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistorialTrailerViewModel(
    private val trailerRepository: TrailerRepository,
    private val csvExportTrailerRepository: CsvExportTrailerRepository
) : ViewModel() {

    val registros: StateFlow<List<RegistroPesajeTrailer>> =
        trailerRepository.obtenerTodos().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun borrarTodos() {
        viewModelScope.launch {
            trailerRepository.borrarTodos()
        }
    }

    fun exportarRegistros(context: Context, nombreArchivo: String) {
        csvExportTrailerRepository.exportarYCompartir(
            context = context,
            nombreArchivo = nombreArchivo,
            registros = registros.value
        )
    }
}
package com.pesaje.presentation.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pesaje.core.data.local.RegistroPesajeGanado
import com.pesaje.core.data.local.RegistroPesajeGanadoDao
import com.pesaje.core.domain.repository.CsvExportRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.WhileSubscribed
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HistorialViewModel(
    private val registroDao: RegistroPesajeGanadoDao,
    private val csvExportRepository: CsvExportRepository

) : ViewModel() {

    val registros: StateFlow<List<RegistroPesajeGanado>> = registroDao.obtenerTodos()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun borrarTodos() {
        viewModelScope.launch {            //Dentro del territorio de vida de este ViewModel, abre una tarea nueva que borre todos los registros.
            registroDao.borrarTodos()
        }
    }

    fun exportarRegistros(context: Context, nombreArchivo: String) {
        csvExportRepository.exportarYCompartir(
            context = context,
            nombreArchivo = nombreArchivo,
            registros = registros.value
        )
    }
}
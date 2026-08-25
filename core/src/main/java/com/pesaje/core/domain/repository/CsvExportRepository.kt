package com.pesaje.core.domain.repository

import android.content.Context
import com.pesaje.core.data.local.RegistroPesajeGanado

interface CsvExportRepository {
    fun exportarYCompartir(
        context: Context,
        nombreArchivo: String,
        registros: List<RegistroPesajeGanado>
    )
}
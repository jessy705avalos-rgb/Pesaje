package com.pesaje.camionero.domain.repository

import android.content.Context
import com.pesaje.core.data.local.RegistroPesajeTrailer

interface CsvExportTrailerRepository {
    fun exportarYCompartir(
        context: Context,
        nombreArchivo: String,
        registros: List<RegistroPesajeTrailer>
    )
}
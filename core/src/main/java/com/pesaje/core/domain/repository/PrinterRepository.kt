package com.pesaje.core.domain.repository

interface PrinterRepository {
    suspend fun printCattleTicket(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?,
        titulo: String,
        piePagina: String,
        fecha: String? = null,
        decimales: Int = 1
    ): Boolean

    suspend fun printTrailerEntrada(
        printerName: String,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String,
        titulo: String = "ENTRADA TRAILER",
        piePagina: String = "Conserve su ticket"
    ): Boolean

    suspend fun printTrailerSalida(
        printerName: String,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String,
        pesoSalida: Double,
        fechaSalida: String,
        pesoNeto: Double,
        titulo: String = "SALIDA TRAILER",
        piePagina: String = "Regrese pronto"
    ): Boolean
}
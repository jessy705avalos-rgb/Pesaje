package com.pesaje.core.domain.repository

interface PrinterRepository {
    suspend fun printCattleTicket(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ): Boolean

    suspend fun printTrailerEntrada(
        printerName: String,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String
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
        pesoNeto: Double
    ): Boolean
}
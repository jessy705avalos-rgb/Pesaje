package com.pesaje.core.domain.repository


interface PrinterRepository {
    suspend fun printCattleTicket(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ): Boolean

    suspend fun printTrailerTicket(
        printerName: String,
        placa: String,
        chofer: String,
        pesoBrutoKg: Double?,
        taraKg:Double?
    ): Boolean
}
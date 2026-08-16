package com.pesaje.core.data.repository

import com.pesaje.core.data.remote.PrinterBluetoothManager
import com.pesaje.core.data.remote.TicketPrinterHelper
import com.pesaje.core.domain.repository.PrinterRepository

class PrinterRepositoryImpl(
    private val printerBluetoothManager: PrinterBluetoothManager,
    private val printerHelper: TicketPrinterHelper
) : PrinterRepository {

    override suspend fun printCattleTicket(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ): Boolean {
        val socket = printerBluetoothManager.connectToPrinter(printerName) ?: return false
        val success = printerHelper.printCattleTicket(socket, areteId, sexo, pesoKg)
        try { socket.close() } catch (_: Exception) {}
        return success
    }

    override suspend fun printTrailerTicket(
        printerName: String,
        placa: String,
        chofer: String,
        pesoBrutoKg: Double?,
        taraKg: Double?
    ): Boolean {
        val socket = printerBluetoothManager.connectToPrinter(printerName) ?: return false
        val success = printerHelper.printTrailerTicket(socket, placa, chofer, pesoBrutoKg, taraKg)
        try { socket.close() } catch (_: Exception) {}
        return success
    }
}
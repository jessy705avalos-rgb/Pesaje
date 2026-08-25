package com.pesaje.core.data.repositoryImpl

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
    override suspend fun printTrailerEntrada(
        printerName: String,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String
    ): Boolean {
        val socket = printerBluetoothManager.connectToPrinter(printerName) ?: return false
        val success = printerHelper.printTrailerEntradaTicket(
            socket, placas, conductor, carga, pesoEntrada, fechaEntrada
        )
        try { socket.close() } catch (_: Exception) {}
        return success
    }

    override suspend fun printTrailerSalida(
        printerName: String,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String,
        pesoSalida: Double,
        fechaSalida: String,
        pesoNeto: Double
    ): Boolean {
        val socket = printerBluetoothManager.connectToPrinter(printerName) ?: return false
        val success = printerHelper.printTrailerSalidaTicket(
            socket, placas, conductor, carga, pesoEntrada, fechaEntrada, pesoSalida, fechaSalida, pesoNeto
        )
        try { socket.close() } catch (_: Exception) {}
        return success
    }
}
package com.pesaje.data.repositoryImpl

import android.bluetooth.BluetoothSocket
import com.pesaje.data.remote.TicketPrinterHelper
import com.pesaje.domain.repository.PrinterRepository

class PrinterRepositoryImpl(
    private val printerHelper: TicketPrinterHelper
) : PrinterRepository {
    override suspend fun printTicket(
        socket: BluetoothSocket?,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ): Boolean {
        return printerHelper.printTicket(socket, areteId, sexo, pesoKg)
    }
}
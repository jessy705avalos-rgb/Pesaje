package com.pesaje.domain.usecase

import android.bluetooth.BluetoothSocket
import com.pesaje.domain.repository.PrinterRepository

class PrintTicketUseCase(
    private val printerRepository: PrinterRepository
) {
    suspend operator fun invoke(
        socket: BluetoothSocket?,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ): Boolean {
        return printerRepository.printTicket(socket, areteId, sexo, pesoKg)
    }
}
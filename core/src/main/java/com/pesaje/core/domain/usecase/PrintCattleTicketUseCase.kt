package com.pesaje.core.domain.usecase

import android.bluetooth.BluetoothSocket
import com.pesaje.core.domain.repository.PrinterRepository

class PrintCattleTicketUseCase(
    private val printerRepository: PrinterRepository
) {
    suspend operator fun invoke(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ): Boolean {
        return printerRepository.printCattleTicket(printerName, areteId, sexo, pesoKg)
    }
}
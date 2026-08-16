package com.pesaje.core.domain.usecase

import com.pesaje.core.domain.repository.PrinterRepository

class PrintTrailerTicketUseCase(
    private val printerRepository: PrinterRepository
) {
    suspend operator fun invoke(
        printerName: String,
        placa: String,
        chofer: String,
        pesoBrutoKg: Double?,
        taraKg: Double?
    ): Boolean {
        return printerRepository.printTrailerTicket(printerName, placa, chofer, pesoBrutoKg, taraKg)
    }
}
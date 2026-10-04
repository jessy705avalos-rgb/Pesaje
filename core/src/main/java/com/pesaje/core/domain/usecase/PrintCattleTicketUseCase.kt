package com.pesaje.core.domain.usecase

import com.pesaje.core.domain.repository.PrinterRepository

class PrintCattleTicketUseCase(
    private val printerRepository: PrinterRepository
) {
    suspend operator fun invoke(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?,
        titulo: String = "PESAJE DE GANADO",
        piePagina: String = "Gracias por su visita",
        fecha: String? = null
    ): Boolean {
        return printerRepository.printCattleTicket(printerName, areteId, sexo, pesoKg, titulo, piePagina, fecha)
    }
}
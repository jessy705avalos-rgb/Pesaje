package com.pesaje.core.domain.usecase

import com.pesaje.core.domain.repository.PrinterRepository

class PrintTrailerEntradaUseCase(
    private val printerRepository: PrinterRepository
) {
    suspend operator fun invoke(
        printerName: String,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String
    ): Boolean {
        return printerRepository.printTrailerEntrada(
            printerName, placas, conductor, carga, pesoEntrada, fechaEntrada
        )
    }
}

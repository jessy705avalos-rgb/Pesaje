package com.pesaje.core.domain.usecase

import com.pesaje.core.domain.repository.PrinterRepository

class PrintTrailerSalidaUseCase(
    private val printerRepository: PrinterRepository
) {
    suspend operator fun invoke(
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
        return printerRepository.printTrailerSalida(
            printerName, placas, conductor, carga,
            pesoEntrada, fechaEntrada, pesoSalida, fechaSalida, pesoNeto
        )
    }
}
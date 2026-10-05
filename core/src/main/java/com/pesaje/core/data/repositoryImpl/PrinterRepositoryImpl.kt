package com.pesaje.core.data.repositoryImpl

import android.bluetooth.BluetoothSocket
import android.util.Log
import com.pesaje.core.data.remote.PrinterBluetoothManager
import com.pesaje.core.data.remote.TicketPrinterHelper
import com.pesaje.core.domain.repository.PrinterRepository
import kotlinx.coroutines.delay

private const val TAG = "PESAJE_PRINT"

// Pausa antes de cerrar para que la impresora termine de recibir los datos
private const val CLOSE_DELAY_MS = 800L

class PrinterRepositoryImpl(
    private val printerBluetoothManager: PrinterBluetoothManager,
    private val printerHelper: TicketPrinterHelper
) : PrinterRepository {

    // Conecta, imprime, espera y cierra. Deja logs para saber en qué paso falla.
    private suspend fun imprimir(
        printerName: String,
        tipo: String,
        bloque: (BluetoothSocket) -> Boolean
    ): Boolean {
        Log.d(TAG, "🖨️ [$tipo] conectando a '$printerName'...")
        val socket = printerBluetoothManager.connectToPrinter(printerName)
        if (socket == null) {
            Log.e(TAG, "❌ [$tipo] connectToPrinter devolvió null (no se pudo conectar)")
            return false
        }

        val success = bloque(socket)
        Log.d(TAG, if (success) "✅ [$tipo] datos enviados" else "❌ [$tipo] el helper devolvió false")

        delay(CLOSE_DELAY_MS)
        try { socket.close() } catch (_: Exception) {}
        return success
    }

    override suspend fun printCattleTicket(
        printerName: String,
        areteId: String,
        sexo: String,
        pesoKg: Double?,
        titulo: String,
        piePagina: String,
        fecha: String?,
        decimales: Int
    ): Boolean = imprimir(printerName, "GANADO") { socket ->
        printerHelper.printCattleTicket(socket, areteId, sexo, pesoKg, titulo, piePagina, fecha, decimales)
    }

    override suspend fun printTrailerEntrada(
        printerName: String,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String,
        titulo: String,
        piePagina: String
    ): Boolean = imprimir(printerName, "ENTRADA") { socket ->
        printerHelper.printTrailerEntradaTicket(
            socket, placas, conductor, carga, pesoEntrada, fechaEntrada, titulo, piePagina
        )
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
        pesoNeto: Double,
        titulo: String,
        piePagina: String
    ): Boolean = imprimir(printerName, "SALIDA") { socket ->
        printerHelper.printTrailerSalidaTicket(
            socket, placas, conductor, carga, pesoEntrada, fechaEntrada, pesoSalida, fechaSalida, pesoNeto, titulo, piePagina
        )
    }
}
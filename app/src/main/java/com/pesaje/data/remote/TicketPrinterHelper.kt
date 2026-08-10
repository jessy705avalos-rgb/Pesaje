package com.pesaje.data.remote

import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketPrinterHelper {

    @Suppress("MissingPermission")
    fun printTicket(
        socket: BluetoothSocket?,
        areteId: String,
        sexo: String,
        pesoKg: Double?,
        titulo: String = "PESAJE DE GANADO",
        piePagina: String = "Gracias por su visita"
    ): Boolean {
        if (socket == null || !socket.isConnected) return false

        return try {
            val outputStream: OutputStream = socket.outputStream
            val commands = ArrayList<Byte>()

            // Reset impresora
            commands.addAll(byteArrayOf(0x1B, 0x40).toTypedArray())

            // Alineación al centro
            commands.addAll(byteArrayOf(0x1B, 0x61, 0x01).toTypedArray())

            // --- TÍTULO (Grande y Negrita) ---
            commands.addAll(byteArrayOf(0x1B, 0x45, 0x01).toTypedArray()) // Negrita ON
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x11).toTypedArray()) // Doble alto + ancho
            commands.addAll("$titulo\n\n".toByteArray(Charsets.ISO_8859_1).toTypedArray())

            // --- RESTAURAR TAMAÑO Y FORMATO NORMAL ---
            commands.addAll(byteArrayOf(0x1B, 0x45, 0x00).toTypedArray()) // Negrita OFF
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x00).toTypedArray()) // Tamaño Normal

            // --- CUERPO DEL TICKET ---
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            val fechaActual = dateFormat.format(Date())
            val pesoTexto = pesoKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--.-"

            val areteLimpio = areteId.trim()
            val sexoLimpio = sexo.trim()

            val ticketContent = StringBuilder().apply {
                append("--------------------------------\n\n")

                if (areteLimpio.isNotEmpty()) {
                    append("Arete: $areteLimpio\n")
                }

                if (sexoLimpio.isNotEmpty()) {
                    append("Sexo: $sexoLimpio\n")
                }

                append("Peso: $pesoTexto kg\n")
                append("Fecha: $fechaActual\n\n")
                append("--------------------------------\n\n")
                append("$piePagina\n\n\n\n")
            }.toString()

            commands.addAll(ticketContent.toByteArray(Charsets.ISO_8859_1).toTypedArray())

            // Avance de línea / corte
            commands.addAll(byteArrayOf(0x1D, 0x56, 0x41, 0x10).toTypedArray())

            outputStream.write(commands.toByteArray())
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
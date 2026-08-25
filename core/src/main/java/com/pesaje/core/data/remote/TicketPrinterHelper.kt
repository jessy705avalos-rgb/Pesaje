package com.pesaje.core.data.remote

import android.bluetooth.BluetoothSocket
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketPrinterHelper {

    @Suppress("MissingPermission")
    fun printCattleTicket(
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

    @Suppress("MissingPermission")
    fun printTrailerEntradaTicket(
        socket: BluetoothSocket?,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String
    ): Boolean {
        if (socket == null || !socket.isConnected) return false
        return try {
            val outputStream: OutputStream = socket.outputStream
            val commands = ArrayList<Byte>()

            commands.addAll(byteArrayOf(0x1B, 0x40).toTypedArray())
            commands.addAll(byteArrayOf(0x1B, 0x61, 0x01).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x01).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x11).toTypedArray())
            commands.addAll("ENTRADA TRAILER\n\n".toByteArray(Charsets.ISO_8859_1).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x00).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x00).toTypedArray())

            val pesoStr = String.format(Locale.US, "%.1f", pesoEntrada)

            val ticketContent = StringBuilder().apply {
                append("--------------------------------\n\n")
                if (placas.trim().isNotEmpty()) append("Placas: ${placas.trim()}\n")
                if (conductor.trim().isNotEmpty()) append("Conductor(a): ${conductor.trim()}\n")
                if (carga.trim().isNotEmpty()) append("Carga: ${carga.trim()}\n")
                append("Peso: $pesoStr kg\n")
                append("Fecha de entrada: $fechaEntrada\n\n")
                append("--------------------------------\n\n")
                append("Conserve su ticket\n\n\n\n")
            }.toString()

            commands.addAll(ticketContent.toByteArray(Charsets.ISO_8859_1).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x56, 0x41, 0x10).toTypedArray())

            outputStream.write(commands.toByteArray())
            outputStream.flush()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    @Suppress("MissingPermission")
    fun printTrailerSalidaTicket(
        socket: BluetoothSocket?,
        placas: String,
        conductor: String,
        carga: String,
        pesoEntrada: Double,
        fechaEntrada: String,
        pesoSalida: Double,
        fechaSalida: String,
        pesoNeto: Double
    ): Boolean {
        if (socket == null || !socket.isConnected) return false
        return try {
            val outputStream: OutputStream = socket.outputStream
            val commands = ArrayList<Byte>()

            commands.addAll(byteArrayOf(0x1B, 0x40).toTypedArray())
            commands.addAll(byteArrayOf(0x1B, 0x61, 0x01).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x01).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x11).toTypedArray())
            commands.addAll("SALIDA TRAILER\n\n".toByteArray(Charsets.ISO_8859_1).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x00).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x00).toTypedArray())

            val entradaStr = String.format(Locale.US, "%.1f", pesoEntrada)
            val salidaStr = String.format(Locale.US, "%.1f", pesoSalida)
            val netoStr = String.format(Locale.US, "%.2f", pesoNeto)

            val ticketContent = StringBuilder().apply {
                append("--------------------------------\n\n")
                if (placas.trim().isNotEmpty()) append("Placas: ${placas.trim()}\n")
                if (conductor.trim().isNotEmpty()) append("Conductor(a): ${conductor.trim()}\n")
                if (carga.trim().isNotEmpty()) append("Carga: ${carga.trim()}\n\n")
                append("Fecha de entrada: $fechaEntrada\n")
                append("Fecha de salida: $fechaSalida\n\n")
                append("Peso de entrada: $entradaStr kg\n")
                append("Peso de salida: $salidaStr kg\n")
                append("Peso Neto: $netoStr kg\n\n")
                append("--------------------------------\n\n")
                append("Regrese pronto\n\n\n\n")
            }.toString()

            commands.addAll(ticketContent.toByteArray(Charsets.ISO_8859_1).toTypedArray())
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
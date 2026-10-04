package com.pesaje.core.data.remote

import android.bluetooth.BluetoothSocket
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketPrinterHelper {

    // Caracteres máximos por línea en tamaño doble (papel 58 mm = 32 cols normales -> 16 dobles).
    // Si usas papel de 80 mm cambia a 24.
    private val maxCharsTituloGrande = 16
    private val regexSegundos = Regex("""(\d{1,2}:\d{2}):\d{2}""")

    private fun ByteArrayOutputStream.cmd(vararg bytes: Int) {
        write(ByteArray(bytes.size) { bytes[it].toByte() })
    }

    private fun ByteArrayOutputStream.text(s: String) {
        write(s.toByteArray(Charsets.ISO_8859_1))
    }

    // Etiqueta en NEGRITA, contenido SIN negrita
    private fun ByteArrayOutputStream.campo(etiqueta: String, valor: String) {
        cmd(0x1B, 0x45, 0x01)   // Negrita ON  -> etiqueta
        text("$etiqueta ")
        cmd(0x1B, 0x45, 0x00)   // Negrita OFF -> contenido
        text("$valor\n")
    }

    private fun normalizarSaltos(s: String): String =
        s.replace("\r\n", "\n").replace('\r', '\n').trim()

    // "03/10/2026 15:12:45" -> "03/10/2026 15:12"
    private fun sinSegundos(fecha: String): String =
        regexSegundos.replace(fecha.trim(), "\$1")
    @Suppress("MissingPermission")
    fun printCattleTicket(
        socket: BluetoothSocket?,
        areteId: String,
        sexo: String,
        pesoKg: Double?,
        titulo: String = "PESAJE DE GANADO",
        piePagina: String = "Gracias por su visita",
        fecha: String? = null
    ): Boolean {
        if (socket == null || !socket.isConnected) return false

        return try {
            val outputStream: OutputStream = socket.outputStream
            val out = ByteArrayOutputStream()

            val tituloLimpio = normalizarSaltos(titulo)
            val pieLimpio = normalizarSaltos(piePagina)

            // Doble alto/ancho solo si es UNA línea corta; si no, tamaño normal
            val tituloGrande = !tituloLimpio.contains('\n') && tituloLimpio.length <= maxCharsTituloGrande

            out.cmd(0x1B, 0x40)          // Reset
            out.cmd(0x1B, 0x61, 0x01)    // Centrado

            // --- TÍTULO (negrita; grande o normal según longitud/líneas) ---
            out.cmd(0x1B, 0x45, 0x01)
            out.cmd(0x1D, 0x21, if (tituloGrande) 0x11 else 0x00)
            out.text("$tituloLimpio\n\n")

            // --- RESTAURAR FORMATO NORMAL ---
            out.cmd(0x1B, 0x45, 0x00)
            out.cmd(0x1D, 0x21, 0x00)

            // --- CUERPO ---
            val fechaImpresion = sinSegundos(
                fecha?.takeIf { it.isNotBlank() }
                    ?: SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
            )
            val pesoTexto = pesoKg?.let { String.format(Locale.US, "%.1f", it) } ?: "--.-"
            val areteLimpio = areteId.trim()
            val sexoLimpio = sexo.trim()

            out.text("--------------------------------\n\n")
            if (areteLimpio.isNotEmpty()) out.campo("Arete:", areteLimpio)
            if (sexoLimpio.isNotEmpty()) out.campo("Sexo:", sexoLimpio)
            out.campo("Peso:", "$pesoTexto kg")
            out.campo("Fecha:", fechaImpresion)
            out.text("\n--------------------------------\n\n")

            // --- PIE (tamaño normal, respeta saltos de línea) ---
            out.text("$pieLimpio\n\n\n\n")

            // Avance / corte
            out.cmd(0x1D, 0x56, 0x41, 0x10)

            outputStream.write(out.toByteArray())
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
        fechaEntrada: String,
        titulo: String = "ENTRADA TRAILER",
        piePagina: String = "Conserve su ticket"
    ): Boolean {
        if (socket == null || !socket.isConnected) return false
        return try {
            val outputStream: OutputStream = socket.outputStream
            val commands = ArrayList<Byte>()

            commands.addAll(byteArrayOf(0x1B, 0x40).toTypedArray())
            commands.addAll(byteArrayOf(0x1B, 0x61, 0x01).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x01).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x11).toTypedArray())
            commands.addAll("$titulo\n\n".toByteArray(Charsets.ISO_8859_1).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x00).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x00).toTypedArray())

            val pesoStr = String.format(Locale.US, "%.0f", pesoEntrada)

            val ticketContent = StringBuilder().apply {
                append("--------------------------------\n\n")
                if (placas.trim().isNotEmpty()) append("Placas: ${placas.trim()}\n")
                if (conductor.trim().isNotEmpty()) append("Conductor(a): ${conductor.trim()}\n")
                if (carga.trim().isNotEmpty()) append("Carga: ${carga.trim()}\n")
                append("Peso: $pesoStr kg\n")
                append("Fecha de entrada: $fechaEntrada\n\n")
                append("--------------------------------\n\n")
                append("$piePagina\n\n\n\n")
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
        pesoNeto: Double,
        titulo: String = "SALIDA TRAILER",
        piePagina: String = "Regrese pronto"
    ): Boolean {
        if (socket == null || !socket.isConnected) return false
        return try {
            val outputStream: OutputStream = socket.outputStream
            val commands = ArrayList<Byte>()

            commands.addAll(byteArrayOf(0x1B, 0x40).toTypedArray())
            commands.addAll(byteArrayOf(0x1B, 0x61, 0x01).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x01).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x11).toTypedArray())
            commands.addAll("$titulo\n\n".toByteArray(Charsets.ISO_8859_1).toTypedArray())

            commands.addAll(byteArrayOf(0x1B, 0x45, 0x00).toTypedArray())
            commands.addAll(byteArrayOf(0x1D, 0x21, 0x00).toTypedArray())

            val entradaStr = String.format(Locale.US, "%.0f", pesoEntrada)
            val salidaStr = String.format(Locale.US, "%.0f", pesoSalida)
            val netoStr = String.format(Locale.US, "%.0f", pesoNeto)

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
                append("$piePagina\n\n\n\n")
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
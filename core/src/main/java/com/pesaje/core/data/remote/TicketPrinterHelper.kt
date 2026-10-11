package com.pesaje.core.data.remote

import android.bluetooth.BluetoothSocket
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TicketPrinterHelper {

    private companion object {
        // Tabla de caracteres de la impresora (ESC t n).
        // Prueba en este orden: 16 (windows-1252), 2 (Cp850), 19 (Cp858)
        const val CODEPAGE_N = 2

        // Papel ~80 mm = 48 columnas normales (24 en tamaño doble).
        // Si la impresora solo imprime 72 mm útiles, prueba con 42 y 21.
        const val COLUMNAS = 48
        const val MAX_CHARS_TITULO_GRANDE = 24

        // Cuántos espacios se recorre el cuerpo hacia la izquierda respecto al centro exacto.
        // 0 = centrado perfecto. Súbelo/bájalo a tu gusto.
        const val AJUSTE_IZQUIERDA = 2

        // Line feeds finales para que el ticket salga completo (sube a 8 o 10 si aún queda corto)
        const val LINEAS_AVANCE_FINAL = 6

        const val CRLF = "\r\n"
    }

    // Debe coincidir con CODEPAGE_N: 16 -> "windows-1252", 2 -> "Cp850", 19 -> "Cp858"
    private val charset = charset("Cp850")

    private val regexSegundos = Regex("""(\d{1,2}:\d{2}):\d{2}""")

    // ==========================================
    // PRIMITIVAS ESC/POS
    // ==========================================
    private fun ByteArrayOutputStream.cmd(vararg bytes: Int) {
        write(ByteArray(bytes.size) { bytes[it].toByte() })
    }

    /** Convierte el texto a bytes con la tabla de caracteres elegida (acentos y ñ). */
    private fun ByteArrayOutputStream.texto(s: String) {
        write(s.toByteArray(charset))
    }

    private fun ByteArrayOutputStream.reset() {
        cmd(0x1B, 0x40)                 // ESC @ : reset
        cmd(0x1C, 0x2E)                 // FS .  : cancela modo de caracteres chinos
        cmd(0x1B, 0x74, CODEPAGE_N)     // ESC t n : tabla de caracteres
    }

    private fun ByteArrayOutputStream.alinear(n: Int) = cmd(0x1B, 0x61, n)      // 0 izq, 1 centro
    private fun ByteArrayOutputStream.negrita(on: Boolean) = cmd(0x1B, 0x45, if (on) 1 else 0)
    private fun ByteArrayOutputStream.tamanoDoble(on: Boolean) = cmd(0x1D, 0x21, if (on) 0x11 else 0x00)

    /** Cada salto de línea sale como CR + LF (ASCII 13 + 10) */
    private fun ByteArrayOutputStream.linea(s: String = "") = texto(s + CRLF)

    // ==========================================
    // UTILIDADES DE TEXTO
    // ==========================================

    /** Unifica saltos de línea a \n SIN recortar nada: cada salto del usuario cuenta,
     *  incluidos los del inicio y del final. */
    private fun normalizarSaltos(s: String): String =
        s.replace("\r\n", "\n").replace('\r', '\n')

    /** Imprime cada línea del texto con CR+LF, respetando todos los saltos del usuario. */
    private fun ByteArrayOutputStream.textoMultilinea(s: String) {
        if (s.isEmpty()) return
        s.split('\n').forEach { linea(it) }
    }

    // "03/10/2026 15:12:45" -> "03/10/2026 15:12"
    private fun sinSegundos(fecha: String): String =
        regexSegundos.replace(fecha.trim(), "\$1")

    private fun kg(valor: Double, decimales: Int = 0): String =
        String.format(Locale.US, "%.${decimales}f", valor)

    // ==========================================
    // BLOQUES REUTILIZABLES
    // ==========================================

    /**
     * ENCABEZADO: centrado y en negrita.
     * Tamaño doble solo si es UNA línea corta; si tiene saltos de línea (o es larga),
     * todo sale en tamaño normal, igual que el pie.
     */
    private fun ByteArrayOutputStream.encabezado(titulo: String) {
        val limpio = normalizarSaltos(titulo)
        val grande = !limpio.contains('\n') && limpio.length <= MAX_CHARS_TITULO_GRANDE

        alinear(1)
        negrita(true)
        tamanoDoble(grande)
        textoMultilinea(limpio)
        linea()

        // Restaurar formato normal
        negrita(false)
        tamanoDoble(false)
    }

    /** PIE: centrado, letra normal, respetando cada salto de línea. */
    private fun ByteArrayOutputStream.pie(piePagina: String) {
        alinear(1)
        negrita(false)
        tamanoDoble(false)
        textoMultilinea(normalizarSaltos(piePagina))
    }

    private fun ByteArrayOutputStream.separador() {
        alinear(1)
        linea("-".repeat(COLUMNAS))
    }

    /**
     * CUERPO: el bloque completo queda centrado en el ticket (con un pequeño ajuste hacia
     * la izquierda). Adentro, las etiquetas (negrita) van alineadas a la izquierda y los
     * valores arrancan todos en la misma columna. Fila ("" to "") = línea en blanco.
     * Si un valor es demasiado largo, continúa en la línea siguiente bajo la misma columna.
     */
    private fun ByteArrayOutputStream.cuerpo(filas: List<Pair<String, String>>) {
        alinear(0) // izquierda; el centrado del bloque se logra con el margen calculado

        val reales = filas.filter { it.first.isNotEmpty() }
        if (reales.isEmpty()) return

        val anchoEtiqueta = reales.maxOf { it.first.length } + 1
        val maxValor = reales.maxOf { it.second.length }
        val anchoValor = maxValor.coerceAtMost(COLUMNAS - anchoEtiqueta).coerceAtLeast(1)
        val anchoBloque = anchoEtiqueta + anchoValor

        val margen = " ".repeat(((COLUMNAS - anchoBloque) / 2 - AJUSTE_IZQUIERDA).coerceAtLeast(0))
        val sangria = margen + " ".repeat(anchoEtiqueta)

        for ((etiqueta, valor) in filas) {
            if (etiqueta.isEmpty()) {
                linea()
                continue
            }
            val partes = valor.chunked(anchoValor).ifEmpty { listOf("") }

            texto(margen)
            negrita(true)                          // etiqueta en negrita
            texto(etiqueta.padEnd(anchoEtiqueta))
            negrita(false)                         // valor sin negrita
            linea(partes.first())

            partes.drop(1).forEach { linea(sangria + it) }
        }
    }

    /** Avance final de papel + corte (NO lo quites: sin él la GLPrinter deja el ticket a medias). */
    private fun ByteArrayOutputStream.finalizar() {
        alinear(0)
        repeat(LINEAS_AVANCE_FINAL) { linea() }
        cmd(0x1D, 0x56, 0x42, 0x00)                // GS V 66 0
    }

    @Suppress("MissingPermission")
    private fun imprimir(
        socket: BluetoothSocket?,
        titulo: String,
        piePagina: String,
        filas: List<Pair<String, String>>
    ): Boolean {
        if (socket == null || !socket.isConnected) return false
        return try {
            val out = ByteArrayOutputStream()

            out.reset()                             // ESC @ obligatorio al inicio
            out.encabezado(titulo)

            out.separador()
            out.linea()
            out.cuerpo(filas)
            out.linea()
            out.separador()
            out.linea()

            out.pie(piePagina)
            out.finalizar()

            socket.outputStream.apply {
                write(out.toByteArray())
                flush()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // ==========================================
    // TICKETS
    // ==========================================
    fun printCattleTicket(
        socket: BluetoothSocket?,
        areteId: String,
        sexo: String,
        pesoKg: Double?,
        titulo: String = "PESAJE DE GANADO",
        piePagina: String = "Gracias por su visita",
        fecha: String? = null,
        decimales: Int = 1
    ): Boolean {
        val fechaImpresion = sinSegundos(
            fecha?.takeIf { it.isNotBlank() }
                ?: SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        )
        val pesoTexto = pesoKg?.let { kg(it, decimales) } ?: "--.-"

        val filas = buildList {
            if (areteId.isNotBlank()) add("Arete:" to areteId.trim())
            if (sexo.isNotBlank()) add("Sexo:" to sexo.trim())
            add("Peso:" to "$pesoTexto kg")
            add("Fecha:" to fechaImpresion)
        }
        return imprimir(socket, titulo, piePagina, filas)
    }

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
        val filas = buildList {
            if (placas.isNotBlank()) add("Placas:" to placas.trim())
            if (conductor.isNotBlank()) add("Conductor(a):" to conductor.trim())
            if (carga.isNotBlank()) add("Carga:" to carga.trim())
            add("Peso:" to "${kg(pesoEntrada)} kg")
            add("Fecha de entrada:" to sinSegundos(fechaEntrada))
        }
        return imprimir(socket, titulo, piePagina, filas)
    }

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
        val blanco = "" to ""
        val filas = buildList {
            if (placas.isNotBlank()) add("Placas:" to placas.trim())
            if (conductor.isNotBlank()) add("Conductor(a):" to conductor.trim())
            if (carga.isNotBlank()) add("Carga:" to carga.trim())
            add(blanco)
            add("Fecha de entrada:" to sinSegundos(fechaEntrada))
            add("Fecha de salida:" to sinSegundos(fechaSalida))
            add(blanco)
            add("Peso de entrada:" to "${kg(pesoEntrada)} kg")
            add("Peso de salida:" to "${kg(pesoSalida)} kg")
            add("Peso Neto:" to "${kg(pesoNeto)} kg")
        }
        return imprimir(socket, titulo, piePagina, filas)
    }
}
package com.pesaje.camionero.data.repositoryImpl

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pesaje.camionero.domain.repository.CsvExportTrailerRepository
import com.pesaje.core.data.local.RegistroPesajeTrailer
import java.io.File
import java.io.FileWriter

class CsvExportTrailerRepositoryImpl : CsvExportTrailerRepository {
    // Quita / \ : * ? " < > | y caracteres de control; evita nombres vacíos
    private fun sanitizarNombreArchivo(nombre: String): String {
        val sinExtension = nombre.trim().let {
            if (it.endsWith(".csv", ignoreCase = true)) it.dropLast(4) else it
        }
        val limpio = sinExtension
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]+"), "_")
            .trim(' ', '.', '_')
            .take(100)
        return (limpio.ifEmpty { "registros_trailer" }) + ".csv"
    }

    // Función auxiliar para separar fecha y hora a partir de una cadena "dd/MM/yyyy HH:mm"
    private fun separarFechaYHora(fechaCompleta: String?): Pair<String, String> {
        val texto = fechaCompleta?.trim().orEmpty()
        if (texto.isEmpty()) return Pair("", "")
        val espacioIndex = texto.indexOf(' ')
        return if (espacioIndex != -1) {
            Pair(texto.substring(0, espacioIndex), texto.substring(espacioIndex + 1))
        } else {
            Pair(texto, "")
        }
    }

    override fun exportarYCompartir(
        context: Context,
        nombreArchivo: String,
        registros: List<RegistroPesajeTrailer>
    ) {
        val nombreLimpio = sanitizarNombreArchivo(nombreArchivo)

        val folder = File(context.filesDir, "csv_exports")
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, nombreLimpio)

        try {
            val writer = FileWriter(file)
            writer.write("\uFEFF")

            // Encabezados con las 4 columnas de fecha y hora separadas
            writer.append("ID,Placas,Conductor,Carga,Peso Entrada,Fecha Entrada,Hora Entrada,Peso Salida,Fecha Salida,Hora Salida,Estado\n")

            registros.forEach { r ->
                val entradaEnt = r.pesoEntrada.toInt()
                val salidaEnt = r.pesoSalida?.toInt()?.toString() ?: ""

                val (fechaEntradaSolo, horaEntradaSolo) = separarFechaYHora(r.fechaEntrada)
                val (fechaSalidaSolo, horaSalidaSolo) = separarFechaYHora(r.fechaSalida)

                writer.append(
                    "${r.id},\"${r.placas}\",\"${r.conductor}\",\"${r.carga}\"," +
                            "$entradaEnt,\"$fechaEntradaSolo\",\"$horaEntradaSolo\"," +
                            "$salidaEnt,\"$fechaSalidaSolo\",\"$horaSalidaSolo\"," +
                            "${if (r.estaAbierto) "Abierto" else "Cerrado"}\n"
                )
            }
            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(
                context, "${context.packageName}.fileprovider", file
            )
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(shareIntent, "Compartir CSV con...").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val resInfoList = context.packageManager.queryIntentActivities(
                chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                context.grantUriPermission(
                    resolveInfo.activityInfo.packageName,
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
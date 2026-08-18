package com.pesaje.presentation.ui.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pesaje.core.data.local.RegistroPesajeGanado
import java.io.File
import java.io.FileWriter

object CsvExporter {

    fun exportarYCompartir(
        context: Context,
        nombreArchivo: String,
        registros: List<RegistroPesajeGanado>
    ) {
        val nombreLimpio = if (nombreArchivo.endsWith(".csv", ignoreCase = true)) {
            nombreArchivo
        } else {
            "$nombreArchivo.csv"
        }

        // Usar filesDir para mejor compatibilidad con WhatsApp y otras apps
        val folder = File(context.filesDir, "csv_exports")
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, nombreLimpio)

        try {
            val writer = FileWriter(file)
            writer.append("ID,Arete,Sexo,Peso (kg),Fecha,Hora\n")

            registros.forEach { registro ->
                val textoFecha = registro.fecha.trim()
                val espacioIndex = textoFecha.indexOf(' ')
                val fechaSolo = if (espacioIndex != -1) textoFecha.substring(0, espacioIndex) else textoFecha
                val horaSolo = if (espacioIndex != -1) textoFecha.substring(espacioIndex + 1) else ""

                writer.append("${registro.id},\"${registro.arete}\",\"${registro.sexo}\",${registro.peso},\"$fechaSolo\",\"$horaSolo\"\n")
            }

            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/comma-separated-values" // Tipo MIME mas compatible con WhatsApp
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Compartir CSV con...").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // Otorga permisos explícitos sobre la URI a la app de destino
            val resInfoList = context.packageManager.queryIntentActivities(chooser, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            for (resolveInfo in resInfoList) {
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    packageName,
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
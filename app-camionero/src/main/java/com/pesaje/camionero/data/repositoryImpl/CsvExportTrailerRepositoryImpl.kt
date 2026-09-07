package com.pesaje.camionero.data.repositoryImpl

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pesaje.camionero.domain.repository.CsvExportTrailerRepository
import com.pesaje.core.data.local.RegistroPesajeTrailer
import java.io.File
import java.io.FileWriter

class CsvExportTrailerRepositoryImpl : CsvExportTrailerRepository {
    override fun exportarYCompartir(
        context: Context,
        nombreArchivo: String,
        registros: List<RegistroPesajeTrailer>
    ) {
        val nombreLimpio =
            if (nombreArchivo.endsWith(".csv", ignoreCase = true)) {
                nombreArchivo
            } else {
                "$nombreArchivo.csv"
            }

        val folder = File(context.filesDir, "csv_exports")
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, nombreLimpio)

        try {
            val writer = FileWriter(file)
            writer.append("ID,Placas,Conductor,Carga,Peso Entrada,Fecha Entrada,Peso Salida,Fecha Salida,Estado\n")

            registros.forEach { r ->
                writer.append(
                    "${r.id},\"${r.placas}\",\"${r.conductor}\",\"${r.carga}\"," +
                            "${r.pesoEntrada},\"${r.fechaEntrada}\"," +
                            "${r.pesoSalida ?: ""},\"${r.fechaSalida ?: ""}\"," +
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
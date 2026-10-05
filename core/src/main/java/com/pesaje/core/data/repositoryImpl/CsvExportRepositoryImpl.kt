package com.pesaje.core.data.repositoryImpl

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.pesaje.core.data.local.RegistroPesajeGanado
import com.pesaje.core.domain.repository.CsvExportRepository
import java.io.File
import java.io.FileWriter

class CsvExportRepositoryImpl : CsvExportRepository {

    // Quita / \ : * ? " < > | y caracteres de control; evita nombres vacíos
    private fun sanitizarNombreArchivo(nombre: String): String {
        val sinExtension = nombre.trim().let {
            if (it.endsWith(".csv", ignoreCase = true)) it.dropLast(4) else it
        }
        val limpio = sinExtension
            .replace(Regex("[\\\\/:*?\"<>|\\p{Cntrl}]+"), "_")
            .trim(' ', '.', '_')
            .take(100)
        return (limpio.ifEmpty { "registros_ganado" }) + ".csv"
    }

    override fun exportarYCompartir(
        context: Context,
        nombreArchivo: String,
        registros: List<RegistroPesajeGanado>
    ) {
        val nombreLimpio = sanitizarNombreArchivo(nombreArchivo)

        val folder = File(context.filesDir, "csv_exports")
        if (!folder.exists()) folder.mkdirs()

        val file = File(folder, nombreLimpio)

        try {
            val writer = FileWriter(file)

            // 1. BOM para que Excel reconozca UTF-8 (acentos, caracteres)
            writer.write("\uFEFF")

            // 2. Encabezados
            writer.append("ID,Arete,Sexo,Peso (kg),Fecha,Hora\n")

            registros.forEach { registro ->
                val textoFecha = registro.fecha.trim()
                val espacioIndex = textoFecha.indexOf(' ')
                val fechaSolo =
                    if (espacioIndex != -1) textoFecha.substring(0, espacioIndex) else textoFecha
                val horaSolo =
                    if (espacioIndex != -1) textoFecha.substring(espacioIndex + 1) else ""

                val pesoFormateado = java.math.BigDecimal.valueOf(registro.peso).toPlainString()
                writer.append("${registro.id},\"${registro.arete}\",\"${registro.sexo}\",$pesoFormateado,\"$fechaSolo\",\"$horaSolo\"\n")
            }

            writer.flush()
            writer.close()

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // 3. MIME estándar para apps de hojas de cálculo
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
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
                val packageName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(chooser)

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
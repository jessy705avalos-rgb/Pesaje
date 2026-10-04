package com.pesaje.core.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.pesaje.core.domain.model.IndicatorProfiles
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "ticket_settings")

class SettingsDataStore(private val context: Context) {

    companion object {
        // --- GANADO ---
        val TICKET_HEADER_KEY = stringPreferencesKey("ticket_header")
        val TICKET_FOOTER_KEY = stringPreferencesKey("ticket_footer")

        // --- TRÁILER ENTRADA ---
        val TRAILER_ENTRADA_HEADER_KEY = stringPreferencesKey("trailer_entrada_header")
        val TRAILER_ENTRADA_FOOTER_KEY = stringPreferencesKey("trailer_entrada_footer")

        // --- TRÁILER SALIDA ---
        val TRAILER_SALIDA_HEADER_KEY = stringPreferencesKey("trailer_salida_header")
        val TRAILER_SALIDA_FOOTER_KEY = stringPreferencesKey("trailer_salida_footer")

        // --- DISPOSITIVOS ---
        val INDICATOR_DEVICE_KEY = stringPreferencesKey("indicator_device")
        val INDICATOR_FORMAT_KEY = stringPreferencesKey("indicator_format")
        val PRINTER_DEVICE_KEY = stringPreferencesKey("printer_device")

        // Formatos disponibles (vienen de IndicatorProfiles)
        val MODELOS_FORMATO = IndicatorProfiles.nombres

        const val DEFAULT_INDICATOR_DEVICE = "BL243902"
        const val DEFAULT_PRINTER_DEVICE = "Printer001"
    }

    // ==========================================
    // FLUJOS DE GANADO
    // ==========================================
    val headerFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TICKET_HEADER_KEY] ?: "PESAJE DE GANADO"
    }

    val footerFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TICKET_FOOTER_KEY] ?: "Gracias por su visita"
    }

    suspend fun saveTicketSettings(header: String, footer: String) {
        context.dataStore.edit { prefs ->
            prefs[TICKET_HEADER_KEY] = header
            prefs[TICKET_FOOTER_KEY] = footer
        }
    }

    // ==========================================
    // DISPOSITIVOS (INDICADOR E IMPRESORA)
    // ==========================================
    val indicatorDeviceFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[INDICATOR_DEVICE_KEY] ?: DEFAULT_INDICATOR_DEVICE
    }

    val indicatorFormatFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[INDICATOR_FORMAT_KEY]?.takeIf { it in MODELOS_FORMATO }
            ?: IndicatorProfiles.LP7516.nombre
    }

    val printerDeviceFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[PRINTER_DEVICE_KEY] ?: DEFAULT_PRINTER_DEVICE
    }

    suspend fun saveDeviceSettings(
        indicatorFormat: String,
        indicatorDevice: String,
        printerDevice: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[INDICATOR_FORMAT_KEY] = indicatorFormat
            prefs[INDICATOR_DEVICE_KEY] = indicatorDevice
            prefs[PRINTER_DEVICE_KEY] = printerDevice
        }
    }

    // ==========================================
    // FLUJOS DE TRÁILER (ENTRADA Y SALIDA)
    // ==========================================
    val trailerEntradaHeaderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TRAILER_ENTRADA_HEADER_KEY] ?: "ENTRADA TRAILER"
    }

    val trailerEntradaFooterFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TRAILER_ENTRADA_FOOTER_KEY] ?: "Conserve su ticket"
    }

    val trailerSalidaHeaderFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TRAILER_SALIDA_HEADER_KEY] ?: "SALIDA TRAILER"
    }

    val trailerSalidaFooterFlow: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[TRAILER_SALIDA_FOOTER_KEY] ?: "Regrese pronto"
    }

    suspend fun saveTrailerTicketSettings(
        entradaHeader: String,
        entradaFooter: String,
        salidaHeader: String,
        salidaFooter: String
    ) {
        context.dataStore.edit { prefs ->
            prefs[TRAILER_ENTRADA_HEADER_KEY] = entradaHeader
            prefs[TRAILER_ENTRADA_FOOTER_KEY] = entradaFooter
            prefs[TRAILER_SALIDA_HEADER_KEY] = salidaHeader
            prefs[TRAILER_SALIDA_FOOTER_KEY] = salidaFooter
        }
    }
}
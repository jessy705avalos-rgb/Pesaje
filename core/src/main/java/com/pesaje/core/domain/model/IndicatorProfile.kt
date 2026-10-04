package com.pesaje.core.domain.model

data class IndicatorProfile(
    val nombre: String,
    val comandoLeer: String?,   // null = el indicador no necesita comando
    val comandoTara: String?,
    val comandoZero: String?,
    val parser: (String) -> WeightReading?
)

object IndicatorProfiles {

    const val NOMBRE_AUTO = "Automático (detecta el formato)"
    const val NOMBRE_LP7516 = "LP7516 (LOCOSC)"

    // Número con signo opcional y unidad opcional: "+0012.5kg", "12,5 kg", "- 3.0lb"
    private val NUM_REGEX = Regex(
        """([-+]?)\s*(\d+(?:[.,]\d+)?)\s*(?:(kg|lbs?|g)\b)?""",
        RegexOption.IGNORE_CASE
    )
    private val UNSTABLE_REGEX = Regex("""\b(US|UN|UNSTABLE)\b""", RegexOption.IGNORE_CASE)
    private val NET_REGEX = Regex("""\b(NT|NET)\b""", RegexOption.IGNORE_CASE)
    private val OVERLOAD_REGEX = Regex("""\bOL\b""", RegexOption.IGNORE_CASE)

    // ---------- Perfil exacto del LP7516 (igual que estaba) ----------
    private fun parseLp7516(raw: String): WeightReading? {
        val cleaned = raw.trim()
        val isStable = cleaned.startsWith("ST")
        val isNet = cleaned.contains("NT")
        val match = Regex("[-+]?\\d+\\.?\\d*").find(cleaned.substringAfter("+").ifEmpty { cleaned })
        val kg = match?.value?.toDoubleOrNull() ?: return null
        return WeightReading(kilograms = kg, isStable = isStable, isNet = isNet)
    }

    // ---------- Perfil automático ----------
    private fun parseAuto(raw: String): WeightReading? {
        val cleaned = raw.trim()
        if (cleaned.isEmpty()) return null
        if (OVERLOAD_REGEX.containsMatchIn(cleaned)) return null // sobrecarga: no es un peso válido

        val matches = NUM_REGEX.findAll(cleaned).toList()
        // Prioriza el número que trae unidad pegada; si no, toma el último
        val m = matches.firstOrNull { it.groupValues[3].isNotEmpty() }
            ?: matches.lastOrNull()
            ?: return null

        var valor = m.groupValues[2].replace(',', '.').toDoubleOrNull() ?: return null
        if (m.groupValues[1] == "-") valor = -valor

        when (m.groupValues[3].lowercase()) {
            "lb", "lbs" -> valor *= 0.45359237
            "g" -> valor /= 1000.0
        }

        // Si la trama no trae indicador de estabilidad se asume estable
        val isStable = !UNSTABLE_REGEX.containsMatchIn(cleaned) && !cleaned.contains('?')
        val isNet = NET_REGEX.containsMatchIn(cleaned)

        return WeightReading(kilograms = valor, isStable = isStable, isNet = isNet)
    }

    val AUTO = IndicatorProfile(
        nombre = NOMBRE_AUTO,
        comandoLeer = "R",
        comandoTara = "T",
        comandoZero = "Z",
        parser = ::parseAuto
    )

    val LP7516 = IndicatorProfile(
        nombre = NOMBRE_LP7516,
        comandoLeer = "R",
        comandoTara = "T",
        comandoZero = "Z",
        parser = ::parseLp7516
    )

    // Para soportar un modelo nuevo: crea su IndicatorProfile y agrégalo aquí
    val todos: List<IndicatorProfile> = listOf(AUTO, LP7516)
    val nombres: List<String> get() = todos.map { it.nombre }

    fun fromModel(nombre: String): IndicatorProfile =
        todos.firstOrNull { it.nombre == nombre } ?: AUTO
}
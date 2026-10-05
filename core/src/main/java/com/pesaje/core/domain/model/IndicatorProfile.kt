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

    // Cuenta cuántos decimales trae el número tal como lo mandó el indicador
    private fun contarDecimales(txt: String): Int {
        val i = txt.indexOfFirst { it == '.' || it == ',' }
        return if (i < 0) 0 else txt.length - i - 1
    }

    // ---------- Perfil exacto del LP7516 ----------
    private fun parseLp7516(raw: String): WeightReading? {
        val cleaned = raw.trim()
        val isStable = cleaned.startsWith("ST")
        val isNet = cleaned.contains("NT")
        val match = Regex("[-+]?\\d+\\.?\\d*").find(cleaned.substringAfter("+").ifEmpty { cleaned })
            ?: return null
        val kg = match.value.toDoubleOrNull() ?: return null
        return WeightReading(
            kilograms = kg,
            isStable = isStable,
            isNet = isNet,
            decimals = contarDecimales(match.value)
        )
    }

    // ---------- Perfil automático ----------
    // Lee el peso sin importar el orden de los campos.
    // La detección de dígitos invertidos existe, pero está APAGADA por defecto.
    private object AutoParser {
        var permitirInvertido = false

        private var lastNormal: Double? = null
        private var lastReversed: Double? = null
        private var scoreNormal = 0.0
        private var scoreReversed = 0.0

        fun reset() {
            lastNormal = null
            lastReversed = null
            scoreNormal = 0.0
            scoreReversed = 0.0
        }

        private fun aDouble(txt: String): Double? = txt.replace(',', '.').toDoubleOrNull()

        fun parse(raw: String): WeightReading? {
            val cleaned = raw.trim()
            if (cleaned.isEmpty()) return null
            if (OVERLOAD_REGEX.containsMatchIn(cleaned)) return null

            val matches = NUM_REGEX.findAll(cleaned).toList()
            val m = matches.firstOrNull { it.groupValues[3].isNotEmpty() }
                ?: matches.lastOrNull()
                ?: return null

            val numTxt = m.groupValues[2]
            val vNormal = aDouble(numTxt) ?: return null
            val vReversed = aDouble(numTxt.reversed()) ?: return null

            // Signo: antes del número, o pegado al final
            val signoAtras = cleaned.getOrNull(m.range.last + 1) == '-'
            val negativo = m.groupValues[1] == "-" || signoAtras

            lastNormal?.let { scoreNormal = scoreNormal * 0.8 + kotlin.math.abs(vNormal - it) }
            lastReversed?.let { scoreReversed = scoreReversed * 0.8 + kotlin.math.abs(vReversed - it) }
            lastNormal = vNormal
            lastReversed = vReversed

            // Solo invierte si el interruptor está encendido Y trae punto Y es claramente más estable
            val tienePunto = numTxt.contains('.') || numTxt.contains(',')
            val usarInvertido = permitirInvertido && tienePunto && scoreReversed < scoreNormal * 0.5
            var valor = if (usarInvertido) vReversed else vNormal
            if (negativo) valor = -valor

            when (m.groupValues[3].lowercase()) {
                "lb", "lbs" -> valor *= 0.45359237
                "g" -> valor /= 1000.0
            }

            val isStable = !UNSTABLE_REGEX.containsMatchIn(cleaned) && !cleaned.contains('?')
            val isNet = NET_REGEX.containsMatchIn(cleaned)

            return WeightReading(
                kilograms = valor,
                isStable = isStable,
                isNet = isNet,
                decimals = contarDecimales(numTxt)
            )
        }
    }

    fun resetAuto() = AutoParser.reset()

    // Enciende o apaga la detección de dígitos invertidos (por defecto: apagada)
    fun permitirInvertido(valor: Boolean) {
        AutoParser.permitirInvertido = valor
    }

    val AUTO = IndicatorProfile(
        nombre = NOMBRE_AUTO,
        comandoLeer = "R",
        comandoTara = "T",
        comandoZero = "Z",
        parser = AutoParser::parse
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
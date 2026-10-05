package com.pesaje.core

import com.pesaje.core.domain.model.IndicatorProfiles
import org.junit.Test

class IndicatorProfilesTest {

    private fun probar(titulo: String, tramas: List<String>) {
        println("\n===== $titulo =====")
        IndicatorProfiles.resetAuto()
        tramas.forEach { trama ->
            val resultado = IndicatorProfiles.AUTO.parser(trama)
            println("Trama original: '$trama'  --->  Peso parseado: ${resultado?.kilograms} kg")
        }
    }

    @Test
    fun prueba1_tramaNormal() {
        probar(
            "PRUEBA 1: Tramas Normales (LP7516)",
            listOf(
                "ST,GS,+0012.0kg",
                "ST,GS,+0012.5kg",
                "ST,GS,+0013.0kg",
                "ST,GS,+0013.5kg",
                "ST,GS,+0014.0kg"
            )
        )
    }

    @Test
    fun prueba2_digitosInvertidos() {
        IndicatorProfiles.permitirInvertido(true)
        try {
            probar(
                "PRUEBA 2: Tramas Invertidas (Ej. 0.21 -> 12.0 kg)",
                listOf("0.21kg", "5.21kg", "0.31kg", "5.31kg", "0.41kg")
            )
        } finally {
            IndicatorProfiles.permitirInvertido(false)
        }
    }
    @Test
    fun prueba3_ordenes_distintos() {
        val casos = listOf(
            "ST,GS,+0012.5kg",      // normal
            "+0012.5kg,GS,ST",      // peso primero
            "US,GS,+0012.5kg",      // inestable
            "ST,NT,-0012.5kg",      // neto y negativo
            "0012.5 lb",            // libras
            "0012.5,02,01"          // peso primero + estados numéricos
        )
        casos.forEach {
            IndicatorProfiles.resetAuto()
            val r = IndicatorProfiles.AUTO.parser(it)
            println("'$it' → ${r?.kilograms} kg | estable=${r?.isStable} | neto=${r?.isNet}")
        }
    }
}
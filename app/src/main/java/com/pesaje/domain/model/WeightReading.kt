package com.pesaje.domain.model

data class WeightReading (
    val kilograms: Double,
    val isStable: Boolean,
    val isNet: Boolean = false // true = Neto (con tara aplicada), false = Bruto
)
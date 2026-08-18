package com.pesaje.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "registros_pesaje_ganado")
data class RegistroPesajeGanado(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val arete: String,
    val sexo: String,
    val peso: Double,
    val fecha: String
)
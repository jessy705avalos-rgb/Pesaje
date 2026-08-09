package com.pesaje.domain.repository

import com.pesaje.domain.model.WeightReading
import kotlinx.coroutines.flow.Flow

interface WeightRepository {
    fun connect(): Flow<Boolean> //true = conectado, false= desconectado
    suspend fun requestWeight(): Unit //manda el comando R
    suspend fun setTare() //Manda 'T'
    suspend fun setZero() //Manda 'Z'
    fun observeWeight(): Flow<WeightReading> // escucha lo que responde el indicador
    fun disconnect()

}
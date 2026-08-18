package com.pesaje.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RegistroPesajeGanadoDao {

    @Insert
    suspend fun insertar(registro: RegistroPesajeGanado)

    @Query("SELECT * FROM registros_pesaje_ganado ORDER BY id DESC")
    fun obtenerTodos(): Flow<List<RegistroPesajeGanado>>

    @Query("DELETE FROM registros_pesaje_ganado")
    suspend fun borrarTodos()
}
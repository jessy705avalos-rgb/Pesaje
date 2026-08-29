package com.pesaje.camionero.domain.repository

import com.pesaje.core.data.local.RegistroPesajeTrailer
import kotlinx.coroutines.flow.Flow

interface TrailerRepository {
    //1.registrar entrada
    suspend fun registrarEntrada(registro: RegistroPesajeTrailer):Long

    //2.registrar salida
    suspend fun registrarSalida(registro: RegistroPesajeTrailer)

    //3.Verificar si está abierto / buscar por placa
    suspend fun buscarAbiertoPorPlacas(placas: String): RegistroPesajeTrailer? //se usa cuando el usuario ya eligió "99JHH" del dropdown, para traer específicamente los datos completos de ESE vehículo (conductor, carga, peso de entrada) y mostrarlos en el recuadro gris.

    //Vehiculos abiertos para el dropdown
    fun obtenerAbiertos(): Flow<List<RegistroPesajeTrailer>> //se usa una vez, al cargar la pantalla de Salidas, para saber qué opciones mostrar en el dropdown

    //4 y 5. Lista completa que se actualiza sola, para historial
    fun obtenerTodos(): Flow<List<RegistroPesajeTrailer>>

    //6. borrar todos
    suspend fun borrarTodos()
}
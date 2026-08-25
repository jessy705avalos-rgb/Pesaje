package com.pesaje.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
//Data Access Object : Es una interfaz que contiene las operaciones que puedes hacer sobre una tabla de Room.
@Dao  //DAO = operaciones de acceso a datos.
interface RegistroPesajeTrailerDao {   //Room, esta interfaz contiene las operaciones de acceso a la base de datos

    @Insert                             //Quiero insertar un nuevo registro de pesaje en la base de datos. ESTA FUNCION HARÁ UN INSERT
    suspend fun insertar(registro: RegistroPesajeTrailer): Long //suspend fun: OPERACIÓN ASÍNCRONA (en segundo plano para no bloquear hilo de android)

    @Update                             //Actualiza un registro que ya existe." @Update le dice a Room que haga un UPDATE.
    suspend fun actualizar(registro: RegistroPesajeTrailer)

    @Query("SELECT * FROM registros_pesaje_trailer WHERE estaAbierto = 1 ORDER BY id DESC") //obtenemos todas las columnas de la tabla  registrospesajetrailer donde estaAbierto es true
    fun obtenerAbiertos(): Flow<List<RegistroPesajeTrailer>>  //flowlist: Te voy a proporcionar una lista de registros y te avisaré cuando esa lista cambie.Flow<List<T>> LISTA QUE SE ACTUALIZA REACTIVAMENTE

    @Query("SELECT * FROM registros_pesaje_trailer WHERE placas = :placas AND estaAbierto= 1 LIMIT 1") //Busca si existe un pesaje abierto correspondiente a estas placas... :placas = El valor vendrá del parámetro de la función
    suspend fun buscarAbiertoPorPlacas(placas: String): RegistroPesajeTrailer?

    @Query("SELECT * FROM registros_pesaje_trailer ORDER BY id DESC") //Dame todos los registros de pesaje de tráiler y ordénalos del más reciente al más antiguo
    fun obtenerTodos(): Flow<List<RegistroPesajeTrailer>>

    @Query("DELETE FROM registros_pesaje_trailer") //Borra todos los registros de esta tabla
    suspend fun borrarTodos()
}
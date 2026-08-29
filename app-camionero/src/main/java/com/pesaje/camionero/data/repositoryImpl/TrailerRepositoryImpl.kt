package com.pesaje.camionero.data.repositoryImpl

import com.pesaje.camionero.domain.repository.TrailerRepository
import com.pesaje.core.data.local.RegistroPesajeTrailer
import com.pesaje.core.data.local.RegistroPesajeTrailerDao
import kotlinx.coroutines.flow.Flow

class TrailerRepositoryImpl(
    private val dao: RegistroPesajeTrailerDao
) : TrailerRepository {

    override suspend fun registrarEntrada(registro: RegistroPesajeTrailer): Long {
        return dao.insertar(registro)
    }

    override suspend fun registrarSalida(registro: RegistroPesajeTrailer) {
        return dao.actualizar(registro)
    }

    override suspend fun buscarAbiertoPorPlacas(placas: String): RegistroPesajeTrailer? {
        return dao.buscarAbiertoPorPlacas(placas)
    }

    override fun obtenerAbiertos(): Flow<List<RegistroPesajeTrailer>> {
        return dao.obtenerAbiertos()
    }

    override fun obtenerTodos(): Flow<List<RegistroPesajeTrailer>> {
        return dao.obtenerTodos()
    }

    override suspend fun borrarTodos() {
        return dao.borrarTodos()
    }



}
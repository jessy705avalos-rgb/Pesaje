package com.pesaje.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

//ESTE ARCHIVO: define cómo se van a guardar los registros de pesaje de un tráiler en una tabla de Room.
//UN REGISTRO DE PESAJE DEBE TENER ESTAS COSAS...

@Entity(tableName = "registros_pesaje_trailer") //creamos la tabla en la base de datos, nombre de tabla registros pesaje trailer
data class RegistroPesajeTrailer(               //creamos una clase de datos, data class sirve para representar informacion.Un objeto RegistroPesajeTrailer contiene toda la información de un pesaje.
    @PrimaryKey(autoGenerate = true)            //creamos las primarykey y que se autogeneren
    val id: Int = 0,                            //empiezan en 0 y Room le asigna su id real conforme avanza

    val placas: String,
    val conductor: String,
    val carga: String,

    val pesoEntrada: Double,
    val fechaEntrada: String,

    val pesoSalida: Double? = null,             //el valor puede ser nulo
    val fechaSalida: String? = null,            //aqui tambien

    val estaAbierto: Boolean = true             //el registro está abierto o no? para su salida. cuando el trailer entra está en true y cuando sale está en false

)

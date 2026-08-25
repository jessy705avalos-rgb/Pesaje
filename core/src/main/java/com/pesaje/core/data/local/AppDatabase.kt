package com.pesaje.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

//AppDatabase es la puerta principal hacia tu base de datos Room.
@Database(
    entities = [                    //Mi base de datos tendrá estas tablas.
        RegistroPesajeGanado::class,
        RegistroPesajeTrailer::class], //esta lista le dice a Room "estas son todas las tablas que debe conocer esta base de datos".
    version = 2
)

abstract class AppDatabase : RoomDatabase(){ //Aquí estás creando tu clase principal de base de datos. RoomDatabase es la clase de Room de la que hereda.
    abstract fun registroPesajeGanadoDao(): RegistroPesajeGanadoDao     //Para trabajar con la tabla de ganado, voy a utilizar este DAO."
    abstract fun registroPesajeTrailerDao(): RegistroPesajeTrailerDao   //"Para trabajar con la tabla de tráiler, voy a utilizar este DAO."
}
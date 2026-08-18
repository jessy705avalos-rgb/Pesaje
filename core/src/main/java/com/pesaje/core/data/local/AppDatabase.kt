package com.pesaje.core.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [RegistroPesajeGanado::class],
    version = 1
)

abstract class AppDatabase : RoomDatabase(){
    abstract fun registroPesajeGanadoDao(): RegistroPesajeGanadoDao
}
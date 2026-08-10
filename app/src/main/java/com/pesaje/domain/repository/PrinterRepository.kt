package com.pesaje.domain.repository

import android.bluetooth.BluetoothSocket

interface PrinterRepository {
    suspend fun printTicket(
        socket: BluetoothSocket?,
        areteId: String,
        sexo: String,
        pesoKg: Double?
    ): Boolean
}
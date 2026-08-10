package com.pesaje.data.remote

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import java.io.IOException
import java.util.UUID

class PrinterBluetoothManager(
    private val context: Context
) {
    private val SPP_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private var bluetoothSocket: BluetoothSocket? = null

    @SuppressLint("MissingPermission")
    fun connectToPrinter(printerName: String): BluetoothSocket? {
        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter: BluetoothAdapter? = bluetoothManager.adapter

        if (adapter == null || !adapter.isEnabled) return null

        //buscar en los dispositivos vinculados / emparejados
        val device = adapter.bondedDevices.find { it.name == printerName } ?: return null

        return try {
            if (bluetoothSocket != null && bluetoothSocket!!.isConnected) {
                return bluetoothSocket
            }

            // Crear y conectar el socket RFCOMM
            bluetoothSocket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            bluetoothSocket?.connect()
            bluetoothSocket
        } catch (e: IOException) {
            e.printStackTrace()
            closeConnection()
            null
        }

    }

    fun closeConnection() {
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            bluetoothSocket = null
        }
    }
}
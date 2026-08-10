package com.pesaje.data.repositoryImpl

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pesaje.domain.model.WeightReading
import com.pesaje.domain.repository.WeightRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID

private val SERVICE_UUID = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
private val NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")
private val WRITE_CHARACTERISTIC_UUID = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3")
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
private const val DEVICE_NAME = "BL243902"

// Tiempo de espera antes de buscar servicios, para evitar el Error 133
private const val DISCOVER_SERVICES_DELAY_MS = 600L

// Tiempo de espera antes de reintentar reconectar tras una desconexión inesperada
private const val RECONNECT_DELAY_MS = 1500L

private const val TAG = "PESAJE_BLE"

@SuppressLint("MissingPermission")
class BleWeightRepository(
    private val context: Context
) : WeightRepository {

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val receiveBuffer = StringBuilder()

    // Bandera para saber si debemos reconectar solos o si el usuario
    // cerró la conexión a propósito (con disconnect())
    private var shouldAutoReconnect = true

    private val weightFlow = MutableSharedFlow<WeightReading>(replay = 1, extraBufferCapacity = 1)

    override fun connect(): Flow<Boolean> = callbackFlow {
        Log.d(TAG, "connect() llamado — iniciando proceso de conexión")
        shouldAutoReconnect = true

        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        val device = adapter.bondedDevices.find { it.name == DEVICE_NAME }

        if (device == null) {
            Log.e(TAG, "❌ No se encontró el dispositivo '$DEVICE_NAME' entre los emparejados")
            trySend(false)
            close()
            return@callbackFlow
        }

        Log.d(TAG, "✅ Dispositivo '$DEVICE_NAME' encontrado, intentando conectar...")

        // Declaramos el callback como var para poder usarlo recursivamente al reconectar
        lateinit var gattCallback: BluetoothGattCallback

        fun attemptConnect() {
            Log.d(TAG, "🔄 Intentando conectar/reconectar...")
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }

        gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(TAG, "onConnectionStateChange → status=$status, newState=$newState")

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(
                            TAG,
                            "🔗 Conexión GATT establecida, esperando ${DISCOVER_SERVICES_DELAY_MS}ms antes de buscar servicios..."
                        )
                        mainHandler.postDelayed({
                            Log.d(TAG, "Buscando servicios ahora...")
                            gatt.discoverServices()
                        }, DISCOVER_SERVICES_DELAY_MS)
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.w(TAG, "🔴 Desconectado del dispositivo (status=$status)")
                        writeCharacteristic = null
                        trySend(false)

                        gatt.close()

                        if (shouldAutoReconnect) {
                            Log.d(TAG, "⏳ Reintentando conexión en ${RECONNECT_DELAY_MS}ms...")
                            mainHandler.postDelayed({
                                if (shouldAutoReconnect) {
                                    attemptConnect()
                                }
                            }, RECONNECT_DELAY_MS)
                        } else {
                            Log.d(TAG, "Desconexión intencional, no se reintenta")
                        }
                    }
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                Log.d(TAG, "onServicesDiscovered → status=$status")

                val service = gatt.getService(SERVICE_UUID)
                if (service == null) {
                    Log.e(TAG, "❌ No se encontró el SERVICE_UUID esperado: $SERVICE_UUID")
                    trySend(false)
                    return
                }
                Log.d(TAG, "✅ Servicio encontrado correctamente")

                writeCharacteristic = service.getCharacteristic(WRITE_CHARACTERISTIC_UUID)
                if (writeCharacteristic == null) {
                    Log.e(TAG, "❌ No se encontró la característica WRITE")
                    trySend(false)
                    return
                }
                Log.d(TAG, "✅ Característica WRITE encontrada y guardada")

                val notifyCharacteristic = service.getCharacteristic(NOTIFY_CHARACTERISTIC_UUID)
                if (notifyCharacteristic == null) {
                    Log.e(TAG, "❌ No se encontró la característica NOTIFY")
                    trySend(false)
                    return
                }

                Log.d(TAG, "✅ Característica NOTIFY encontrada, activando notificaciones...")
                gatt.setCharacteristicNotification(notifyCharacteristic, true)
                val descriptor = notifyCharacteristic.getDescriptor(CCCD_UUID)

                if (descriptor == null) {
                    Log.e(TAG, "❌ No se encontró el descriptor CCCD")
                    trySend(false)
                    return
                }

                descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                val writeStarted = gatt.writeDescriptor(descriptor)
                Log.d(
                    TAG,
                    "📝 writeDescriptor() iniciado: $writeStarted — esperando confirmación..."
                )
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                Log.d(TAG, "onDescriptorWrite → status=$status (0 = éxito)")

                val readyToUse = writeCharacteristic != null && status == BluetoothGatt.GATT_SUCCESS
                Log.d(
                    TAG,
                    if (readyToUse) "🎉 CONEXIÓN REALMENTE LISTA (confirmada)" else "⚠️ Falló activar notificaciones"
                )
                trySend(readyToUse)
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                Log.d(TAG, "onCharacteristicWrite → status=$status (0 = éxito)")
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                Log.d(TAG, "📩 onCharacteristicChanged disparado — llegó un dato nuevo")

                if (characteristic.uuid == NOTIFY_CHARACTERISTIC_UUID) {
                    val raw = characteristic.value?.toString(Charsets.UTF_8)
                    Log.d(TAG, "📦 Dato crudo recibido: '$raw'")

                    if (raw == null) {
                        Log.e(TAG, "❌ El dato recibido es null")
                        return
                    }

                    receiveBuffer.append(raw)

                    while (receiveBuffer.contains("\r\n")) {
                        val end = receiveBuffer.indexOf("\r\n")
                        val frame = receiveBuffer.substring(0, end)

                        receiveBuffer.delete(0, end + 2)
                        Log.d(TAG, "📨 Trama completa recibida: '$frame'")

                        val reading = parseWeight(frame)

                        if (reading != null) {
                            Log.d(
                                TAG,
                                "✅ Peso parseado correctamente: ${reading.kilograms} kg (estable=${reading.isStable})"
                            )

                            val emitted = weightFlow.tryEmit(reading)
                            Log.d(TAG, "📡 tryEmit resultado: $emitted")
                        } else {
                            Log.e(TAG, "❌ No se pudo parsear la trama: '$frame'")
                        }
                    }
                } else {
                    Log.w(
                        TAG,
                        "⚠️ Llegó un dato de una característica distinta a la esperada: ${characteristic.uuid}"
                    )
                }
            }
        }

        attemptConnect()

        awaitClose {
            Log.d(TAG, "connect() Flow cerrado, cerrando GATT")
            shouldAutoReconnect = false
            mainHandler.removeCallbacksAndMessages(null)
            bluetoothGatt?.close()
        }
    }

    override suspend fun requestWeight() {
        Log.d(TAG, "requestWeight() llamado — botón 'Leer peso' presionado")

        val characteristic = writeCharacteristic
        if (characteristic == null) {
            Log.e(
                TAG,
                "❌ No se puede leer: writeCharacteristic es null (¿ya estás conectado de verdad?)"
            )
            return
        }

        characteristic.value = "R".toByteArray()
        val success = bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "📤 Comando 'R' enviado — resultado del envío: $success")
    }

    override fun observeWeight(): Flow<WeightReading> = weightFlow.asSharedFlow()

    override fun disconnect() {
        Log.d(TAG, "disconnect() llamado — desconexión intencional")
        shouldAutoReconnect = false
        mainHandler.removeCallbacksAndMessages(null)
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
    }

    override suspend fun setTare() {
        Log.d(TAG, "setTare() llamado - enviando comando 'T'")
        val characteristic = writeCharacteristic
        if (characteristic == null) {
            Log.e(TAG, "❌ No se puede enviar TARE: writeCharacteristic es null")
            return
        }
        characteristic.value = "T".toByteArray()
        val success = bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "📤 Comando 'T' enviado — resultado del envío: $success")
    }

    override suspend fun setZero() {
        Log.d(TAG, "setZero() llamado - enviando comando 'Z'")
        val characteristic = writeCharacteristic
        if (characteristic == null) {
            Log.e(TAG, "❌ No se puede enviar ZERO: writeCharacteristic es null")
            return
        }
        characteristic.value = "Z".toByteArray()
        val success = bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "📤 Comando 'Z' enviado — resultado del envío: $success")

    }

    private fun parseWeight(raw: String): WeightReading? {
        val cleaned = raw.trim()
        Log.d(TAG, "parseWeight recibiendo: '$cleaned'")


        val isStable = cleaned.startsWith("ST")
        val isNet = cleaned.contains("NT") // GS = bruto, NT = neto

        val regex = Regex("[-+]?\\d+\\.?\\d*")
        val match = regex.find(cleaned.substringAfter("+").ifEmpty { cleaned })

        val kilograms = match?.value?.toDoubleOrNull()
        if (kilograms == null) {
            Log.e(TAG, "❌ parseWeight: no se encontró un número válido en '$cleaned'")
            return null
        }

        Log.d(TAG, "✅ parseWeight: número extraído = $kilograms, neto=$isNet")
        return WeightReading(kilograms = kilograms, isStable = isStable, isNet = isNet)
    }
}
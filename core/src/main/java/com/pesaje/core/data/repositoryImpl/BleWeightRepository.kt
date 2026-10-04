package com.pesaje.core.data.repositoryImpl

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.pesaje.core.data.local.SettingsDataStore
import com.pesaje.core.domain.model.IndicatorProfile
import com.pesaje.core.domain.model.IndicatorProfiles
import com.pesaje.core.domain.model.WeightReading
import com.pesaje.core.domain.repository.WeightRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

// ---------- BLE ----------
// UUIDs conocidos (módulos tipo Microchip). Si el indicador no los tiene, se buscan solos.
private val SERVICE_UUID = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455")
private val NOTIFY_CHARACTERISTIC_UUID = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616")
private val WRITE_CHARACTERISTIC_UUID = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3")
private val CCCD_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

// ---------- Clásico (SPP) ----------
private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

// Tiempo de espera antes de buscar servicios, para evitar el Error 133
private const val DISCOVER_SERVICES_DELAY_MS = 600L

// Tiempo de espera antes de reintentar reconectar tras una desconexión inesperada
private const val RECONNECT_DELAY_MS = 1500L

// En dispositivos duales: si BLE no queda listo en este tiempo, se prueba clásico
private const val BLE_TIMEOUT_MS = 8000L

// Espera entre reintentos de conexión clásica
private const val SPP_RETRY_MS = 3000L

private const val TAG = "PESAJE_BLE"

@SuppressLint("MissingPermission")
class BleWeightRepository(
    private val context: Context,
    private val settingsDataStore: SettingsDataStore
) : WeightRepository {

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null
    private var notifyUuid: UUID? = null
    private var profile: IndicatorProfile = IndicatorProfiles.AUTO
    private val mainHandler = Handler(Looper.getMainLooper())
    private val receiveBuffer = StringBuilder()

    // Conexión clásica
    private var sppSocket: BluetoothSocket? = null
    private var sppJob: Job? = null
    private var modoSpp = false

    // BLE confirmado (notificaciones activas)
    private var bleListo = false

    // Para que una conexión vieja no pise el estado de la nueva
    private var generacionActual = 0

    // Bandera para saber si debemos reconectar solos o si el usuario
    // cerró la conexión a propósito (con disconnect())
    private var shouldAutoReconnect = true

    private val weightFlow = MutableSharedFlow<WeightReading>(replay = 1, extraBufferCapacity = 1)

    // ================= UTILIDADES =================

    // Busca las características de escritura y notificación:
    // primero por UUID conocido, si no, la primera que tenga esas propiedades.
    private fun encontrarCaracteristicas(
        gatt: BluetoothGatt
    ): Pair<BluetoothGattCharacteristic?, BluetoothGattCharacteristic?> {
        val conocido = gatt.getService(SERVICE_UUID)
        var write = conocido?.getCharacteristic(WRITE_CHARACTERISTIC_UUID)
        var notify = conocido?.getCharacteristic(NOTIFY_CHARACTERISTIC_UUID)

        if (write == null || notify == null) {
            for (service in gatt.services) {
                val id = service.uuid.toString()
                if (id.startsWith("00001800") || id.startsWith("00001801")) continue
                for (c in service.characteristics) {
                    val p = c.properties
                    if (notify == null &&
                        (p and (BluetoothGattCharacteristic.PROPERTY_NOTIFY or
                                BluetoothGattCharacteristic.PROPERTY_INDICATE)) != 0
                    ) notify = c
                    if (write == null &&
                        (p and (BluetoothGattCharacteristic.PROPERTY_WRITE or
                                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) != 0
                    ) write = c
                }
            }
        }
        return write to notify
    }

    // Corta el buffer por \r\n, \r o \n y procesa cada trama completa
    private fun procesarBuffer() {
        while (true) {
            val idx = receiveBuffer.indexOfFirst { it == '\r' || it == '\n' }
            if (idx < 0) return

            val frame = receiveBuffer.substring(0, idx)
            receiveBuffer.delete(0, idx + 1)
            if (frame.isBlank()) continue

            Log.d(TAG, "📨 Trama completa recibida: '$frame'")
            val reading = profile.parser(frame)
            if (reading != null) {
                Log.d(TAG, "✅ Peso parseado: ${reading.kilograms} kg (estable=${reading.isStable})")
                weightFlow.tryEmit(reading)
            } else {
                Log.e(TAG, "❌ No se pudo parsear la trama: '$frame'")
            }
        }
    }

    // Abre un socket clásico SPP. Primero el método estándar; si falla, el alterno (canal 1)
    private fun abrirSocketSpp(device: BluetoothDevice): BluetoothSocket? {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        try { manager.adapter?.cancelDiscovery() } catch (_: Exception) {}

        try {
            val socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
            socket.connect()
            return socket
        } catch (e: IOException) {
            Log.w(TAG, "SPP estándar falló (${e.message}), probando método alterno...")
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ Falta permiso BLUETOOTH_CONNECT")
            return null
        }

        return try {
            val metodo = device.javaClass.getMethod("createRfcommSocket", Int::class.javaPrimitiveType)
            val socket = metodo.invoke(device, 1) as BluetoothSocket
            socket.connect()
            socket
        } catch (e: Exception) {
            Log.e(TAG, "❌ SPP alterno también falló: ${e.message}")
            null
        }
    }

    private fun cerrarSpp() {
        sppJob?.cancel()
        sppJob = null
        try { sppSocket?.close() } catch (_: Exception) {}
        sppSocket = null
    }

    // ================= CONEXIÓN =================

    override fun connect(): Flow<Boolean> = callbackFlow {
        Log.d(TAG, "connect() llamado — iniciando proceso de conexión")
        val generacion = ++generacionActual
        shouldAutoReconnect = true
        weightFlow.resetReplayCache()

        mainHandler.removeCallbacksAndMessages(null)
        bluetoothGatt?.close()
        bluetoothGatt = null
        cerrarSpp()
        writeCharacteristic = null
        notifyUuid = null
        modoSpp = false
        bleListo = false
        receiveBuffer.setLength(0)

        val bluetoothManager =
            context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val adapter = bluetoothManager.adapter

        // Indicador y formato elegidos por el usuario en Configuración
        val deviceName = settingsDataStore.indicatorDeviceFlow.first()
        profile = IndicatorProfiles.fromModel(settingsDataStore.indicatorFormatFlow.first())
        Log.d(TAG, "Perfil de indicador: ${profile.nombre}")

        val device = adapter.bondedDevices.find { it.name == deviceName }

        if (device == null) {
            Log.e(TAG, "❌ No se encontró el dispositivo '$deviceName' entre los emparejados")
            trySend(false)
            close()
            return@callbackFlow
        }

        val tipo = device.type
        Log.d(TAG, "✅ Dispositivo '$deviceName' encontrado, tipo=$tipo (1=clásico, 2=BLE, 3=dual)")

        lateinit var gattCallback: BluetoothGattCallback

        // ---------- Modo clásico (SPP) ----------
        fun iniciarSpp() {
            Log.d(TAG, "🔵 Usando conexión CLÁSICA (SPP)")
            modoSpp = true
            receiveBuffer.setLength(0)

            sppJob?.cancel()
            sppJob = launch(Dispatchers.IO) {
                while (isActive && shouldAutoReconnect) {
                    if (device.bondState != BluetoothDevice.BOND_BONDED) {
                        Log.e(TAG, "❌ '${device.name}' ya no está vinculado, se detiene la conexión")
                        trySend(false)
                        break
                    }
                    val socket = abrirSocketSpp(device)
                    if (socket == null) {
                        trySend(false)
                        delay(SPP_RETRY_MS)
                        continue
                    }

                    sppSocket = socket
                    Log.d(TAG, "🎉 SPP conectado")
                    trySend(true)

                    try {
                        val input = socket.inputStream
                        val buf = ByteArray(1024)
                        while (isActive) {
                            val n = input.read(buf)
                            if (n < 0) break
                            receiveBuffer.append(String(buf, 0, n, Charsets.UTF_8))
                            procesarBuffer()
                        }
                    } catch (e: IOException) {
                        Log.w(TAG, "SPP: lectura interrumpida (${e.message})")
                    } finally {
                        try { socket.close() } catch (_: Exception) {}
                        sppSocket = null
                    }

                    trySend(false)
                    if (shouldAutoReconnect) delay(SPP_RETRY_MS)
                }
            }
        }

        // En duales/desconocidos: si BLE no funciona, se cambia a clásico
        fun intentarSppComoRespaldo(): Boolean {
            if (tipo == BluetoothDevice.DEVICE_TYPE_LE || modoSpp) return false
            Log.w(TAG, "⚠️ BLE no funcionó, probando conexión clásica...")
            mainHandler.removeCallbacksAndMessages(null)
            try { bluetoothGatt?.disconnect() } catch (_: Exception) {}
            try { bluetoothGatt?.close() } catch (_: Exception) {}
            bluetoothGatt = null
            writeCharacteristic = null
            iniciarSpp()
            return true
        }

        fun attemptConnect() {
            Log.d(TAG, "🔄 Intentando conectar/reconectar (BLE)...")
            bluetoothGatt = device.connectGatt(context, false, gattCallback)
        }

        gattCallback = object : BluetoothGattCallback() {

            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                Log.d(TAG, "onConnectionStateChange → status=$status, newState=$newState")
                if (modoSpp) {
                    // Ya pasamos a clásico: ignorar BLE
                    if (newState == BluetoothProfile.STATE_DISCONNECTED) gatt.close()
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        Log.d(TAG, "🔗 GATT establecido, esperando ${DISCOVER_SERVICES_DELAY_MS}ms...")
                        mainHandler.postDelayed({
                            Log.d(TAG, "Buscando servicios ahora...")
                            gatt.discoverServices()
                        }, DISCOVER_SERVICES_DELAY_MS)
                    }

                    BluetoothProfile.STATE_DISCONNECTED -> {
                        Log.w(TAG, "🔴 Desconectado del dispositivo (status=$status)")
                        writeCharacteristic = null
                        bleListo = false
                        trySend(false)

                        gatt.close()

                        if (shouldAutoReconnect) {
                            Log.d(TAG, "⏳ Reintentando conexión en ${RECONNECT_DELAY_MS}ms...")
                            mainHandler.postDelayed({
                                if (shouldAutoReconnect && !modoSpp && device.bondState == BluetoothDevice.BOND_BONDED) {
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
                if (modoSpp) return

                if (status != BluetoothGatt.GATT_SUCCESS) {
                    if (!intentarSppComoRespaldo()) trySend(false)
                    return
                }

                val (write, notify) = encontrarCaracteristicas(gatt)

                if (notify == null) {
                    Log.e(TAG, "❌ No se encontró ninguna característica de notificación")
                    if (!intentarSppComoRespaldo()) trySend(false)
                    return
                }

                // La escritura es opcional: hay indicadores que solo transmiten
                writeCharacteristic = write
                notifyUuid = notify.uuid
                Log.d(TAG, "✅ NOTIFY=${notify.uuid} WRITE=${write?.uuid ?: "ninguna (solo recibe)"}")

                gatt.setCharacteristicNotification(notify, true)
                val descriptor = notify.getDescriptor(CCCD_UUID)

                if (descriptor == null) {
                    Log.e(TAG, "❌ No se encontró el descriptor CCCD")
                    if (!intentarSppComoRespaldo()) trySend(false)
                    return
                }

                val soloIndicate =
                    (notify.properties and BluetoothGattCharacteristic.PROPERTY_NOTIFY) == 0
                descriptor.value =
                    if (soloIndicate) BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    else BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE

                val writeStarted = gatt.writeDescriptor(descriptor)
                Log.d(TAG, "📝 writeDescriptor() iniciado: $writeStarted — esperando confirmación...")
            }

            override fun onDescriptorWrite(
                gatt: BluetoothGatt,
                descriptor: BluetoothGattDescriptor,
                status: Int
            ) {
                Log.d(TAG, "onDescriptorWrite → status=$status (0 = éxito)")
                if (modoSpp) return

                val readyToUse = status == BluetoothGatt.GATT_SUCCESS
                bleListo = readyToUse
                Log.d(
                    TAG,
                    if (readyToUse) "🎉 CONEXIÓN BLE LISTA (confirmada)" else "⚠️ Falló activar notificaciones"
                )
                if (readyToUse) mainHandler.removeCallbacksAndMessages(null)
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
                if (modoSpp) return
                if (characteristic.uuid != notifyUuid) {
                    Log.w(TAG, "⚠️ Dato de una característica distinta: ${characteristic.uuid}")
                    return
                }

                val raw = characteristic.value?.toString(Charsets.UTF_8)
                Log.d(TAG, "📦 Dato crudo recibido: '$raw'")
                if (raw == null) {
                    Log.e(TAG, "❌ El dato recibido es null")
                    return
                }

                receiveBuffer.append(raw)
                procesarBuffer()
            }
        }

        // ---------- Elegir tipo de conexión ----------
        if (tipo == BluetoothDevice.DEVICE_TYPE_CLASSIC) {
            iniciarSpp()
        } else {
            attemptConnect()
            // Dual o desconocido: si BLE no queda listo a tiempo, probar clásico
            if (tipo != BluetoothDevice.DEVICE_TYPE_LE) {
                mainHandler.postDelayed({
                    if (!bleListo && !modoSpp && shouldAutoReconnect) {
                        intentarSppComoRespaldo()
                    }
                }, BLE_TIMEOUT_MS)
            }
        }

        awaitClose {
            // Si ya hay una conexión más nueva, no tocar su estado
            if (generacion != generacionActual) return@awaitClose

            Log.d(TAG, "connect() Flow cerrado, cerrando conexión")
            shouldAutoReconnect = false
            mainHandler.removeCallbacksAndMessages(null)
            cerrarSpp()
            bluetoothGatt?.close()
        }
    }

    // ================= COMANDOS =================

    private suspend fun enviarComando(comando: String?, nombre: String) {
        if (comando == null) {
            Log.d(TAG, "ℹ️ El perfil '${profile.nombre}' no usa comando para $nombre")
            return
        }

        // --- Clásico ---
        if (modoSpp) {
            val socket = sppSocket
            if (socket == null) {
                Log.e(TAG, "❌ No se puede enviar $nombre: SPP no está conectado")
                return
            }
            withContext(Dispatchers.IO) {
                try {
                    socket.outputStream.write(comando.toByteArray())
                    socket.outputStream.flush()
                    Log.d(TAG, "📤 Comando '$comando' ($nombre) enviado por SPP")
                } catch (e: IOException) {
                    Log.e(TAG, "❌ Error enviando $nombre por SPP: ${e.message}")
                }
            }
            return
        }

        // --- BLE ---
        val characteristic = writeCharacteristic
        if (characteristic == null) {
            Log.e(TAG, "❌ No se puede enviar $nombre: no hay característica de escritura")
            return
        }
        characteristic.writeType =
            if ((characteristic.properties and BluetoothGattCharacteristic.PROPERTY_WRITE) != 0)
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            else BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

        characteristic.value = comando.toByteArray()
        val success = bluetoothGatt?.writeCharacteristic(characteristic)
        Log.d(TAG, "📤 Comando '$comando' ($nombre) enviado — resultado: $success")
    }

    override suspend fun requestWeight() = enviarComando(profile.comandoLeer, "LEER")

    override suspend fun setTare() = enviarComando(profile.comandoTara, "TARA")

    override suspend fun setZero() = enviarComando(profile.comandoZero, "ZERO")

    override fun observeWeight(): Flow<WeightReading> = weightFlow.asSharedFlow()

    override fun disconnect() {
        Log.d(TAG, "disconnect() llamado — desconexión intencional")
        shouldAutoReconnect = false
        mainHandler.removeCallbacksAndMessages(null)
        cerrarSpp()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
        writeCharacteristic = null
    }
}
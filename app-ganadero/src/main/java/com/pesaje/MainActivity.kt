package com.pesaje

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.pesaje.core.data.repositoryImpl.BleWeightRepository
import com.pesaje.presentation.ui.screens.WeightScreen
import com.pesaje.presentation.ui.theme.PesajeTheme
import com.pesaje.presentation.viewmodel.WeightViewModel
import android.Manifest
import com.pesaje.core.data.remote.TicketPrinterHelper
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.room.Room
import com.pesaje.core.data.local.AppDatabase
import com.pesaje.core.data.remote.PrinterBluetoothManager
import com.pesaje.core.data.repositoryImpl.PrinterRepositoryImpl
import com.pesaje.core.domain.usecase.PrintCattleTicketUseCase
import com.pesaje.presentation.ui.screens.HistorialScreen
import com.pesaje.presentation.viewmodel.HistorialViewModel
import kotlin.getValue

private const val TAG = "PESAJE_MAIN"

class MainActivity : ComponentActivity() {

    //lista de permisos
    private val bluetoothPermissions =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

    private val weightRepository by lazy { BleWeightRepository(applicationContext) }
    private val printerBluetoothManager by lazy { PrinterBluetoothManager(applicationContext) }
    private val printerHelper by lazy { TicketPrinterHelper() }
    private val printerRepository by lazy {
        PrinterRepositoryImpl(printerBluetoothManager, printerHelper)
    }

    private val printCattleTicketUseCase by lazy {
        PrintCattleTicketUseCase(printerRepository)
    }

    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pesaje_ganado_db"
        ).build()
    }

    private val registroDao by lazy { database.registroPesajeGanadoDao() }

    private val viewModel by lazy {
        WeightViewModel(
            repository = weightRepository,
            printCattleTicketUseCase = printCattleTicketUseCase,
            registroDao = registroDao
        )
    }

    private val historialViewModel by lazy {
        HistorialViewModel(registroDao)
    }
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            Log.d(TAG, "Resultado de permisos: $results")
            val allGranted = results.values.all { it }
            if (allGranted) {
                Log.d(TAG, "✅ Todos los permisos concedidos, llamando a viewModel.connect()")
                viewModel.connect() // SOLO conectamos si YA nos dieron permiso
            } else {
                Log.e(TAG, "❌ No se concedieron todos los permisos necesarios")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Log.d(TAG, "onCreate — pidiendo permisos: ${bluetoothPermissions.joinToString()}")


        requestPermissionLauncher.launch(bluetoothPermissions)

        setContent {
            PesajeTheme {
                var pantallaActual by remember { mutableStateOf("principal") }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(
                                selected = pantallaActual == "principal",
                                onClick = { pantallaActual = "principal" },
                                icon = { Icon(Icons.Default.Scale, contentDescription = "Pesaje") },
                                label = { Text("Pesaje") }
                            )
                            NavigationBarItem(
                                selected = pantallaActual == "historial",
                                onClick = { pantallaActual = "historial" },
                                icon = { Icon(Icons.Default.List, contentDescription = "Historial") },
                                label = { Text("Historial") }
                            )
                        }
                    }
                ) { innerPadding ->
                    when (pantallaActual) {
                        "principal" -> {
                            WeightScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                        "historial" -> {
                            HistorialScreen(
                                viewModel = historialViewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}
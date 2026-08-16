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
import com.pesaje.core.data.remote.PrinterBluetoothManager
import com.pesaje.core.data.repository.PrinterRepositoryImpl
import com.pesaje.core.domain.usecase.PrintCattleTicketUseCase

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

    private val viewModel by lazy {
        WeightViewModel(
            repository = weightRepository,
            printCattleTicketUseCase = printCattleTicketUseCase
        )
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
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WeightScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
package com.pesaje

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.room.Room
import com.pesaje.core.data.local.AppDatabase
import com.pesaje.core.data.remote.PrinterBluetoothManager
import com.pesaje.core.data.remote.TicketPrinterHelper
import com.pesaje.core.data.repositoryImpl.BleWeightRepository
import com.pesaje.core.data.repositoryImpl.CsvExportRepositoryImpl
import com.pesaje.core.data.repositoryImpl.PrinterRepositoryImpl
import com.pesaje.core.domain.usecase.PrintCattleTicketUseCase
import com.pesaje.presentation.ui.screens.MainScreen
import com.pesaje.presentation.ui.theme.PesajeTheme
import com.pesaje.presentation.viewmodel.HistorialViewModel
import com.pesaje.presentation.viewmodel.WeightViewModel

private const val TAG = "PESAJE_MAIN"

class MainActivity : ComponentActivity() {

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

    // Inicializaciones perezosas de la capa Data y Domain
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
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }

    private val registroDao by lazy { database.registroPesajeGanadoDao() }
    private val csvExportRepository by lazy { CsvExportRepositoryImpl() }

    private val weightViewModel by lazy {
        WeightViewModel(
            repository = weightRepository,
            printCattleTicketUseCase = printCattleTicketUseCase,
            registroDao = registroDao
        )
    }

    private val historialViewModel by lazy {
        HistorialViewModel(registroDao, csvExportRepository)
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            Log.d(TAG, "Resultado de permisos: $results")
            val allGranted = results.values.all { it }
            if (allGranted) {
                Log.d(TAG, "✅ Todos los permisos concedidos, llamando a viewModel.connect()")
                weightViewModel.connect()
            } else {
                Log.e(TAG, "❌ No se concedieron todos los permisos necesarios")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        requestPermissionLauncher.launch(bluetoothPermissions)

        setContent {
            PesajeTheme {
                MainScreen(
                    weightViewModel = weightViewModel,
                    historialViewModel = historialViewModel
                )
            }
        }
    }
}
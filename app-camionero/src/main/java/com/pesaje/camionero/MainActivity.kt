package com.pesaje.camionero

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.room.Room
import com.pesaje.camionero.data.repositoryImpl.TrailerRepositoryImpl
import com.pesaje.camionero.presentation.ui.screens.EntradaScreen
import com.pesaje.camionero.presentation.ui.screens.MainScreen
import com.pesaje.camionero.presentation.ui.screens.SalidaScreen
import com.pesaje.camionero.presentation.ui.theme.PesajeTheme
import com.pesaje.camionero.presentation.viewmodel.TrailerViewModel
import com.pesaje.core.data.local.AppDatabase
import com.pesaje.core.data.remote.PrinterBluetoothManager
import com.pesaje.core.data.remote.TicketPrinterHelper
import com.pesaje.core.data.repositoryImpl.BleWeightRepository
import com.pesaje.core.data.repositoryImpl.PrinterRepositoryImpl
import com.pesaje.core.domain.usecase.PrintTrailerEntradaUseCase
import com.pesaje.core.domain.usecase.PrintTrailerSalidaUseCase

private const val TAG = "CAMIONERO_MAIN"

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

    // ---------- Báscula ----------
    private val weightRepository by lazy { BleWeightRepository(applicationContext) }

    // ---------- Impresora ----------
    private val printerBluetoothManager by lazy { PrinterBluetoothManager(applicationContext) }
    private val printerHelper by lazy { TicketPrinterHelper() }
    private val printerRepository by lazy {
        PrinterRepositoryImpl(printerBluetoothManager, printerHelper)
    }
    private val printTrailerEntradaUseCase by lazy { PrintTrailerEntradaUseCase(printerRepository) }
    private val printTrailerSalidaUseCase by lazy { PrintTrailerSalidaUseCase(printerRepository) }

    // ---------- Base de datos ----------
    private val database by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pesaje_camionero_db"
        )
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()
    }
    private val registroDao by lazy { database.registroPesajeTrailerDao() }
    private val trailerRepository by lazy { TrailerRepositoryImpl(registroDao) }

    // ---------- ViewModel ----------
    private val viewModel by lazy {
        TrailerViewModel(
            repository = weightRepository,
            trailerRepository = trailerRepository,
            printTrailerEntradaUseCase = printTrailerEntradaUseCase,
            printTrailerSalidaUseCase = printTrailerSalidaUseCase
        )
    }

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            Log.d(TAG, "Resultado de permisos: $results")
            val allGranted = results.values.all { it }
            if (allGranted) {
                viewModel.connect()
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
                MainScreen(viewModel= viewModel)
            }
        }
    }
}
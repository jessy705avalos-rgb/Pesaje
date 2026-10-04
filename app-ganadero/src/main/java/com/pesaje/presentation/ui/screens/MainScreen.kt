package com.pesaje.presentation.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.pesaje.presentation.viewmodel.HistorialViewModel
import com.pesaje.presentation.viewmodel.WeightViewModel

@Composable
fun MainScreen(
    weightViewModel: WeightViewModel,
    historialViewModel: HistorialViewModel,
    modifier: Modifier = Modifier
) {
    var pantallaActual by rememberSaveable { mutableStateOf("principal") }

    val headerState by weightViewModel.ticketHeader.collectAsState(initial = "PESAJE DE GANADO")
    val footerState by weightViewModel.ticketFooter.collectAsState(initial = "Gracias por su visita")

    // Configuración de dispositivos
    val indicatorDeviceState by weightViewModel.indicatorDevice.collectAsState(initial = "")
    val indicatorFormatState by weightViewModel.indicatorFormat.collectAsState(initial = "")
    val printerDeviceState by weightViewModel.printerDevice.collectAsState(initial = "")

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (pantallaActual != "configuracion") {
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
        }
    ) { innerPadding ->
        when (pantallaActual) {
            "principal" -> {
                WeightScreen(
                    viewModel = weightViewModel,
                    onNavigateToSettings = { pantallaActual = "configuracion" },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            "historial" -> {
                HistorialScreen(
                    viewModel = historialViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            "configuracion" -> {
                SettingsScreen(
                    initialHeader = headerState,
                    initialFooter = footerState,
                    initialIndicatorFormat = indicatorFormatState,
                    initialIndicatorDevice = indicatorDeviceState,
                    initialPrinterDevice = printerDeviceState,
                    onBackClick = { pantallaActual = "principal" },
                    onSave = { header, footer, indFormat, indDevice, prnDevice ->
                        weightViewModel.guardarConfiguracion(
                            header, footer, indFormat, indDevice, prnDevice
                        )
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
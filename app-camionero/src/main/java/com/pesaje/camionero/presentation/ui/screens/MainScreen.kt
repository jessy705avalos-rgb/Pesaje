package com.pesaje.camionero.presentation.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.List
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.pesaje.camionero.presentation.viewmodel.HistorialTrailerViewModel
import com.pesaje.camionero.presentation.viewmodel.TrailerViewModel

@Composable
fun MainScreen(
    viewModel: TrailerViewModel,
    historialTrailerViewModel: HistorialTrailerViewModel,
    modifier: Modifier = Modifier
) {
    var pantallaActual by rememberSaveable { mutableStateOf("entrada") }

    // Obtenemos los estados correspondientes a Entrada y Salida desde el TrailerViewModel
    val entradaHeaderState by viewModel.entradaHeader.collectAsState()
    val entradaFooterState by viewModel.entradaFooter.collectAsState()
    val salidaHeaderState by viewModel.salidaHeader.collectAsState()
    val salidaFooterState by viewModel.salidaFooter.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (pantallaActual != "configuracion") {
                NavigationBar {
                    NavigationBarItem(
                        selected = pantallaActual == "entrada",
                        onClick = { pantallaActual = "entrada" },
                        icon = { Icon(Icons.Default.ArrowDownward, contentDescription = "Entradas") },
                        label = { Text("Entradas") }
                    )
                    NavigationBarItem(
                        selected = pantallaActual == "salida",
                        onClick = { pantallaActual = "salida" },
                        icon = { Icon(Icons.Default.ArrowUpward, contentDescription = "Salidas") },
                        label = { Text("Salidas") }
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
            "entrada" -> {
                EntradaScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { pantallaActual = "configuracion" },
                    modifier = Modifier.padding(innerPadding)
                )
            }

            "salida" -> {
                SalidaScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            "historial" -> {
                HistorialTrailerScreen(
                    viewModel = historialTrailerViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }

            "configuracion" -> {
                SettingsScreen(
                    initialEntradaHeader = entradaHeaderState,
                    initialEntradaFooter = entradaFooterState,
                    initialSalidaHeader = salidaHeaderState,
                    initialSalidaFooter = salidaFooterState,
                    onBackClick = { pantallaActual = "entrada" },
                    onSave = { eHeader, eFooter, sHeader, sFooter ->
                        viewModel.guardarConfiguracionTickets(eHeader, eFooter, sHeader, sFooter)
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
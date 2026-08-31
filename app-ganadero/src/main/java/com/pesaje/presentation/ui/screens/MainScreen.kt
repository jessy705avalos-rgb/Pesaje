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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pesaje.presentation.viewmodel.HistorialViewModel
import com.pesaje.presentation.viewmodel.WeightViewModel

@Composable
fun MainScreen(
    weightViewModel: WeightViewModel,
    historialViewModel: HistorialViewModel,
    modifier: Modifier = Modifier
) {
    // rememberSaveable mantiene la pestaña seleccionada si el usuario rota la pantalla
    var pantallaActual by rememberSaveable { mutableStateOf("principal") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                    viewModel = weightViewModel,
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
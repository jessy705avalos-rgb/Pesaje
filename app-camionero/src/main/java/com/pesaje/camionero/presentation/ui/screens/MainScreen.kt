package com.pesaje.camionero.presentation.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import com.pesaje.camionero.presentation.viewmodel.TrailerViewModel

@Composable
fun MainScreen(
    viewModel: TrailerViewModel,
    modifier: Modifier = Modifier
) {
    // Preserva la pantalla seleccionada tras rotaciones de pantalla
    var pantallaActual by rememberSaveable { mutableStateOf("entrada") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
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
            }
        }
    ) { innerPadding ->
        when (pantallaActual) {
            "entrada" -> {
                EntradaScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            "salida" -> {
                SalidaScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
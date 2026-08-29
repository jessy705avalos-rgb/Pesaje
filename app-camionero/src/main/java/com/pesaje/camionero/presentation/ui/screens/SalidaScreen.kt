package com.pesaje.camionero.presentation.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pesaje.camionero.presentation.viewmodel.TrailerViewModel

@Composable
fun SalidaScreen(
    viewModel: TrailerViewModel,
    modifier: Modifier = Modifier
) {
    //lo que escucha al viewmodel constantemente...
    val isConnected by viewModel.isConnected.collectAsState()
    val currentWeight by viewModel.currentWeight.collectAsState()
    val vehiculosAbiertos by viewModel.vehiculosAbiertos.collectAsState()
    val vehiculoSeleccionado by viewModel.vehiculoSeleccionado.collectAsState()



}
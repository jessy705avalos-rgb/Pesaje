package com.pesaje.camionero.presentation.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesaje.camionero.presentation.viewmodel.TrailerViewModel

@Composable
fun EntradaScreen(
    viewModel: TrailerViewModel,
    modifier: Modifier = Modifier
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val currentWeight by viewModel.currentWeight.collectAsState()

    var placas by remember { mutableStateOf("") }
    var conductor by remember { mutableStateOf("") }
    var carga by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Nueva Entrada",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isConnected) "Indicador conectado" else "Indicador desconectado",
                color = if (isConnected) androidx.compose.ui.graphics.Color(0xFF2E7D32)
                else androidx.compose.ui.graphics.Color(0xFFC62828),
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { viewModel.connect() }) {
                Text("Conectar")
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = currentWeight?.kilograms?.toString() ?: "",
                onValueChange = {},
                label = { Text("Peso (kg)") },
                readOnly = true,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = { viewModel.readWeight() },
                enabled = isConnected
            ) {
                Text("Leer")
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.setZero() }, modifier = Modifier.weight(1f)) {
                Text("ZERO")
            }
            OutlinedButton(onClick = { viewModel.setTare() }, modifier = Modifier.weight(1f)) {
                Text("TARE")
            }
        }

        OutlinedTextField(
            value = placas,
            onValueChange = { placas = it },
            label = { Text("Placas del vehículo") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = conductor,
            onValueChange = { conductor = it },
            label = { Text("Nombre del conductor") },
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = carga,
            onValueChange = { carga = it },
            label = { Text("Carga del vehículo") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                viewModel.registrarEntrada(placas, conductor, carga, imprimirDespues = false)
                placas = ""
                conductor = ""
                carga = ""
            },
            enabled = placas.isNotBlank() && conductor.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Registrar sin imprimir")
        }

        Button(
            onClick = {
                viewModel.registrarEntrada(placas, conductor, carga, imprimirDespues = true)
                placas = ""
                conductor = ""
                carga = ""
            },
            enabled = placas.isNotBlank() && conductor.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("Registrar e imprimir")
        }
    }
}
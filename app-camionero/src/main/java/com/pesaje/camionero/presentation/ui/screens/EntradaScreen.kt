package com.pesaje.camionero.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesaje.camionero.presentation.viewmodel.TrailerViewModel

@Composable
fun EntradaScreen(
    viewModel: TrailerViewModel,
    modifier: Modifier = Modifier,
) {
    val isConnected by viewModel.isConnected.collectAsState()
    val currentWeight by viewModel.currentWeight.collectAsState()

    var placas by remember { mutableStateOf("") }
    var conductor by remember { mutableStateOf("") }
    var carga by remember { mutableStateOf("") }

    val context = LocalContext.current
    val mensaje by viewModel.mensaje.collectAsState()
    LaunchedEffect(mensaje) {
        mensaje?.let { texto ->
            Toast.makeText(context, texto, Toast.LENGTH_LONG).show()
            viewModel.clearMensaje()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF5F7F2))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ============ SECCIÓN 1: Encabezado ============
        Text(
            text = "Nueva Entrada",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        // ============ SECCIÓN 2: Barra de conexión ============
        // ============ SECCIÓN 2: Barra de conexión ============
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isConnected) Color.Green else Color.Red)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isConnected) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = null,
                        tint = Color.White
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (isConnected) "CONECTADO" else "DESCONECTADO",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "BL243902",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Button(
                onClick = { viewModel.connect() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = if (isConnected) Color.Green else Color.Red
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(if (isConnected) "Reconectar" else "Conectar")
            }
        }


        // ============ SECCIÓN 3: Tarjeta de peso ============
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PESO",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = currentWeight?.kilograms?.let { "%.1f".format(it) } ?: "--.-",
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "kg",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }

        // ============ SECCIÓN 4: Botón Leer peso, Tara y Zero ============
        Button(
            onClick = { viewModel.readWeight() },
            enabled = isConnected,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Balance, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Leer peso", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.setZero() },
                enabled = isConnected,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Zero")
            }
            OutlinedButton(
                onClick = { viewModel.setTare() },
                enabled = isConnected,
                modifier = Modifier.weight(1f).height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Tare")
            }
        }

        // ============ SECCIÓN 5: FORMULARIO TRAILER (Placas, Conductor, Carga) ============
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Datos del Vehículo",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = placas,
                    onValueChange = { placas = it },
                    label = { Text("Placas del vehículo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = conductor,
                    onValueChange = { conductor = it },
                    label = { Text("Nombre del conductor") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = carga,
                    onValueChange = { carga = it },
                    label = { Text("Carga del vehículo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // ============ SECCIÓN 6: Acciones (Registrar e Imprimir) ============
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    viewModel.registrarEntrada(
                        placas = placas,
                        conductor = conductor,
                        carga = carga,
                        imprimirDespues = false
                    )
                    placas = ""
                    conductor = ""
                    carga = ""
                },
                enabled = placas.trim().isNotEmpty() && conductor.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar sin Imprimir", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = {
                    viewModel.registrarEntrada(
                        placas = placas,
                        conductor = conductor,
                        carga = carga,
                        imprimirDespues = true,
                        printerName = "Printer001"
                    )
                    placas = ""
                    conductor = ""
                    carga = ""
                },
                enabled = placas.trim().isNotEmpty() && conductor.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF5C518),
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Registrar e Imprimir", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
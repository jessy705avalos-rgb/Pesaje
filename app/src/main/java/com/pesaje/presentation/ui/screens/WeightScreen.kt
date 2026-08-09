package com.pesaje.presentation.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Balance
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesaje.pesaje.R
import com.pesaje.presentation.ui.theme.CardBackground
import com.pesaje.presentation.ui.theme.ConnectedGreen
import com.pesaje.presentation.ui.theme.DisconnectedRed
import com.pesaje.presentation.ui.theme.SaveYellow
import com.pesaje.presentation.viewmodel.WeightViewModel

@Composable
fun WeightScreen(viewModel: WeightViewModel, modifier: Modifier = Modifier) {
    val isConnected by viewModel.isConnected.collectAsState()
    val weight by viewModel.currentWeight.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CardBackground)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ============ SECCIÓN 1: Encabezado ============
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo LebenPro",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "LEBENPRO",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Control de Pesaje",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            IconButton(
                onClick = { },
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White)
            ) {
                Icon(Icons.Default.Settings, contentDescription = "Configuración")
            }
        }
        // ============ FIN SECCIÓN 1 ============

        // ============ SECCIÓN 2: Barra de conexión ============
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (isConnected) ConnectedGreen else DisconnectedRed)
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
                    contentColor = if (isConnected) ConnectedGreen else DisconnectedRed
                ),
                shape = RoundedCornerShape(50)
            ) {
                Text(if (isConnected) "Reconectar" else "Conectar")
            }
        }
        // ============ FIN SECCIÓN 2 ============

        // ============ SECCIÓN 3: Tarjeta de peso ============
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(vertical = 32.dp),
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
                    text = weight?.let { "%.1f".format(it.kilograms) } ?: "--.-",
                    fontSize = 56.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "kg",
                    fontSize = 20.sp,
                    color = ConnectedGreen,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = {
                        Text(if (weight?.isStable == true) "ESTABLE" else "INESTABLE")
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (weight?.isStable == true)
                            ConnectedGreen.copy(alpha = 0.12f) else Color(0xFFFFF3CD),
                        labelColor = if (weight?.isStable == true)
                            ConnectedGreen else Color(0xFF8A6D00)
                    )
                )
                AssistChip(
                    onClick = {},
                    label = {
                        Text(if (weight?.isNet == true) "NETO" else "BRUTO")
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = Color(0xFFE8EAF6),
                        labelColor = Color(0xFF3949AB)
                    )
                )
            }
        }
        // ============ FIN SECCIÓN 3 ============

        // ============ SECCIÓN 4: Botón Leer peso ============
        Button(
            onClick = { viewModel.readWeight() },
            enabled = isConnected,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ConnectedGreen),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Outlined.Balance, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Leer peso", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        // ============ FIN SECCIÓN 4 ============

        // ============ SECCIÓN 5: Tara y Zero ============
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {viewModel.setTare()},
                enabled = isConnected,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Tara")
            }
            OutlinedButton(
                onClick = { viewModel.setZero()},
                enabled= isConnected,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Zero")
            }
        }
        // ============ FIN SECCIÓN 5 ============

        // ============ SECCIÓN 6: Guardar ============
        Button(
            onClick = { },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = SaveYellow,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Save, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Guardar", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }
        // ============ FIN SECCIÓN 6 ============

    } //  cierra el Column PRINCIPAL de toda la pantalla
}
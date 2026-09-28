package com.pesaje.camionero.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pesaje.camionero.presentation.ui.theme.CardBackground
import com.pesaje.camionero.presentation.ui.theme.SaveYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialEntradaHeader: String,
    initialEntradaFooter: String,
    initialSalidaHeader: String,
    initialSalidaFooter: String,
    onBackClick: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var entradaHeader by remember(initialEntradaHeader) { mutableStateOf(initialEntradaHeader) }
    var entradaFooter by remember(initialEntradaFooter) { mutableStateOf(initialEntradaFooter) }

    var salidaHeader by remember(initialSalidaHeader) { mutableStateOf(initialSalidaHeader) }
    var salidaFooter by remember(initialSalidaFooter) { mutableStateOf(initialSalidaFooter) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración de Tickets", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CardBackground)
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // TARJETA 1: Configuración Ticket Entrada
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Ticket de ENTRADA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = entradaHeader,
                        onValueChange = { entradaHeader = it },
                        label = { Text("Título (Encabezado)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = entradaFooter,
                        onValueChange = { entradaFooter = it },
                        label = { Text("Pie de Página") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // TARJETA 2: Configuración Ticket Salida
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Receipt, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Ticket de SALIDA", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = salidaHeader,
                        onValueChange = { salidaHeader = it },
                        label = { Text("Título (Encabezado)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = salidaFooter,
                        onValueChange = { salidaFooter = it },
                        label = { Text("Pie de Página") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Botón Guardar
            Button(
                onClick = {
                    onSave(entradaHeader, entradaFooter, salidaHeader, salidaFooter)
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SaveYellow,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Guardar Configuración", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
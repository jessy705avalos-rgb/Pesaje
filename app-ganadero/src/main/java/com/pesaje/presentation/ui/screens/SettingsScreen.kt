package com.pesaje.presentation.ui.screens

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
import com.pesaje.presentation.ui.theme.CardBackground
import com.pesaje.presentation.ui.theme.SaveYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialHeader: String,
    initialFooter: String,
    onBackClick: () -> Unit,
    onSave: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    var headerText by remember(initialHeader) { mutableStateOf(initialHeader) }
    var footerText by remember(initialFooter) { mutableStateOf(initialFooter) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración", fontWeight = FontWeight.Bold) },
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
            // SECCIÓN: Impresión de Ticket
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Formato de Ticket",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    HorizontalDivider()

                    OutlinedTextField(
                        value = headerText,
                        onValueChange = { headerText = it },
                        label = { Text("Encabezado (Título del Ticket)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = footerText,
                        onValueChange = { footerText = it },
                        label = { Text("Pie de Página") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // Aquí podrás agregar más Secciones (Tarjetas) en el futuro
            // ...

            Button(
                onClick = {
                    onSave(headerText, footerText)
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
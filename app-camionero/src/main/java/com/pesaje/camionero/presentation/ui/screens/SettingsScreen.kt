package com.pesaje.camionero.presentation.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Schedule
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
import com.pesaje.camionero.presentation.ui.theme.CardBackground
import com.pesaje.camionero.presentation.ui.theme.SaveYellow
import com.pesaje.camionero.R
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
    var expandidoEmpresa by remember { mutableStateOf(false) }

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
            // Título y descripción explicativa
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Edición de tickets de impresión",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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

            Spacer(modifier=Modifier.height(8.dp))

            //==================================================
            // ======= SECCIÓN ACERCA DE LA EMPRESA ================

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                onClick = { expandidoEmpresa = !expandidoEmpresa }
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Acerca de la empresa",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }

                        Icon(
                            imageVector = if (expandidoEmpresa) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expandidoEmpresa) "Colapsar" else "Expandir",
                            tint = Color.Black
                        )
                    }

                    AnimatedVisibility(visible = expandidoEmpresa) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider()

                            Image(
                                painter = painterResource(id = R.drawable.logolebenpro),
                                contentDescription = "Logo LEBENPRO",
                                modifier = Modifier
                                    .height(72.dp)
                                    .align(Alignment.CenterHorizontally)
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "LEBENPRO S.A. DE C.V.",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )

                            // Dirección
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Calle 2 de Abril 280, Nueva Villahermosa, 86000 Villahermosa, Tab.",
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }

                            // Teléfono
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "993 312 4417",
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }

                            // Email
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "lebenpro@lebenpro.com",
                                    fontSize = 14.sp,
                                    color = Color.Black
                                )
                            }

                            // Horario
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Horario de atención:",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                    Text(
                                        text = "• Lun a Vie: 9:00 a.m. a 6:00 p.m.\n• Sáb: 9:00 a.m. a 2:00 p.m.",
                                        fontSize = 14.sp,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // === PIE DE PÁGINA DE LA PANTALLA: VERSIÓN (GRIS, CHICO Y CENTRADO) ===
            // =========================================================================
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Versión 1.0.0",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}
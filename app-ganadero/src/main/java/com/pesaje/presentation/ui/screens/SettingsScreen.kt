package com.pesaje.presentation.ui.screens

import android.Manifest
import com.pesaje.presentation.ui.theme.ConnectedGreen
import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pesaje.core.data.local.SettingsDataStore
import com.pesaje.presentation.ui.theme.CardBackground
import com.pesaje.presentation.ui.theme.SaveYellow
import com.pesaje.pesaje.R

private fun tienePermisoBluetooth(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

@SuppressLint("MissingPermission")
private fun obtenerDispositivosVinculados(context: Context): List<String> {
    if (!tienePermisoBluetooth(context)) return emptyList()
    val adapter = context.getSystemService(BluetoothManager::class.java)?.adapter ?: return emptyList()
    return try {
        adapter.bondedDevices.mapNotNull { it.name }.distinct().sorted()
    } catch (e: SecurityException) {
        emptyList()
    }
}

// Dropdown genérico de solo selección
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CampoDropdown(
    label: String,
    opciones: List<String>,
    seleccion: String,
    onSeleccion: (String) -> Unit,
    placeholder: String = "Selecciona una opción"
) {
    var expandido by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expandido,
        onExpandedChange = { expandido = it }
    ) {
        OutlinedTextField(
            value = seleccion,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandido) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp)
        )
        ExposedDropdownMenu(
            expanded = expandido,
            onDismissRequest = { expandido = false }
        ) {
            if (opciones.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Sin dispositivos vinculados") },
                    onClick = { expandido = false },
                    enabled = false
                )
            }
            opciones.forEach { opcion ->
                DropdownMenuItem(
                    text = { Text(opcion) },
                    onClick = {
                        onSeleccion(opcion)
                        expandido = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    initialHeader: String,
    initialFooter: String,
    initialIndicatorFormat: String,
    initialIndicatorDevice: String,
    initialPrinterDevice: String,
    onBackClick: () -> Unit,
    onSave: (
        header: String,
        footer: String,
        indicatorFormat: String,
        indicatorDevice: String,
        printerDevice: String
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    var headerText by remember(initialHeader) { mutableStateOf(initialHeader) }
    var footerText by remember(initialFooter) { mutableStateOf(initialFooter) }
    var indicatorFormat by remember(initialIndicatorFormat) { mutableStateOf(initialIndicatorFormat) }
    var indicatorDevice by remember(initialIndicatorDevice) { mutableStateOf(initialIndicatorDevice) }
    var printerDevice by remember(initialPrinterDevice) { mutableStateOf(initialPrinterDevice) }
    var expandidoEmpresa by remember { mutableStateOf(false) }

    // Dispositivos Bluetooth vinculados (pide permiso en Android 12+ si falta)
    var dispositivos by remember { mutableStateOf(obtenerDispositivosVinculados(context)) }
    val permisoLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { dispositivos = obtenerDispositivosVinculados(context) }

    LaunchedEffect(Unit) {
        if (!tienePermisoBluetooth(context)) {
            permisoLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }

    // Solo dispositivos que siguen vinculados; la selección actual se sigue mostrando en su campo
    val opcionesIndicador = dispositivos
    val opcionesImpresora = dispositivos

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
            // ===== 1. DISPOSITIVOS =====
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
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Dispositivos",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        TextButton(
                            onClick = { dispositivos = obtenerDispositivosVinculados(context) },
                            colors = ButtonDefaults.textButtonColors(contentColor = ConnectedGreen)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bluetooth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Actualizar", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    HorizontalDivider()

                    // a) Indicador de peso (Bluetooth vinculados)
                    CampoDropdown(
                        label = "Indicador de peso",
                        opciones = opcionesIndicador,
                        seleccion = indicatorDevice,
                        onSeleccion = { indicatorDevice = it },
                        placeholder = "Selecciona un dispositivo"
                    )

                    // b) Formato de datos del indicador (cómo se lee la trama)
                    CampoDropdown(
                        label = "Formato de datos del indicador",
                        opciones = SettingsDataStore.MODELOS_FORMATO,
                        seleccion = indicatorFormat,
                        onSeleccion = { indicatorFormat = it }
                    )

                    // c) Impresora (Bluetooth vinculados)
                    CampoDropdown(
                        label = "Impresora",
                        opciones = opcionesImpresora,
                        seleccion = printerDevice,
                        onSeleccion = { printerDevice = it },
                        placeholder = "Selecciona un dispositivo"
                    )
                }
            }

            // ===== 2. EDICIÓN DE TICKET =====
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Edición de ticket de impresión",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

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
                        singleLine = false,
                        minLines = 2,
                        maxLines = Int.MAX_VALUE,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default // Enter = salto de línea
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    OutlinedTextField(
                        value = footerText,
                        onValueChange = { footerText = it },
                        label = { Text("Pie de Página") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        minLines = 2,
                        maxLines = Int.MAX_VALUE,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Default
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // ===== 3. BOTÓN GUARDAR =====
            Button(
                onClick = {
                    onSave(
                        headerText,
                        footerText,
                        indicatorFormat,
                        indicatorDevice,
                        printerDevice
                    )
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
            Spacer(modifier = Modifier.height(12.dp))

            // ===== 4. ACERCA DE LA EMPRESA (colapsable) =====
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

            // ===== 5. VERSIÓN DE LA APP =====
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Versión 1.0.0",
                fontSize = 12.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Normal,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}
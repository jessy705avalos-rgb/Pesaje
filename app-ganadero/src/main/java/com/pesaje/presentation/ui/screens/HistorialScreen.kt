package com.pesaje.presentation.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pesaje.core.data.local.RegistroPesajeGanado
import com.pesaje.presentation.ui.theme.ConnectedGreen
import com.pesaje.presentation.ui.utils.CsvExporter
import com.pesaje.presentation.viewmodel.HistorialViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    viewModel: HistorialViewModel,
    modifier: Modifier = Modifier
) {
    val registros by viewModel.registros.collectAsState()
    val context = LocalContext.current

    var mostrarDialogoBorrar by remember { mutableStateOf(false) }
    var mostrarDialogoExportar by remember { mutableStateOf(false) }
    var nombreArchivo by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Registros") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ConnectedGreen,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        if (registros.isEmpty()) {
                            Toast.makeText(context, "No hay registros para exportar", Toast.LENGTH_SHORT).show()
                        } else {
                            nombreArchivo = "registros_pesaje"
                            mostrarDialogoExportar = true
                        }
                    }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Exportar")
                    }
                    IconButton(onClick = { mostrarDialogoBorrar = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Borrar todo")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            TablaRegistros(registros = registros)
        }
    }

    // Diálogo de confirmación para exportar a CSV
    if (mostrarDialogoExportar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoExportar = false },
            title = { Text("Exportar CSV") },
            text = {
                Column {
                    Text("Ingresa el nombre del archivo a exportar:")
                    OutlinedTextField(
                        value = nombreArchivo,
                        onValueChange = { nombreArchivo = it },
                        label = { Text("Nombre del archivo") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val nombreFinal = nombreArchivo.trim()
                    if (nombreFinal.isNotEmpty()) {
                        mostrarDialogoExportar = false
                        CsvExporter.exportarYCompartir(
                            context = context,
                            nombreArchivo = nombreFinal,
                            registros = registros
                        )
                    } else {
                        Toast.makeText(context, "Escribe un nombre válido", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoExportar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Diálogo de confirmación para borrar
    if (mostrarDialogoBorrar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoBorrar = false },
            title = { Text("Confirmación") },
            text = { Text("¿Está seguro que desea borrar todos los registros? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.borrarTodos()
                    mostrarDialogoBorrar = false
                }) {
                    Text("Borrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoBorrar = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
private fun TablaRegistros(registros: List<RegistroPesajeGanado>) {
    val scrollHorizontal = rememberScrollState()
    val scrollVertical = rememberScrollState()

    val maxCaracteresArete = remember(registros) {
        val maxEnLista = registros.maxOfOrNull { it.arete.length } ?: 0
        maxOf(5, maxEnLista)
    }
    val anchoArete = (maxCaracteresArete * 10).dp.coerceAtLeast(80.dp)

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .horizontalScroll(scrollHorizontal)
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .verticalScroll(scrollVertical)
            ) {
                FilaEncabezado(anchoArete = anchoArete)
                HorizontalDivider()

                registros.forEach { registro ->
                    FilaRegistro(registro = registro, anchoArete = anchoArete)
                    HorizontalDivider()
                }
            }
        }
        BarraDeScroll(scrollState = scrollHorizontal)
    }
}

@Composable
private fun BarraDeScroll(scrollState: androidx.compose.foundation.ScrollState) {
    val coroutineScope = rememberCoroutineScope()
    val maxValue = scrollState.maxValue.coerceAtLeast(1)
    val progreso = scrollState.value.toFloat() / maxValue.toFloat()
    val density = androidx.compose.ui.platform.LocalDensity.current

    var anchoBarraTotalPx by remember { mutableStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(16.dp)
            .padding(horizontal = 8.dp)
            .onGloballyPositioned { coordinates ->
                anchoBarraTotalPx = coordinates.size.width.toFloat()
            }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    coroutineScope.launch {
                        val nuevoValor = (scrollState.value + dragAmount.x.toInt())
                            .coerceIn(0, scrollState.maxValue)
                        scrollState.scrollTo(nuevoValor)
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .align(Alignment.CenterStart)
                .background(Color.LightGray, RoundedCornerShape(3.dp))
        )

        val anchoBarritaPx = anchoBarraTotalPx * 0.3f
        val espacioDisponiblePx = anchoBarraTotalPx - anchoBarritaPx
        val offsetPx = progreso * espacioDisponiblePx
        val offsetDp = with(density) { offsetPx.toDp() }

        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = 0.3f)
                .height(6.dp)
                .align(Alignment.CenterStart)
                .offset(x = offsetDp)
                .background(ConnectedGreen, RoundedCornerShape(3.dp))
        )
    }
}

@Composable
private fun FilaEncabezado(anchoArete: androidx.compose.ui.unit.Dp) {
    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
        Text("ID", modifier = Modifier.width(50.dp).padding(end = 16.dp), fontWeight = FontWeight.Bold)
        Text("Arete", modifier = Modifier.width(anchoArete).padding(end = 16.dp), fontWeight = FontWeight.Bold)
        Text("Sexo", modifier = Modifier.width(80.dp).padding(end = 16.dp), fontWeight = FontWeight.Bold)
        Text("Peso", modifier = Modifier.width(100.dp).padding(end = 16.dp), fontWeight = FontWeight.Bold)
        Text("Fecha", modifier = Modifier.width(180.dp), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun FilaRegistro(registro: RegistroPesajeGanado, anchoArete: androidx.compose.ui.unit.Dp) {
    Row(modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)) {
        Text("${registro.id}", modifier = Modifier.width(50.dp).padding(end = 16.dp), maxLines = 1)
        Text(registro.arete, modifier = Modifier.width(anchoArete).padding(end = 16.dp), maxLines = 1)
        Text(registro.sexo, modifier = Modifier.width(80.dp).padding(end = 16.dp), maxLines = 1)
        Text("${registro.peso} kg", modifier = Modifier.width(100.dp).padding(end = 16.dp), maxLines = 1)
        Text(registro.fecha, modifier = Modifier.width(180.dp), maxLines = 1)
    }
}
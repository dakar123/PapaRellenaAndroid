package com.example.paparellena.ui

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreenContent(
    discoveredGames: List<NsdServiceInfo>,
    onHostGame: (String, Int) -> Unit, // username, timeInMinutes
    onJoinGame: (String, NsdServiceInfo) -> Unit, // username, service
    onRefreshDiscovery: () -> Unit
) {
    var showHostDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf<NsdServiceInfo?>(null) }
    var username by remember { mutableStateOf("") }
    
    val timeOptions = listOf(1, 2, 4)
    var selectedTime by remember { mutableIntStateOf(timeOptions[0]) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "PAPA RELLENA",
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Partidas Disponibles:", fontWeight = FontWeight.Bold)
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            if (discoveredGames.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Buscando partidas...", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(discoveredGames) { service ->
                        ListItem(
                            headlineContent = { Text(service.serviceName) },
                            supportingContent = { 
                                val hostAddr = service.host?.hostAddress ?: "Resolviendo..."
                                Text("$hostAddr:${service.port}") 
                            },
                            modifier = Modifier.clickable {
                                selectedService = service
                                showJoinDialog = true
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onRefreshDiscovery,
                modifier = Modifier.weight(1f)
            ) {
                Text("Refrescar")
            }
            
            Button(
                onClick = { showHostDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Crear Partida")
            }
        }
    }

    if (showHostDialog) {
        AlertDialog(
            onDismissRequest = { showHostDialog = false },
            title = { Text("Configurar Partida") },
            text = {
                Column {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Tu Nombre (Nombre de la Partida)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Tiempo de partida:", fontWeight = FontWeight.Bold)
                    Column(Modifier.selectableGroup()) {
                        timeOptions.forEach { time ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .selectable(
                                        selected = (time == selectedTime),
                                        onClick = { selectedTime = time },
                                        role = Role.RadioButton
                                    )
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = (time == selectedTime),
                                    onClick = null 
                                )
                                Text(
                                    text = "$time min",
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (username.isNotBlank()) {
                            onHostGame(username, selectedTime)
                            showHostDialog = false
                        }
                    }
                ) {
                    Text("Crear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showHostDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showJoinDialog && selectedService != null) {
        AlertDialog(
            onDismissRequest = { showJoinDialog = false },
            title = { Text("Unirse a: ${selectedService?.serviceName}") },
            text = {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Tu Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (username.isNotBlank()) {
                            onJoinGame(username, selectedService!!)
                            showJoinDialog = false
                        }
                    }
                ) {
                    Text("Entrar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJoinDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

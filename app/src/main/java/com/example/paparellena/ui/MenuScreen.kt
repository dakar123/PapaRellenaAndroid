package com.example.paparellena.ui

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreenContent(
    discoveredGames: List<NsdServiceInfo>,
    onHostGame: (String, Int) -> Unit,
    onJoinGame: (String, NsdServiceInfo) -> Unit,
    onRefreshDiscovery: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    var showJoinList by remember { mutableStateOf(false) }
    var selectedService by remember { mutableStateOf<NsdServiceInfo?>(null) }
    var username by remember { mutableStateOf("") }
    var selectedTime by remember { mutableIntStateOf(60) }
    var isHostingAction by remember { mutableStateOf(true) }

    val potatoBrown = Color(0xFF8B4513)
    val potatoYellow = Color(0xFFF4D03F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF5E6))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(RoundedCornerShape(40.dp))
                .background(potatoBrown),
            contentAlignment = Alignment.Center
        ) {
            Text("🥔", fontSize = 64.sp)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "PAPA RELLENA",
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold,
            color = potatoBrown
        )
        
        Text(
            text = "¡No dejes que se queme!",
            fontSize = 18.sp,
            color = Color(0xFF5D4037)
        )

        Spacer(modifier = Modifier.height(48.dp))

        if (!showJoinList) {
            Button(
                onClick = { 
                    isHostingAction = true
                    showNameDialog = true 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = potatoBrown)
            ) {
                Text("CREAR PARTIDA", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { 
                    showJoinList = true
                    onRefreshDiscovery()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = potatoYellow, contentColor = potatoBrown)
            ) {
                Text("BUSCAR PARTIDAS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Partidas Disponibles", fontWeight = FontWeight.Bold, color = potatoBrown)
                IconButton(onClick = onRefreshDiscovery) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refrescar", tint = potatoBrown)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(discoveredGames) { service ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                selectedService = service
                                isHostingAction = false
                                showNameDialog = true
                            },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Text(
                            text = service.serviceName,
                            modifier = Modifier.padding(16.dp),
                            color = potatoBrown
                        )
                    }
                }
            }
            
            Button(
                onClick = { showJoinList = false },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text("VOLVER")
            }
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text(if (isHostingAction) "Configurar Partida" else "Unirse a Partida") },
            text = {
                Column {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Tu Apodo") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    if (isHostingAction) {
                        Spacer(modifier = Modifier.height(16.dp))
                        val minutes = selectedTime / 60
                        val seconds = selectedTime % 60
                        Text("Tiempo máximo: ${minutes}m ${seconds}s")
                        Slider(
                            value = selectedTime.toFloat(),
                            onValueChange = { selectedTime = it.toInt() },
                            valueRange = 60f..300f,
                            steps = 3
                        )
                        Text(
                            "El juego terminará aleatoriamente en los últimos 10 segundos.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (username.isNotBlank()) {
                            showNameDialog = false
                            if (isHostingAction) {
                                onHostGame(username, selectedTime)
                            } else {
                                selectedService?.let { onJoinGame(username, it) }
                            }
                        }
                    }
                ) {
                    Text("ACEPTAR")
                }
            }
        )
    }
}

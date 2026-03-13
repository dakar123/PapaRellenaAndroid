package com.example.paparellena.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LobbyScreenContent(
    players: List<Player>,
    isHost: Boolean,
    onStartGame: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SALA DE ESPERA",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = if (isHost) "Eres el Host" else "Esperando al Host...",
            color = if (isHost) Color.Blue else Color.Gray,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(24.dp))
        
        Text("Jugadores conectados (${players.size}):", fontWeight = FontWeight.Bold)
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            LazyColumn {
                items(players) { player ->
                    ListItem(
                        headlineContent = { Text(player.name) },
                        leadingContent = { Text("👤", fontSize = 24.sp) }
                    )
                    HorizontalDivider()
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f)
            ) {
                Text("Salir")
            }
            
            if (isHost) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier.weight(1f),
                    enabled = players.size >= 2
                ) {
                    Text("Iniciar Partida")
                }
            }
        }
        
        if (isHost && players.size < 2) {
            Text(
                "Se necesitan al menos 2 jugadores",
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

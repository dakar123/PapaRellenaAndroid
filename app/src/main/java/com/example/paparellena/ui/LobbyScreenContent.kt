package com.example.paparellena.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    val potatoBrown = Color(0xFF8B4513)
    val potatoLight = Color(0xFFD2B48C)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF5E6))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "SALA DE ESPERA",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = potatoBrown
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Surface(
            color = if (isHost) Color(0xFFF4D03F) else potatoLight,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.padding(bottom = 24.dp)
        ) {
            Text(
                text = if (isHost) " ERES EL HOST " else " ESPERANDO AL HOST... ",
                color = potatoBrown,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }

        Text(
            "Jugadores en la olla (${players.size}):",
            fontWeight = FontWeight.Bold,
            color = Color(0xFF5D4037),
            modifier = Modifier.align(Alignment.Start)
        )
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 12.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(players) { player ->
                    ListItem(
                        headlineContent = { 
                            Text(
                                player.name, 
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF5D4037)
                            ) 
                        },
                        leadingContent = { 
                            Text(if (player.hasPotato) "🥔" else "👤", fontSize = 28.sp) 
                        },
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                }
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (isHost) {
                Button(
                    onClick = onStartGame,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = players.size >= 2,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = potatoBrown)
                ) {
                    Text("¡EMPEZAR YA!", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
            
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = potatoBrown)
            ) {
                Text("ABANDONAR", fontWeight = FontWeight.Bold)
            }
        }
        
        if (isHost && players.size < 2) {
            Text(
                "Se necesitan al menos 2 papas para empezar",
                color = Color(0xFFE74C3C),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

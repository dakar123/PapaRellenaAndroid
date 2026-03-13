package com.example.paparellena.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MenuScreen(
    onHostGame: () -> Unit,
    onJoinGame: () -> Unit
) {
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
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onHostGame,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Text("Crear Partida (Host)", fontSize = 18.sp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = onJoinGame,
            modifier = Modifier.fillMaxWidth(0.8f).height(56.dp)
        ) {
            Text("Unirse a Partida", fontSize = 18.sp)
        }
    }
}

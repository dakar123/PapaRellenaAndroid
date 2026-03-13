package com.example.paparellena.ui

import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
    onJoinGame: (String, NsdServiceInfo?) -> Unit,
    onRefreshDiscovery: () -> Unit
) {
    var showHostDialog by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    
    val potatoBrown = Color(0xFF8B4513)
    val potatoLight = Color(0xFFD2B48C)
    val potatoYellow = Color(0xFFF4D03F)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFDF5E6)) // Cream background
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Icon Placeholder (Potato)
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
        
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Tu Apodo") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = potatoBrown,
                focusedLabelColor = potatoBrown
            )
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { if(username.isNotBlank()) onHostGame(username, 1) },
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
            onClick = { if(username.isNotBlank()) onJoinGame(username, null) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = potatoYellow, contentColor = potatoBrown)
        ) {
            Text("BUSCAR PARTIDAS", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        }
    }
}

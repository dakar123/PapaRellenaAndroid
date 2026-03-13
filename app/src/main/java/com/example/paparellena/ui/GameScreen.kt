package com.example.paparellena.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

data class Player(
    val id: String,
    val name: String,
    val hasPotato: Boolean = false,
    val isRequesting: Boolean = false
)

@Composable
fun GameScreen(
    players: List<Player>,
    currentPlayerId: String,
    timeLeftSeconds: Int,
    onPassPotato: (String) -> Unit, // target player id
    onReceivePotato: () -> Unit,
    onRequestPotato: () -> Unit
) {
    val currentPlayer = players.find { it.id == currentPlayerId }
    val hasPotato = currentPlayer?.hasPotato ?: false
    
    // Last 10 seconds red flash effect
    val isUrgent = timeLeftSeconds in 1..10
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    val flashAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isUrgent) 0.4f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "flashAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isUrgent) Color.Red.copy(alpha = flashAlpha) else MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (hasPotato) "¡TIENES LA PAPA!" else "Busca la papa...",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 24.dp),
                color = if (hasPotato) Color.Red else MaterialTheme.colorScheme.onBackground
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(players) { player ->
                    PlayerItem(
                        player = player,
                        isSelf = player.id == currentPlayerId,
                        canReceive = hasPotato && player.id != currentPlayerId,
                        onClick = {
                            if (hasPotato && player.id != currentPlayerId) {
                                onPassPotato(player.id)
                            }
                        }
                    )
                }
            }

            if (!hasPotato) {
                Button(
                    onClick = onRequestPotato,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFA500))
                ) {
                    Text("SOLICITAR PAPA", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PlayerItem(
    player: Player,
    isSelf: Boolean,
    canReceive: Boolean,
    onClick: () -> Unit
) {
    // Blinking effect if player is requesting
    val infiniteTransition = rememberInfiniteTransition(label = "blink")
    val blinkColor by animateColorAsState(
        targetValue = if (player.isRequesting) Color.Yellow else Color.Transparent,
        animationSpec = if (player.isRequesting) {
            infiniteRepeatable(tween(500), RepeatMode.Reverse)
        } else {
            snap()
        },
        label = "blinkColor"
    )

    Card(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .clickable(enabled = canReceive) { onClick() }
            .border(
                width = if (player.hasPotato) 4.dp else 1.dp,
                color = if (player.hasPotato) Color.Red else Color.Gray,
                shape = MaterialTheme.shapes.medium
            )
            .background(blinkColor),
        colors = CardDefaults.cardColors(
            containerColor = if (player.hasPotato) Color.Red.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(if (player.hasPotato) Color.Red else Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (player.hasPotato) "🥔" else "👤",
                        fontSize = 32.sp
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isSelf) "${player.name} (Tú)" else player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (player.isRequesting) {
                    Text(
                        text = "¡QUIERE LA PAPA!",
                        color = Color(0xFFE65100),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

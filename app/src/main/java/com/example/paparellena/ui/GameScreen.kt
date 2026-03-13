package com.example.paparellena.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColor
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen(
    players: List<Player>,
    currentPlayerId: String,
    timeLeftSeconds: Int,
    countdown: Int,
    onPassPotato: (String) -> Unit
) {
    val context = LocalContext.current
    val currentPlayer = players.find { it.id == currentPlayerId }
    val hasPotato = currentPlayer?.hasPotato ?: false
    
    // Vibrate when potato is received
    LaunchedEffect(hasPotato) {
        if (hasPotato) {
            vibrate(context)
        }
    }

    val isUrgent = timeLeftSeconds in 1..5
    val infiniteTransition = rememberInfiniteTransition(label = "flash")
    
    // Background color animation for urgency
    val urgentColor by infiniteTransition.animateColor(
        initialValue = Color.Red.copy(alpha = 0.4f),
        targetValue = Color.Yellow.copy(alpha = 0.4f),
        animationSpec = infiniteRepeatable(
            animation = tween(250, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "urgentBg"
    )

    // Background color animation for holding the potato
    val holdingColor by infiniteTransition.animateColor(
        initialValue = Color.Red.copy(alpha = 0.1f),
        targetValue = Color.Red.copy(alpha = 0.6f),
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "holdingBg"
    )

    val backgroundColor = when {
        isUrgent -> urgentColor
        hasPotato -> holdingColor
        else -> MaterialTheme.colorScheme.background
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (countdown > 0) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = countdown.toString(),
                        fontSize = 120.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                Text(
                    text = if (hasPotato) "¡TIENES LA PAPA!" else "¡PÁSALO!",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 24.dp),
                    color = if (hasPotato || isUrgent) Color.Black else MaterialTheme.colorScheme.onBackground
                )

                Text(
                    text = "Tiempo: $timeLeftSeconds s",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isUrgent) Color.Red else MaterialTheme.colorScheme.onBackground
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
                            onClick = { onPassPotato(player.id) }
                        )
                    }
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
    Card(
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .clickable(enabled = canReceive) { onClick() }
            .border(
                width = if (player.hasPotato) 4.dp else 1.dp,
                color = if (player.hasPotato) Color.Red else Color.Gray,
                shape = MaterialTheme.shapes.medium
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (player.hasPotato) Color.Red.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
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
            }
        }
    }
}

private fun vibrate(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(300)
    }
}

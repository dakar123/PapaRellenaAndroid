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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun GameScreen(
    players: List<Player>,
    currentPlayerId: String,
    timeLeftSeconds: Int,
    countdown: Int,
    holdTimeMs: Long,
    onPassPotato: (String) -> Unit
) {
    val context = LocalContext.current
    val currentPlayer = players.find { it.id == currentPlayerId }
    val hasPotato = currentPlayer?.hasPotato ?: false
    
    // Logic: 2.5s minimum hold time, 5s maximum hold time.
    val minHoldTime = 2500L
    val canPass = hasPotato && holdTimeMs >= minHoldTime
    val isBurning = hasPotato && holdTimeMs > 3500L

    // Move positions: Shuffle players whenever the potato changes owner or player list changes.
    val potatoOwnerId = remember(players) { players.find { it.hasPotato }?.id }
    val shuffledPlayers = remember(potatoOwnerId, players.size) { players.shuffled() }

    LaunchedEffect(hasPotato) {
        if (hasPotato) {
            vibrate(context, 300)
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "gameEffects")
    
    val burningColor by infiniteTransition.animateColor(
        initialValue = Color(0xFFE74C3C),
        targetValue = Color(0xFFF39C12),
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "burningBg"
    )

    val backgroundColor = when {
        isBurning -> burningColor
        hasPotato -> Color(0xFFD2B48C).copy(alpha = 0.3f)
        else -> Color(0xFFFDF5E6)
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
            Text(
                text = if (hasPotato) "" else "¡PREPÁRATE!",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isBurning) Color.Red else Color(0xFF5D4037),
                modifier = Modifier.padding(top = 32.dp, bottom = 8.dp)
            )

            // The progress bar and "Pasala ya/La tienes" texts have been removed as requested.
            // Background colors and potato icon provide the necessary feedback.

            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(shuffledPlayers) { player ->
                    PlayerItem(
                        player = player,
                        isSelf = player.id == currentPlayerId,
                        canReceive = canPass && player.id != currentPlayerId,
                        onClick = { onPassPotato(player.id) }
                    )
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
            .aspectRatio(0.9f)
            .clickable(enabled = canReceive) { onClick() }
            .border(
                width = if (player.hasPotato) 4.dp else 2.dp,
                color = if (player.hasPotato) Color(0xFFE74C3C) else Color(0xFF8B4513),
                shape = RoundedCornerShape(16.dp)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (player.hasPotato) Color(0xFFFADBD8) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (player.hasPotato) Color(0xFFF4D03F) else Color(0xFFD2B48C)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (player.hasPotato) "🥔" else "👤",
                        fontSize = 36.sp
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = if (isSelf) "Tú" else player.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF5D4037),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

private fun vibrate(context: Context, duration: Long) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(duration)
    }
}

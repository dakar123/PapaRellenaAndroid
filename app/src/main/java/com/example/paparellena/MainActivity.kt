package com.example.paparellena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.paparellena.ui.GameScreen
import com.example.paparellena.ui.MenuScreen
import com.example.paparellena.ui.Player
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("menu") }
            var username by remember { mutableStateOf("") }
            var gameTimeMinutes by remember { mutableIntStateOf(1) }
            
            // Game State
            var players by remember { 
                mutableStateOf(listOf<Player>()) 
            }
            var timeLeft by remember { mutableIntStateOf(60) }
            val currentPlayerId = "me" // Simplified for local demo

            if (currentScreen == "menu") {
                MenuScreen(
                    onHostGame = { name, time ->
                        username = name
                        gameTimeMinutes = time
                        timeLeft = time * 60
                        players = listOf(
                            Player("me", name, hasPotato = true),
                            Player("2", "Jugador 2"),
                            Player("3", "Jugador 3"),
                            Player("4", "Jugador 4")
                        )
                        currentScreen = "game"
                    },
                    onJoinGame = { name ->
                        username = name
                        timeLeft = 60 // Default or from host
                        players = listOf(
                            Player("1", "Host", hasPotato = true),
                            Player("me", name),
                            Player("3", "Jugador 3")
                        )
                        currentScreen = "game"
                    }
                )
            } else if (currentScreen == "game") {
                // Simple timer simulation
                LaunchedEffect(Unit) {
                    while (timeLeft > 0) {
                        delay(1000)
                        timeLeft--
                    }
                }

                GameScreen(
                    players = players,
                    currentPlayerId = currentPlayerId,
                    timeLeftSeconds = timeLeft,
                    onPassPotato = { targetId ->
                        players = players.map { 
                            it.copy(
                                hasPotato = it.id == targetId,
                                isRequesting = if (it.id == targetId) false else it.isRequesting
                            ) 
                        }
                    },
                    onReceivePotato = {
                        // Logic when receiving
                    },
                    onRequestPotato = {
                        players = players.map {
                            if (it.id == currentPlayerId) it.copy(isRequesting = true) else it
                        }
                    }
                )
            }
        }
    }
}

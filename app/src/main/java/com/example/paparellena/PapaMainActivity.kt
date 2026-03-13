package com.example.paparellena

import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.paparellena.ui.GameScreen
import com.example.paparellena.ui.MenuScreenContent
import com.example.paparellena.ui.Player
import kotlinx.coroutines.delay

class PapaMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var currentScreen by remember { mutableStateOf("menu") }
            var username by remember { mutableStateOf("") }
            
            // Discovery State
            val discoveredGames by remember { mutableStateOf(listOf<NsdServiceInfo>()) }

            // Game State
            var players by remember { 
                mutableStateOf(listOf<Player>()) 
            }
            var timeLeft by remember { mutableIntStateOf(60) }
            val currentPlayerId = "me"

            if (currentScreen == "menu") {
                MenuScreenContent(
                    discoveredGames = discoveredGames,
                    onHostGame = { name, time ->
                        username = name
                        timeLeft = time * 60
                        players = listOf(
                            Player("me", name, hasPotato = true),
                            Player("2", "Jugador 2"),
                            Player("3", "Jugador 3"),
                            Player("4", "Jugador 4")
                        )
                        currentScreen = "game"
                    },
                    onJoinGame = { name, service ->
                        username = name
                        timeLeft = 60 
                        players = listOf(
                            Player("1", service.serviceName, hasPotato = true),
                            Player("me", name),
                            Player("3", "Jugador 3")
                        )
                        currentScreen = "game"
                    },
                    onRefreshDiscovery = {
                        // Refresh logic
                    }
                )
            } else if (currentScreen == "game") {
                LaunchedEffect(currentScreen) {
                    while (timeLeft > 0) {
                        delay(1000)
                        timeLeft--
                    }
                }

                GameScreen(
                    players = players,
                    currentPlayerId = currentPlayerId,
                    timeLeftSeconds = timeLeft,
                    countdown = 0,
                    onPassPotato = { targetId: String ->
                        players = players.map { 
                            it.copy(
                                hasPotato = it.id == targetId,
                                isRequesting = if (it.id == targetId) false else it.isRequesting
                            ) 
                        }
                    }
                )
            }
        }
    }
}

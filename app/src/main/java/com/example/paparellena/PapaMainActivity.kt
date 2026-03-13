package com.example.paparellena

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import com.example.paparellena.ui.GameScreen
import com.example.paparellena.ui.MenuScreenContent
import com.example.paparellena.game.GameManager
import com.example.paparellena.game.Player
import kotlinx.coroutines.delay

class PapaMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val gameManager = GameManager.getInstance()

        setContent {
            var currentScreen by remember { mutableStateOf("menu") }
            var players by remember { mutableStateOf(gameManager.players) }
            var timeLeft by remember { mutableIntStateOf(0) }
            var holdTimeMs by remember { mutableLongStateOf(0L) }
            val localPlayerId = gameManager.localPlayer?.id ?: "me"

            DisposableEffect(Unit) {
                gameManager.setListener(object : GameManager.GameEventListener {
                    override fun onPlayerJoined(player: Player) {
                        players = ArrayList(gameManager.players)
                    }

                    override fun onGameStarted(starterId: String) {
                        players = ArrayList(gameManager.players)
                        currentScreen = "game"
                    }

                    override fun onPotatoReceived() {
                        players = ArrayList(gameManager.players)
                    }

                    override fun onPotatoPassed() {
                        players = ArrayList(gameManager.players)
                        holdTimeMs = 0
                    }

                    override fun onGameOver(loserId: String) {
                        // Handle Game Over
                    }

                    override fun onTick(seconds: Int) {
                        timeLeft = seconds
                    }

                    override fun onHoldTimeUpdate(elapsedMs: Long) {
                        holdTimeMs = elapsedMs
                    }
                })
                onDispose { gameManager.setListener(null) }
            }

            if (currentScreen == "menu") {
                MenuScreenContent(
                    discoveredGames = emptyList(),
                    onHostGame = { name, _ ->
                        gameManager.initAsHost(name, "127.0.0.1")
                        gameManager.startGame()
                        currentScreen = "game"
                    },
                    onJoinGame = { name, _ ->
                        gameManager.initAsClient(name, "127.0.0.1", "127.0.0.1")
                        currentScreen = "game"
                    },
                    onRefreshDiscovery = {}
                )
            } else if (currentScreen == "game") {
                GameScreen(
                    players = players,
                    currentPlayerId = localPlayerId,
                    timeLeftSeconds = timeLeft,
                    countdown = 0,
                    holdTimeMs = holdTimeMs,
                    onPassPotato = { targetId ->
                        if (gameManager.canPassPotato()) {
                            gameManager.passPotatoToNext()
                        }
                    }
                )
            }
        }
    }
}

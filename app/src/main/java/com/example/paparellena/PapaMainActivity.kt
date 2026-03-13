package com.example.paparellena

import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import com.example.paparellena.ui.GameScreen
import com.example.paparellena.ui.MenuScreenContent
import com.example.paparellena.game.GameManager
import com.example.paparellena.network.NsdHelper
import com.example.paparellena.utils.NetworkUtils
import com.example.paparellena.game.Player as GamePlayer
import com.example.paparellena.ui.Player as UiPlayer

class PapaMainActivity : ComponentActivity() {
    private lateinit var nsdHelper: NsdHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val gameManager = GameManager.getInstance()
        nsdHelper = NsdHelper(this)

        setContent {
            var currentScreen by remember { mutableStateOf("menu") }
            var players by remember { mutableStateOf(gameManager.players) }
            var timeLeft by remember { mutableIntStateOf(0) }
            var holdTimeMs by remember { mutableLongStateOf(0L) }
            var discoveredGames by remember { mutableStateOf(listOf<NsdServiceInfo>()) }
            
            val localPlayerId = gameManager.localPlayer?.id ?: ""
            val myIp = NetworkUtils.getLocalIpAddress(this@PapaMainActivity)

            DisposableEffect(Unit) {
                gameManager.setListener(object : GameManager.GameEventListener {
                    override fun onPlayerJoined(player: GamePlayer) {
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
                        // Opcional: navegar a pantalla de resultados o mostrar diálogo
                    }

                    override fun onTick(seconds: Int) {
                        timeLeft = seconds
                    }

                    override fun onHoldTimeUpdate(elapsedMs: Long) {
                        holdTimeMs = elapsedMs
                    }
                })
                onDispose { 
                    gameManager.setListener(null)
                    nsdHelper.stopDiscovery()
                    nsdHelper.unregisterService()
                }
            }

            when (currentScreen) {
                "menu" -> {
                    MenuScreenContent(
                        discoveredGames = discoveredGames,
                        onHostGame = { name, time ->
                            gameManager.reset()
                            gameManager.setInitialTime(time)
                            gameManager.initAsHost(name, myIp)
                            nsdHelper.registerService(8888, "$name's Potato Game")
                            currentScreen = "waiting"
                        },
                        onJoinGame = { name, service ->
                            gameManager.reset()
                            val hostIp = service.host?.hostAddress ?: ""
                            gameManager.initAsClient(name, myIp, hostIp)
                            currentScreen = "waiting"
                        },
                        onRefreshDiscovery = {
                            discoveredGames = emptyList()
                            nsdHelper.stopDiscovery()
                            nsdHelper.discoverServices(object : NsdHelper.DiscoveryCallback {
                                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                                    if (!discoveredGames.any { it.serviceName == serviceInfo.serviceName }) {
                                        discoveredGames = discoveredGames + serviceInfo
                                    }
                                }

                                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                                    discoveredGames = discoveredGames.filter { it.serviceName != serviceInfo.serviceName }
                                }
                            })
                        }
                    )
                }
                "waiting" -> {
                    Column {
                        Text("Esperando jugadores... (${players.size})")
                        players.forEach { p -> Text(p.name) }
                        if (gameManager.isHost && players.size >= 2) {
                            Button(onClick = { gameManager.startGame() }) {
                                Text("EMPEZAR JUEGO")
                            }
                        }
                        Button(onClick = { 
                            gameManager.reset()
                            nsdHelper.stopDiscovery()
                            nsdHelper.unregisterService()
                            currentScreen = "menu" 
                        }) {
                            Text("CANCELAR")
                        }
                    }
                }
                "game" -> {
                    GameScreen(
                        players = players.map { p ->
                            UiPlayer(
                                id = p.id,
                                name = p.name,
                                hasPotato = p.isHasPotato
                            )
                        },
                        currentPlayerId = localPlayerId,
                        timeLeftSeconds = timeLeft,
                        countdown = 0,
                        holdTimeMs = holdTimeMs,
                        onPassPotato = {
                            if (gameManager.canPassPotato()) {
                                gameManager.passPotatoToNext()
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdHelper.stopDiscovery()
        nsdHelper.unregisterService()
    }
}

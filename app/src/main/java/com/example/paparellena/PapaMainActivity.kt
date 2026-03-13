package com.example.paparellena

import android.net.nsd.NsdServiceInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
            val myIp = NetworkUtils.getLocalIpAddress(this@PapaMainActivity) ?: "127.0.0.1"

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
                        // Podríamos mostrar un diálogo aquí antes de volver al menú
                        // currentScreen = "menu"
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
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFFFDF5E6)) {
                when (currentScreen) {
                    "menu" -> {
                        MenuScreenContent(
                            discoveredGames = discoveredGames,
                            onHostGame = { name, time ->
                                gameManager.reset()
                                gameManager.setInitialTime(time)
                                gameManager.initAsHost(name, myIp)
                                nsdHelper.registerService(8888, "$name's Potato")
                                players = ArrayList(gameManager.players)
                                currentScreen = "waiting"
                            },
                            onJoinGame = { name, service ->
                                gameManager.reset()
                                // Para simplificar asumimos que el host es accesible por el nombre del servicio o IP resuelta
                                // En una red real se requiere nsdHelper.resolveService(service)
                                val hostIp = service.host?.hostAddress ?: myIp 
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
                        WaitingRoom(
                            players = players,
                            isHost = gameManager.isHost,
                            onStart = { gameManager.startGame() },
                            onCancel = {
                                gameManager.reset()
                                nsdHelper.stopDiscovery()
                                nsdHelper.unregisterService()
                                currentScreen = "menu"
                            }
                        )
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
    }

    override fun onDestroy() {
        super.onDestroy()
        nsdHelper.stopDiscovery()
        nsdHelper.unregisterService()
    }
}

@Composable
fun WaitingRoom(
    players: List<GamePlayer>,
    isHost: Boolean,
    onStart: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "SALA DE ESPERA",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF8B4513)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text("Jugadores conectados: ${players.size}", fontSize = 18.sp)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(players) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Text(player.name, modifier = Modifier.padding(16.dp), fontWeight = FontWeight.Medium)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (isHost) {
            Button(
                onClick = onStart,
                enabled = players.size >= 2,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B4513))
            ) {
                Text("EMPEZAR JUEGO")
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("SALIR")
        }
    }
}

package com.example.paparellena

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.*
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.paparellena.network.*
import com.example.paparellena.ui.GameScreen
import com.example.paparellena.ui.MenuScreenContent
import com.example.paparellena.ui.LobbyScreenContent
import com.example.paparellena.ui.Player
import com.example.paparellena.utils.SoundManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import java.util.*

class MainActivity : ComponentActivity() {
    private lateinit var nsdHelper: NsdHelper
    private var server: Server? = null
    private var client: Client? = null
    private lateinit var soundManager: SoundManager
    
    private val myId = UUID.randomUUID().toString()
    private val gson = Gson()
    
    private val masterPlayers = mutableListOf<Player>()
    private var isHost = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nsdHelper = NsdHelper(this)
        soundManager = SoundManager(this)

        setContent {
            val context = LocalContext.current
            var currentScreen by remember { mutableStateOf("menu") }
            var username by remember { mutableStateOf("") }
            var players by remember { mutableStateOf(listOf<Player>()) }
            var timeLeft by remember { mutableIntStateOf(60) }
            var countdown by remember { mutableIntStateOf(0) }
            var discoveredGames by remember { mutableStateOf(listOf<NsdServiceInfo>()) }
            var isGameOver by remember { mutableStateOf(false) }
            var gameResultText by remember { mutableStateOf("") }

            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val allGranted = results.values.all { it }
                if (!allGranted) {
                    Toast.makeText(context, "Se necesitan permisos para jugar", Toast.LENGTH_LONG).show()
                }
            }

            fun checkWifiAndPermissions(onSuccess: () -> Unit) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                if (!wifiManager.isWifiEnabled) {
                    Toast.makeText(context, "Activa el Wi-Fi", Toast.LENGTH_LONG).show()
                    return
                }
                val missingPermissions = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }
                if (missingPermissions.isEmpty()) onSuccess() else permissionLauncher.launch(permissionsToRequest)
            }

            LaunchedEffect(currentScreen) {
                if (currentScreen == "menu") {
                    startDiscovery { discoveredGames = it }
                } else {
                    nsdHelper.stopDiscovery()
                }
            }

            // Interface for incoming messages to update UI state
            DisposableEffect(Unit) {
                messageCallback = { msg ->
                    when (msg.type) {
                        GameMessage.TYPE_PLAYER_LIST -> {
                            val listType = object : TypeToken<List<Player>>() {}.type
                            players = gson.fromJson(msg.content, listType)
                        }
                        GameMessage.TYPE_START -> {
                            currentScreen = "game"
                        }
                        GameMessage.TYPE_COUNTDOWN -> {
                            countdown = msg.content.toInt()
                        }
                        GameMessage.TYPE_TICK -> {
                            timeLeft = msg.content.toInt()
                        }
                        GameMessage.TYPE_GAME_OVER -> {
                            isGameOver = true
                            gameResultText = "¡Perdió ${msg.content}!"
                        }
                    }
                }
                onDispose { messageCallback = null }
            }

            when (currentScreen) {
                "menu" -> MenuScreenContent(
                    discoveredGames = discoveredGames,
                    onHostGame = { name, time ->
                        checkWifiAndPermissions {
                            username = name
                            timeLeft = time * 60
                            isHost = true
                            startHost(name)
                            currentScreen = "lobby"
                        }
                    },
                    onJoinGame = { name, service ->
                        checkWifiAndPermissions {
                            username = name
                            isHost = false
                            startClient(service, name)
                            currentScreen = "lobby"
                        }
                    },
                    onRefreshDiscovery = {
                        checkWifiAndPermissions { startDiscovery { discoveredGames = it } }
                    }
                )
                "lobby" -> LobbyScreenContent(
                    players = players,
                    isHost = isHost,
                    onStartGame = {
                        if (isHost) {
                            hostStartGame(timeLeft)
                        }
                    },
                    onBack = {
                        resetGame()
                        currentScreen = "menu"
                    }
                )
                "game" -> Box {
                    GameScreen(
                        players = players,
                        currentPlayerId = myId,
                        timeLeftSeconds = timeLeft,
                        countdown = countdown,
                        onPassPotato = { targetId: String ->
                            if (!isGameOver && countdown == 0) {
                                val msg = GameMessage(GameMessage.TYPE_PASS_POTATO, myId, username, targetId)
                                client?.sendMessage(msg)
                            }
                        }
                    )

                    if (isGameOver) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("¡BOOM!", fontSize = 64.sp, fontWeight = FontWeight.Bold, color = Color.Red)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(gameResultText, fontSize = 24.sp, color = Color.White)
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(onClick = { 
                                    resetGame()
                                    currentScreen = "menu"
                                    isGameOver = false
                                }) { Text("Volver al Menú") }
                            }
                        }
                    }
                }
            }
        }
    }

    private var messageCallback: ((GameMessage) -> Unit)? = null

    private fun startDiscovery(onUpdate: (List<NsdServiceInfo>) -> Unit) {
        nsdHelper.stopDiscovery()
        val currentList = mutableListOf<NsdServiceInfo>()
        nsdHelper.discoverServices(object : NsdHelper.DiscoveryCallback {
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                runOnUiThread {
                    if (!currentList.any { it.serviceName == serviceInfo.serviceName }) {
                        currentList.add(serviceInfo)
                        onUpdate(ArrayList(currentList))
                    }
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                runOnUiThread {
                    currentList.removeAll { it.serviceName == serviceInfo.serviceName }
                    onUpdate(ArrayList(currentList))
                }
            }
        })
    }

    private fun resetGame() {
        server?.stop()
        server = null
        client?.disconnect()
        client = null
        nsdHelper.unregisterService()
        masterPlayers.clear()
        isHost = false
    }

    private fun startHost(name: String) {
        masterPlayers.clear()
        server = Server(object : Server.ServerCallback {
            override fun onMessageReceived(message: GameMessage) {
                handleHostLogic(message)
            }
            override fun onClientConnected(ip: String) {}
        })
        server?.start()
        nsdHelper.registerService(8888, name)
        
        startClientInternal("127.0.0.1", 8888, name)
    }

    private fun startClient(service: NsdServiceInfo, name: String) {
        val host = service.host?.hostAddress ?: "127.0.0.1"
        val port = service.port
        startClientInternal(host, port, name)
    }

    private fun startClientInternal(host: String, port: Int, name: String) {
        client = Client(host, port, object : Client.ClientCallback {
            override fun onMessageReceived(message: GameMessage) {
                runOnUiThread { messageCallback?.invoke(message) }
            }
            override fun onConnected() {
                client?.sendMessage(GameMessage(GameMessage.TYPE_JOIN, myId, name, ""))
            }
            override fun onDisconnected() {
                runOnUiThread { Toast.makeText(this@MainActivity, "Desconectado", Toast.LENGTH_SHORT).show() }
            }
        })
        client?.connect()
    }

    private fun handleHostLogic(msg: GameMessage) {
        when (msg.type) {
            GameMessage.TYPE_JOIN -> {
                synchronized(masterPlayers) {
                    if (!masterPlayers.any { it.id == msg.senderId }) {
                        masterPlayers.add(Player(msg.senderId, msg.senderName))
                        broadcastPlayerList()
                    }
                }
            }
            GameMessage.TYPE_PASS_POTATO -> {
                val targetId = msg.content
                synchronized(masterPlayers) {
                    for (i in masterPlayers.indices) {
                        masterPlayers[i] = masterPlayers[i].copy(hasPotato = masterPlayers[i].id == targetId)
                    }
                    broadcastPlayerList()
                }
            }
        }
    }

    private fun hostStartGame(totalTime: Int) {
        CoroutineScope(Dispatchers.Default).launch {
            server?.broadcast(GameMessage(GameMessage.TYPE_START, myId, ""))
            
            for (i in 3 downTo 1) {
                server?.broadcast(GameMessage(GameMessage.TYPE_COUNTDOWN, myId, i.toString()))
                delay(1000)
            }
            server?.broadcast(GameMessage(GameMessage.TYPE_COUNTDOWN, myId, "0"))

            synchronized(masterPlayers) {
                if (masterPlayers.isNotEmpty()) {
                    val randomIndex = (0 until masterPlayers.size).random()
                    for (i in masterPlayers.indices) {
                        masterPlayers[i] = masterPlayers[i].copy(hasPotato = i == randomIndex)
                    }
                }
            }
            broadcastPlayerList()

            var currentTimer = totalTime
            while (currentTimer > 0) {
                delay(1000)
                currentTimer--
                server?.broadcast(GameMessage(GameMessage.TYPE_TICK, myId, currentTimer.toString()))
            }
            
            val loser = synchronized(masterPlayers) { masterPlayers.find { it.hasPotato } }
            server?.broadcast(GameMessage(GameMessage.TYPE_GAME_OVER, myId, loser?.name ?: "Nadie"))
        }
    }

    private fun broadcastPlayerList() {
        val json = gson.toJson(masterPlayers)
        server?.broadcast(GameMessage(GameMessage.TYPE_PLAYER_LIST, myId, json))
    }
}

package com.example.paparellena

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.nsd.NsdServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
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
            var discoveredGames by remember { mutableStateOf(listOf<NsdServiceInfo>()) }
            var isGameOver by remember { mutableStateOf(false) }
            var gameResultText by remember { mutableStateOf("") }

            var showPermissionDialog by remember { mutableStateOf(false) }
            var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }

            val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.NEARBY_WIFI_DEVICES)
            } else {
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions()
            ) { results ->
                val allGranted = results.values.all { it }
                if (allGranted) {
                    pendingAction?.invoke()
                    pendingAction = null
                } else {
                    Toast.makeText(context, "Se necesitan permisos de red para jugar", Toast.LENGTH_LONG).show()
                }
            }

            fun checkWifiAndPermissions(onSuccess: () -> Unit) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                if (!wifiManager.isWifiEnabled) {
                    Toast.makeText(context, "Por favor, activa el Wi-Fi", Toast.LENGTH_LONG).show()
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val panelIntent = Intent(Settings.Panel.ACTION_WIFI)
                        context.startActivity(panelIntent)
                    } else {
                        context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                    }
                    return
                }

                val missingPermissions = permissionsToRequest.filter {
                    ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
                }

                if (missingPermissions.isEmpty()) {
                    onSuccess()
                } else {
                    pendingAction = onSuccess
                    permissionLauncher.launch(permissionsToRequest)
                }
            }

            // Discovery logic - auto refresh when in menu
            LaunchedEffect(currentScreen) {
                if (currentScreen == "menu") {
                    // We don't force permissions here to avoid annoying the user immediately, 
                    // but we start discovery if we have them.
                    val hasPermissions = permissionsToRequest.all {
                        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
                    }
                    if (hasPermissions) {
                        startDiscovery { games -> discoveredGames = games }
                    }
                } else {
                    nsdHelper.stopDiscovery()
                }
            }

            if (currentScreen == "menu") {
                MenuScreenContent(
                    discoveredGames = discoveredGames,
                    onHostGame = { name, time ->
                        checkWifiAndPermissions {
                            username = name
                            timeLeft = time * 60
                            isGameOver = false
                            startHost(name, time * 60) { msg ->
                                handleIncomingMessage(msg, 
                                    { players = it }, 
                                    { timeLeft = it }, 
                                    { winner -> 
                                        isGameOver = true
                                        gameResultText = winner
                                    }
                                )
                            }
                            currentScreen = "game"
                        }
                    },
                    onJoinGame = { name, service ->
                        checkWifiAndPermissions {
                            username = name
                            isGameOver = false
                            startClient(service, name) { msg ->
                                handleIncomingMessage(msg, 
                                    { players = it }, 
                                    { timeLeft = it }, 
                                    { winner -> 
                                        isGameOver = true
                                        gameResultText = winner
                                    }
                                )
                            }
                            currentScreen = "game"
                        }
                    },
                    onRefreshDiscovery = {
                        checkWifiAndPermissions {
                            discoveredGames = emptyList()
                            startDiscovery { games -> discoveredGames = games }
                        }
                    }
                )
            } else {
                Box {
                    GameScreen(
                        players = players,
                        currentPlayerId = myId,
                        timeLeftSeconds = timeLeft,
                        onPassPotato = { targetId ->
                            if (!isGameOver) {
                                val msg = GameMessage(GameMessage.TYPE_PASS_POTATO, myId, username, targetId)
                                client?.sendMessage(msg) ?: server?.broadcast(msg)
                            }
                        },
                        onReceivePotato = {},
                        onRequestPotato = {}
                    )

                    if (isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Red.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "¡BOOM!",
                                    fontSize = 64.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = gameResultText,
                                    fontSize = 24.sp,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(32.dp))
                                Button(onClick = { 
                                    currentScreen = "menu" 
                                    resetGame()
                                }) {
                                    Text("Volver al Menú")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

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
    }

    private fun startHost(name: String, time: Int, onMessage: (GameMessage) -> Unit) {
        masterPlayers.clear()
        masterPlayers.add(Player(myId, name, hasPotato = true))
        
        server = Server(object : Server.ServerCallback {
            override fun onMessageReceived(message: GameMessage) {
                handleHostLogic(message)
                runOnUiThread { onMessage(message) }
            }
            override fun onClientConnected(ip: String) {}
        })
        server?.start()
        nsdHelper.registerService(8888, name)
        
        client = Client("127.0.0.1", 8888, object : Client.ClientCallback {
            override fun onMessageReceived(message: GameMessage) {}
            override fun onConnected() {
                client?.sendMessage(GameMessage(GameMessage.TYPE_JOIN, myId, name, ""))
            }
            override fun onDisconnected() {}
        })
        client?.connect()

        CoroutineScope(Dispatchers.Default).launch {
            var currentTimer = time
            while (currentTimer > 0) {
                delay(1000)
                currentTimer--
                server?.broadcast(GameMessage(GameMessage.TYPE_TICK, myId, name, currentTimer.toString()))
            }
            val loser = masterPlayers.find { it.hasPotato }
            server?.broadcast(GameMessage(GameMessage.TYPE_GAME_OVER, myId, name, loser?.name ?: "Nadie"))
        }
    }

    private fun handleHostLogic(msg: GameMessage) {
        when (msg.type) {
            GameMessage.TYPE_JOIN -> {
                if (!masterPlayers.any { it.id == msg.senderId }) {
                    masterPlayers.add(Player(msg.senderId, msg.senderName))
                    broadcastPlayerList()
                }
            }
            GameMessage.TYPE_PASS_POTATO -> {
                val targetId = msg.content
                for (i in masterPlayers.indices) {
                    masterPlayers[i] = masterPlayers[i].copy(hasPotato = masterPlayers[i].id == targetId)
                }
                broadcastPlayerList()
            }
        }
    }

    private fun broadcastPlayerList() {
        val json = gson.toJson(masterPlayers)
        server?.broadcast(GameMessage(GameMessage.TYPE_PLAYER_LIST, myId, "Host", json))
    }

    private fun startClient(service: NsdServiceInfo, name: String, onMessage: (GameMessage) -> Unit) {
        val host = service.host?.hostAddress
        val port = service.port
        client = Client(host, port, object : Client.ClientCallback {
            override fun onMessageReceived(message: GameMessage) {
                runOnUiThread { onMessage(message) }
            }
            override fun onConnected() {
                client?.sendMessage(GameMessage(GameMessage.TYPE_JOIN, myId, name, ""))
            }
            override fun onDisconnected() {
                runOnUiThread { Toast.makeText(this@MainActivity, "Servidor desconectado", Toast.LENGTH_SHORT).show() }
            }
        })
        client?.connect()
    }

    private fun handleIncomingMessage(
        msg: GameMessage, 
        updatePlayers: (List<Player>) -> Unit,
        setTime: (Int) -> Unit,
        onGameOver: (String) -> Unit
    ) {
        when (msg.type) {
            GameMessage.TYPE_PLAYER_LIST -> {
                val listType = object : TypeToken<List<Player>>() {}.type
                val playerList: List<Player> = gson.fromJson(msg.content, listType)
                updatePlayers(playerList)
            }
            GameMessage.TYPE_TICK -> {
                setTime(msg.content.toInt())
            }
            GameMessage.TYPE_GAME_OVER -> {
                onGameOver("¡Perdió ${msg.content}!")
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        resetGame()
    }
}

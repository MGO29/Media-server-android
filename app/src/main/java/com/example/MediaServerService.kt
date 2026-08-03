package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetAddress
import java.net.NetworkInterface

class MediaServerService : Service() {

    private var ktorServer: KtorServer? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_START) {
            val rootFolderUri = intent.getStringExtra(EXTRA_ROOT_FOLDER_URI) ?: return START_NOT_STICKY
            val port = intent.getIntExtra(EXTRA_PORT, 8080)
            startServer(rootFolderUri, port)
        } else if (action == ACTION_STOP) {
            stopServer()
        }
        return START_NOT_STICKY
    }

    private fun startServer(rootFolderUri: String, port: Int) {
        if (ktorServer != null) return
        
        MediaServerService.logMessage("INFO", "Starting Media Server on port $port...")
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, createNotification("Starting server..."), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else {
                startForeground(NOTIFICATION_ID, createNotification("Starting server..."))
            }
        } catch (e: Exception) {
            val errorMsg = "Failed to start service: ${e.message}"
            MediaServerService.logMessage("ERROR", errorMsg)
            _serverUrl.value = errorMsg
            _errorMessage.value = "FGS Error: ${e.message}"
            stopSelf()
            return
        }

        if (!isPortAvailable(port)) {
            val errorMsg = "Failed to start Ktor: Port $port is already in use."
            MediaServerService.logMessage("ERROR", errorMsg)
            _serverUrl.value = errorMsg
            _errorMessage.value = "Port $port is already in use."
            stopSelf()
            return
        }

        ktorServer = KtorServer(
            appCtx = this,
            rootFolderUri = rootFolderUri,
            port = port,
            onClientConnected = { clientIp ->
                serviceScope.launch {
                    val currentClients = _connectedClients.value.toMutableSet()
                    currentClients.add(clientIp)
                    _connectedClients.value = currentClients
                }
            }
        )
        
        serviceScope.launch {
            try {
                ktorServer?.start()
                withContext(Dispatchers.Main) {
                    _isRunning.value = true
                    _serverUrl.value = "http://${getLocalIpAddress()}:$port"
                    MediaServerService.logMessage("INFO", "Server successfully started on ${_serverUrl.value}")
                    val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NOTIFICATION_ID, createNotification("Server is running on ${_serverUrl.value}"))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    MediaServerService.logMessage("ERROR", "Ktor Server crashed: ${e.message}")
                    _isRunning.value = false
                    _serverUrl.value = "Failed to start Ktor: ${e.message}"
                    _errorMessage.value = "Ktor Error: ${e.message}"
                    stopSelf()
                }
            }
        }
    }

    private fun stopServer() {
        MediaServerService.logMessage("INFO", "Stopping Media Server...")
        ktorServer?.stop()
        ktorServer = null
        _isRunning.value = false
        _serverUrl.value = null
        _connectedClients.value = emptySet()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun isPortAvailable(port: Int): Boolean {
        var serverSocket: java.net.ServerSocket? = null
        return try {
            serverSocket = java.net.ServerSocket(port)
            serverSocket.reuseAddress = true
            true
        } catch (e: Exception) {
            false
        } finally {
            serverSocket?.close()
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Media Server")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_info_details)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Media Server Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopServer()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private suspend fun getLocalIpAddress(): String = withContext(Dispatchers.IO) {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            for (intf in interfaces) {
                val addrs = intf.inetAddresses
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        return@withContext addr.hostAddress ?: ""
                    }
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return@withContext "127.0.0.1"
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_ROOT_FOLDER_URI = "EXTRA_ROOT_FOLDER_URI"
        const val EXTRA_PORT = "EXTRA_PORT"

        private const val CHANNEL_ID = "MediaServerChannel"
        private const val NOTIFICATION_ID = 1

        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()

        private val _serverUrl = MutableStateFlow<String?>(null)
        val serverUrl = _serverUrl.asStateFlow()

        private val _connectedClients = MutableStateFlow<Set<String>>(emptySet())
        val connectedClients = _connectedClients.asStateFlow()

        private val _errorMessage = MutableStateFlow<String?>(null)
        val errorMessage = _errorMessage.asStateFlow()
        
        private val _systemLogs = MutableStateFlow<List<LogEntry>>(emptyList())
        val systemLogs = _systemLogs.asStateFlow()
        
        fun logMessage(level: String, message: String) {
            val newList = _systemLogs.value.toMutableList()
            newList.add(LogEntry(level = level, message = message))
            if (newList.size > 200) {
                newList.removeAt(0)
            }
            _systemLogs.value = newList
        }
        
        fun clearError() {
            _errorMessage.value = null
        }

        fun clearLogs() {
            _systemLogs.value = emptyList()
        }
    }
}

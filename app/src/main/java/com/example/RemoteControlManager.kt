package com.example

import io.ktor.websocket.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

data class RemotePlaybackState(
    val isConnected: Boolean = false,
    val mediaTitle: String = "No Media Playing",
    val currentTime: Float = 0f,
    val duration: Float = 0f,
    val isPaused: Boolean = true,
    val volume: Float = 1.0f,
    val subtitleTrack: String = "Off",
    val activeClientsCount: Int = 0,
    val pairingCode: String = "8492-MED"
)

object RemoteControlManager {

    private val sessions = ConcurrentHashMap.newKeySet<DefaultWebSocketSession>()
    
    private val _playbackState = MutableStateFlow(RemotePlaybackState())
    val playbackState: StateFlow<RemotePlaybackState> = _playbackState.asStateFlow()

    private var currentPairingCode = generatePairingCode()

    fun getPairingCode(): String {
        return currentPairingCode
    }

    fun generateNewPairingCode(): String {
        currentPairingCode = generatePairingCode()
        _playbackState.value = _playbackState.value.copy(pairingCode = currentPairingCode)
        return currentPairingCode
    }

    private fun generatePairingCode(): String {
        val num = Random.nextInt(1000, 9999)
        return "$num-MED"
    }

    fun addSession(session: DefaultWebSocketSession) {
        sessions.add(session)
        updateClientsCount()
    }

    fun removeSession(session: DefaultWebSocketSession) {
        sessions.remove(session)
        updateClientsCount()
    }

    private fun updateClientsCount() {
        val count = sessions.size
        _playbackState.value = _playbackState.value.copy(
            isConnected = count > 0,
            activeClientsCount = count
        )
    }

    suspend fun handleIncomingMessage(session: DefaultWebSocketSession, text: String) {
        try {
            val json = JSONObject(text)
            val msgType = json.optString("type", "")

            if (msgType == "state") {
                // State broadcast from Web UI player
                val title = json.optString("title", "Unknown Stream")
                val curTime = json.optDouble("currentTime", 0.0).toFloat()
                val dur = json.optDouble("duration", 0.0).toFloat()
                val paused = json.optBoolean("paused", true)
                val vol = json.optDouble("volume", 1.0).toFloat()
                val sub = json.optString("subtitle", "Off")

                _playbackState.value = _playbackState.value.copy(
                    mediaTitle = title,
                    currentTime = curTime,
                    duration = dur,
                    isPaused = paused,
                    volume = vol,
                    subtitleTrack = sub,
                    isConnected = true
                )

                // Broadcast state to all other connected clients
                broadcastMessage(text, excludeSession = session)
            } else if (msgType == "command") {
                // Command received from Mobile Remote or Android app
                broadcastMessage(text, excludeSession = session)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun sendCommand(action: String, extraParams: Map<String, Any> = emptyMap()) {
        val json = JSONObject()
        json.put("type", "command")
        json.put("action", action)
        extraParams.forEach { (k, v) -> json.put(k, v) }

        val msg = json.toString()
        broadcastMessage(msg)
    }

    private suspend fun broadcastMessage(message: String, excludeSession: DefaultWebSocketSession? = null) {
        sessions.forEach { session ->
            if (session != excludeSession) {
                try {
                    session.send(Frame.Text(message))
                } catch (e: Exception) {
                    sessions.remove(session)
                }
            }
        }
    }
}

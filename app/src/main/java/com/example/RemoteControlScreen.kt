package com.example

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate100 = Color(0xFFF1F5F9)
private val Indigo600 = Color(0xFF4F46E5)
private val Emerald500 = Color(0xFF10B981)

@Composable
fun RemoteControlScreen(
    serverUrl: String,
    modifier: Modifier = Modifier
) {
    val state by RemoteControlManager.playbackState.collectAsState()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val cleanServerUrl = remember(serverUrl) {
        if (serverUrl.startsWith("http://") || serverUrl.startsWith("https://")) serverUrl else "http://localhost:8080"
    }

    val pairingUrl = remember(cleanServerUrl, state.pairingCode) {
        "$cleanServerUrl/?pair=${state.pairingCode}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. NOW PLAYING & WEBSOCKET STATUS CARD ---
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(if (state.isConnected) Emerald500 else Slate400)
                        )
                        Text(
                            text = if (state.isConnected) "REMOTE LINKED (${state.activeClientsCount} WS)" else "DISCONNECTED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isConnected) Emerald500 else Slate400,
                            letterSpacing = 1.sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Indigo600.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = "CODE: ${state.pairingCode}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Indigo600,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = state.mediaTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate900,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                if (state.duration > 0) {
                    val progress = (state.currentTime / state.duration).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(CircleShape),
                        color = Indigo600,
                        trackColor = Slate100
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = formatSeconds(state.currentTime),
                            fontSize = 11.sp,
                            color = Slate400
                        )
                        Text(
                            text = formatSeconds(state.duration),
                            fontSize = 11.sp,
                            color = Slate400
                        )
                    }
                }
            }
        }

        // --- 2. MEDIA CONTROLS PANEL ---
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "LIVE PLAYER REMOTE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate400,
                    letterSpacing = 1.5.sp
                )

                // Row 1: Primary Playback Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            coroutineScope.launch { RemoteControlManager.sendCommand("seek_backward") }
                        }
                    ) {
                        Icon(Icons.Default.Replay10, contentDescription = "Seek -10s", tint = Slate800, modifier = Modifier.size(32.dp))
                    }

                    Surface(
                        shape = CircleShape,
                        color = Indigo600,
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(64.dp)
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch { RemoteControlManager.sendCommand("toggle_play") }
                            }
                        ) {
                            Icon(
                                imageVector = if (state.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                contentDescription = if (state.isPaused) "Play" else "Pause",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            coroutineScope.launch { RemoteControlManager.sendCommand("seek_forward") }
                        }
                    ) {
                        Icon(Icons.Default.Forward10, contentDescription = "Seek +10s", tint = Slate800, modifier = Modifier.size(32.dp))
                    }
                }

                Divider(color = Slate100)

                // Row 2: Secondary Controls (Volume & Features)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(
                        onClick = { coroutineScope.launch { RemoteControlManager.sendCommand("volume_down") } }
                    ) {
                        Icon(Icons.Default.VolumeDown, contentDescription = "Volume Down", tint = Slate800)
                    }

                    IconButton(
                        onClick = { coroutineScope.launch { RemoteControlManager.sendCommand("volume_up") } }
                    ) {
                        Icon(Icons.Default.VolumeUp, contentDescription = "Volume Up", tint = Slate800)
                    }

                    IconButton(
                        onClick = { coroutineScope.launch { RemoteControlManager.sendCommand("mute") } }
                    ) {
                        Icon(Icons.Default.VolumeMute, contentDescription = "Mute", tint = Slate800)
                    }

                    IconButton(
                        onClick = { coroutineScope.launch { RemoteControlManager.sendCommand("toggle_osd") } }
                    ) {
                        Icon(Icons.Default.Info, contentDescription = "Toggle OSD Info", tint = Indigo600)
                    }

                    IconButton(
                        onClick = { coroutineScope.launch { RemoteControlManager.sendCommand("close_player") } }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close Player", tint = Color.Red)
                    }
                }
            }
        }

        // --- 3. QR PAIRING CODE CARD ---
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WEB UI PAIRING QR CODE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 1.5.sp
                    )

                    IconButton(
                        onClick = {
                            val newCode = RemoteControlManager.generateNewPairingCode()
                            Toast.makeText(context, "Pairing code refreshed: $newCode", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh Pairing Code", tint = Indigo600)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Custom QR Code View Component
                QrCodeView(
                    content = pairingUrl,
                    size = 180.dp,
                    dotColor = Slate900,
                    backgroundColor = Slate100
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = pairingUrl,
                    fontSize = 12.sp,
                    color = Slate800,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(pairingUrl))
                            Toast.makeText(context, "Pairing URL copied to clipboard", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Slate100, contentColor = Slate900),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Link")
                    }

                    Button(
                        onClick = {
                            val newCode = RemoteControlManager.generateNewPairingCode()
                            Toast.makeText(context, "New Code: $newCode", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Indigo600, contentColor = Color.White),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("New Code")
                    }
                }
            }
        }
    }
}

private fun formatSeconds(seconds: Float): String {
    if (seconds.isNaN() || seconds < 0) return "00:00"
    val total = seconds.toInt()
    val m = total / 60
    val s = total % 60
    return String.format("%02d:%02d", m, s)
}

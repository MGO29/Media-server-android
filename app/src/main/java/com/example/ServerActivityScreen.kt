package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ServerActivityScreen(
    modifier: Modifier = Modifier,
    viewModel: ServerActivityViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filterLevels = listOf("ALL", "HTTP", "TRANSCODE", "INFO", "WARN", "ERROR")
    val dateFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Top Header Title and Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Server Activity",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800
                )
                Text(
                    text = "Real-time Ktor & MediaCodec logs",
                    fontSize = 12.sp,
                    color = Slate500
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Pause / Stream Toggle
                IconButton(
                    onClick = { viewModel.togglePause() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (uiState.isPaused) Amber100 else Indigo50)
                ) {
                    Icon(
                        imageVector = if (uiState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (uiState.isPaused) "Resume Stream" else "Pause Stream",
                        tint = if (uiState.isPaused) Amber600 else Indigo600,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Clear Logs Button
                IconButton(
                    onClick = { viewModel.clearLogs() },
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Slate100)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Logs",
                        tint = Slate600,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Realtime Streaming Status Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (uiState.isPaused) Amber600
                                else if (uiState.isServerRunning) Emerald500
                                else Slate400
                            )
                    )
                    Column {
                        Text(
                            text = if (uiState.isPaused) "STREAM PAUSED"
                            else if (uiState.isServerRunning) "STREAMING LIVE"
                            else "SERVER INACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate700,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "${uiState.filteredLogs.size} logs shown • ${uiState.activeClientsCount} active client(s)",
                            fontSize = 11.sp,
                            color = Slate500
                        )
                    }
                }

                if (uiState.serverUrl != null) {
                    Text(
                        text = uiState.serverUrl!!,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Indigo600
                    )
                }
            }
        }

        // Search Text Field
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = { Text("Filter logs by text...", fontSize = 13.sp, color = Slate400) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Slate400,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = Slate400,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = Indigo600,
                unfocusedBorderColor = Slate200
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .height(50.dp)
        )

        // Filter Chips Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            items(filterLevels) { filter ->
                val isSelected = uiState.selectedFilter == filter
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setFilter(filter) },
                    label = {
                        Text(
                            text = filter,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Indigo600,
                        selectedLabelColor = Color.White,
                        containerColor = Color.White,
                        labelColor = Slate600
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = Slate200,
                        selectedBorderColor = Indigo600
                    ),
                    shape = RoundedCornerShape(20.dp)
                )
            }
        }

        // Real-time Logs List Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 16.dp)
        ) {
            if (uiState.filteredLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.SettingsInputAntenna,
                            contentDescription = null,
                            tint = Slate300,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (uiState.isPaused) "Log stream paused" else "No logs recorded",
                            color = Slate500,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = if (uiState.isPaused) "Tap play button above to resume" else "Incoming HTTP requests and transcoding events will appear here",
                            color = Slate400,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = true
                ) {
                    items(
                        items = uiState.filteredLogs.reversed(),
                        key = { it.id }
                    ) { log ->
                        val (badgeBg, badgeFg) = when (log.level.uppercase()) {
                            "HTTP" -> Pair(Indigo50, Indigo600)
                            "TRANSCODE" -> Pair(Color(0xFFF3E8FF), Color(0xFF7E22CE))
                            "ERROR" -> Pair(Color(0xFFFEE2E2), Color(0xFFDC2626))
                            "WARN" -> Pair(Amber100, Amber600)
                            else -> Pair(Emerald50, Emerald600)
                        }

                        val timeStr = dateFormat.format(Date(log.timestamp))

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(badgeBg)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = log.level.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = badgeFg,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    Text(
                                        text = timeStr,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = Slate400
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = log.message,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Slate800,
                                lineHeight = 16.sp
                            )
                        }
                        HorizontalDivider(color = Slate50, modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

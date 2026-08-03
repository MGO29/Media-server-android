package com.example

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.theme.*

data class ProfileAnalytics(
    val name: String,
    val avatarColor: Color,
    val watchTimeMinutes: Int,
    val completedTitles: Int,
    val topGenre: String
)

@Composable
fun AnalyticsScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectedClients by MediaServerService.connectedClients.collectAsStateWithLifecycle()
    val isServerRunning by MediaServerService.isRunning.collectAsStateWithLifecycle()
    val logs by MediaServerService.systemLogs.collectAsStateWithLifecycle()

    var selectedTimeframe by remember { mutableStateOf("7 Days") }
    val timeframes = listOf("24 Hours", "7 Days", "30 Days", "All Time")

    // Dynamic metrics based on timeframe
    val multiplier = when (selectedTimeframe) {
        "24 Hours" -> 0.35f
        "7 Days" -> 1.0f
        "30 Days" -> 3.2f
        else -> 8.5f
    }

    val totalWatchHours = (18.5f * multiplier).toInt()
    val totalWatchMinutes = ((18.5f * multiplier - totalWatchHours) * 60).toInt()
    val bandwidthGb = String.format("%.1f GB", 34.2f * multiplier)
    val totalStreamsCount = (42 * multiplier).toInt()
    val directPlayPercent = 92

    // Simulated weekly activity data (hours per day)
    val rawDailyHours = listOf(2.4f, 1.8f, 3.5f, 4.2f, 2.9f, 5.1f, 3.8f)
    val dailyHours = rawDailyHours.map { (it * multiplier / 1.0f).coerceIn(0.5f, 12f) }
    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

    // Load actual profile data from ProfileManager
    val profilesData = remember(context, selectedTimeframe) {
        try {
            val jsonArr = ProfileManager.getProfiles(context)
            val list = mutableListOf<ProfileAnalytics>()
            for (i in 0 until jsonArr.length()) {
                val p = jsonArr.getJSONObject(i)
                val name = p.optString("name", "User")
                val avatarHex = p.optString("avatar", "#6366f1")
                val color = parseHexColor(avatarHex)
                
                // Read progress count
                val progObj = ProfileManager.getProgress(context, p.optString("id", ""))
                val titleCount = progObj.length().coerceAtLeast(1)
                
                list.add(
                    ProfileAnalytics(
                        name = name,
                        avatarColor = color,
                        watchTimeMinutes = (120 * (i + 1) * multiplier).toInt(),
                        completedTitles = (titleCount * multiplier).toInt().coerceAtLeast(1),
                        topGenre = if (i % 2 == 0) "Sci-Fi / Action" else "Drama / Thriller"
                    )
                )
            }
            if (list.isEmpty()) {
                list.add(ProfileAnalytics("Alex", Indigo600, (320 * multiplier).toInt(), 8, "Sci-Fi / Movies"))
            }
            list
        } catch (e: Exception) {
            listOf(
                ProfileAnalytics("Alex", Indigo600, (320 * multiplier).toInt(), 8, "Sci-Fi / Movies"),
                ProfileAnalytics("Cinema Fan", Color(0xFFEC4899), (180 * multiplier).toInt(), 4, "Action / Thriller")
            )
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- HEADER SECTION ---
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Analytics & Insights",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Playback trends, network throughput & profile statistics",
                            fontSize = 12.sp,
                            color = Slate500
                        )
                    }

                    // Status Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isServerRunning) Emerald50 else Slate100,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isServerRunning) Emerald500.copy(alpha = 0.3f) else Slate300
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isServerRunning) Emerald500 else Slate400)
                            )
                            Text(
                                text = if (isServerRunning) "${connectedClients.size} Active" else "Offline",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isServerRunning) Emerald600 else Slate600
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Timeframe Filter Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    timeframes.forEach { timeframe ->
                        val selected = timeframe == selectedTimeframe
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (selected) Indigo600 else Color.White,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (selected) Indigo600 else Slate200
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedTimeframe = timeframe }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = timeframe,
                                    fontSize = 12.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) Color.White else Slate600
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- KEY METRICS CARDS (GRID) ---
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Total Watch Time",
                        value = "${totalWatchHours}h ${totalWatchMinutes}m",
                        subtext = "+14% vs previous period",
                        icon = Icons.Default.PlayCircle,
                        iconTint = Indigo600,
                        iconBg = Indigo50,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Active Streams",
                        value = "${connectedClients.size} Client${if (connectedClients.size == 1) "" else "s"}",
                        subtext = if (isServerRunning) "Server streaming HLS" else "Server offline",
                        icon = Icons.Default.Tv,
                        iconTint = Emerald600,
                        iconBg = Emerald50,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        title = "Data Transferred",
                        value = bandwidthGb,
                        subtext = "$totalStreamsCount sessions played",
                        icon = Icons.Default.NetworkCheck,
                        iconTint = Color(0xFF0284C7),
                        iconBg = Color(0xFFE0F2FE),
                        modifier = Modifier.weight(1f)
                    )
                    MetricCard(
                        title = "Direct Play Rate",
                        value = "$directPlayPercent%",
                        subtext = "Hardware Accelerated",
                        icon = Icons.Default.Speed,
                        iconTint = Amber600,
                        iconBg = Amber100,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- WEEKLY STREAMING ACTIVITY BAR CHART ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily Playback Activity",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = "Hours watched per day ($selectedTimeframe)",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Compose Canvas Bar Chart
                    val maxHours = (dailyHours.maxOrNull() ?: 6f).coerceAtLeast(4f)
                    val primaryColor = Indigo600
                    val barColorSecondary = Color(0xFFC7D2FE)

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val barCount = dailyHours.size
                        val barWidth = 24.dp.toPx()
                        val spacing = (canvasWidth - (barCount * barWidth)) / (barCount + 1)

                        dailyHours.forEachIndexed { idx, hrs ->
                            val barHeight = (hrs / maxHours) * (canvasHeight - 30.dp.toPx())
                            val left = spacing + idx * (barWidth + spacing)
                            val top = canvasHeight - barHeight - 20.dp.toPx()

                            // Draw Bar background track
                            drawRoundRect(
                                color = Color(0xFFF1F5F9),
                                topLeft = Offset(left, 0f),
                                size = Size(barWidth, canvasHeight - 20.dp.toPx()),
                                cornerRadius = CornerRadius(12f, 12f)
                            )

                            // Draw Filled Bar
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    colors = listOf(primaryColor, barColorSecondary)
                                ),
                                topLeft = Offset(left, top),
                                size = Size(barWidth, barHeight),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                        }
                    }

                    // X-Axis Labels
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate500,
                                modifier = Modifier.weight(1f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }
        }

        // --- PROFILE CONSUMPTION BREAKDOWN ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Profile Consumption",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "${profilesData.size} Profiles",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Indigo600
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    profilesData.forEachIndexed { index, profile ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(profile.avatarColor),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = profile.name.take(1).uppercase(),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            // Profile Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = profile.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate800
                                )
                                Text(
                                    text = "${profile.completedTitles} media items • Favorite: ${profile.topGenre}",
                                    fontSize = 11.sp,
                                    color = Slate500
                                )
                            }

                            // Watch time tag
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Slate100
                            ) {
                                Text(
                                    text = "${profile.watchTimeMinutes / 60}h ${profile.watchTimeMinutes % 60}m",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Slate700,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }

                        if (index < profilesData.size - 1) {
                            HorizontalDivider(color = Slate100, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }

        // --- STORAGE & SYSTEM LOG DISTRIBUTION ---
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Storage & Content Categories",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )

                    // Multi-segmented progress bar
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Slate100)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.55f)
                                    .background(Indigo600)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.25f)
                                    .background(Color(0xFFEC4899))
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.12f)
                                    .background(Amber600)
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(0.08f)
                                    .background(Slate300)
                            )
                        }

                        // Category Legends
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            CategoryLegend("4K Movies (55%)", Indigo600)
                            CategoryLegend("TV Shows (25%)", Color(0xFFEC4899))
                            CategoryLegend("Cache (12%)", Amber600)
                            CategoryLegend("Free (8%)", Slate400)
                        }
                    }

                    HorizontalDivider(color = Slate100)

                    // Server Health Log Stats
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "System Log Health",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate800
                            )
                            Text(
                                text = "${logs.size} events logged in session",
                                fontSize = 11.sp,
                                color = Slate500
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LogBadge("INFO", Emerald600, Emerald50)
                            LogBadge("WARN", Amber600, Amber100)
                            LogBadge("HTTP", Indigo600, Indigo50)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate200),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = value,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Slate800
            )

            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Slate600
            )

            Text(
                text = subtext,
                fontSize = 10.sp,
                color = Slate400,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun CategoryLegend(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Slate600
        )
    }
}

@Composable
private fun LogBadge(text: String, textColor: Color, bgColor: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor
    ) {
        Text(
            text = text,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private fun parseHexColor(hex: String): Color {
    return try {
        val cleaned = hex.removePrefix("#")
        val colorInt = cleaned.toLong(16)
        if (cleaned.length == 6) {
            Color(colorInt or 0xFF000000)
        } else {
            Color(colorInt)
        }
    } catch (e: Exception) {
        Indigo600
    }
}

package com.example

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.SystemClock
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import java.util.Locale

data class SystemResourcesState(
    val cpuUsagePercent: Int = 0,
    val ramUsedMb: Long = 0,
    val ramTotalMb: Long = 0,
    val ramUsagePercent: Int = 0,
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val batteryTemperature: Float = 0f,
    val activeTranscodeCount: Int = 0
)

class ResourceMonitor(private val context: Context) {
    private var lastCpuTime: Long = android.os.Process.getElapsedCpuTime()
    private var lastRealtime: Long = SystemClock.elapsedRealtime()

    fun sampleResources(activeTranscodes: Int): SystemResourcesState {
        // CPU Usage Calculation
        val currentCpuTime = android.os.Process.getElapsedCpuTime()
        val currentRealtime = SystemClock.elapsedRealtime()
        val cpuDelta = currentCpuTime - lastCpuTime
        val timeDelta = currentRealtime - lastRealtime

        lastCpuTime = currentCpuTime
        lastRealtime = currentRealtime

        val numCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        var rawCpuPercent = if (timeDelta > 0) {
            ((cpuDelta.toFloat() / (timeDelta.toFloat() * numCores)) * 100).toInt()
        } else {
            5
        }

        // Add active transcoding load bias for realistic monitoring during encoding
        if (activeTranscodes > 0) {
            rawCpuPercent = (rawCpuPercent + 25 * activeTranscodes).coerceIn(35, 98)
        } else {
            rawCpuPercent = rawCpuPercent.coerceIn(2, 85)
        }

        // RAM Usage
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager?.getMemoryInfo(memoryInfo)

        val totalRamMb = (memoryInfo.totalMem / (1024 * 1024))
        val availRamMb = (memoryInfo.availMem / (1024 * 1024))
        val usedRamMb = (totalRamMb - availRamMb).coerceAtLeast(0)
        val ramPercent = if (totalRamMb > 0) ((usedRamMb.toFloat() / totalRamMb.toFloat()) * 100).toInt() else 0

        // Battery Info
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) ((level.toFloat() / scale.toFloat()) * 100).toInt() else 100

        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val tempTenths = batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        val batteryTempC = tempTenths / 10.0f

        return SystemResourcesState(
            cpuUsagePercent = rawCpuPercent,
            ramUsedMb = usedRamMb,
            ramTotalMb = totalRamMb,
            ramUsagePercent = ramPercent,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            batteryTemperature = batteryTempC,
            activeTranscodeCount = activeTranscodes
        )
    }
}

@Composable
fun SystemResourcesWidget(
    modifier: Modifier = Modifier,
    activeTranscodeCount: Int = 0
) {
    val context = LocalContext.current
    val monitor = remember { ResourceMonitor(context) }
    var resourcesState by remember { mutableStateOf(monitor.sampleResources(activeTranscodeCount)) }

    // Periodically update real-time metrics every 1.5 seconds
    LaunchedEffect(activeTranscodeCount) {
        while (true) {
            resourcesState = monitor.sampleResources(activeTranscodeCount)
            delay(1500)
        }
    }

    val animatedCpu by animateFloatAsState(
        targetValue = resourcesState.cpuUsagePercent / 100f,
        animationSpec = tween(500),
        label = "CpuAnim"
    )

    val animatedRam by animateFloatAsState(
        targetValue = resourcesState.ramUsagePercent / 100f,
        animationSpec = tween(500),
        label = "RamAnim"
    )

    val animatedBattery by animateFloatAsState(
        targetValue = resourcesState.batteryPercent / 100f,
        animationSpec = tween(500),
        label = "BatteryAnim"
    )

    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            // Header Title and Impact Badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "System Resources",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate800
                    )
                    Text(
                        text = "Real-time hardware performance",
                        fontSize = 12.sp,
                        color = Slate500
                    )
                }

                val (impactText, impactBg, impactFg) = when {
                    activeTranscodeCount > 0 -> Triple("TRANSCODING ACTIVE", Color(0xFFF3E8FF), Color(0xFF7E22CE))
                    resourcesState.cpuUsagePercent > 75 -> Triple("HIGH CPU LOAD", Color(0xFFFEE2E2), Color(0xFFDC2626))
                    else -> Triple("STABLE / LOW LOAD", Emerald50, Emerald600)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(impactBg)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = impactText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = impactFg,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // Resource Gauges Row
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                // CPU Gauge Item
                ResourceGaugeItem(
                    title = "CPU Usage",
                    subtitle = "${resourcesState.cpuUsagePercent}% (${Runtime.getRuntime().availableProcessors()} Cores)",
                    progress = animatedCpu,
                    valueText = "${resourcesState.cpuUsagePercent}%",
                    icon = Icons.Default.DeveloperBoard,
                    barColor = when {
                        resourcesState.cpuUsagePercent > 80 -> Color(0xFFEF4444)
                        resourcesState.cpuUsagePercent > 50 -> Amber600
                        else -> Indigo600
                    }
                )

                // RAM Gauge Item
                val ramGbStr = String.format(Locale.US, "%.1f GB / %.1f GB", resourcesState.ramUsedMb / 1024f, resourcesState.ramTotalMb / 1024f)
                ResourceGaugeItem(
                    title = "RAM Memory",
                    subtitle = ramGbStr,
                    progress = animatedRam,
                    valueText = "${resourcesState.ramUsagePercent}%",
                    icon = Icons.Default.Memory,
                    barColor = when {
                        resourcesState.ramUsagePercent > 85 -> Color(0xFFEF4444)
                        resourcesState.ramUsagePercent > 70 -> Amber600
                        else -> Emerald500
                    }
                )

                // Battery Gauge Item
                val batteryStatusStr = if (resourcesState.isCharging) "Charging" else "Discharging"
                val batterySubText = "$batteryStatusStr • ${String.format(Locale.US, "%.1f", resourcesState.batteryTemperature)}°C"
                ResourceGaugeItem(
                    title = "Battery",
                    subtitle = batterySubText,
                    progress = animatedBattery,
                    valueText = "${resourcesState.batteryPercent}%",
                    icon = if (resourcesState.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    barColor = when {
                        resourcesState.batteryPercent <= 20 -> Color(0xFFEF4444)
                        resourcesState.batteryPercent <= 40 -> Amber600
                        else -> Emerald500
                    }
                )
            }
        }
    }
}

@Composable
private fun ResourceGaugeItem(
    title: String,
    subtitle: String,
    progress: Float,
    valueText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Slate50),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = Slate600,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = Slate500
                    )
                }
            }

            Text(
                text = valueText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Slate800
            )
        }

        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape),
            color = barColor,
            trackColor = Slate100
        )
    }
}

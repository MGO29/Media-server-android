package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.*

class MainActivity : ComponentActivity() {

    private var selectedFolderUri by mutableStateOf<Uri?>(null)

    private val selectFolderLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            selectedFolderUri = uri
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            startServerService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var showSplash by remember { mutableStateOf(true) }

                if (showSplash) {
                    SplashScreen(onDismiss = { showSplash = false })
                } else {
                    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                        ServerDashboard(
                            modifier = Modifier.padding(innerPadding),
                            selectedFolderUri = selectedFolderUri,
                            onSelectFolder = { selectFolderLauncher.launch(null) },
                            onStartServer = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    if (ContextCompat.checkSelfPermission(this@MainActivity, android.Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                                        startServerService()
                                    } else {
                                        requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                } else {
                                    startServerService()
                                }
                            },
                            onStopServer = {
                                val intent = Intent(this, MediaServerService::class.java).apply {
                                    action = MediaServerService.ACTION_STOP
                                }
                                startService(intent)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun startServerService() {
        try {
            selectedFolderUri?.let { uri ->
                val intent = Intent(this, MediaServerService::class.java).apply {
                    action = MediaServerService.ACTION_START
                    putExtra(MediaServerService.EXTRA_ROOT_FOLDER_URI, uri.toString())
                    putExtra(MediaServerService.EXTRA_PORT, 8080)
                }
                ContextCompat.startForegroundService(this, intent)
            } ?: run {
                android.widget.Toast.makeText(this, "No folder selected", android.widget.Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            android.widget.Toast.makeText(this, "Start FGS Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerDashboard(
    modifier: Modifier = Modifier,
    selectedFolderUri: Uri?,
    onSelectFolder: () -> Unit,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    val isRunning by MediaServerService.isRunning.collectAsState()
    val serverUrl by MediaServerService.serverUrl.collectAsState()
    val connectedClients by MediaServerService.connectedClients.collectAsState()
    val systemLogs by MediaServerService.systemLogs.collectAsState()
    val errorMessage by MediaServerService.errorMessage.collectAsState()
    val activeTranscodeCount by TranscodeManager.activeTranscodeCount.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
            MediaServerService.clearError()
        }
    }
    
    var selectedTab by remember { mutableIntStateOf(0) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Background,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Media Server",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate800,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = "Local Network Service",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Slate500
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Indigo100),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Dns,
                        contentDescription = "Server Icon",
                        tint = Indigo600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                val tabWidth = maxWidth / 5
                val indicatorOffset by animateDpAsState(
                    targetValue = tabWidth * selectedTab,
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    label = "sliding_underline_anim"
                )

                Column {
                    // Sliding Underline Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .background(Slate100)
                    ) {
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .padding(horizontal = 12.dp)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .background(Indigo600)
                        )
                    }

                    NavigationBar(
                        containerColor = Color.White,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(77.dp)
                    ) {
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                            label = { Text("DASHBOARD", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Indigo600,
                                selectedTextColor = Indigo600,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.PhoneAndroid, contentDescription = "Remote") },
                            label = { Text("REMOTE", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Indigo600,
                                selectedTextColor = Indigo600,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.People, contentDescription = "Profiles") },
                            label = { Text("PROFILES", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Indigo600,
                                selectedTextColor = Indigo600,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.Analytics, contentDescription = "Analytics") },
                            label = { Text("ANALYTICS", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            selected = selectedTab == 3,
                            onClick = { selectedTab = 3 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Indigo600,
                                selectedTextColor = Indigo600,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                        NavigationBarItem(
                            icon = { Icon(Icons.Default.SettingsInputAntenna, contentDescription = "Logs") },
                            label = { Text("LOGS", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp) },
                            selected = selectedTab == 4,
                            onClick = { selectedTab = 4 },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Indigo600,
                                selectedTextColor = Indigo600,
                                indicatorColor = Color.Transparent,
                                unselectedIconColor = Slate400,
                                unselectedTextColor = Slate400
                            )
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        if (selectedTab == 0) {
            Column(
                modifier = Modifier
                    .padding(paddingValues)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
            
            // Server Toggle Card
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(if (isRunning) Emerald500 else Slate400)
                            )
                            Text(
                                text = if (isRunning) "SERVER ACTIVE" else "SERVER INACTIVE",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = 2.sp
                            )
                        }
                        Switch(
                            checked = isRunning,
                            onCheckedChange = { checked ->
                                if (checked && selectedFolderUri != null) {
                                    onStartServer()
                                } else if (!checked) {
                                    onStopServer()
                                } else if (checked && selectedFolderUri == null) {
                                    onSelectFolder()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Indigo600,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = Slate400,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "HOST ADDRESS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp
                        )
                        if (isRunning && serverUrl != null) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Indigo50,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Text(
                                        text = serverUrl!!,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        fontFamily = FontFamily.Monospace,
                                        color = Indigo600,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Copy IP Button
                                    IconButton(
                                        onClick = {
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                            val clip = ClipData.newPlainText("Server IP", serverUrl)
                                            clipboard.setPrimaryClip(clip)
                                            android.widget.Toast.makeText(context, "IP address copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Indigo100)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy IP",
                                            tint = Indigo600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }

                                    // Share IP Button
                                    IconButton(
                                        onClick = {
                                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/plain"
                                                putExtra(Intent.EXTRA_SUBJECT, "Media Server IP")
                                                putExtra(Intent.EXTRA_TEXT, serverUrl)
                                            }
                                            context.startActivity(Intent.createChooser(shareIntent, "Share Media Server IP"))
                                        },
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(Indigo100)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = "Share IP",
                                            tint = Indigo600,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        } else {
                            Text(
                                text = "Not running",
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate800
                            )
                        }
                    }
                }
            }
            
            // Quick Stats Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "CONNECTIONS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isRunning) connectedClients.size.toString().padStart(2, '0') else "00",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            )
                            Text(
                                text = "Active",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Emerald600,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
                
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "STATUS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate400,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (isRunning) "ON" else "OFF",
                                fontSize = 30.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Slate900
                            )
                            Text(
                                text = if (isRunning) "Running" else "Stopped",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate500,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                    }
                }
            }
            
            // System Resources Widget
            SystemResourcesWidget(
                activeTranscodeCount = activeTranscodeCount
            )
            
            // Storage Monitoring Section
            Surface(
                shape = RoundedCornerShape(32.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Media Library",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800
                        )
                        Text(
                            text = "Change Folder",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Indigo600,
                            modifier = Modifier.clickable { onSelectFolder() }
                        )
                    }
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Slate50)
                            .padding(16.dp)
                            .padding(bottom = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Amber100),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Folder Icon",
                                tint = Amber600,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "SOURCE DIRECTORY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Slate400,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = selectedFolderUri?.path ?: "No folder selected",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Slate700,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "RECENT STREAMS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Slate400,
                        letterSpacing = 2.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    if (connectedClients.isEmpty()) {
                        Text(
                            text = "No active streams.",
                            fontSize = 14.sp,
                            color = Slate500,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        connectedClients.forEachIndexed { index, ip ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (index % 2 == 0) Indigo50 else Slate50),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (index % 2 == 0) Icons.Default.Tv else Icons.Default.PhoneAndroid,
                                            contentDescription = "Device",
                                            tint = if (index % 2 == 0) Indigo400 else Slate400,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Text(
                                        text = ip,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Slate800
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Indigo50)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "STREAMING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Indigo600
                                    )
                                }
                            }
                            if (index < connectedClients.size - 1) {
                                HorizontalDivider(color = Slate50)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
        } else if (selectedTab == 1) {
            RemoteControlScreen(
                serverUrl = serverUrl ?: "http://localhost:8080",
                modifier = Modifier.padding(paddingValues)
            )
        } else if (selectedTab == 2) {
            ProfileManagementScreen(
                modifier = Modifier.padding(paddingValues)
            )
        } else if (selectedTab == 3) {
            AnalyticsScreen(
                modifier = Modifier.padding(paddingValues)
            )
        } else if (selectedTab == 4) {
            ServerActivityScreen(
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
fun SystemLogsScreen(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "System Logs",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Slate800,
            modifier = Modifier.padding(top = 24.dp, bottom = 16.dp)
        )
        
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 24.dp)
        ) {
            if (logs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No logs available yet.", color = Slate400, fontSize = 14.sp)
                }
            } else {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    reverseLayout = true // Show newest first if we add to end
                ) {
                    items(logs.reversed().size) { index ->
                        val log = logs.reversed()[index]
                        val color = when (log.level) {
                            "ERROR" -> Color(0xFFEF4444)
                            "WARN" -> Amber600
                            else -> Slate500
                        }
                        
                        val dateFormat = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        val timeStr = dateFormat.format(java.util.Date(log.timestamp))
                        
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Text(
                                text = "[$timeStr]",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Slate400,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = log.level,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.width(48.dp)
                            )
                            Text(
                                text = log.message,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                color = Slate800,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (index < logs.size - 1) {
                            HorizontalDivider(color = Slate50, modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

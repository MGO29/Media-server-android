package com.example

import android.content.Context
import android.widget.Toast
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileManagementScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var profilesList by remember { mutableStateOf(getProfilesList(context)) }
    var activeProfileId by remember { mutableStateOf(getActiveProfileId(context)) }
    var showAddDialog by remember { mutableStateOf(false) }
    var profileToDelete by remember { mutableStateOf<JSONObject?>(null) }

    fun refreshProfiles() {
        profilesList = getProfilesList(context)
        activeProfileId = getActiveProfileId(context)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "User & Profile Management",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Slate800,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "Manage multi-user profiles, avatars & streaming progress",
                    fontSize = 12.sp,
                    color = Slate500,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Indigo600,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "Add Profile",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Profile", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        // Overview Summary Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, Slate100),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Indigo100),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Indigo600,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${profilesList.size} Registered Profiles",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Slate800,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Active Profile: ${getActiveProfileName(profilesList, activeProfileId)}",
                            fontSize = 12.sp,
                            color = Indigo600,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Emerald50,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD1FAE5))
                ) {
                    Text(
                        text = "MULTI-USER ACTIVE",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Emerald600,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Profiles List
        Text(
            text = "PROFILES LIST",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Slate400,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            items(profilesList) { profile ->
                val id = profile.optString("id")
                val name = profile.optString("name", "User")
                val avatarHex = profile.optString("avatar", "#6366f1")
                val isActive = (id == activeProfileId)
                val avatarColor = parseHexColor(avatarHex)

                val playlistsCount = ProfileManager.getPlaylists(context, id).length()
                val progressObj = ProfileManager.getProgress(context, id)
                val watchedMediaCount = progressObj.length()

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(
                        1.5.dp,
                        if (isActive) Indigo600 else Slate100
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(avatarColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(1).uppercase(),
                                        color = Color.White,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Slate800,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (isActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Indigo50)
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "CURRENT",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Indigo600
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "ID: $id",
                                        fontSize = 11.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Slate400,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (!isActive) {
                                    OutlinedButton(
                                        onClick = {
                                            setActiveProfileId(context, id)
                                            refreshProfiles()
                                            Toast.makeText(context, "Switched active profile to $name", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Indigo600)
                                    ) {
                                        Text("Select", fontSize = 12.sp, color = Indigo600, fontWeight = FontWeight.Bold)
                                    }

                                    IconButton(
                                        onClick = { profileToDelete = profile },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFFEE2E2))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Profile",
                                            tint = Color(0xFFDC2626),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = Slate100)
                        Spacer(modifier = Modifier.height(12.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Slate50,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("PLAYLISTS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                                    Text("$playlistsCount saved", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                }
                                Box(
                                    modifier = Modifier
                                        .width(1.dp)
                                        .height(24.dp)
                                        .background(Slate200)
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(start = 16.dp)
                                ) {
                                    Text("WATCH HISTORY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Slate400)
                                    Text("$watchedMediaCount item(s)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Slate700)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Profile Dialog
    if (showAddDialog) {
        AddProfileDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, colorHex ->
                ProfileManager.addProfile(context, name, colorHex)
                refreshProfiles()
                showAddDialog = false
                Toast.makeText(context, "Created profile '$name'", Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Confirm Delete Dialog
    profileToDelete?.let { prof ->
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Delete Profile?", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to delete profile '${prof.optString("name")}'? All stored watch histories and playlists for this profile will be permanently removed.") },
            confirmButton = {
                Button(
                    onClick = {
                        val pid = prof.optString("id")
                        ProfileManager.deleteProfile(context, pid)
                        profileToDelete = null
                        refreshProfiles()
                        Toast.makeText(context, "Profile deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddProfileDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var nameText by remember { mutableStateOf("") }
    val presetColors = listOf("#6366f1", "#ec4899", "#10b981", "#f59e0b", "#3b82f6", "#8b5cf6", "#ef4444", "#06b6d4")
    var selectedColorHex by remember { mutableStateOf(presetColors[0]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Create New User Profile", fontWeight = FontWeight.Bold, fontSize = 18.sp)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Enter a display name and choose an avatar theme color for this profile.", fontSize = 13.sp, color = Slate600)

                OutlinedTextField(
                    value = nameText,
                    onValueChange = { nameText = it },
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g. Living Room TV, Sarah") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("AVATAR THEME COLOR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Slate400)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(presetColors) { colorHex ->
                        val parsed = parseHexColor(colorHex)
                        val isSelected = (colorHex == selectedColorHex)
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parsed)
                                .border(
                                    width = if (isSelected) 3.dp else 0.dp,
                                    color = if (isSelected) Slate800 else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = colorHex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameText.isNotBlank()) {
                        onConfirm(nameText.trim(), selectedColorHex)
                    }
                },
                enabled = nameText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = Indigo600)
            ) {
                Text("Create Profile", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

private fun getProfilesList(context: Context): List<JSONObject> {
    val jsonArray = ProfileManager.getProfiles(context)
    val list = mutableListOf<JSONObject>()
    for (i in 0 until jsonArray.length()) {
        list.add(jsonArray.getJSONObject(i))
    }
    return list
}

private fun getActiveProfileId(context: Context): String {
    val prefs = context.getSharedPreferences("media_hub_prefs", Context.MODE_PRIVATE)
    return prefs.getString("active_profile_id", "default") ?: "default"
}

private fun setActiveProfileId(context: Context, id: String) {
    val prefs = context.getSharedPreferences("media_hub_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("active_profile_id", id).apply()
}

private fun getActiveProfileName(profiles: List<JSONObject>, activeId: String): String {
    return profiles.firstOrNull { it.optString("id") == activeId }?.optString("name") ?: "Alex"
}

private fun parseHexColor(hex: String): Color {
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        Indigo600
    }
}

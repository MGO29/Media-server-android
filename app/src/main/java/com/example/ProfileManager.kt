package com.example

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import android.util.Log

object ProfileManager {
    private fun getProfilesFile(appCtx: Context): File {
        return File(appCtx.filesDir, "profiles.json")
    }

    private fun getProgressDir(appCtx: Context): File {
        val dir = File(appCtx.filesDir, "progress")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getProfiles(appCtx: Context): JSONArray {
        val file = getProfilesFile(appCtx)
        if (!file.exists()) {
            // Create default profiles
            val defaultProfiles = JSONArray().apply { 
                put(JSONObject().put("id", "default").put("name", "Alex").put("avatar", "#6366f1")) 
                put(JSONObject().put("id", "profile_2").put("name", "Cinema Fan").put("avatar", "#ec4899")) 
            }
            file.writeText(defaultProfiles.toString())
            return defaultProfiles
        }
        return try {
            JSONArray(file.readText())
        } catch (e: Exception) {
            Log.e("ProfileManager", "Error reading profiles", e)
            JSONArray()
        }
    }

    fun addProfile(appCtx: Context, name: String, avatarColor: String = "#6366f1"): JSONObject {
        val profiles = getProfiles(appCtx)
        val id = java.util.UUID.randomUUID().toString()
        val newProfile = JSONObject().put("id", id).put("name", name).put("avatar", avatarColor)
        profiles.put(newProfile)
        getProfilesFile(appCtx).writeText(profiles.toString())
        return newProfile
    }

    fun deleteProfile(appCtx: Context, profileId: String): Boolean {
        val profiles = getProfiles(appCtx)
        val updated = JSONArray()
        var found = false
        for (i in 0 until profiles.length()) {
            val p = profiles.getJSONObject(i)
            if (p.optString("id") == profileId) {
                found = true
            } else {
                updated.put(p)
            }
        }
        if (found) {
            getProfilesFile(appCtx).writeText(updated.toString())
            // Also cleanup progress & playlists
            val progFile = File(getProgressDir(appCtx), "$profileId.json")
            if (progFile.exists()) progFile.delete()
            val playlistFile = File(getPlaylistDir(appCtx), "$profileId.json")
            if (playlistFile.exists()) playlistFile.delete()
        }
        return found
    }

    private fun getPlaylistDir(appCtx: Context): File {
        val dir = File(appCtx.filesDir, "playlists")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getPlaylists(appCtx: Context, profileId: String): JSONArray {
        val file = File(getPlaylistDir(appCtx), "$profileId.json")
        if (!file.exists()) return JSONArray()
        return try {
            JSONArray(file.readText())
        } catch (e: Exception) {
            JSONArray()
        }
    }

    fun savePlaylists(appCtx: Context, profileId: String, playlists: JSONArray) {
        File(getPlaylistDir(appCtx), "$profileId.json").writeText(playlists.toString())
    }

    fun getProgress(appCtx: Context, profileId: String): JSONObject {
        val file = File(getProgressDir(appCtx), "$profileId.json")
        if (!file.exists()) return JSONObject()
        return try {
            JSONObject(file.readText())
        } catch (e: Exception) {
            JSONObject()
        }
    }

    fun updateProgress(appCtx: Context, profileId: String, mediaName: String, time: Double, pct: Double) {
        val progress = getProgress(appCtx, profileId)
        val mediaProgress = JSONObject().put("time", time).put("pct", pct)
        progress.put(mediaName, mediaProgress)
        File(getProgressDir(appCtx), "$profileId.json").writeText(progress.toString())
    }
}

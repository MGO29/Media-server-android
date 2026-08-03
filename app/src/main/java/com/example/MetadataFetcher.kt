package com.example

import org.json.JSONObject
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log
import android.content.Context
import java.io.File

object MetadataFetcher {

    private fun getMetadataDir(appCtx: Context): File {
        val dir = File(appCtx.filesDir, "metadata")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun getSavedMetadata(appCtx: Context, filename: String): JSONObject? {
        try {
            val file = File(getMetadataDir(appCtx), "$filename.json")
            if (file.exists()) {
                val json = file.readText()
                return JSONObject(json)
            }
        } catch (e: Exception) {
            Log.e("MetadataFetcher", "Error reading saved metadata for $filename", e)
        }
        return null
    }

    fun saveMetadata(appCtx: Context, filename: String, metadata: JSONObject) {
        try {
            val file = File(getMetadataDir(appCtx), "$filename.json")
            file.writeText(metadata.toString())
        } catch (e: Exception) {
            Log.e("MetadataFetcher", "Error saving metadata for $filename", e)
        }
    }

    suspend fun searchMetadata(rawQuery: String): JSONArray = withContext(Dispatchers.IO) {
        val finalResults = JSONArray()
        
        var cleanName = rawQuery.substringBeforeLast(".")
        cleanName = cleanName.replace(".", " ").replace("_", " ")
        val yearRegex = Regex("\\b(19|20)\\d{2}\\b")
        val match = yearRegex.find(cleanName)
        if (match != null) {
            cleanName = cleanName.substring(0, match.range.first)
        }
        cleanName = cleanName.trim()
        
        val apiKey = BuildConfig.TMDB_API_KEY
        if (apiKey.isNotEmpty() && apiKey != "YOUR_TMDB_API_KEY") {
            try {
                val query = URLEncoder.encode(cleanName, "UTF-8")
                val urlStr = "https://api.themoviedb.org/3/search/multi?api_key=$apiKey&query=$query"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val results = json.optJSONArray("results")
                    if (results != null) {
                        for (i in 0 until Math.min(results.length(), 10)) {
                            val item = results.getJSONObject(i)
                            val title = item.optString("title").ifEmpty { item.optString("name") }
                            if (title.isNotEmpty()) {
                                val out = JSONObject()
                                out.put("title", title)
                                val posterPath = item.optString("poster_path", "")
                                if (posterPath.isNotEmpty()) {
                                    out.put("poster_url", "https://image.tmdb.org/t/p/w500$posterPath")
                                }
                                out.put("overview", item.optString("overview", ""))
                                out.put("vote_average", item.optDouble("vote_average", 0.0))
                                finalResults.put(out)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MetadataFetcher", "Error searching TMDB", e)
            }
        }
        
        if (finalResults.length() == 0) {
            // TVMaze fallback
            try {
                val query = URLEncoder.encode(cleanName, "UTF-8")
                val urlStr = "https://api.tvmaze.com/search/shows?q=$query"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val array = JSONArray(response)
                    for (i in 0 until Math.min(array.length(), 10)) {
                        val show = array.getJSONObject(i).optJSONObject("show")
                        if (show != null) {
                            val out = JSONObject()
                            out.put("title", show.optString("name"))
                            val image = show.optJSONObject("image")
                            if (image != null) {
                                val posterUrl = image.optString("original").ifEmpty { image.optString("medium") }
                                if (posterUrl.isNotEmpty()) out.put("poster_url", posterUrl)
                            }
                            val rating = show.optJSONObject("rating")
                            if (rating != null && rating.has("average") && !rating.isNull("average")) {
                                out.put("vote_average", rating.getDouble("average"))
                            }
                            var summary = show.optString("summary", "")
                            summary = summary.replace(Regex("<[^>]*>"), "")
                            if (summary.isNotEmpty()) out.put("overview", summary)
                            finalResults.put(out)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MetadataFetcher", "Error searching TVMaze", e)
            }
        }
        
        if (finalResults.length() == 0) {
            // iTunes fallback
            try {
                val query = URLEncoder.encode(cleanName, "UTF-8")
                val urlStr = "https://itunes.apple.com/search?term=$query&media=movie&limit=10"
                val url = URL(urlStr)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                
                if (conn.responseCode == 200) {
                    val response = conn.inputStream.bufferedReader().use { it.readText() }
                    val json = JSONObject(response)
                    val results = json.optJSONArray("results")
                    if (results != null) {
                        for (i in 0 until Math.min(results.length(), 10)) {
                            val item = results.getJSONObject(i)
                            val out = JSONObject()
                            out.put("title", item.optString("trackName"))
                            var artwork = item.optString("artworkUrl100", "")
                            if (artwork.isNotEmpty()) {
                                artwork = artwork.replace("100x100bb.jpg", "600x600bb.jpg")
                                out.put("poster_url", artwork)
                            }
                            val desc = item.optString("longDescription", "")
                            if (desc.isNotEmpty()) out.put("overview", desc)
                            finalResults.put(out)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MetadataFetcher", "Error searching iTunes", e)
            }
        }
        
        return@withContext finalResults
    }
}

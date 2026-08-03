package com.example

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.partialcontent.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import io.ktor.server.http.content.*
import io.ktor.http.content.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.io.File
import org.json.JSONArray
import org.json.JSONObject
import java.io.InputStream
import kotlinx.coroutines.Job
import kotlin.coroutines.CoroutineContext

class KtorServer(
    private val appCtx: Context,
    private val rootFolderUri: String,
    private val port: Int = 8080,
    private val onClientConnected: (String) -> Unit
) {
    private var server: EmbeddedServer<*, *>? = null
    private val transcodeManager = TranscodeManager(appCtx)
    
    fun start() {
        server = embeddedServer(CIO, port = port) {
            install(PartialContent) {
                // Maximum number of ranges that will be accepted from a HTTP request.
                // If the HTTP request specifies more ranges, they will all be merged into a single range.
                maxRangeCount = 10
            }
            install(CORS) {
                anyHost()
                allowHeader(HttpHeaders.ContentType)
                allowHeader(HttpHeaders.Authorization)
                allowHeader(HttpHeaders.Range)
            }
            install(WebSockets) {
                masking = false
            }
            
            // For simplicity in this example, basic auth is defined.
            install(Authentication) {
                basic("auth-basic") {
                    realm = "Access to the Media Server"
                    validate { credentials ->
                        if (credentials.name == "admin" && credentials.password == "password") {
                            UserIdPrincipal(credentials.name)
                        } else {
                            null
                        }
                    }
                }
            }
            
            intercept(ApplicationCallPipeline.Call) {
                val method = call.request.httpMethod.value
                val uri = call.request.uri
                val client = call.request.local.remoteHost
                MediaServerService.logMessage("HTTP", "$method $uri ($client)")
            }

            routing {
                // Remote Control WebSocket Route (Unauthenticated or Basic Auth compatible)
                webSocket("/ws/remote") {
                    val clientIp = call.request.local.remoteHost
                    MediaServerService.logMessage("WS", "Remote WebSocket connected from $clientIp")
                    RemoteControlManager.addSession(this)
                    try {
                        for (frame in incoming) {
                            if (frame is Frame.Text) {
                                val text = frame.readText()
                                RemoteControlManager.handleIncomingMessage(this, text)
                            }
                        }
                    } catch (e: Exception) {
                        MediaServerService.logMessage("WS", "WebSocket session closed ($clientIp)")
                    } finally {
                        RemoteControlManager.removeSession(this)
                        MediaServerService.logMessage("WS", "Remote WebSocket disconnected from $clientIp")
                    }
                }

                get("/api/remote/pairing") {
                    val code = RemoteControlManager.getPairingCode()
                    val state = RemoteControlManager.playbackState.value
                    val resp = JSONObject()
                    resp.put("pairingCode", code)
                    resp.put("connectedClients", state.activeClientsCount)
                    resp.put("mediaTitle", state.mediaTitle)
                    call.respondText(resp.toString(), ContentType.Application.Json)
                }

                get("/api/remote/qr") {
                    val host = call.request.host()
                    val pairingCode = RemoteControlManager.getPairingCode()
                    val targetUrl = "http://$host:$port/?pair=$pairingCode"
                    val matrix = QrCodeGenerator.encodeToMatrix(targetUrl)
                    val matrixSize = matrix.size
                    val svg = StringBuilder()
                    svg.append("""<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 $matrixSize $matrixSize" shape-rendering="crispEdges">""")
                    svg.append("""<rect width="100%" height="100%" fill="#ffffff"/>""")
                    for (r in 0 until matrixSize) {
                        for (c in 0 until matrixSize) {
                            if (matrix[r][c]) {
                                svg.append("""<rect x="$c" y="$r" width="0.95" height="0.95" fill="#0f172a" rx="0.1"/>""")
                            }
                        }
                    }
                    svg.append("</svg>")
                    call.respondText(svg.toString(), ContentType.Image.SVG)
                }

                post("/api/remote/command") {
                    val text = call.receiveText()
                    try {
                        val json = JSONObject(text)
                        val action = json.optString("action", "")
                        RemoteControlManager.sendCommand(action)
                        call.respondText("""{"status":"ok"}""", ContentType.Application.Json)
                    } catch (e: Exception) {
                        call.respond(HttpStatusCode.BadRequest, e.message ?: "Error")
                    }
                }

                authenticate("auth-basic") {
                    get("/") {
                        val clientIp = call.request.local.remoteHost
                        MediaServerService.logMessage("INFO", "Client connected from $clientIp")
                        onClientConnected(clientIp)
                        val html = appCtx.assets.open("index.html").bufferedReader().use { it.readText() }
                        call.respondText(html, ContentType.Text.Html)
                    }
                    
                    
                    
                    get("/api/storage") {
                        try {
                            val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                            val anyFile = rootFile?.listFiles()?.firstOrNull { it.isFile }
                            
                            var totalBytes = 0L
                            var freeBytes = 0L
                            
                            var pfd: android.os.ParcelFileDescriptor? = null
                            if (anyFile != null) {
                                try {
                                    pfd = appCtx.contentResolver.openFileDescriptor(anyFile.uri, "r")
                                } catch (e: Exception) {}
                            }
                            
                            if (pfd != null) {
                                val stat = android.system.Os.fstatvfs(pfd.fileDescriptor)
                                totalBytes = stat.f_blocks * stat.f_frsize
                                freeBytes = stat.f_bavail * stat.f_frsize
                                pfd.close()
                            } else {
                                val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().absolutePath)
                                totalBytes = stat.totalBytes
                                freeBytes = stat.availableBytes
                            }
                            
                            val obj = JSONObject()
                            obj.put("total", totalBytes)
                            obj.put("free", freeBytes)
                            obj.put("used", totalBytes - freeBytes)
                            call.respondText(obj.toString(), ContentType.Application.Json)
                        } catch (e: Exception) {
                            val obj = JSONObject()
                            obj.put("total", 1)
                            obj.put("free", 1)
                            call.respondText(obj.toString(), ContentType.Application.Json)
                        }
                    }
                    get("/api/network-test") {
                        // 5 MB dummy data for network testing
                        val size = 5 * 1024 * 1024 
                        val bytes = ByteArray(size) { (it % 256).toByte() }
                        call.respondBytes(bytes, ContentType.Application.OctetStream)
                    }
                    get("/api/profiles") {
                        val profiles = ProfileManager.getProfiles(appCtx)
                        call.respondText(profiles.toString(), ContentType.Application.Json)
                    }
                    post("/api/profiles") {
                        val jsonString = call.receiveText()
                        val req = JSONObject(jsonString)
                        val name = req.optString("name")
                        val avatar = req.optString("avatar", "#6366f1")
                        val newProfile = ProfileManager.addProfile(appCtx, name, avatar)
                        call.respondText(newProfile.toString(), ContentType.Application.Json)
                    }
                    delete("/api/profiles/{id}") {
                        val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
                        val success = ProfileManager.deleteProfile(appCtx, id)
                        if (success) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                    get("/api/profiles/{id}/playlists") {
                        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val playlists = ProfileManager.getPlaylists(appCtx, id)
                        call.respondText(playlists.toString(), ContentType.Application.Json)
                    }
                    post("/api/profiles/{id}/playlists") {
                        val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val jsonString = call.receiveText()
                        val playlists = JSONArray(jsonString)
                        ProfileManager.savePlaylists(appCtx, id, playlists)
                        call.respond(HttpStatusCode.OK)
                    }
                    get("/api/profiles/{id}/progress") {
                        val id = call.parameters["id"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val progress = ProfileManager.getProgress(appCtx, id)
                        call.respondText(progress.toString(), ContentType.Application.Json)
                    }
                    post("/api/profiles/{id}/progress") {
                        val id = call.parameters["id"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val jsonString = call.receiveText()
                        val req = JSONObject(jsonString)
                        val media = req.optString("media")
                        val time = req.optDouble("time", 0.0)
                        val pct = req.optDouble("pct", 0.0)
                        ProfileManager.updateProgress(appCtx, id, media, time, pct)
                        call.respond(HttpStatusCode.OK)
                    }

                    get("/api/files") {
                        val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                        val files = rootFile?.listFiles() ?: emptyArray()
                        val jsonArray = JSONArray()
                        for (file in files) {
                            if (file.isFile && file.name != null) {
                                val obj = JSONObject()
                                obj.put("name", file.name)
                                obj.put("size", file.length())
                                obj.put("type", file.type)
                                val metadata = MetadataFetcher.getSavedMetadata(appCtx, file.name!!)
                                if (metadata != null) {
                                    obj.put("metadata", metadata)
                                }
                                jsonArray.put(obj)
                            }
                        }
                        call.respondText(jsonArray.toString(), ContentType.Application.Json)
                    }
                    get("/api/thumbnail/{name}") {
                        val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                        val file = rootFile?.listFiles()?.find { it.name == name }
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                            
                        val thumbnailFile = transcodeManager.getThumbnail(name, file)
                        if (thumbnailFile != null && thumbnailFile.exists()) {
                            call.respondFile(thumbnailFile)
                        } else {
                            MediaServerService.logMessage("WARN", "Thumbnail missing for $name")
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                    get("/api/metadata/{name}") {
                        val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        var metadata = MetadataFetcher.getSavedMetadata(appCtx, name)
                        if (metadata != null) {
                            call.respondText(metadata.toString(), ContentType.Application.Json)
                        } else {
                            val results = MetadataFetcher.searchMetadata(name)
                            if (results.length() > 0) {
                                metadata = results.getJSONObject(0)
                                MetadataFetcher.saveMetadata(appCtx, name, metadata)
                                call.respondText(metadata.toString(), ContentType.Application.Json)
                            } else {
                                var cleanTitle = name.substringBeforeLast(".")
                                    .replace(".", " ")
                                    .replace("_", " ")
                                    .replace("-", " ")
                                    .replace(Regex("\\s+"), " ")
                                    .trim()
                                val fallback = JSONObject().apply {
                                    put("title", cleanTitle)
                                    put("overview", "Library media file: $name")
                                    put("vote_average", 8.0)
                                }
                                MetadataFetcher.saveMetadata(appCtx, name, fallback)
                                call.respondText(fallback.toString(), ContentType.Application.Json)
                            }
                        }
                    }
                    get("/api/metadata/search") {
                        val query = call.request.queryParameters["q"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val results = MetadataFetcher.searchMetadata(query)
                        call.respondText(results.toString(), ContentType.Application.Json)
                    }
                    post("/api/metadata/{name}") {
                        val name = call.parameters["name"] ?: return@post call.respond(HttpStatusCode.BadRequest)
                        val jsonString = call.receiveText()
                        val json = JSONObject(jsonString)
                        MetadataFetcher.saveMetadata(appCtx, name, json)
                        call.respond(HttpStatusCode.OK)
                    }
                    get("/api/subtitles/{name}") {
                        val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                        val files = rootFile?.listFiles() ?: emptyArray()
                        val baseName = name.substringBeforeLast(".")
                        val subs = JSONArray()
                        for (file in files) {
                            val fileName = file.name
                            if (fileName != null && (fileName.endsWith(".srt", ignoreCase = true) || fileName.endsWith(".vtt", ignoreCase = true))) {
                                val isMatch = fileName.startsWith(baseName, ignoreCase = true) || 
                                              fileName.contains(baseName, ignoreCase = true) || 
                                              baseName.contains(fileName.substringBeforeLast("."), ignoreCase = true)
                                if (isMatch) {
                                    val obj = JSONObject()
                                    var label = fileName
                                    val suffix = fileName.removePrefix(baseName).removeSuffix(".srt").removeSuffix(".vtt").trim('.', '_', ' ')
                                    if (suffix.isNotEmpty()) {
                                        label = when (suffix.lowercase()) {
                                            "en", "eng", "english" -> "English ($fileName)"
                                            "es", "spa", "spanish" -> "Spanish ($fileName)"
                                            "fr", "fre", "fra", "french" -> "French ($fileName)"
                                            "de", "ger", "german" -> "German ($fileName)"
                                            "pt", "por", "portuguese" -> "Portuguese ($fileName)"
                                            "it", "ita", "italian" -> "Italian ($fileName)"
                                            else -> "${suffix.uppercase()} ($fileName)"
                                        }
                                    }
                                    obj.put("name", fileName)
                                    obj.put("label", label)
                                    val encodedName = java.net.URLEncoder.encode(fileName, "UTF-8").replace("+", "%20")
                                    obj.put("url", "/api/subtitles/vtt/$encodedName")
                                    subs.put(obj)
                                }
                            }
                        }
                        call.respondText(subs.toString(), ContentType.Application.Json)
                    }
                    get("/api/subtitles/vtt/{filename}") {
                        val filename = call.parameters["filename"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                        val file = rootFile?.listFiles()?.find { it.name == filename }
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                            
                        val inputStream = appCtx.contentResolver.openInputStream(file.uri)
                        if (inputStream != null) {
                            val rawText = inputStream.bufferedReader().use { it.readText() }
                            val vttText = if (filename.endsWith(".srt", ignoreCase = true)) {
                                val converted = rawText.replace(Regex("(\\d{2}:\\d{2}:\\d{2}),(\\d{3})"), "$1.$2")
                                "WEBVTT\n\n$converted"
                            } else {
                                rawText
                            }
                            call.respondText(vttText, ContentType.parse("text/vtt; charset=utf-8"))
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                    get("/api/transcode/{name}") {
                        val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                        val file = rootFile?.listFiles()?.find { it.name == name }
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                            
                        val hlsUrl = transcodeManager.startTranscode(name, file)?.let { "/hls/${java.net.URLEncoder.encode(name, "UTF-8").replace("+", "%20")}/playlist.m3u8" }
                        if (hlsUrl != null) {
                            val obj = JSONObject()
                            obj.put("status", "ready")
                            obj.put("url", hlsUrl)
                            call.respondText(obj.toString(), ContentType.Application.Json)
                        } else {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                    get("/media/{name}") {
                        val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                        val file = rootFile?.listFiles()?.find { it.name == name }
                            ?: return@get call.respond(HttpStatusCode.NotFound)
                            
                        val type = file.type ?: "application/octet-stream"
                        val size = file.length()
                        
                        val rangeHeader = call.request.header(HttpHeaders.Range)
                        
                        if (rangeHeader != null) {
                            val ranges = rangeHeader.removePrefix("bytes=").split("-")
                            val start = ranges.getOrNull(0)?.toLongOrNull() ?: 0L
                            val end = ranges.getOrNull(1)?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: (size - 1)
                            val contentLength = end - start + 1
                            
                            call.response.header(HttpHeaders.ContentRange, "bytes $start-$end/$size")
                            call.response.header(HttpHeaders.AcceptRanges, "bytes")
                            call.respond(HttpStatusCode.PartialContent, object : OutgoingContent.ReadChannelContent() {
                                override val contentLength: Long = contentLength
                                override val contentType: ContentType = ContentType.parse(type)
                                override fun readFrom(): ByteReadChannel {
                                    val fd = appCtx.contentResolver.openFileDescriptor(file.uri, "r")
                                    return if (fd != null) {
                                        val inputStream = java.io.FileInputStream(fd.fileDescriptor).apply {
                                            channel.position(start)
                                        }
                                        inputStream.toByteReadChannel()
                                    } else {
                                        ByteReadChannel.Empty
                                    }
                                }
                            })
                        } else {
                            call.response.header(HttpHeaders.AcceptRanges, "bytes")
                            call.respond(object : OutgoingContent.ReadChannelContent() {
                                override val contentLength: Long = size
                                override val contentType: ContentType = ContentType.parse(type)
                                override fun readFrom(): ByteReadChannel {
                                    val inputStream = appCtx.contentResolver.openInputStream(file.uri)
                                    return inputStream?.toByteReadChannel() ?: ByteReadChannel.Empty
                                }
                            })
                        }
                    }
                    get("/hls/{folder}/{file}") {
                        val folder = call.parameters["folder"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val fileName = call.parameters["file"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        
                        var targetFile = File(transcodeManager.getTranscodeDir(folder), fileName)
                        if (!targetFile.exists()) {
                            targetFile = File(appCtx.cacheDir, "hls/$folder/$fileName")
                        }
                        
                        // If requesting playlist and not ready yet, trigger on-demand transcode
                        if (!targetFile.exists() && (fileName == "playlist.m3u8" || fileName.endsWith(".m3u8"))) {
                            val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
                            val files = rootFile?.listFiles() ?: emptyArray()
                            val docFile = files.find { 
                                it.name == folder || 
                                it.name == java.net.URLDecoder.decode(folder, "UTF-8")
                            }
                            if (docFile != null) {
                                val generatedPlaylist = transcodeManager.startTranscode(folder, docFile)
                                if (generatedPlaylist != null && generatedPlaylist.exists()) {
                                    targetFile = if (fileName == "playlist.m3u8") generatedPlaylist else File(generatedPlaylist.parentFile, fileName)
                                }
                            }
                        }
                        
                        if (targetFile.exists()) {
                            call.respondFile(targetFile)
                        } else {
                            MediaServerService.logMessage("WARN", "HLS file not found: $folder/$fileName")
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                }
            }
        }.start(wait = false)
    }
    
    fun stop() {
        server?.stop(1000, 5000)
    }
}

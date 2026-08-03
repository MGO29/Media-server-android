package com.example

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import android.util.Log

class TranscodeManager(private val context: Context) {
    companion object {
        private val _activeTranscodeCount = MutableStateFlow(0)
        val activeTranscodeCount = _activeTranscodeCount.asStateFlow()
    }
    private val activeSessions = mutableMapOf<String, FFmpegSession>()
    private val transcodeDir = File(context.cacheDir, "transcodes")
    private val thumbnailDir = File(context.cacheDir, "thumbnails")

    init {
        transcodeDir.mkdirs()
        thumbnailDir.mkdirs()
    }

    suspend fun getThumbnail(name: String, file: DocumentFile): File? = withContext(Dispatchers.IO) {
        if (!thumbnailDir.exists()) thumbnailDir.mkdirs()
        val thumbnailFile = File(thumbnailDir, "${name}.jpg")
        if (thumbnailFile.exists()) {
            return@withContext thumbnailFile
        }

        val inputSaf = FFmpegKitConfig.getSafParameterForRead(context, file.uri)
        val command = "-y -i \"$inputSaf\" -ss 00:00:01.000 -vframes 1 \"${thumbnailFile.absolutePath}\""
        
        Log.d("Transcoder", "Executing thumbnail command: $command")
        
        val session = FFmpegKit.execute(command)
        
        if (com.arthenica.ffmpegkit.ReturnCode.isSuccess(session.returnCode)) {
            thumbnailFile
        } else {
            Log.e("Transcoder", "Thumbnail extraction failed: ${session.failStackTrace}")
            null
        }
    }

    suspend fun startTranscode(name: String, file: DocumentFile): File? = withContext(Dispatchers.IO) {
        val outDir = File(transcodeDir, name)
        if (!outDir.exists()) outDir.mkdirs()

        val playlistFile = File(outDir, "playlist.m3u8")
        if (playlistFile.exists()) {
            return@withContext playlistFile // Already transcoded or transcoding
        }

        val inputSaf = FFmpegKitConfig.getSafParameterForRead(context, file.uri)
        val segmentPattern = File(outDir, "%03d.ts").absolutePath
        
        // Transcode video using hardware-accelerated MediaCodec, transcode audio to aac stereo
        val command = "-y -hwaccel mediacodec -i \"$inputSaf\" -c:v h264_mediacodec -b:v 2M -c:a aac -ac 2 -f hls -hls_time 10 -hls_list_size 0 -hls_segment_filename \"$segmentPattern\" \"${playlistFile.absolutePath}\""
        
        MediaServerService.logMessage("TRANSCODE", "Starting MediaCodec transcode for: $name")
        Log.d("Transcoder", "Executing command: $command")
        
        val session = FFmpegKit.executeAsync(command) { session ->
            Log.d("Transcoder", "Session finished with state: ${session.state}")
            MediaServerService.logMessage("TRANSCODE", "Transcode session for $name ended: ${session.state}")
            activeSessions.remove(name)
            _activeTranscodeCount.value = activeSessions.size
        }
        activeSessions[name] = session
        _activeTranscodeCount.value = activeSessions.size
        
        // Wait until playlist is generated or session fails
        var retries = 0
        while (!playlistFile.exists() && retries < 100) {
            val state = session.state
            if (state == com.arthenica.ffmpegkit.SessionState.FAILED || state == com.arthenica.ffmpegkit.SessionState.COMPLETED) {
                break
            }
            Thread.sleep(200)
            retries++
        }
        
        if (playlistFile.exists()) {
            playlistFile
        } else {
            null
        }
    }
    
    fun getTranscodeDir(name: String): File {
        return File(transcodeDir, name)
    }
}

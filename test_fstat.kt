import android.content.Context
import androidx.documentfile.provider.DocumentFile
import android.system.Os
import android.os.StatFs
import android.os.Environment

fun testStorage(appCtx: Context, rootFolderUri: String) {
    try {
        val rootFile = DocumentFile.fromTreeUri(appCtx, android.net.Uri.parse(rootFolderUri))
        val firstFile = rootFile?.listFiles()?.firstOrNull { it.isFile }
        
        var pfd: android.os.ParcelFileDescriptor? = null
        if (firstFile != null) {
            pfd = appCtx.contentResolver.openFileDescriptor(firstFile.uri, "r")
        }
        
        val stat = if (pfd != null) {
            android.os.StatFs("/proc/self/fd/${pfd.fd}")
        } else {
            android.os.StatFs(Environment.getExternalStorageDirectory().absolutePath)
        }
        pfd?.close()
        
        println(stat.availableBytes)
    } catch (e: Exception) {
        println(e)
    }
}

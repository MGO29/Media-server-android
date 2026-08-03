import sys

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'r') as f:
    text = f.read()

endpoints = """
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
"""

text = text.replace("""
                    get("/api/storage") {
                        try {
                            val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().absolutePath)
                            val total = stat.totalBytes
                            val free = stat.availableBytes
                            val obj = JSONObject()
                            obj.put("total", total)
                            obj.put("free", free)
                            obj.put("used", total - free)
                            call.respondText(obj.toString(), ContentType.Application.Json)
                        } catch (e: Exception) {
                            val obj = JSONObject()
                            obj.put("total", 1)
                            obj.put("free", 1)
                            call.respondText(obj.toString(), ContentType.Application.Json)
                        }
                    }
""", endpoints)

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'w') as f:
    f.write(text)

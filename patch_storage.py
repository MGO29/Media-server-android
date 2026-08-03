import sys

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'r') as f:
    text = f.read()

endpoints = """
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
"""

if 'get("/api/storage")' not in text:
    text = text.replace('get("/api/network-test") {', endpoints + '                    get("/api/network-test") {')
    with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'w') as f:
        f.write(text)

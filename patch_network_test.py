import sys

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'r') as f:
    text = f.read()

endpoints = """
                    get("/api/network-test") {
                        // 5 MB dummy data for network testing
                        val size = 5 * 1024 * 1024 
                        val bytes = ByteArray(size) { (it % 256).toByte() }
                        call.respondBytes(bytes, ContentType.Application.OctetStream)
                    }
"""

if 'get("/api/network-test")' not in text:
    text = text.replace('get("/api/profiles") {', endpoints + '                    get("/api/profiles") {')
    with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'w') as f:
        f.write(text)

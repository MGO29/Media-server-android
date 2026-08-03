import sys

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'r') as f:
    text = f.read()

endpoints = """
                    get("/api/profiles") {
                        val profiles = ProfileManager.getProfiles(appCtx)
                        call.respondText(profiles.toString(), ContentType.Application.Json)
                    }
                    post("/api/profiles") {
                        val jsonString = call.receiveText()
                        val name = JSONObject(jsonString).optString("name")
                        val newProfile = ProfileManager.addProfile(appCtx, name)
                        call.respondText(newProfile.toString(), ContentType.Application.Json)
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
"""

if 'get("/api/profiles")' not in text:
    text = text.replace('get("/api/files") {', endpoints + '\n                    get("/api/files") {')
    with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'w') as f:
        f.write(text)

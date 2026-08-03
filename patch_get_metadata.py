import sys

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'r') as f:
    text = f.read()

target = """                    get("/api/metadata/search") {"""
replacement = """                    get("/api/metadata/{name}") {
                        val name = call.parameters["name"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        val metadata = MetadataFetcher.getSavedMetadata(appCtx, name)
                        if (metadata != null) {
                            call.respondText(metadata.toString(), ContentType.Application.Json)
                        } else {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }
                    get("/api/metadata/search") {"""

if 'get("/api/metadata/{name}") {' not in text:
    text = text.replace(target, replacement)
    with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'w') as f:
        f.write(text)

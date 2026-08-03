import sys

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'r') as f:
    text = f.read()

target = """                                val obj = JSONObject()
                                obj.put("name", file.name)
                                obj.put("size", file.length())
                                obj.put("type", file.type)
                                jsonArray.put(obj)"""

replacement = """                                val obj = JSONObject()
                                obj.put("name", file.name)
                                obj.put("size", file.length())
                                obj.put("type", file.type)
                                val metadata = MetadataFetcher.getSavedMetadata(appCtx, file.name!!)
                                if (metadata != null) {
                                    obj.put("metadata", metadata)
                                }
                                jsonArray.put(obj)"""

text = text.replace(target, replacement)
with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'w') as f:
    f.write(text)

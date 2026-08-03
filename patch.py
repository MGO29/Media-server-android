import sys

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'r') as f:
    text = f.read()

text = text.replace('\\"', '"')

with open('/app/applet/app/src/main/java/com/example/KtorServer.kt', 'w') as f:
    f.write(text)

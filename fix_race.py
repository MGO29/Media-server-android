import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

text = text.replace("switchProfile(true);", "switchProfile(false);")

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

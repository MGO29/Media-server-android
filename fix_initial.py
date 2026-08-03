import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

text = text.replace("function switchProfile(initial = false) {", "function switchProfile() {")
text = text.replace("""                    if (!initial) {
                        renderMediaList();
                    }""", "                    renderMediaList();")

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

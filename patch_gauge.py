import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

header_search = """        <h1>Android Media Server</h1>"""
header_replace = """        <div style="display: flex; align-items: center; gap: 20px;">
            <h1 style="margin: 0;">Android Media Server</h1>
            <div id="storageGauge" style="display: flex; align-items: center; gap: 10px; font-size: 14px; background: rgba(128,128,128,0.1); padding: 8px 12px; border-radius: 8px;">
                <span id="storageText">Loading storage...</span>
                <div style="width: 100px; height: 8px; background: rgba(128,128,128,0.3); border-radius: 4px; overflow: hidden;">
                    <div id="storageBar" style="width: 0%; height: 100%; background: var(--primary);"></div>
                </div>
            </div>
        </div>"""

text = text.replace(header_search, header_replace)

js_code = """
        function loadStorage() {
            fetch('/api/storage')
                .then(res => res.json())
                .then(data => {
                    const total = data.total;
                    const free = data.free;
                    const used = data.used;
                    if (total > 1) {
                        const pct = (used / total) * 100;
                        document.getElementById('storageBar').style.width = pct + '%';
                        
                        let color = 'var(--primary)';
                        if (pct > 90) color = '#ef5350';
                        else if (pct > 75) color = '#FF9800';
                        document.getElementById('storageBar').style.background = color;
                        
                        document.getElementById('storageText').innerText = formatBytes(free) + ' free';
                    } else {
                        document.getElementById('storageGauge').style.display = 'none';
                    }
                })
                .catch(e => {
                    document.getElementById('storageGauge').style.display = 'none';
                });
        }
"""

text = text.replace('        function loadProfiles() {', js_code + '\n        function loadProfiles() {')
text = text.replace('        loadProfiles();', '        loadProfiles();\n        loadStorage();')

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

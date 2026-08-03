import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

header_search = """        <div style="display: flex; gap: 15px; align-items: center;">
            <select id="profileSelect" """

header_replace = """        <div style="display: flex; gap: 15px; align-items: center;">
            <button onclick="testNetworkHealth()" style="padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(128,128,128,0.5); background: transparent; color: inherit; font-size: 14px; cursor: pointer;">Network Health</button>
            <select id="profileSelect" """

text = text.replace(header_search, header_replace)

modal_html = """
    <div class="identify-modal" id="networkModal">
        <div class="identify-modal-content" style="max-width: 400px; text-align: center;">
            <div class="identify-header">
                <h3 style="margin:0">Network Health Test</h3>
                <button class="close-modal-btn" onclick="closeNetworkModal()">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M18 6L6 18M6 6l12 12"/></svg>
                </button>
            </div>
            <div class="identify-body">
                <div id="networkTestStatus" style="font-size: 16px; margin-bottom: 20px;">Ready to test.</div>
                <div id="networkTestResult" style="font-size: 24px; font-weight: bold; margin-bottom: 20px;"></div>
                <button onclick="runNetworkTest()" style="padding: 10px 20px; background: var(--primary); color: white; border: none; border-radius: 8px; cursor: pointer; font-size: 16px;">Start Test</button>
            </div>
        </div>
    </div>
"""

text = text.replace('    <div class="identify-modal" id="identifyModal">', modal_html + '    <div class="identify-modal" id="identifyModal">')

js_code = """
        function testNetworkHealth() {
            document.getElementById('networkModal').style.display = 'flex';
            document.getElementById('networkTestStatus').innerText = 'Ready to test.';
            document.getElementById('networkTestResult').innerText = '';
        }
        
        function closeNetworkModal() {
            document.getElementById('networkModal').style.display = 'none';
        }
        
        function runNetworkTest() {
            const statusEl = document.getElementById('networkTestStatus');
            const resultEl = document.getElementById('networkTestResult');
            statusEl.innerText = 'Downloading 5MB dummy file...';
            resultEl.innerText = '';
            
            const url = '/api/network-test?cb=' + Date.now();
            const startTime = performance.now();
            
            fetch(url)
                .then(res => res.blob())
                .then(blob => {
                    const endTime = performance.now();
                    const durationSec = (endTime - startTime) / 1000;
                    const sizeBits = blob.size * 8;
                    const bps = sizeBits / durationSec;
                    const mbps = bps / (1024 * 1024);
                    
                    let quality = "Poor";
                    let color = "#ef5350"; // red
                    if (mbps > 25) {
                        quality = "4K";
                        color = "#4CAF50"; // green
                    } else if (mbps > 10) {
                        quality = "1080p";
                        color = "#8BC34A"; // light green
                    } else if (mbps > 5) {
                        quality = "720p";
                        color = "#FFEB3B"; // yellow
                    } else if (mbps > 2) {
                        quality = "480p";
                        color = "#FF9800"; // orange
                    }
                    
                    statusEl.innerText = `Bandwidth: ${mbps.toFixed(2)} Mbps`;
                    resultEl.innerHTML = `Est. Quality: <span style="color: ${color}">${quality}</span>`;
                })
                .catch(err => {
                    statusEl.innerText = 'Test failed.';
                    console.error(err);
                });
        }
"""

text = text.replace('        function closeIdentifyModal() {', js_code + '        function closeIdentifyModal() {')

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

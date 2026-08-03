import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

target = """            vjsPlayer.on('timeupdate', function() {
                if (currentMediaName && vjsPlayer.duration()) {
                    const currentTime = vjsPlayer.currentTime();
                    const duration = vjsPlayer.duration();
                    const pct = (currentTime / duration) * 100;
                    
                    localStorage.setItem('progress_time_' + currentMediaName, currentTime);
                    localStorage.setItem('progress_pct_' + currentMediaName, pct);
                }
            });"""

replacement = """            let lastSaveTime = 0;
            vjsPlayer.on('timeupdate', function() {
                if (currentMediaName && vjsPlayer.duration() && window.currentProfile) {
                    const ct = vjsPlayer.currentTime();
                    const dur = vjsPlayer.duration();
                    const pct = (ct / dur) * 100;
                    
                    // Local state update
                    window.userProgress[currentMediaName] = { time: ct, pct: pct };
                    
                    // Server save throttle (every 5 seconds)
                    const now = Date.now();
                    if (now - lastSaveTime > 5000) {
                        lastSaveTime = now;
                        fetch('/api/profiles/' + window.currentProfile + '/progress', {
                            method: 'POST',
                            body: JSON.stringify({ media: currentMediaName, time: ct, pct: pct })
                        }).catch(e => {});
                    }
                }
            });
            
            // Save on pause/close as well
            vjsPlayer.on('pause', function() {
                if (currentMediaName && window.currentProfile) {
                    const ct = vjsPlayer.currentTime();
                    const dur = vjsPlayer.duration();
                    const pct = (ct / dur) * 100;
                    fetch('/api/profiles/' + window.currentProfile + '/progress', {
                        method: 'POST',
                        body: JSON.stringify({ media: currentMediaName, time: ct, pct: pct })
                    }).catch(e => {});
                }
            });"""

text = text.replace(target, replacement)

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

# 1. Header Profile Selector
header_target = """        <div style="display: flex; gap: 15px; align-items: center;">
            <input type="text" id="filterInput" """
header_replace = """        <div style="display: flex; gap: 15px; align-items: center;">
            <select id="profileSelect" style="padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(128,128,128,0.5); background: transparent; color: inherit; font-size: 14px;" onchange="switchProfile()">
            </select>
            <input type="text" id="filterInput" """

if '<select id="profileSelect"' not in text:
    text = text.replace(header_target, header_replace)

# 2. Add global variables and initialization for profiles
init_target = """        window.allFiles = [];
        window.allMetadata = {};"""
init_replace = """        window.allFiles = [];
        window.allMetadata = {};
        window.currentProfile = null;
        window.userProgress = {};

        function loadProfiles() {
            fetch('/api/profiles')
                .then(res => res.json())
                .then(profiles => {
                    const sel = document.getElementById('profileSelect');
                    sel.innerHTML = '';
                    profiles.forEach(p => {
                        const opt = document.createElement('option');
                        opt.value = p.id;
                        opt.textContent = p.name;
                        opt.style.color = 'black';
                        sel.appendChild(opt);
                    });
                    const optAdd = document.createElement('option');
                    optAdd.value = '_add_';
                    optAdd.textContent = '+ Add Profile';
                    optAdd.style.color = 'black';
                    sel.appendChild(optAdd);

                    const savedId = localStorage.getItem('last_profile_id');
                    if (savedId && profiles.find(p => p.id === savedId)) {
                        sel.value = savedId;
                    } else {
                        sel.value = profiles[0].id;
                    }
                    switchProfile(true);
                });
        }

        function switchProfile(initial = false) {
            const sel = document.getElementById('profileSelect');
            if (sel.value === '_add_') {
                const name = prompt("Enter new profile name:");
                if (name) {
                    fetch('/api/profiles', {
                        method: 'POST',
                        body: JSON.stringify({ name: name })
                    }).then(res => res.json()).then(p => {
                        localStorage.setItem('last_profile_id', p.id);
                        loadProfiles();
                    });
                } else {
                    loadProfiles(); // Reset selection
                }
                return;
            }
            window.currentProfile = sel.value;
            localStorage.setItem('last_profile_id', window.currentProfile);
            
            fetch('/api/profiles/' + window.currentProfile + '/progress')
                .then(res => res.json())
                .then(progress => {
                    window.userProgress = progress;
                    if (!initial) {
                        renderMediaList();
                    }
                });
        }
"""
if 'function loadProfiles()' not in text:
    text = text.replace(init_target, init_replace)

# 3. Use server progress instead of localStorage when rendering cards
render_target = """                const savedProgress = parseFloat(localStorage.getItem('progress_pct_' + file.name)) || 0;"""
render_replace = """                const progObj = window.userProgress[file.name] || {};
                const savedProgress = progObj.pct || 0;"""
text = text.replace(render_target, render_replace)

# 4. Modify how playback progress is saved and loaded
play_target = """                            const savedTime = localStorage.getItem('progress_time_' + currentMediaName);
                            if (savedTime) {
                                vjsPlayer.currentTime(parseFloat(savedTime));
                            }"""
play_replace = """                            const progObj = window.userProgress[currentMediaName];
                            if (progObj && progObj.time) {
                                vjsPlayer.currentTime(progObj.time);
                            }"""
text = text.replace(play_target, play_replace)

save_target = """            vjsPlayer.on('timeupdate', function() {
                if (currentMediaName) {
                    const ct = vjsPlayer.currentTime();
                    const dur = vjsPlayer.duration();
                    const pct = (ct / dur) * 100;
                    localStorage.setItem('progress_time_' + currentMediaName, ct);
                    localStorage.setItem('progress_pct_' + currentMediaName, pct);
                }
            });"""
save_replace = """            let lastSaveTime = 0;
            vjsPlayer.on('timeupdate', function() {
                if (currentMediaName && window.currentProfile) {
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
text = text.replace(save_target, save_replace)

# Also need to make sure loadProfiles is called at init instead of just loadMediaList.
# But wait, we want loadMediaList to wait for switchProfile(initial = true), so:
init_call_target = """        loadMediaList();
        
        // Background sync to auto-refresh the media list every 5 seconds
        setInterval(loadMediaList, 5000);"""
init_call_replace = """        loadProfiles();
        loadMediaList();
        
        // Background sync to auto-refresh the media list every 5 seconds
        setInterval(loadMediaList, 5000);"""
if 'loadProfiles();' not in text:
    text = text.replace(init_call_target, init_call_replace)

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

header_search = """    <div class="header">
        <h1>Android Media Server</h1>
        <button class="theme-btn" onclick="toggleTheme()" title="Toggle Dark/Light Mode">"""

header_replace = """    <div class="header">
        <h1>Android Media Server</h1>
        <div style="display: flex; gap: 15px; align-items: center;">
            <input type="text" id="filterInput" placeholder="Filter by name..." style="padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(128,128,128,0.5); background: transparent; color: inherit; font-size: 14px; width: 150px;" oninput="renderMediaList()">
            <select id="sortSelect" style="padding: 8px 12px; border-radius: 8px; border: 1px solid rgba(128,128,128,0.5); background: transparent; color: inherit; font-size: 14px;" onchange="renderMediaList()">
                <option value="name_asc" style="color: black">Name (A-Z)</option>
                <option value="name_desc" style="color: black">Name (Z-A)</option>
                <option value="size_desc" style="color: black">Size (Largest)</option>
                <option value="size_asc" style="color: black">Size (Smallest)</option>
                <option value="rating_desc" style="color: black">Rating (Highest)</option>
                <option value="rating_asc" style="color: black">Rating (Lowest)</option>
            </select>
            <button class="theme-btn" onclick="toggleTheme()" title="Toggle Dark/Light Mode">"""

text = text.replace(header_search, header_replace)
text = text.replace('        </button>\n    </div>', '            </button>\n        </div>\n    </div>')


# Now let's replace the JS
js_search = """        function loadMediaList() {
            fetch('/api/files')
                .then(res => res.json())
                .then(files => {
                    const newFilesList = JSON.stringify(files.map(f => f.name).sort());
                    if (newFilesList === currentFilesList) return; // No changes
                    currentFilesList = newFilesList;
                    
                    const list = document.getElementById('fileList');
                    list.innerHTML = '';
                    
                    const continueList = document.getElementById('continueList');
                    continueList.innerHTML = '';
                    let hasContinueWatching = false;
                    
                    files.forEach(file => {
                        if(!file.type || !file.type.startsWith('video/')) return;
                        
                        const savedProgress = parseFloat(localStorage.getItem('progress_pct_' + file.name)) || 0;
                        
                        const createCard = () => {
                            const card = document.createElement('div');
                            card.className = 'media-card';
                            card.onclick = () => playMedia(file.name);
                            
                            const thumb = document.createElement('div');
                            thumb.className = 'media-thumb';
                            thumb.style.backgroundImage = `url('/api/thumbnail/${encodeURIComponent(file.name)}')`;
                            thumb.style.backgroundSize = 'cover';
                            thumb.style.backgroundPosition = 'center';
                            thumb.innerHTML = `<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>`;
                            
                            const progressBar = document.createElement('div');
                            progressBar.className = 'media-progress-bar';
                            progressBar.innerHTML = `<div class="media-progress-fill" style="width: ${savedProgress}%"></div>`;
                            thumb.appendChild(progressBar);
                            
                            const identifyBtn = document.createElement('button');
                            identifyBtn.className = 'identify-btn';
                            identifyBtn.title = 'Identify Media';
                            identifyBtn.innerHTML = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.3-4.3"/></svg>`;
                            identifyBtn.onclick = (e) => {
                                e.stopPropagation();
                                openIdentifyModal(file.name);
                            };
                            thumb.appendChild(identifyBtn);
                            
                            const info = document.createElement('div');
                            info.className = 'media-info';
                            const title = document.createElement('div');
                            title.className = 'media-title';
                            title.textContent = file.name;
                            
                            const meta = document.createElement('div');
                            meta.className = 'media-meta';
                            meta.textContent = formatBytes(file.size);
                            
                            info.appendChild(title);
                            info.appendChild(meta);
                            card.appendChild(thumb);
                            card.appendChild(info);
                            
                            // Fetch metadata
                            fetch(`/api/metadata/${encodeURIComponent(file.name)}`)
                                .then(res => {
                                    if(res.ok) return res.json();
                                    throw new Error('No metadata');
                                })
                                .then(data => {
                                    if (data.poster_url) {
                                        thumb.style.backgroundImage = `url('${data.poster_url}')`;
                                    } else if (data.poster_path) {
                                        thumb.style.backgroundImage = `url('https://image.tmdb.org/t/p/w500${data.poster_path}')`;
                                    }
                                    const displayTitle = data.title || data.name;
                                    if (displayTitle) {
                                        title.textContent = displayTitle;
                                    }
                                    if (data.vote_average) {
                                        const rating = document.createElement('div');
                                        rating.className = 'media-rating';
                                        rating.style.color = '#f59e0b';
                                        rating.style.fontSize = '12px';
                                        rating.style.marginTop = '4px';
                                        rating.innerHTML = `★ ${data.vote_average.toFixed(1)}`;
                                        info.appendChild(rating);
                                    }
                                    if (data.overview) {
                                        const desc = document.createElement('div');
                                        desc.className = 'media-desc';
                                        desc.style.fontSize = '12px';
                                        desc.style.color = 'var(--text-muted)';
                                        desc.style.marginTop = '4px';
                                        desc.style.display = '-webkit-box';
                                        desc.style.webkitLineClamp = '2';
                                        desc.style.webkitBoxOrient = 'vertical';
                                        desc.style.overflow = 'hidden';
                                        desc.textContent = data.overview;
                                        info.appendChild(desc);
                                    }
                                })
                                .catch(e => { /* Ignore missing metadata */ });
                                
                            return card;
                        };
                        
                        list.appendChild(createCard());
                        
                        if (savedProgress > 0 && savedProgress < 95) {
                            continueList.appendChild(createCard());
                            hasContinueWatching = true;
                        }
                    });
                    
                    document.getElementById('continueWatchingSection').style.display = hasContinueWatching ? 'block' : 'none';
                });
        }"""

js_replace = """        window.allFiles = [];
        window.allMetadata = {};

        function loadMediaList() {
            fetch('/api/files')
                .then(res => res.json())
                .then(files => {
                    const newFilesList = JSON.stringify(files.map(f => f.name).sort());
                    if (newFilesList === currentFilesList) return; // No changes
                    currentFilesList = newFilesList;
                    
                    window.allFiles = files.filter(f => f.type && f.type.startsWith('video/'));
                    
                    // Fetch metadata for all files
                    Promise.all(window.allFiles.map(file => {
                        return fetch(`/api/metadata/${encodeURIComponent(file.name)}`)
                            .then(res => res.ok ? res.json() : null)
                            .then(data => {
                                if (data) window.allMetadata[file.name] = data;
                            })
                            .catch(e => { /* Ignore */ });
                    })).then(() => {
                        renderMediaList();
                    });
                });
        }
        
        function renderMediaList() {
            const list = document.getElementById('fileList');
            list.innerHTML = '';
            
            const continueList = document.getElementById('continueList');
            continueList.innerHTML = '';
            let hasContinueWatching = false;
            
            const filterText = (document.getElementById('filterInput').value || '').toLowerCase();
            const sortVal = document.getElementById('sortSelect').value || 'name_asc';
            
            let files = [...window.allFiles];
            
            // Filter
            if (filterText) {
                files = files.filter(file => {
                    const meta = window.allMetadata[file.name];
                    const title = meta && meta.title ? meta.title : file.name;
                    return title.toLowerCase().includes(filterText);
                });
            }
            
            // Sort
            files.sort((a, b) => {
                const metaA = window.allMetadata[a.name] || {};
                const metaB = window.allMetadata[b.name] || {};
                
                if (sortVal === 'name_asc' || sortVal === 'name_desc') {
                    const titleA = metaA.title || a.name;
                    const titleB = metaB.title || b.name;
                    return sortVal === 'name_asc' ? titleA.localeCompare(titleB) : titleB.localeCompare(titleA);
                }
                
                if (sortVal === 'size_asc' || sortVal === 'size_desc') {
                    return sortVal === 'size_asc' ? a.size - b.size : b.size - a.size;
                }
                
                if (sortVal === 'rating_asc' || sortVal === 'rating_desc') {
                    const ratingA = metaA.vote_average || 0;
                    const ratingB = metaB.vote_average || 0;
                    return sortVal === 'rating_asc' ? ratingA - ratingB : ratingB - ratingA;
                }
                
                return 0;
            });
            
            files.forEach(file => {
                const savedProgress = parseFloat(localStorage.getItem('progress_pct_' + file.name)) || 0;
                
                const createCard = () => {
                    const card = document.createElement('div');
                    card.className = 'media-card';
                    card.onclick = () => playMedia(file.name);
                    
                    const thumb = document.createElement('div');
                    thumb.className = 'media-thumb';
                    thumb.style.backgroundImage = `url('/api/thumbnail/${encodeURIComponent(file.name)}')`;
                    thumb.style.backgroundSize = 'cover';
                    thumb.style.backgroundPosition = 'center';
                    thumb.innerHTML = `<svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5"><path d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z"/></svg>`;
                    
                    const progressBar = document.createElement('div');
                    progressBar.className = 'media-progress-bar';
                    progressBar.innerHTML = `<div class="media-progress-fill" style="width: ${savedProgress}%"></div>`;
                    thumb.appendChild(progressBar);
                    
                    const identifyBtn = document.createElement('button');
                    identifyBtn.className = 'identify-btn';
                    identifyBtn.title = 'Identify Media';
                    identifyBtn.innerHTML = `<svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="11" cy="11" r="8"/><path d="M21 21l-4.3-4.3"/></svg>`;
                    identifyBtn.onclick = (e) => {
                        e.stopPropagation();
                        openIdentifyModal(file.name);
                    };
                    thumb.appendChild(identifyBtn);
                    
                    const info = document.createElement('div');
                    info.className = 'media-info';
                    const title = document.createElement('div');
                    title.className = 'media-title';
                    title.textContent = file.name;
                    
                    const meta = document.createElement('div');
                    meta.className = 'media-meta';
                    meta.textContent = formatBytes(file.size);
                    
                    info.appendChild(title);
                    info.appendChild(meta);
                    card.appendChild(thumb);
                    card.appendChild(info);
                    
                    const data = window.allMetadata[file.name];
                    if (data) {
                        if (data.poster_url) {
                            thumb.style.backgroundImage = `url('${data.poster_url}')`;
                        } else if (data.poster_path) {
                            thumb.style.backgroundImage = `url('https://image.tmdb.org/t/p/w500${data.poster_path}')`;
                        }
                        const displayTitle = data.title || data.name;
                        if (displayTitle) {
                            title.textContent = displayTitle;
                        }
                        if (data.vote_average) {
                            const rating = document.createElement('div');
                            rating.className = 'media-rating';
                            rating.style.color = '#f59e0b';
                            rating.style.fontSize = '12px';
                            rating.style.marginTop = '4px';
                            rating.innerHTML = `★ ${data.vote_average.toFixed(1)}`;
                            info.appendChild(rating);
                        }
                        if (data.overview) {
                            const desc = document.createElement('div');
                            desc.className = 'media-desc';
                            desc.style.fontSize = '12px';
                            desc.style.color = 'var(--text-muted)';
                            desc.style.marginTop = '4px';
                            desc.style.display = '-webkit-box';
                            desc.style.webkitLineClamp = '2';
                            desc.style.webkitBoxOrient = 'vertical';
                            desc.style.overflow = 'hidden';
                            desc.textContent = data.overview;
                            info.appendChild(desc);
                        }
                    }
                        
                    return card;
                };
                
                list.appendChild(createCard());
                
                if (savedProgress > 0 && savedProgress < 95) {
                    continueList.appendChild(createCard());
                    hasContinueWatching = true;
                }
            });
            
            document.getElementById('continueWatchingSection').style.display = hasContinueWatching ? 'block' : 'none';
        }"""

text = text.replace(js_search, js_replace)

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

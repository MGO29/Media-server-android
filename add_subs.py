import sys

with open('/app/applet/app/src/main/assets/index.html', 'r') as f:
    text = f.read()

target = """                        vjsPlayer.src({
                            src: data.url,
                            type: 'application/x-mpegURL'
                        });"""

replacement = """                        vjsPlayer.src({
                            src: data.url,
                            type: 'application/x-mpegURL'
                        });
                        
                        const oldTracks = vjsPlayer.remoteTextTracks();
                        if (oldTracks) {
                            let i = oldTracks.length;
                            while (i--) {
                                vjsPlayer.removeRemoteTextTrack(oldTracks[i]);
                            }
                        }
                        
                        fetch("/api/subtitles/" + encodeURIComponent(name))
                            .then(res => res.json())
                            .then(subs => {
                                subs.forEach(sub => {
                                    vjsPlayer.addRemoteTextTrack({
                                        kind: "captions",
                                        label: sub.name,
                                        src: sub.url
                                    }, false);
                                });
                            });"""

text = text.replace(target, replacement)

with open('/app/applet/app/src/main/assets/index.html', 'w') as f:
    f.write(text)

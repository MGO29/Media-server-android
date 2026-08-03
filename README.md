# 🎬 Android Media Server & Stream Player

A modern, high-performance local media server and streaming client application built for Android. It combines an embedded Ktor web server with Jetpack Compose UI controls, Video.js playback with On-Screen Display (OSD), multi-profile management, and real-time streaming analytics.

---

## ✨ Features

- **🌐 Local Ktor Media Server**: Stream video, audio, and documents locally over Wi-Fi with HLS video transcoding support.
- **📊 Real-time Analytics Dashboard**: Monitor total watch time, bandwidth usage, streaming protocol distribution, direct play rate, and profile consumption trends.
- **📺 Video Player with On-Screen Display (OSD)**: 
  - Subtitle track switching, playback speed controls, and gesture navigation.
  - Interactive OSD info panel (press `O`, `I`, or `D`).
  - Jump to exact timestamps (`MM:SS` or `HH:MM:SS`).
- **👥 Multi-Profile Management**: Dedicated user profiles with individual watch history, theme customization, and playback progress tracking.
- **🎨 Modern Material 3 & Glassmorphic Web UI**:
  - Jetpack Compose interface with smooth spring-animated sliding underline tab navigation.
  - Modern web app hosted in webview assets with customizable accent themes.
- **⚡ Keyboard Shortcuts**: Space (play/pause), Arrow Keys (seek), `O` / `I` (toggle OSD overlay), `Escape` (close player/overlays).

---

## 🏗️ Architecture & Tech Stack

### Android & Native UI
- **Kotlin & Jetpack Compose**: Modern reactive UI with Material Design 3 guidelines.
- **State Management & ViewModel**: Flow, StateFlow, and `collectAsStateWithLifecycle`.
- **Custom Canvas Charts**: Lightweight Compose Canvas bar chart rendering for weekly activity metrics.
- **Spring Animations**: Smooth tab sliding underline transitions powered by `animateDpAsState` and `spring()`.

### Backend & Web Assets
- **Ktor HTTP Server**: Embedded Kotlin server for local streaming and REST API endpoints.
- **Video.js**: HTML5 HLS video player integration with custom OSD overlay and controls.

---

## 🛠️ Building & Running

### Requirements
- Android SDK (API level 24+)
- Kotlin 1.9+ & Gradle (Kotlin DSL)

### Build Commands
```bash
# Build & verify application compilation
compile_applet
```

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for details.

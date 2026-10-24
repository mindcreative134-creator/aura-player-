# Apna Player 🎬🎵
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Target Android](https://img.shields.io/badge/Target%20Android-14%20(API%2036)-green.svg)](app/build.gradle.kts)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](build.gradle.kts)

**Apna Player** is an ultra-fast, modern, all-in-one Media Player for Android engineered with Jetpack Compose, Material 3 Expressive Design, ExoPlayer/Media3 for high-fidelity audio, and native hardware-accelerated video playback.

---

## 🌟 Key Features

### 🎬 Video Player
- **Ultra HD & HDR Hardware Acceleration:** Smooth 4K/60fps playback with zero drop-frames.
- **Ambient Cinema Lighting:** Dynamic, subtle letterbox glow for an immersive theater experience.
- **Intuitive MX-Style Gestures:**
  - Left side swipe: Brightness control.
  - Right side swipe: Volume boost up to 200%.
  - Horizontal swipe: Precision scrubbing.
  - Double tap to seek (+/- 10s) with ripple feedback.
  - Pinch-to-zoom and aspect ratio fitting.
- **Advanced Subtitles:** Complete ASS/SSA styling, SRT, VTT, and embedded track switching.
- **Picture-in-Picture (PiP) & Background Playback:** Multitask seamlessly.

### 🎵 Apna Music & Audio Suite
- **Full-featured Audio Player:** Integrated background playback with lockscreen controls.
- **Synchronized LRC Lyrics:** Real-time synced lyric scrolling powered by LRCLib.
- **Online World Radio:** Stream thousands of high-quality radio stations powered by RadioBrowser.
- **Audio Visualizer & Equalizer:** Real-time frequency response and audio profile boosts.

### 🌐 Network Streaming & Tools
- **Stream Link:** Play direct HTTP/HTTPS/HLS/DASH/RTSP streams with one tap.
- **Integrated Downloader:** Save direct streaming links for offline viewing.
- **Clean Folder & Media Browsing:** Stacked folder visuals, multi-file badges, grid & list view modes.

---

## 🔒 Privacy & Architecture

- **100% Offline-First:** No accounts, no sign-in, no remote database tracking.
- **Zero Telemetry:** No Google Firebase, no ad SDKs, no user analytics.
- **Permission Transparent:** Only requests storage access needed to play local media.

---

## 📜 Open Source & Lineage Acknowledgments

Apna Player is licensed under the [Apache License 2.0](LICENSE). We express our gratitude to the foundational open-source media projects that inspired and contributed to the Android media ecosystem:

- **[mpv-android](https://github.com/mpv-android/mpv-android)** & **[mpvKt](https://github.com/abdallahmehiz/mpvKt)**
- **[mpvEx](https://github.com/marlboro-advance/mpvEx)**
- **[AndroidX Media3 / ExoPlayer](https://github.com/androidx/media)**
- **[Next Player](https://github.com/anilbeesetti/nextplayer)**
- **[Gramophone](https://github.com/FoedusProgramme/Gramophone)**

---

## 🛠️ Building From Source

```bash
# Clone the repository
git clone https://github.com/your-username/apna-player.git

# Build debug APK
./gradlew assembleStandardDebug

# Build release bundle
./gradlew bundlePlaystoreRelease
```

# Apna Player 🎬🎵
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Target Android](https://img.shields.io/badge/Target%20Android-14%20(API%2036)-green.svg)](app/build.gradle.kts)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-purple.svg)](build.gradle.kts)

**Apna Player** is an ultra-fast, modern, all-in-one Media Player for Android engineered with Jetpack Compose, Material 3 Expressive Design, ExoPlayer/Media3 for high-fidelity audio, and native hardware-accelerated video playback.

---

## 🌟 Key Features

### 🎬 Video Player
- **Hardware-Accelerated Video Playback:** Smooth playback powered by Android MediaCodec decoders, supporting up to 4K/60fps and HDR on capable hardware.
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

- **Offline-First for Local Media:** Core video and audio playback operates 100% offline with zero sign-in or cloud requirement.
- **Optional Cloud Features:** Internet radio, synchronized lyrics (LRCLib), and AI subtitle translation are strictly user-initiated.
- **Zero Telemetry:** No Google Firebase Analytics, no ad SDKs, no behavioral trackers.
- **Permission Transparent:** The Google Play flavor uses Android Scoped Storage and strictly excludes All Files Access (`MANAGE_EXTERNAL_STORAGE`).

---

## 📜 Open Source & Lineage Acknowledgments

Apna Player is licensed under the [Apache License 2.0](LICENSE). For full details and third-party notices, please see the [NOTICE](NOTICE) file. We express our gratitude to foundational open-source media projects:

- **[mpv-android](https://github.com/mpv-android/mpv-android)** & **[mpvEx](https://github.com/marlboro-advance/mpvEx)**
- **[AndroidX Media3 / ExoPlayer](https://github.com/androidx/media)**
- **[MediaInfo](https://mediaarea.net/MediaInfo)**
- **[Next Player](https://github.com/anilbeesetti/nextplayer)**
- **[Gramophone](https://github.com/FoedusProgramme/Gramophone)**

---

## 🛠️ Building From Source

```bash
# Clone the repository
git clone https://github.com/mindcreative134-creator/aura-player-.git

# Build debug APK
./gradlew assembleStandardDebug

# Build release bundle
./gradlew bundlePlaystoreRelease
```

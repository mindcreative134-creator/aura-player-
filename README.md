# Aura Player 🎬✨
<div align="center">

![Aura Player Banner](https://raw.githubusercontent.com/mindcreative134-creator/aura-player-/main/assets/banner.png)

### Ultra-Fast, High-Fidelity Local & Network Media Player for Android
*Crafted with Jetpack Compose • Obsidian & Carbon Design System • Media3 HW+ Pipeline*

[![Release](https://img.shields.io/badge/Release-v1.4.0--Aura-00E5FF.svg?style=for-the-badge&logo=android)](https://github.com/mindcreative134-creator/aura-player-/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg?style=for-the-badge)](LICENSE)
[![Target Android](https://img.shields.io/badge/Target-Android%2015%20(API%2035)-00C853.svg?style=for-the-badge&logo=android)](app/build.gradle.kts)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0+-7C4DFF.svg?style=for-the-badge&logo=kotlin)](build.gradle.kts)

</div>

---

## 🌟 Overview

**Aura Player** (`com.cmstudio.auraplayer`) is an original, state-of-the-art Android media playback powerhouse. Designed from the ground up for film enthusiasts, audiophiles, and power users, Aura Player marries the tactile elegance of the **Obsidian & Carbon Design System** with the raw computing performance of **Android Media3 Hardware Acceleration (ExoPlayer)** and an ultra-responsive gesture control pipeline.

Whether you are enjoying 4K HDR Blu-ray rips, organizing a vast FLAC/ALAC library, streaming live network feeds, or safeguarding personal clips in the encrypted PIN-protected Vault, Aura Player delivers an uncompromising, privacy-first mobile experience.

---

## 💎 Design Philosophy & UI/UX Innovations

```mermaid
graph TD
    A[Aura Player Design Language] --> B[Obsidian & Carbon Surfaces]
    A --> C[Tactile Squircle Elements]
    A --> D[Minimalist Floating HUD]
    A --> E[Categorized Settings Architecture]

    B --> B1[Deep Obsidian #060709 Background]
    B --> B2[Frosted Carbon Cards with 1dp Subtle Borders]

    C --> C1[2x2 Quick Action Tiles: Recent, Favorite, Audio, Video]
    C --> C2[Smart Color-Coded Folders: Camera, Downloads, Movies, Audio]

    D --> D1[Floating Glassmorphic Capsule: 10s Rewind | Play | 10s Forward]
    D --> D2[Cyan Scrubber Timeline & Minimal Slide-Over Drawers]

    E --> E1[Solid Vibrant Squircles: Emerald, Blue, Orange, Magenta, Cyan]
    E --> E2[Search & Live Preference Synchronization]
```

### 1. Home Dashboard — "nPlayer Pro" Aesthetic
- **Storage Telemetry Visualizer:** Real-time scoped storage capacity calculator with multi-segment visualizer (Cyan for videos, Amber for audio, Violet for cache), real video count & size, and rescan trigger.
- **Fast Ingestion Strip:** 3 dedicated elevated tiles:
  - 🌐 **Stream Link:** Instant M3U8 / Direct URL network streaming and downloads.
  - 🖧 **Network NAS:** SMB / FTP / WebDAV network storage browsing.
  - 📑 **Playlists:** Real-time Room database playlist manager with live item counter.
- **2×2 Tactile Squircle Grid:** Elevated rounded cards for immediate access:
  - 🌸 **Recent:** Pink/Coral clock squircle (`#FF3B7A`) with real-time history count.
  - ⭐ **Favorite:** Gold/Amber heart squircle (`#FFB300`) with pinned video count.
  - 🎵 **Audio:** Cobalt blue music note squircle (`#2979FF`) leading to the music library.
  - 🎥 **Video:** Vibrant orange reels squircle (`#FF6D00`) for complete media library browsing.
- **"Frequently Used" Smart Folder Section:**
  - Folders automatically receive category-colored squircles: Camera (Orange `#FF7A00`), Downloads (Amber `#FFB800`), Movies/Recordings (Emerald Green `#00E676`), and Audio (Violet `#7C4DFF`).
  - Dual action buttons on every folder row: Quick Play (1-tap playback) and 3-dot management menu.
- **Hero "Continue Watching" Card:** Ambient glowing progress bar, file size metadata, and tactile 1-tap resume.
- **View Mode Toggle:** Seamless animated switching between multi-column Grid and detailed List layouts.

### 2. Video Player HUD — Floating Glassmorphic Experience
- **Floating Center Capsule:** Symmetrical glassmorphic pill background housing:
  - `⏪ 10s` Quick rewind with haptic pulse.
  - `⏯️` Ambient cyan play/pause button with animated vector morphing.
  - `⏩ 10s` Quick forward jump with haptic pulse.
  - In Playlist mode, seamlessly adapts to Previous / Play / Next track controls.
- **Ultra-Minimal Top & Bottom Bars:**
  - Top: Back arrow, centered video title with playlist indicator chip, and action menu.
  - Bottom: High-precision cyan scrubber timeline (`#00E5FF`) with elapsed/remaining timestamps, chapter markers, and docked fly-out drawers.
- **Tactile Gesture Feedback:**
  - Left swipe: Brightness level control with frosted HUD indicator.
  - Right swipe: 200% volume boost with software preamp limiter.
  - Horizontal scrub: Precise seek bar scrub with real-time thumbnail preview.

### 3. Aura Music Experience & Equalizer Suite
- **Library Browsing:** Dedicated Album, Artist, and Track hierarchy with 3-column art grid.
- **Floating Mini-Player Bar:** Persistent audio bar across all tabs with quick resume and play/pause controls.
- **Audio Precision:** Built-in multi-band equalizer, bass booster dial, surround simulation, and synchronized LRC lyric rendering powered by LRCLib.

### 4. Settings Architecture
- **Categorized Hub:** 10 dedicated preference categories organized into tactile cards with vibrant colored squircle badges:
  - 🟢 **Video Playback Engine:** Auto-resume, aspect ratio, picture-in-picture, background behavior.
  - 🔵 **Touch Gestures & Controls:** Swipe sensitivity, double-tap seek window, horizontal scrub.
  - 🟠 **Player HUD & Button Layout:** Customizable button placement for portrait and landscape.
  - 🟡 **Audio & Music Engine:** Volume normalization, pitch shift, night mode compressor.
  - 🌐 **Subtitles & Captions:** Custom fonts, ASS/SSA styling, encoding, auto-download.
  - 🟣 **Appearance & Theming:** Curated color palettes, AMOLED pure black, typography.
  - 🔴 **Folders & Exclusions:** Scanned directories, `.nomedia` support, storage filters.
  - 🟦 **Hardware Decoder & Filters:** GPU MediaCodec decoding, debanding, deinterlacing presets.
  - 🔴 **Private Vault & Security:** PIN lock, encrypted local media safe, hidden vault.
  - 🔷 **Advanced Parameters & Diagnostics:** Custom buffer sizes, player bridge properties, device diagnostics.

---

## 🔒 Privacy, Security & Google Play Store Compliance

Aura Player is strictly engineered to safeguard user privacy and comply with Google Play Console policies:

- **Application ID:** `com.cmstudio.auraplayer`
- **Scoped Storage Architecture:** The `playstore` flavor strictly adheres to Android 14/15/16 Scoped Storage permissions (`READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_IMAGES`).
- **No Sensitive File Access in Play Store Builds:** `MANAGE_EXTERNAL_STORAGE` is strictly excluded from `playstore` build manifests.
- **100% Offline Core:** Core playback, audio engine, local metadata indexing, and thumbnail caches operate completely offline with no network requirement.
- **Zero Third-Party Trackers:** No Google Firebase Analytics, no Facebook SDK, no advertising libraries, and no behavioral profiling.

---

## 📊 Architecture & Tech Stack

| Layer | Technologies |
| :--- | :--- |
| **Language & Framework** | Kotlin 2.0+, Jetpack Compose, Kotlin Coroutines, StateFlow |
| **Design System** | Custom Aura Design Tokens (Obsidian `#060709`, Carbon `#14171F`, Cyan `#00E5FF`) |
| **Playback Pipelines** | Google AndroidX Media3 / ExoPlayer 1.5+ with MediaCodec HW+ Acceleration |
| **Dependency Injection** | Koin for Android & Compose |
| **Local Persistence** | AndroidX Room Database, Datastore Preferences |
| **Metadata & Subtitles** | MediaInfoLib Native, libass, LRCLib API |
| **Image & Artwork** | Coil Compose with in-memory LRU thumbnail caching |

---

## 🛠️ Building & Testing

### Prerequisites
- Android Studio Ladybug (2024.2+) or IntelliJ IDEA with Android SDK API 35+
- JDK 17 or JDK 21
- Android NDK (r26b or later for native modules)

### Build Variants & Gradle Tasks
```bash
# 1. Compile Kotlin sources
./gradlew compileStandardDebugKotlin

# 2. Run unit tests
./gradlew testStandardDebugUnitTest

# 3. Assemble Universal Standard Debug APK
./gradlew assembleStandardDebug

# 4. Assemble Release APK
./gradlew assembleStandardRelease

# 5. Build Google Play Store Bundle (AAB)
./gradlew bundlePlaystoreRelease
```

---

## 📜 Attribution & Open Source Licenses

Aura Player is distributed under the **Apache License 2.0**. For comprehensive third-party copyright notices and dependency licenses, please review the [NOTICE](NOTICE) file.

We gratefully acknowledge the pioneering work of open-source projects that make modern mobile media playback possible:
- [mpv-android](https://github.com/mpv-android/mpv-android) & [mpvEx](https://github.com/marlboro-advance/mpvEx)
- [AndroidX Media3 / ExoPlayer](https://github.com/androidx/media)
- [MediaInfoLib](https://mediaarea.net/MediaInfo)
- [Next Player](https://github.com/anilbeesetti/nextplayer)
- [Gramophone](https://github.com/FoedusProgramme/Gramophone)
- [Material Design 3](https://m3.material.io)

---

<div align="center">
<b>Aura Player</b> — <i>Cinema-grade playback in the palm of your hand.</i>
</div>

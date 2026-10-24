# Privacy Policy for Aura Player

**Effective Date:** October 24, 2026  
**Application Name:** Aura Player  
**Package Name:** `com.cmstudio.auraplayer`  
**License:** Apache License 2.0  

---

## 🔒 Summary (TL;DR)

- **Zero Data Collection:** We do not collect, transmit, store, sell, or share any personal information.
- **No Ads & No Analytics:** The application contains zero advertising SDKs, zero analytics tracking (no Google/Firebase Analytics), and zero telemetry.
- **Local Processing:** All media discovery, playback history, thumbnails, and settings remain 100% on your device.
- **Offline First:** Aura Player is an offline-first multimedia player with client-side processing.

---

## 1. Introduction

Aura Player ("the Application", "we", "us", or "our") is a multimedia player for Android built upon modern native Android media playback engines. We are committed to maintaining the highest privacy standards. This Privacy Policy describes how Aura Player handles information on Android devices.

Because Aura Player operates strictly as a local, client-side media player utility, we do not operate user accounts, remote user databases, or data tracking services.

---

## 2. Information We Do NOT Collect

We do not collect or monitor any personal or device data:

- **Personal Information:** No names, email addresses, phone numbers, or physical addresses.
- **Device Identifiers:** No IMEI, Android ID, MAC address, advertising ID, or hardware fingerprints.
- **Usage Telemetry:** No analytics libraries, event trackers, or usage heatmaps.
- **Media Content:** Local media files (videos, songs, photos) are never uploaded to our servers. If and only if you explicitly configure and initiate the optional AI Subtitle Translation / Transcription feature, only the designated subtitle text or audio excerpt is sent directly to your selected third-party AI provider (Groq, OpenAI, or Anthropic) using your own API key.

---

## 3. Android Permissions & Usage

To function properly as a media player, Aura Player requests the following Android permissions. Each permission is used strictly for on-device functionality:

| Permission | Category | Purpose |
| :--- | :--- | :--- |
| `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Storage | Allows the app to scan, browse, display thumbnails, read media tags (via MediaInfo), and play local audio/video files. |
| `MANAGE_EXTERNAL_STORAGE` *(Standard standalone flavor only)* | All Files Access | Enables advanced file management (move, rename, delete files, subtitle loading across arbitrary directories, USB OTG drives). *Strictly removed from Google Play Store builds.* |
| `INTERNET` | Network | Required exclusively to play user-requested network streams (via "Stream Link", HTTP/HLS/DASH/RTSP), fetch synchronized lyrics (LRCLib), play online radio (RadioBrowser), and communicate with AI APIs if configured. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Background Playback | Allows uninterrupted audio and video playback when navigating outside the app, using Picture-in-Picture (PiP), or turning off the screen. |
| `POST_NOTIFICATIONS` | Notifications | Displays ongoing playback controls (play, pause, seek, track info) on Android 13+. |

You may grant or revoke permissions at any time via Android System Settings (**Settings > Apps > Aura Player > Permissions**).

---

## 4. Local Data Storage & User Data Control

All app-related data is stored locally on your device:

- **Playback History & Positions:** Saved in a local SQLite/Room database on your device to let you resume playback where you left off.
- **App Preferences & Vault:** Playback speed, gestures, subtitle preferences, and player settings are saved in Android DataStore/SharedPreferences. Files placed in the Private Vault are moved to the app's isolated private sandbox directory (`.vault`) guarded by an in-app PIN code and hidden from device galleries with a `.nomedia` marker.
- **Data Deletion:** You can clear your playback history at any time in the app settings, or clear all application data via Android System Settings (**Settings > Apps > Aura Player > Storage > Clear Data**). Uninstalling the app permanently deletes all local data.

---

## 5. Network Streaming, Online Services & AI Providers

- **Network Streams:** Direct connections are established from your device to the specified stream URL (HTTP/HTTPS/HLS/RTSP/RTMP).
- **Online Radio & Lyrics:** Stations are fetched from public community directories (RadioBrowser, LRCLib) directly from your device.
- **Optional AI Subtitles & Transcription:** If you choose to configure an AI provider for subtitle translation or speech-to-text, data is transmitted directly from your device to the chosen provider's official API endpoint (Groq, OpenAI, or Anthropic) utilizing your personal API key. We do not operate any intermediate proxy servers and do not collect, view, or log these requests. Your API keys are kept strictly within private local app storage.
- **No Third-Party Analytics or Ads:** No data is shared with data brokers, advertisers, or analytics platforms.

---

## 6. Crash Reporting

Aura Player provides an on-device crash dialog (`CrashActivity`) for diagnosing unexpected crashes locally. Crash logs are **never** transmitted automatically.

---

## 7. Children's Privacy

Aura Player does not collect personal information from any user, including children under the age of 13. It complies with COPPA and GDPR regulations.

---

## 8. License

Aura Player is distributed under the **Apache License 2.0**.

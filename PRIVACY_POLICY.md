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
- **Media Content:** Local media files (videos, songs, photos) are never collected or uploaded to any server operated by Aura Player.

---

## 3. Android Permissions & Usage

To function properly as a media player, Aura Player requests the following Android permissions. Each permission is used strictly for on-device functionality:

| Permission | Category | Purpose |
| :--- | :--- | :--- |
| `READ_MEDIA_VIDEO` / `READ_MEDIA_AUDIO` / `READ_EXTERNAL_STORAGE` | Storage | Allows the app to scan, browse, display thumbnails, read media tags (via MediaInfo), and play local audio/video files. `READ_EXTERNAL_STORAGE` is strictly limited to Android 12 and below (`maxSdkVersion="32"`). |
| `MANAGE_EXTERNAL_STORAGE` *(Standard standalone flavor only)* | All Files Access | Enables advanced file management (move, rename, delete files, subtitle loading across arbitrary directories, USB OTG drives). *Strictly removed from Google Play Store builds.* |
| `INTERNET` | Network | Required exclusively to play user-requested network streams (via "Stream Link", HTTP/HLS/DASH/RTSP), fetch synchronized lyrics (LRCLib), play online radio (RadioBrowser), and communicate with AI APIs if explicitly configured by the user. |
| `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Background Playback | Allows uninterrupted audio and video playback when navigating outside the app, using Picture-in-Picture (PiP), or turning off the screen. |
| `POST_NOTIFICATIONS` | Notifications | Displays ongoing playback controls (play, pause, seek, track info) on Android 13+. |

You may grant or revoke permissions at any time via Android System Settings (**Settings > Apps > Aura Player > Permissions**).

---

## 4. Local Data Storage, Private Vault & Security

All app-related data is stored locally on your device:

- **Playback History & Positions:** Saved in a local SQLite/Room database on your device to let you resume playback where you left off.
- **Private Vault:** Videos moved to the Private Vault are copied into the app's isolated private sandbox directory (`files/.vault`) with a `.nomedia` marker to prevent indexing by public gallery apps. Vault access is protected by an in-app PIN verified using cryptographic PBKDF2-HMAC-SHA256 hashing with random salts and attempt throttling (rate limiting) to prevent brute-force attacks. Media files themselves reside in this application-private filesystem sandbox rather than encrypted on a per-file cryptographic level.
- **API Key Storage:** User-provided third-party AI API keys are encrypted at rest using the **Android Keystore system** (AES-256-GCM). Plaintext keys are never written to unencrypted storage or logs.
- **Data Deletion:** You can clear your playback history at any time in the app settings, or clear all application data via Android System Settings (**Settings > Apps > Aura Player > Storage > Clear Data**). Uninstalling the app permanently deletes all local data.

---

## 5. Network Streaming, Online Services & Optional AI Processing

### Network Streaming & Online Directories
- **Network Streams:** Direct connections are established from your device to the specified stream URL (HTTP/HTTPS/HLS/RTSP/RTMP).
- **Online Radio & Lyrics:** Stations are fetched from public community directories (RadioBrowser, LRCLib) directly from your device.

### Optional AI Subtitles & Transcription
Aura Player includes an optional AI-assisted subtitle translation and speech-to-text transcription feature. This feature is completely inactive by default and requires explicit configuration and initiation by the user:

1. **User Initiation:** Data is only transmitted when the user explicitly clicks the "Translate" or "Whisper Sub" action button in the AI Subtitle dialog.
2. **Data Transmitted for Subtitle Translation:** The text cues and dialogue lines of the selected subtitle file (`.srt`) are sent to the user-chosen provider's chat completion endpoint.
3. **Data Transmitted for Speech-to-Text Transcription:** The complete audio file (or audio extracted from the video) is uploaded directly to the user-chosen provider's transcription endpoint.
4. **Supported Third-Party Providers & Endpoints:**
   - **Groq Inc.:**
     - Translation: `https://api.groq.com/openai/v1/chat/completions`
     - Transcription: `https://api.groq.com/openai/v1/audio/transcriptions`
     - Governed by [Groq Privacy Policy](https://groq.com/privacy-policy/)
   - **OpenAI LLC:**
     - Translation: `https://api.openai.com/v1/chat/completions`
     - Transcription: `https://api.openai.com/v1/audio/transcriptions`
     - Governed by [OpenAI Privacy Policy](https://openai.com/policies/privacy-policy/)
   - **Anthropic PBC:**
     - Translation: `https://api.anthropic.com/v1/messages`
     - Governed by [Anthropic Privacy Policy](https://www.anthropic.com/legal/privacy)
5. **No Intermediate Servers:** Requests are made directly from your device to the official API endpoints using your personal API key. Aura Player developers do not operate any intermediary proxy servers and never intercept, inspect, or log your API keys, subtitle text, or audio recordings.
6. **Third-Party Processing:** Third-party AI providers process submitted data according to their own terms and privacy policies.

---

## 6. Crash Reporting

Aura Player provides an on-device crash dialog (`CrashActivity`) for diagnosing unexpected crashes locally. Crash logs are **never** transmitted automatically. If you choose to share crash logs, you do so manually using Android's standard share sheet.

---

## 7. Children's Privacy

Aura Player does not collect personal information from any user, including children under the age of 13. It complies with COPPA and GDPR regulations.

---

## 8. License

Aura Player is distributed under the **Apache License 2.0**.

# Phase 2 — License and Provenance Issues Report

**Project:** Apna Player  
**Current Version:** 1.3.1  
**Audit Date:** October 24, 2026  

---

## 1. Executive Legal Disclaimer & Provenance Notice

> [!WARNING]
> **Renaming packages, classes, and symbols (`is.xyz.mpv` → `com.cmstudio.apnaplayer.engine`, `MPVLib` → `PlayerBridge`) does NOT extinguish upstream copyright, licensing obligations, or legal provenance.**
> 
> The project cannot claim to be legally independent solely by virtue of a technical refactor. Any code derived from or inspired by upstream projects (including `mpv-android`, `mpvEx`, or other open-source media player frontends) continues to carry its original licensing requirements.

---

## 2. Dependency & Component License Audit

### 2.1 Google AndroidX Media3 (ExoPlayer)
- **Resolved Version:** Media3 1.5.0
- **License:** Apache License 2.0
- **Obligations:**
  - Preserve Apache 2.0 notice in the app's About / Open Source Licenses screen.
  - Retain Google AndroidX copyright notices.
- **Status:** Fully compliant. Permissive commercial and proprietary use permitted.

### 2.2 MediaInfo (`mediainfo-android` / `net.mediaarea.mediainfo.lib`)
- **Packaged Native Binaries:**
  - `lib/arm64-v8a/libmediainfo.so`
  - `lib/arm64-v8a/libzen.so`
  - (and corresponding armeabi-v7a, x86, x86_64 splits)
- **License:** GNU Lesser General Public License version 2.1 (LGPL v2.1) or BSD 2-Clause (ZenLib)
- **Obligations:**
  - Because `libmediainfo.so` and `libzen.so` are dynamically loaded shared objects (`.so`), LGPL v2.1 permits linking with proprietary or differently-licensed applications, provided that:
    1. The LGPL license text is displayed in the app's third-party notices.
    2. Any modifications made to the MediaInfo library itself are made available under the LGPL v2.1.
    3. Users are permitted to re-link or replace the LGPL library if desired.
- **Status:** Needs prominent disclosure in the app's Open Source Licenses section.

### 2.3 Upstream `mpv-android` / `mpvEx` UI & Bridge Code
- **Original Package:** `is.xyz.mpv` / `app.marlboroadvance.mpvex`
- **Original Upstream Licenses:**
  - `mpv-android`: GPL v3 / LGPL v2.1
  - `mpvEx` (fork): GPL v3 or Apache 2.0 depending on upstream fork branch
- **Refactoring Status:**
  - The native `libmpv` binary is **not packaged** in Apna Player (playback uses Media3 ExoPlayer).
  - However, the Kotlin state-machine bridge (`PlayerBridge`, formerly `MPVLib`), UI gesture logic, and preferences flow trace their design lineage back to `mpv-android` and `mpvEx`.
- **Legal Safeguards:**
  - **DO NOT** claim full copyright ownership over upstream-derived code structures.
  - **DO NOT** replace upstream copyright headers with generic Apache-2.0 headers without independent rewrite or author authorization.
  - **MUST** retain attribution and copyright notices for original authors in the app's `About` / `NOTICE` files.

### 2.4 FSAF (Fuck Storage Access Framework) Dependency
- **Audit Outcome:** Completed in Phase 1.1.
- **Resolution:** Completely removed from `gradle/libs.versions.toml`, `app/build.gradle.kts`, and all Kotlin source files.
- **Current Packaged State:** 100% absent from the resolved dependency tree and release APKs.
- **Licensing Impact:** Completely eliminated; no residual FSAF license obligations.

### 2.5 NanoHTTPD (Local Streaming Server)
- **License:** BSD 3-Clause License
- **Obligations:** Retain copyright notice and disclaimer in binary distributions.
- **Status:** Compliant.

---

## 3. Required Action Items for Full Compliance

1. **Maintain Third-Party License Screen:**
   Ensure the `AboutScreen.kt` and `fastlane` metadata clearly list all open-source libraries used (Media3, MediaInfo, ZenLib, NanoHTTPD, Koin, Compose, Kotlin Coroutines).
2. **Clarify Provenance in NOTICE File:**
   Ensure the project's root `NOTICE` file acknowledges that while Apna Player uses Google Media3 ExoPlayer for playback, its UI layer was originally built upon open-source media player foundations developed by the `mpv-android` and `mpvEx` communities.
3. **Avoid Misleading Marketing:**
   User-facing marketing and Play Store descriptions must not falsely claim "100% proprietary in-house developed from scratch" to avoid copyright disputes.

# Phase 2 — Remaining References and Preserved Contracts

**Project:** Apna Player  
**Current Version:** 1.3.1  
**Audit Date:** October 24, 2026  

---

## 1. Classification of Remaining References

Following the Phase 2 audit and refactor, all occurrences of `is.xyz.mpv` have been completely removed from source code, unit tests, and ProGuard configuration. Any remaining occurrences of "mpv" or "mpvex" in the codebase belong strictly to the following essential categories:

### Category A: Persisted User Data & Storage Keys (Mandatory to Preserve)
To ensure that existing app updates do not wipe user settings, playback history, or bookmarks, the following keys have been deliberately preserved:
1. **`"mpv_profile"` Preference Key:**
   - **Location:** `app/src/main/java/app/marlboroadvance/mpvex/preferences/DecoderPreferences.kt`
   - **Reason:** Stored in Android `SharedPreferences`/`DataStore`. Renaming this key would reset the user's hardware decoding and profile choices to default upon upgrading.
2. **`"mpvex.db"` Database Name:**
   - **Location:** Room database initializers and configuration.
   - **Reason:** Contains user playback positions, history, and bookmarks. Renaming the SQLite database file would orphan or wipe user data.
3. **Internal Event Strings (`"user-data/mpvex/..."`):**
   - **Location:** `ApnaPlayerView.kt`, `ExoPlayerHolder.kt`
   - **Properties:**
     - `"user-data/mpvex/show_text"`
     - `"user-data/mpvex/toggle_ui"`
     - `"user-data/mpvex/show_panel"`
     - `"user-data/mpvex/set_button_title"`
     - `"user-data/mpvex/reset_button_title"`
     - `"user-data/mpvex/toggle_button"`
     - `"user-data/mpvex/seek_by"`
     - `"user-data/mpvex/seek_to"`
     - `"user-data/mpvex/seek_by_with_text"`
     - `"user-data/mpvex/seek_to_with_text"`
     - `"user-data/mpvex/software_keyboard"`
   - **Reason:** These are in-memory event channels consumed by the controls overlay. They never touch disk or external APIs, but renaming them without simultaneous UI rewrite carries high risk of regression.

---

### Category B: Backwards-Compatibility Typealiases
To prevent any external or future calling code from failing compilation if legacy classes are referenced:
1. **`typealias MPVView = ApnaPlayerView`**
   - **Location:** `app/src/main/java/app/marlboroadvance/mpvex/ui/player/ApnaPlayerView.kt`
   - **Reason:** Provides drop-in type compatibility for any legacy references.
2. **`typealias MPVPipHelper = PipHelper`**
   - **Location:** `app/src/main/java/app/marlboroadvance/mpvex/ui/player/PipHelper.kt`
   - **Reason:** Provides drop-in type compatibility.

---

### Category C: Filter Tag Identifiers
1. **`"@mpvex_hflip"`, `"@mpvex_vflip"`**
   - **Location:** `ExoPlayerHolder.kt`, `PlayerBridgeTest.kt`
   - **Reason:** Internal string tags used to identify active video transform filters in `command("vf", "add", ...)` and `command("vf", "remove", ...)`.

---

### Category D: Application Package Hierarchy (`app.marlboroadvance.mpvex`)
- **Location:** `app/src/main/java/app/marlboroadvance/mpvex/`
- **Reason:** This is the application layer source tree (activities, fragments, viewmodels, UI components), separate from the `is.xyz.mpv` engine package. Moving the entire app package will be performed in a dedicated app namespace migration phase to prevent merge conflicts with ongoing UI development.

---

### Category E: Play Store Application ID (`com.cmstudio.auraplayer`)
- **Location:** `app/build.gradle.kts`, `app/src/playstore/AndroidManifest.xml`
- **Reason:** Strictly preserved as required. Changing this value would result in publishing an entirely new app on Google Play rather than updating the existing user base.

---

## 2. Summary Audit Table

| Residual Identifier | File / Component | Category | Risk if Renamed | Recommendation |
|---|---|---|---|---|
| `is.xyz.mpv` | None | **Eliminated** | N/A | Fully replaced by `com.cmstudio.apnaplayer.engine` |
| `MPVLib` | None | **Eliminated** | N/A | Fully replaced by `PlayerBridge` |
| `BaseMPVView` | None | **Eliminated** | N/A | Fully replaced by `BasePlayerView` |
| `MPVNode` | None | **Eliminated** | N/A | Fully replaced by `PlayerNode` |
| `MPVProfile` | None | **Eliminated** | N/A | Fully replaced by `PlayerProfile` |
| `mpvProperty` | None | **Eliminated** | N/A | Fully replaced by `engineProperty` |
| `MPVView` | `ApnaPlayerView.kt` | Typealias | Zero (alias) | Keep alias for compatibility |
| `MPVPipHelper` | `PipHelper.kt` | Typealias | Zero (alias) | Keep alias for compatibility |
| `"mpv_profile"` | `DecoderPreferences.kt` | Persisted Key | **High** (Data loss) | **Preserve** |
| `"mpvex.db"` | Room Database | Persisted Key | **Critical** (Data loss) | **Preserve** |
| `"user-data/mpvex/*"` | Player / UI Event Bus | Internal bus | Medium | Plan for future UI cleanup |

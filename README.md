# Still — Minimal Launcher

> A minimalist Android home-screen replacement designed to eliminate unnecessary stimulation and make the user choose what they intend to do before opening distracting applications.

[![Android Build](https://github.com/DALI951/still-launcher/actions/workflows/android-release.yml/badge.svg)](https://github.com/DALI951/still-launcher/actions)
[![Latest Release](https://img.shields.io/github/v/release/DALI951/still-launcher?color=4E8CFF&label=Release)](https://github.com/DALI951/still-launcher/releases)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

---

## Philosophy & Core Capabilities

Still is **not** a Niagara Launcher clone, an elaborate widget platform, a news feed, or a collection of unrelated productivity utilities. It is a purpose-built, distraction-resistant launcher centered on five capabilities:

1. **Genuinely Minimalist Android Home Screen**:
   - **Top Section**: Large digital clock (12h/24h), current date with day of week, and optional battery percentage.
   - **Middle Section**: User-selected favorite applications displayed as clean, high-contrast typography by default (Minimalist Phone style) or optional simple monochrome icons. Clear, comfortable spacing without decorative cards or colorful containers.
   - **Bottom Section**: One discoverable search pill to access installed apps. No navigation bar, no feeds, and no algorithmic recommendations.
   - **True-Black OLED Design**: Pitch-black (`#000000`) by default to eliminate visual fatigue and save battery, with Dark and Light themes available.

2. **Fast, Deliberate Access to Installed Applications**:
   - Tap the bottom search pill to instantly open the search screen with the keyboard ready.
   - Live real-time filtering as you type.
   - Clean alphabetical list fallback when the search query is empty.
   - Zero gesture confusion, no alphabetical sidebars, and no gestures that interfere with Android system back navigation.

3. **Strict Built-in YouTube Link Gate (Single App Architecture)**:
   - Built directly into Still (not a companion app, separate APK, or external service).
   - Tapping YouTube from the launcher prompts: **"What do you want to watch?"**
   - Validates legitimate YouTube video URLs (`youtube.com/watch?v=ID`, `youtu.be/ID`, `youtube.com/shorts/ID`, `youtube.com/embed/ID`) and preserves timestamps (`t=120s`).
   - Plays video directly in Still's dedicated distraction-free WebView player (`youtube-nocookie.com`).
   - **Anti-Rabbit Hole**: Strictly blocks external navigation to YouTube home feeds, trending tabs, search pages, or Shorts shelves.
   - **Anti-Autoplay**: Detects video completion via JavaScript Player API bridge and immediately returns the viewer to Still's intentional viewing screen.
   - **Local History**: Optional list of watched links stored strictly on-device (can clear individual items or clear all; never used for recommendations).

4. **Honest Anti-Distraction Controls & Focus Profiles**:
   - **Per-Application Launch Policies**:
     - **Normal**: Opens immediately with zero delay.
     - **Delayed**: Mindful breathing pause (configurable 3s, 5s, 10s, 15s) with a calm pulsating visual guide before launching.
     - **Scheduled**: Restricted during configured focus windows.
     - **Blocked**: Restricted from opening while active.
   - **Focus Profiles**:
     - **Default**: Standard intentional launcher mode.
     - **Study Mode**: Restricts distracting applications during study hours.
     - **Sleep Mode**: Enforces calm bedtime hours (supports schedules crossing midnight, e.g. 22:00 to 07:00).
     - **Custom**: User-defined schedule and blocked application list.
   - Essential utilities (Phone, SMS, Clock, Settings) remain reliably accessible.

5. **Integrated Application Updater & Maintenance**:
   - Built-in GitHub Releases updater targeting `DALI951/still-launcher`.
   - Checks release metadata, displays changelogs, downloads release APKs with integrity checks, and hands off to Android's native package installer.
   - In-place upgrade compatibility: preserves `applicationId = "com.niagaraclone.launcher"` and keystore signature so existing installations upgrade smoothly without data loss.

---

## Project Structure

```text
still-launcher/
├── app/
│   ├── build.gradle.kts
│   ├── keystore/
│   │   └── niagara.jks              # Preserved release signing keystore
│   └── src/main/
│       ├── AndroidManifest.xml      # Home launcher intent filters & permissions
│       ├── res/
│       │   ├── values/strings.xml   # Still branding & UI strings
│       │   └── values/themes.xml
│       └── java/com/niagaraclone/launcher/
│           ├── MainActivity.kt      # Edge-to-edge transparent system activity
│           ├── data/
│           │   ├── AppRepository.kt           # Package manager & app launcher
│           │   ├── AppUpdateRepository.kt     # GitHub Releases updater
│           │   ├── PermissionsManager.kt      # RoleManager & Android permissions
│           │   ├── PreferencesRepository.kt   # Jetpack DataStore preferences
│           │   ├── SystemStatusProvider.kt    # Clock, date, battery monitor
│           │   └── model/
│           │       ├── AppInfo.kt             # Application model & metadata
│           │       ├── LauncherSettings.kt    # Themes, styles, focus profiles
│           │       └── SystemStatus.kt        # Glanceable status state
│           └── ui/
│               ├── LauncherViewModel.kt       # Central reactive state holder
│               ├── components/
│               │   ├── AppContextMenuSheet.kt     # Quick favorite/rule menu
│               │   ├── AppLaunchFrictionScreen.kt # Mindful breathing pause & focus block
│               │   ├── AppRowItem.kt              # Minimalist typography row item
│               │   ├── AppSearchOverlay.kt        # Fast search screen
│               │   ├── AtAGlanceHeader.kt         # Clock, date & battery header
│               │   ├── LauncherHomeScreen.kt      # Purpose-built home screen
│               │   ├── LauncherSettingsScreen.kt  # Clean 7-section settings
│               │   └── YouTubeGateScreen.kt       # Built-in YouTube Link Gate
│               └── theme/
│                   ├── Color.kt
│                   ├── Theme.kt               # StillLauncherTheme
│                   └── Type.kt
└── .github/
    └── workflows/
        └── android-release.yml      # CI/CD pipeline building Still-v1.0.X.apk
```

---

## Settings Overview

The settings interface is organized into seven clear sections:

| Section | Capabilities |
| :--- | :--- |
| **Appearance** | True Black / Dark / Light themes, text size, 12h/24h format, date toggle, battery toggle, monochrome icons vs text-only mode, accent colors. |
| **Favorites & Apps** | Reorder favorites (move up/down), remove favorites, add favorites, and configure per-application launch rules (Normal, Delayed, Scheduled, Blocked). |
| **Focus** | Switch active profile (Default, Study, Sleep, Custom), set breathing pause duration (3s, 5s, 10s, 15s), set study and sleep schedule hours (supports midnight crossing). |
| **YouTube** | Enable/disable built-in Intentional Gate (with explicit confirmation warning), toggle local playback history, clear history, and test playback. |
| **Updates** | Check for updates from GitHub Releases, view release notes, download APKs, and launch the Android package installer. |
| **Privacy & Permissions** | Manage Default Home App status, Usage Stats permission, Notification permission, and review Still's 100% offline privacy pledge. |
| **About** | Application name, version, repository link (`DALI951/still-launcher`), and build diagnostics. |

---

## Building & Installation

### Prerequisites
- **Android Studio**: Koala / Ladybug / Jellyfish or newer
- **JDK**: Java 17
- **Android SDK**: Min SDK 26 (Android 8.0), Target SDK 34 (Android 14)

### Command-Line Build

```bash
# Debug build (generates app-debug.apk)
./gradlew assembleDebug

# Signed release build (generates app-release.apk using niagara.jks)
./gradlew assembleRelease
```

---

## Privacy & Security

- **Zero Telemetry**: Still contains no analytics SDKs, advertising trackers, or remote logging.
- **Offline First**: All user settings, application launch rules, and YouTube playback history are stored strictly on-device in Jetpack DataStore.
- **Controlled Network Use**: Network calls only occur when you explicitly tap "Check for Updates" or enter a YouTube video URL into the intentional player.

---

## License

This project is licensed under the Apache License, Version 2.0.

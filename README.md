# Niagara Launcher Clone (Android + Jetpack Compose)

A high-performance, minimalist Android launcher inspired by **Niagara Launcher**, crafted natively with **Kotlin**, **Jetpack Compose (Material 3)**, and modern Android architecture.

---

## Features

- **Wallpaper-Transparent UI:** Integrates with the Android Wallpaper Engine (`windowShowWallpaper="true"`) with a subtle gradient scrim for legibility over any wallpaper.
- **Niagara Wave Scroller:** Signature vertical alphabet scroller with raised-cosine mathematical wave distortion:
  $$\Delta x_i = -A_{\max} \cdot \frac{1 + \cos(\pi \cdot d_i)}{2} \cdot \text{waveProgress}$$
  - Dynamic vertical spreading: $\Delta y_i = \text{sign}(y_i - y_{\text{touch}}) \cdot S_{\max} \cdot \sin(\pi \cdot (1 - d_i)) \cdot \text{waveProgress}$
  - Haptic feedback tick (`HapticFeedbackConstants.CLOCK_TICK`) when scrubbing across letters.
  - Floating letter preview bubble.
- **At-a-Glance Header:** Minimalist light-weight clock, date, live battery percentage, charging state indicator, next alarm clock pill, and calendar shortcuts.
- **Favorites & Initial Seeding:** Pinned favorites section on top of the home list. On first launch, automatically seeds up to 6 essential apps (Phone, Messages, Browser, Camera, Gallery, Settings).
- **Instant App Search:** Search overlay with prefix-match ranking, IME keyboard search launch action, and real-time filtering.
- **Context Action Sheet:** Long-pressing any app displays a bottom sheet to:
  - Pin to / Remove from Favorites
  - Reorder Favorites (Move Up / Down)
  - Hide / Unhide App
  - Open System App Info (`LauncherApps.startAppDetailsActivity`)
  - Request App Uninstallation (`ACTION_DELETE`)
- **Manage Apps Sheet:** Dedicated tabbed management sheet for Favorites ordering and Hidden Apps unhiding.
- **Package Callback Support:** Uses `LauncherApps.Callback` to automatically reflect app installs, updates, and uninstalls in real time.
- **Modern Jetpack Stack:**
  - Jetpack Compose (BOM 2024.06.00)
  - Lifecycle ViewModel Compose 2.8+
  - DataStore Preferences for favorites ordering & hidden app states
  - Kotlin Coroutines & `StateFlow`

---

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml
├── res/
│   ├── drawable/ic_launcher_foreground.xml
│   └── values/
│       ├── colors.xml
│       ├── strings.xml
│       └── themes.xml
└── java/com/niagaraclone/launcher/
    ├── MainActivity.kt
    ├── data/
    │   ├── AppRepository.kt
    │   ├── PreferencesRepository.kt
    │   ├── SystemStatusProvider.kt
    │   └── model/
    │       ├── AppInfo.kt
    │       └── SystemStatus.kt
    └── ui/
        ├── LauncherViewModel.kt
        ├── components/
        │   ├── AppContextMenuSheet.kt
        │   ├── AppRowItem.kt
        │   ├── AppSearchOverlay.kt
        │   ├── AtAGlanceHeader.kt
        │   ├── LauncherHomeScreen.kt
        │   ├── ManageAppsSheet.kt
        │   └── NiagaraWaveScroller.kt
        └── theme/
            ├── Color.kt
            ├── Theme.kt
            └── Type.kt
```

---

## Requirements & Building

- **Android Studio:** Hedgehog (2023.1.1) / Iguana / Jellyfish / Koala or newer
- **JDK:** 17
- **Min SDK:** 26 (Android 8.0)
- **Target / Compile SDK:** 34 (Android 14)

Open the project folder in Android Studio and run on an Android device or emulator.

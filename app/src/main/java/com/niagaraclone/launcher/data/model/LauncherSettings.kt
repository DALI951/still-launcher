package com.niagaraclone.launcher.data.model

enum class IconStyle(val label: String) {
    UNIFIED_THEMED("Unified Niagara Icons"),
    THEMED_MONOCHROME("Themed Monochrome"),
    THEMED_DUOTONE("Themed Duotone"),
    MINIMAL_DOTS("Niagara Dots"),
    ORIGINAL("Original Color")
}

enum class IconShape(val label: String) {
    CIRCLE("Circle"),
    SQUIRCLE("Squircle"),
    BARE("No Background")
}

enum class IconThemingMode(val label: String) {
    GENTLE_SMART("Gentle Smart"),
    SUBTLE_TONAL("Subtle Tonal"),
    FULL_MONOCHROME("Strict Monochrome"),
    ORIGINAL("Original Vibrant")
}

enum class ThemePreset(
    val label: String,
    val accentHex: Long,
    val accentLightHex: Long,
    val scrimBaseHex: Long
) {
    NIAGARA_BLUE("Ocean Blue", 0xFF4E8CFF, 0xFF99C2FF, 0xFF0A111F),
    AMOLED_PITCH("AMOLED Black", 0xFF90A4AE, 0xFFECEFF1, 0xFF000000),
    EMERALD_MINT("Emerald Mint", 0xFF2ECC71, 0xFF82E0AA, 0xFF071A10),
    ROYAL_VIOLET("Royal Violet", 0xFF9B59B6, 0xFFD2B4DE, 0xFF140A1C),
    SUNSET_AMBER("Sunset Amber", 0xFFF39C12, 0xFFFAD7A0, 0xFF1C1206),
    CRIMSON_ROSE("Crimson Rose", 0xFFE74C3C, 0xFFF5B7B1, 0xFF1C0909),
    FROST_CYAN("Frost Cyan", 0xFF00BCD4, 0xFF80DEEA, 0xFF06181C)
}

enum class ClockStyle(val label: String) {
    NIAGARA_CLEAN("Clean Sans"),
    ROUNDED_MINIMAL("Rounded"),
    MODERN_MONO("Monospace"),
    SERIF_ELEGANT("Serif Elegant"),
    THIN_AESTHETIC("Ultra Thin"),
    BOLD_COMPACT("Bold Compact"),
    STACKED_MINIMAL("Stacked Hour/Min"),
    CLEAN_CONDENSED("Tall Condensed")
}

enum class WaveVisibility(val label: String) {
    ALWAYS_VISIBLE("Always Visible"),
    TOUCH_ONLY("Hidden Until Touched"),
    SUBTLE_DOTS("Minimal Dots")
}

enum class WaveVerticalPosition(val label: String) {
    CENTER("Center"),
    TOP("Top"),
    BOTTOM("Bottom")
}

enum class WallpaperTheme(
    val label: String,
    val description: String,
    val topColorHex: Long,
    val bottomColorHex: Long,
    val accentHex: Long,
    val accentLightHex: Long,
    val assetFileName: String? = null,
    val liveVideoFileName: String? = null
) {
    SYSTEM_WALLPAPER("System Live Wallpaper", "Renders system device wallpaper", 0x00000000, 0x00000000, 0xFF4E8CFF, 0xFF99C2FF),
    BLACK_DESERT("Black Desert", "Curved black dunes with misty twilight gray sky", 0xFF1C1D21, 0xFF0A0A0C, 0xFFB0B3BA, 0xFFE0E2E6, "wallpaper_black_desert.jpg"),
    DUNE_TWILIGHT("Dune Twilight", "Golden copper sand dunes with dark obsidian shadows", 0xFF1A1614, 0xFF0A0807, 0xFFD49B6A, 0xFFF5C7A1, "wallpaper_dune_twilight.jpg"),
    NOCTURNE_MINIMAL("Nocturne Minimal", "Jagged mountain peaks with crescent moon and starlight", 0xFF121316, 0xFF050507, 0xFF8E929A, 0xFFD8DCE3, "wallpaper_nocturne_mountain.jpg"),
    NORDIC_SLATE("Nordic Slate", "Silhouetted pine forest in dense cold gray fog", 0xFF181D24, 0xFF0C0F13, 0xFF6C8DAE, 0xFFAEC4DC, "wallpaper_nordic_fog.jpg"),
    DEEP_SPACE("Deep Space", "Cosmic pitch-black void with live moving black hole accretion disk", 0xFF0D111A, 0xFF04060A, 0xFF3D6EB4, 0xFF8AB5F8, "wallpaper_deep_space.jpg", "live_black_hole.webm"),
    CYBERPUNK_DARK("Cyberpunk Dark", "Tokyo rain alley with electric cyan & neon violet", 0xFF0D1117, 0xFF050608, 0xFF00E5FF, 0xFF80F2FF, "wallpaper_cyberpunk.jpg"),
    EMERALD_NIGHT("Emerald Night", "Arctic midnight fjord with live flowing aurora borealis", 0xFF0F1A14, 0xFF060D09, 0xFF4E9B6E, 0xFF98D4B0, "wallpaper_aurora.jpg", "live_aurora.webm"),
    OBSIDIAN_ROSE("Obsidian Rose", "Sculpted graphite crystal with dusty mauve twilight", 0xFF1C1417, 0xFF0D080A, 0xFFB87082, 0xFFE2A8B6, "wallpaper_obsidian_rose.jpg"),
    ABYSSAL_GOLD("Abyssal Gold", "Obsidian black marble with champagne gold veins", 0xFF141414, 0xFF050505, 0xFFC5A866, 0xFFEAD5A0, "wallpaper_abyssal_gold.jpg"),
    SOLAR_ECLIPSE("Solar Eclipse", "Pitch-black moon disc with glowing silver corona", 0xFF121216, 0xFF060608, 0xFFC0C4CC, 0xFFF0F2F6, "wallpaper_solar_eclipse.jpg"),
    CUSTOM_PHOTO("Custom Photo / Smart Palette", "Extracted from chosen image", 0xFF000000, 0xFF000000, 0xFF4E8CFF, 0xFF99C2FF)
}

data class LauncherSettings(
    val iconSizeDp: Int = 26,             // range 18..42
    val iconStyle: IconStyle = IconStyle.UNIFIED_THEMED,
    val iconShape: IconShape = IconShape.BARE,
    val iconThemingMode: IconThemingMode = IconThemingMode.GENTLE_SMART,
    val themePreset: ThemePreset = ThemePreset.NIAGARA_BLUE,
    val edgePaddingDp: Int = 44,          // range 20..76
    val rowHeightDp: Int = 46,            // range 36..60
    val textSizeSp: Int = 15,             // range 12..20
    val favoritesTopOffsetDp: Int = 56,   // range 0..180
    val scrimOpacityPercent: Int = 55,    // range 0..95
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val showBatteryAndAlarm: Boolean = true,
    val waveOnLeftSide: Boolean = false,
    val clockStyle: ClockStyle = ClockStyle.NIAGARA_CLEAN,
    val clockFontSizeSp: Int = 54,        // range 32..72
    val waveVisibility: WaveVisibility = WaveVisibility.ALWAYS_VISIBLE,
    val waveHeightPercent: Int = 68,      // range 40..90
    val waveLetterSizeSp: Int = 10,       // range 8..14
    val waveVerticalPosition: WaveVerticalPosition = WaveVerticalPosition.CENTER,
    val wallpaperTheme: WallpaperTheme = WallpaperTheme.BLACK_DESERT,
    val customPhotoPath: String? = null,
    val customAccentHex: Long = 0xFFB0B3BA,
    val customAccentLightHex: Long = 0xFFE0E2E6,
    val swipeUpToSearch: Boolean = true,
    val liveWallpaperEnabled: Boolean = true
) {
    val activeAccentHex: Long
        get() = when (wallpaperTheme) {
            WallpaperTheme.SYSTEM_WALLPAPER -> themePreset.accentHex
            else -> customAccentHex
        }

    val activeAccentLightHex: Long
        get() = when (wallpaperTheme) {
            WallpaperTheme.SYSTEM_WALLPAPER -> themePreset.accentLightHex
            else -> customAccentLightHex
        }
}

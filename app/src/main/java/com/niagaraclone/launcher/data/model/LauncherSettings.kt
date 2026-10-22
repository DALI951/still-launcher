package com.niagaraclone.launcher.data.model

enum class IconStyle(val label: String) {
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
    val accentLightHex: Long
) {
    SYSTEM_WALLPAPER("System Live Wallpaper", "Renders system device wallpaper", 0x00000000, 0x00000000, 0xFF4E8CFF, 0xFF99C2FF),
    BLACK_DESERT("Black Desert", "Pitch black dunes with misty light gray twilight", 0xFF1C1D21, 0xFF0A0A0C, 0xFFB0B3BA, 0xFFE0E2E6),
    DUNE_TWILIGHT("Dune Twilight", "Warm obsidian dunes with copper twilight", 0xFF1A1614, 0xFF0A0807, 0xFFD49B6A, 0xFFF5C7A1),
    NOCTURNE_MINIMAL("Nocturne Minimal", "Onyx dark with subtle star-silver glow", 0xFF121316, 0xFF050507, 0xFF8E929A, 0xFFD8DCE3),
    NORDIC_SLATE("Nordic Slate", "Cool charcoal with serene mist blue", 0xFF181D24, 0xFF0C0F13, 0xFF6C8DAE, 0xFFAEC4DC),
    DEEP_SPACE("Deep Space", "Midnight black with deep sapphire glow", 0xFF0D111A, 0xFF04060A, 0xFF3D6EB4, 0xFF8AB5F8),
    ABYSSAL_GOLD("Abyssal Gold", "Pitch black with champagne gold luster", 0xFF141414, 0xFF050505, 0xFFC5A866, 0xFFEAD5A0),
    CYBERPUNK_DARK("Cyberpunk Dark", "Carbon night with electric cyan and magenta", 0xFF0D1117, 0xFF050608, 0xFF00E5FF, 0xFF80F2FF),
    OBSIDIAN_ROSE("Obsidian Rose", "Smoky graphite with muted dusty mauve", 0xFF1C1417, 0xFF0D080A, 0xFFB87082, 0xFFE2A8B6),
    EMERALD_NIGHT("Emerald Night", "Deep pine shadows with pale sage accents", 0xFF0F1A14, 0xFF060D09, 0xFF4E9B6E, 0xFF98D4B0),
    CUSTOM_PHOTO("Custom Photo / Smart Palette", "Extracted from chosen image", 0xFF000000, 0xFF000000, 0xFF4E8CFF, 0xFF99C2FF)
}

data class LauncherSettings(
    val iconSizeDp: Int = 26,             // range 18..42
    val iconStyle: IconStyle = IconStyle.THEMED_MONOCHROME,
    val iconShape: IconShape = IconShape.CIRCLE,
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
    val swipeUpToSearch: Boolean = true
) {
    val activeAccentHex: Long
        get() = when (wallpaperTheme) {
            WallpaperTheme.CUSTOM_PHOTO -> customAccentHex
            WallpaperTheme.SYSTEM_WALLPAPER -> themePreset.accentHex
            else -> wallpaperTheme.accentHex
        }

    val activeAccentLightHex: Long
        get() = when (wallpaperTheme) {
            WallpaperTheme.CUSTOM_PHOTO -> customAccentLightHex
            WallpaperTheme.SYSTEM_WALLPAPER -> themePreset.accentLightHex
            else -> wallpaperTheme.accentLightHex
        }
}

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
    val waveOnLeftSide: Boolean = false
)

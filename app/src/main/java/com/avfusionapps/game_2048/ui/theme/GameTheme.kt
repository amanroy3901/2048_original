package com.avfusionapps.game_2048.ui.theme

import androidx.compose.ui.graphics.Color

sealed class GameTheme(val name: String) {
    abstract val primaryColor: Color
    abstract val secondaryColor: Color
    abstract val backgroundColor: Color
    abstract val surfaceColor: Color
    abstract val tileColors: Map<Int, Color>
    abstract val textColor: Color
    abstract val accentColor: Color
    abstract val isDark: Boolean

    /**
     * Right-hand stop of the hero neon sweep (title, CTAs, card borders). Themes that
     * only have a two-tone identity leave this as the secondary, which collapses
     * [heroGradient] back to the original two stops.
     */
    open val gradientEndColor: Color get() = secondaryColor

    /** The signature sweep used by the wordmark, the primary CTAs and card borders. */
    val heroGradient: List<Color>
        get() = listOf(primaryColor, secondaryColor, gradientEndColor).distinct()

    object ElementalSprites : GameTheme("Elemental Sprites") {
        override val primaryColor = Color(0xFFB57BFF)
        override val secondaryColor = Color(0xFFFFC93A)
        override val gradientEndColor = Color(0xFF47C6FF)
        override val backgroundColor = Color(0xFF130A2E)
        override val surfaceColor = Color(0xFF22124F)
        override val textColor = Color(0xFFF6F0FF)
        override val accentColor = Color(0xFFFF9A1F)
        override val isDark = true

        // Card colours of each sprite (see ui/sprites/ElementalSprites.kt); 0 is the empty cell.
        override val tileColors = mapOf(
            0 to Color(0xFF1A0C42),
            2 to Color(0xFF2A86E8),
            4 to Color(0xFF3FA52C),
            8 to Color(0xFFF0A01E),
            16 to Color(0xFF3C8EDB),
            32 to Color(0xFF6B35D6),
            64 to Color(0xFFD9481C),
            128 to Color(0xFF3FA532),
            256 to Color(0xFF2F52D8),
            512 to Color(0xFFE58A16),
            1024 to Color(0xFF7E5BD8),
            2048 to Color(0xFFF2A01A)
        )
    }
}

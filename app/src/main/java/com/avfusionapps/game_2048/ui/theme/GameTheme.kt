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

    object NeonRush : GameTheme("Neon Rush") {
        override val primaryColor = Purple80
        override val secondaryColor = PurpleGrey80
        override val backgroundColor = PurpleDarkBackground
        override val surfaceColor = Purplefade
        override val textColor = Color.White
        override val accentColor = HighLighter
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF3E3E3E),
            2 to Color(0xFF6B6B6B),
            4 to Color(0xFF9E9E9E),
            8 to Color(0xFFFFA726),
            16 to Color(0xFFFF7043),
            32 to Color(0xFFFF5722),
            64 to Color(0xFFF44336),
            128 to Color(0xFFFFEB3B),
            256 to Color(0xFFFDD835),
            512 to Color(0xFFFBC02D),
            1024 to Color(0xFFF9A825),
            2048 to Color(0xFFF57F17)
        )
    }

    object Classic2048 : GameTheme("Classic 2048") {
        override val primaryColor = Color(0xFF8F7A66)
        override val secondaryColor = Color(0xFFBBADA0)
        override val backgroundColor = Color(0xFFFAF8EF)
        override val surfaceColor = Color(0xFFBBADA0)
        override val textColor = Color(0xFF776E65)
        override val accentColor = Color(0xFF8F7A66)
        override val isDark = false

        override val tileColors = mapOf(
            0 to Color(0xFFCDC1B4),
            2 to Color(0xFFEEE4DA),
            4 to Color(0xFFEDE0C8),
            8 to Color(0xFFF2B179),
            16 to Color(0xFFF59563),
            32 to Color(0xFFF67C5F),
            64 to Color(0xFFF65E3B),
            128 to Color(0xFFEDCF72),
            256 to Color(0xFFEDCC61),
            512 to Color(0xFFEDC850),
            1024 to Color(0xFFEDC53F),
            2048 to Color(0xFFEDC22E)
        )
    }

    companion object {
        fun fromName(name: String): GameTheme {
            return when (name) {
                "Classic 2048" -> Classic2048
                else -> NeonRush
            }
        }
    }
}

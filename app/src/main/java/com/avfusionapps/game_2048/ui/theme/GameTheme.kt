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
        override val primaryColor = Color(0xFFFF006E)
        override val secondaryColor = Color(0xFF8338EC)
        override val backgroundColor = Color(0xFF070B16)
        override val surfaceColor = Color(0xFF141C2F)
        override val textColor = Color.White
        override val accentColor = Color(0xFFFF4DA6)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF1B1B2F),
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

    object CyberBlue : GameTheme("Cyber Blue") {
        override val primaryColor = Color(0xFF00C2FF)
        override val secondaryColor = Color(0xFF4D7CFE)
        override val backgroundColor = Color(0xFF07111F)
        override val surfaceColor = Color(0xFF13233D)
        override val textColor = Color.White
        override val accentColor = Color(0xFF00E5FF)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF1A2333),
            2 to Color(0xFF1E88E5),
            4 to Color(0xFF29B6F6),
            8 to Color(0xFF42A5F5),
            16 to Color(0xFF5C6BC0),
            32 to Color(0xFF7E57C2),
            64 to Color(0xFF8E24AA),
            128 to Color(0xFF00BCD4),
            256 to Color(0xFF00ACC1),
            512 to Color(0xFF0097A7),
            1024 to Color(0xFF00E5FF),
            2048 to Color(0xFF64FFDA)
        )
    }

    object EmeraldMatrix : GameTheme("Emerald Matrix") {
        override val primaryColor = Color(0xFF00E676)
        override val secondaryColor = Color(0xFF00C853)
        override val backgroundColor = Color(0xFF07140B)
        override val surfaceColor = Color(0xFF122019)
        override val textColor = Color.White
        override val accentColor = Color(0xFF69F0AE)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF1B2A1F),
            2 to Color(0xFF43A047),
            4 to Color(0xFF4CAF50),
            8 to Color(0xFF66BB6A),
            16 to Color(0xFF81C784),
            32 to Color(0xFF00C853),
            64 to Color(0xFF00E676),
            128 to Color(0xFF69F0AE),
            256 to Color(0xFFB9F6CA),
            512 to Color(0xFF64DD17),
            1024 to Color(0xFF76FF03),
            2048 to Color(0xFFC6FF00)
        )
    }

    object RoyalPurple : GameTheme("Royal Purple") {
        override val primaryColor = Color(0xFF9C27B0)
        override val secondaryColor = Color(0xFF7B1FA2)
        override val backgroundColor = Color(0xFF12081A)
        override val surfaceColor = Color(0xFF21102D)
        override val textColor = Color.White
        override val accentColor = Color(0xFFD500F9)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF24192E),
            2 to Color(0xFF7E57C2),
            4 to Color(0xFF9575CD),
            8 to Color(0xFFAB47BC),
            16 to Color(0xFFBA68C8),
            32 to Color(0xFFCE93D8),
            64 to Color(0xFFD500F9),
            128 to Color(0xFFE040FB),
            256 to Color(0xFFEA80FC),
            512 to Color(0xFFB388FF),
            1024 to Color(0xFFFF80AB),
            2048 to Color(0xFFFFD54F)
        )
    }

    object SunsetOrange : GameTheme("Sunset Orange") {
        override val primaryColor = Color(0xFFFF7043)
        override val secondaryColor = Color(0xFFFFA726)
        override val backgroundColor = Color(0xFF1A0D08)
        override val surfaceColor = Color(0xFF2A1811)
        override val textColor = Color.White
        override val accentColor = Color(0xFFFFB74D)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF322019),
            2 to Color(0xFFFF8A65),
            4 to Color(0xFFFF7043),
            8 to Color(0xFFFF5722),
            16 to Color(0xFFFF9800),
            32 to Color(0xFFFFB74D),
            64 to Color(0xFFFFC107),
            128 to Color(0xFFFFD54F),
            256 to Color(0xFFFFE082),
            512 to Color(0xFFFFAB40),
            1024 to Color(0xFFFF6E40),
            2048 to Color(0xFFFFEA00)
        )
    }

    object MinimalWhite : GameTheme("Minimal White") {
        override val primaryColor = Color(0xFF37474F)
        override val secondaryColor = Color(0xFF90A4AE)
        override val backgroundColor = Color(0xFFF8FAFC)
        override val surfaceColor = Color(0xFFFFFFFF)
        override val textColor = Color(0xFF263238)
        override val accentColor = Color(0xFF546E7A)
        override val isDark = false

        override val tileColors = mapOf(
            0 to Color(0xFFECEFF1),
            2 to Color(0xFFFFFFFF),
            4 to Color(0xFFF5F5F5),
            8 to Color(0xFFE3F2FD),
            16 to Color(0xFFBBDEFB),
            32 to Color(0xFF90CAF9),
            64 to Color(0xFF64B5F6),
            128 to Color(0xFFFFF59D),
            256 to Color(0xFFFFEE58),
            512 to Color(0xFFFFE082),
            1024 to Color(0xFFFFCA28),
            2048 to Color(0xFFFFA000)
        )
    }

    object AmoledBlack : GameTheme("AMOLED Black") {
        override val primaryColor = Color.White
        override val secondaryColor = Color(0xFFBDBDBD)
        override val backgroundColor = Color(0xFF000000)
        override val surfaceColor = Color(0xFF111111)
        override val textColor = Color.White
        override val accentColor = Color.White
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF151515),
            2 to Color(0xFF2E2E2E),
            4 to Color(0xFF424242),
            8 to Color(0xFF616161),
            16 to Color(0xFF757575),
            32 to Color(0xFF9E9E9E),
            64 to Color(0xFFBDBDBD),
            128 to Color(0xFFE0E0E0),
            256 to Color(0xFFFFFFFF),
            512 to Color(0xFFFFD54F),
            1024 to Color(0xFFFFB300),
            2048 to Color(0xFFFF6F00)
        )
    }

    companion object {
        fun fromName(name: String): GameTheme {
            return when (name) {
                "Classic 2048" -> Classic2048
                "Cyber Blue" -> CyberBlue
                "Emerald Matrix" -> EmeraldMatrix
                "Royal Purple" -> RoyalPurple
                "Sunset Orange" -> SunsetOrange
                "Minimal White" -> MinimalWhite
                "AMOLED Black" -> AmoledBlack
                else -> NeonRush
            }
        }

        fun allThemes() = listOf(
            NeonRush,
            Classic2048,
            CyberBlue,
            EmeraldMatrix,
            RoyalPurple,
            SunsetOrange,
            MinimalWhite,
            AmoledBlack
        )
    }
}

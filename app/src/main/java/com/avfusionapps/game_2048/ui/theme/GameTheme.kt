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

    object NeonPink : GameTheme("Neon Pink") {
        override val primaryColor = Color(0xFFFF006E)
        override val secondaryColor = Color(0xFF8338EC)
        override val backgroundColor = Color(0xFF0C0714)
        override val surfaceColor = Color(0xFF181124)
        override val textColor = Color.White
        override val accentColor = Color(0xFFFF4DA6)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF1B112C),
            2 to Color(0xFF5A164B),
            4 to Color(0xFF8D185A),
            8 to Color(0xFFB11A65),
            16 to Color(0xFFE52565),
            32 to Color(0xFFF13D5F),
            64 to Color(0xFFF75D56),
            128 to Color(0xFFF88E23),
            256 to Color(0xFFF6A41E),
            512 to Color(0xFFF5B819),
            1024 to Color(0xFFF4CC14),
            2048 to Color(0xFFF3E00F)
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

    object Emerald : GameTheme("Emerald") {
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

    object Sunset : GameTheme("Sunset") {
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

    object AmoledBlack : GameTheme("Amoled Black") {
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

    object OceanTeal : GameTheme("Ocean Teal") {
        override val primaryColor = Color(0xFF00BFA5)
        override val secondaryColor = Color(0xFF1DE9B6)
        override val backgroundColor = Color(0xFF001A18)
        override val surfaceColor = Color(0xFF00332E)
        override val textColor = Color.White
        override val accentColor = Color(0xFF64FFDA)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF004D45),
            2 to Color(0xFF00695C),
            4 to Color(0xFF00796B),
            8 to Color(0xFF00897B),
            16 to Color(0xFF009688),
            32 to Color(0xFF26A69A),
            64 to Color(0xFF4DB6AC),
            128 to Color(0xFF80CBC4),
            256 to Color(0xFFB2DFDB),
            512 to Color(0xFFE0F2F1),
            1024 to Color(0xFF1DE9B6),
            2048 to Color(0xFF64FFDA)
        )
    }

    object GoldenHour : GameTheme("Golden Hour") {
        override val primaryColor = Color(0xFFFFB300)
        override val secondaryColor = Color(0xFFFFCA28)
        override val backgroundColor = Color(0xFF1F1800)
        override val surfaceColor = Color(0xFF332800)
        override val textColor = Color.White
        override val accentColor = Color(0xFFFFD54F)
        override val isDark = true

        override val tileColors = mapOf(
            0 to Color(0xFF4D3C00),
            2 to Color(0xFF665000),
            4 to Color(0xFF806400),
            8 to Color(0xFF997700),
            16 to Color(0xFFB38B00),
            32 to Color(0xFFCC9F00),
            64 to Color(0xFFE6B300),
            128 to Color(0xFFFFC700),
            256 to Color(0xFFFFD54F),
            512 to Color(0xFFFFE082),
            1024 to Color(0xFFFFECB3),
            2048 to Color(0xFFFFF8E1)
        )
    }

    companion object {
        fun fromName(name: String): GameTheme {
            return when (name) {
                "Neon Rush", "Neon Pink" -> NeonPink
                "Cyber Blue" -> CyberBlue
                "Emerald Matrix", "Emerald" -> Emerald
                "Sunset Orange", "Sunset" -> Sunset
                "Royal Purple" -> RoyalPurple
                "Minimal White" -> MinimalWhite
                "AMOLED Black", "Amoled Black" -> AmoledBlack
                "Ocean Teal" -> OceanTeal
                "Golden Hour" -> GoldenHour
                else -> NeonPink
            }
        }

        fun allThemes() = listOf(
            NeonPink,
            CyberBlue,
            Emerald,
            Sunset,
            RoyalPurple,
            MinimalWhite,
            AmoledBlack,
            OceanTeal,
            GoldenHour
        )
    }
}
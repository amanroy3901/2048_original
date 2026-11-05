package com.avfusionapps.game_2048.shared.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.avfusionapps.game_2048.shared.res.Res
import com.avfusionapps.game_2048.shared.res.baloo2_extrabold
import com.avfusionapps.game_2048.shared.res.lilitaone_regular
import org.jetbrains.compose.resources.Font

/** Lilita One: titles and sprite names (Android: SpriteDisplayFont). */
@Composable
fun displayFont(): FontFamily = FontFamily(Font(Res.font.lilitaone_regular))

/** Baloo 2 ExtraBold: numbers and badges (Android: SpriteNumberFont). */
@Composable
fun numberFont(): FontFamily = FontFamily(Font(Res.font.baloo2_extrabold, FontWeight.ExtraBold))

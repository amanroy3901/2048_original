package com.avfusionapps.game_2048.shared.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.shared.sprites.SpriteTile

/** Themes: the shared app has one look, so this shows the selected Elemental Sprites card. */
@Composable
fun ThemeSettingsScreen() {
    Box(Modifier.fillMaxSize()) {
        SpriteNightBackground(Modifier.fillMaxSize(), scene = false)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().widthIn(max = 600.dp), verticalAlignment = Alignment.CenterVertically) {
                BackRoundButton()
                Spacer(Modifier.width(12.dp))
                Text("Themes", color = Color.White, fontFamily = displayFont(), fontSize = 28.sp)
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().widthIn(max = 600.dp).spritePanel()
                    .border(2.dp, Color(0xFFFFC93A), RoundedCornerShape(28.dp)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (v in listOf(2, 8, 64, 512, 2048)) SpriteTile(v, Modifier.size(52.dp))
                }
                Spacer(Modifier.height(12.dp))
                Text("Elemental Sprites", color = Color.White, fontFamily = displayFont(), fontSize = 22.sp)
                Text("Selected", color = Color(0xFFFFC93A), fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Text("Living tiles with moods, motion and magic.", color = Color(0xFFC9B8F0), fontSize = 13.sp)
            }
        }
    }
}

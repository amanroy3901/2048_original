package com.avfusionapps.game_2048.shared.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.shared.game.GameSettingsRepository
import com.avfusionapps.game_2048.shared.game.Progress
import com.avfusionapps.game_2048.shared.sprites.SpriteTile

private val ProfileMuted = Color(0xFFC9B8F0)
private val ProfileLine = Color(0x52BE96FF)
private val ProfileGold = Color(0xFFFFC93A)

@Composable
internal fun BackRoundButton() {
    RoundButton(size = 40.dp, description = "Back", onClick = { Nav.back() }) {
        val c = center
        val r = size.minDimension / 2f
        drawLine(Color.White, Offset(c.x + r * 0.4f, c.y), Offset(c.x - r * 0.5f, c.y), strokeWidth = r * 0.28f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(c.x - r * 0.5f, c.y), Offset(c.x - r * 0.05f, c.y - r * 0.5f), strokeWidth = r * 0.28f, cap = StrokeCap.Round)
        drawLine(Color.White, Offset(c.x - r * 0.5f, c.y), Offset(c.x - r * 0.05f, c.y + r * 0.5f), strokeWidth = r * 0.28f, cap = StrokeCap.Round)
    }
}

/** Player card (Blaze avatar, name, rank, best score) and the sound / vibration switches. */
@Composable
fun ProfileScreen() {
    val name by GameSettingsRepository.playerNameFlow.collectAsState()
    val high by GameSettingsRepository.highScoreFlow.collectAsState()
    val coins by GameSettingsRepository.coinsFlow.collectAsState()
    val sound by GameSettingsRepository.soundEnabledFlow.collectAsState()
    val vibration by GameSettingsRepository.vibrationEnabledFlow.collectAsState()
    val stats by Progress.stats.collectAsState()
    Box(Modifier.fillMaxSize()) {
        SpriteNightBackground(Modifier.fillMaxSize(), scene = false)
        Column(
            Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth().widthIn(max = 600.dp), verticalAlignment = Alignment.CenterVertically) {
                BackRoundButton()
                Spacer(Modifier.width(12.dp))
                Text("Profile", color = Color.White, fontFamily = displayFont(), fontSize = 28.sp)
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().widthIn(max = 600.dp).spritePanel().padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(84.dp).background(Color(0x33FFFFFF), RoundedCornerShape(42.dp)).border(2.dp, ProfileGold, RoundedCornerShape(42.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    SpriteTile(64, Modifier.size(56.dp), showCard = false, showNumber = false, showAura = false, showEffects = false)
                }
                Spacer(Modifier.height(10.dp))
                Text(name, color = Color.White, fontFamily = displayFont(), fontSize = 26.sp, maxLines = 1)
                Text(spriteRank(stats.biggestTile), color = ProfileGold, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ScoreBox("BEST", high, Modifier.weight(1f))
                    ScoreBox("COINS", coins, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(Modifier.fillMaxWidth().widthIn(max = 600.dp).spritePanel().padding(horizontal = 18.dp, vertical = 8.dp)) {
                SettingSwitch("Sound", "Sprite pops, chimes and coins", sound) { GameSettingsRepository.updateSoundEnabled(it) }
                Box(Modifier.fillMaxWidth().height(1.dp).background(ProfileLine))
                SettingSwitch("Vibration", "Enable or disable vibration", vibration) { GameSettingsRepository.updateVibrationEnabled(it) }
            }
        }
    }
}

@Composable
private fun SettingSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontFamily = displayFont(), fontSize = 18.sp)
            Text(subtitle, color = ProfileMuted, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ProfileGold, uncheckedTrackColor = Color(0x33FFFFFF))
        )
    }
}

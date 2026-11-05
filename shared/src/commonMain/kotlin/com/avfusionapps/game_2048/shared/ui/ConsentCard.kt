package com.avfusionapps.game_2048.shared.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.shared.game.Progress
import com.avfusionapps.game_2048.shared.platform.Telemetry
import com.avfusionapps.game_2048.shared.sprites.SpriteEmotion
import com.avfusionapps.game_2048.shared.sprites.SpriteTile
import kotlinx.coroutines.delay

/**
 * Asks once, on the main menu after the player's first game, whether they'll share anonymous gameplay stats.
 * Both answers are saved and the card never comes back; Profile has the switch to change it later.
 */
@Composable
internal fun ConsentPrompt() {
    remember { Progress.init() } // stats load lazily otherwise
    val stats by Progress.stats.collectAsState()
    val due = Telemetry.shouldAsk && Nav.route == "main" && stats.gamesPlayed >= 1
    var show by remember { mutableStateOf(false) }
    LaunchedEffect(due) {
        show = false
        if (due) {
            delay(700) // let the menu settle first
            show = true
        }
    }
    if (show && Telemetry.shouldAsk) ConsentCard(
        onAllow = { Telemetry.setAnalyticsEnabled(true) },
        onNotNow = { Telemetry.setAnalyticsEnabled(false) }
    )
}

@Composable
internal fun ConsentCard(onAllow: () -> Unit, onNotNow: () -> Unit) {
    SpriteDialog(onDismiss = {}, dismissable = false, testTag = "ConsentCard") {
        SpriteTile(64, Modifier.size(84.dp), showCard = false, showNumber = false, emotion = SpriteEmotion.HAPPY)
        Spacer(Modifier.height(8.dp))
        Text("Help Blaze get better!", color = Color.White, fontFamily = displayFont(), fontSize = 28.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            "Can we see which modes and power-ups you play? It helps us make the game more fun. " +
                "The stats are anonymous: no ads, no tracking, nothing that identifies you.",
            color = Color(0xFFC9B8F0), fontSize = 15.sp, lineHeight = 21.sp, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        SpriteGoldButton("Allow", onAllow, Modifier.fillMaxWidth(), height = 56.dp)
        Spacer(Modifier.height(10.dp))
        SpriteGhostButton("Not now", onNotNow, Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        Text("You can change this any time in Profile.", color = Color(0xFFC9B8F0), fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

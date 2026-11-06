package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.platform.testTag
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun GameOverDialog(
    score: Int,
    onNewGame: () -> Unit,
    onExit: () -> Unit,
    /** Optional ways to keep playing (e.g. spend an Undo), shown above Play Again. */
    rescue: (@Composable ColumnScope.() -> Unit)? = null
) {
    val theme = LocalGameTheme.current
    SpriteGameOverDialog(score = score, onNewGame = onNewGame, onExit = onExit, rescue = rescue)
}

@Composable
fun TimeAttackGameOverDialog(
    finalScore: Int,
    timeSurvived: Long,
    isTimeUp: Boolean = true,
    onPlayAgain: () -> Unit,
    onExit: () -> Unit
) {
    val theme = LocalGameTheme.current
    val minutes = (timeSurvived / 60000).toInt()
    val seconds = ((timeSurvived % 60000) / 1000).toInt()
    SpriteGameOverDialog(
        score = finalScore,
        title = if (isTimeUp) "Time's up!" else "No moves left",
        body = "You kept the sprites going for %d:%02d. Merge faster to earn more time.".format(minutes, seconds),
        primary = "Play again",
        onNewGame = onPlayAgain,
        onExit = onExit
    )
}


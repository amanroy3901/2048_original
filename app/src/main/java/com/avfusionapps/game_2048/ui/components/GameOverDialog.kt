package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.avfusionapps.game_2048.ui.NeonRoundedButton
import com.avfusionapps.game_2048.ui.theme.HighLighter
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

@Composable
fun GameOverDialog(score: Int, onNewGame: () -> Unit, onExit: () -> Unit) {
    val theme = LocalGameTheme.current
    Dialog(onDismissRequest = {}) {
        Surface(
            modifier = Modifier
                .testTag("GameScreen_Dialog_GameOver")
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            color = theme.surfaceColor,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Game Over!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HighLighter
                )
                Text(
                    "Final Score: $score",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    NeonRoundedButton(
                        modifier = Modifier.testTag("GameScreen_Button_GameOverNewGame"),
                        onClick = onNewGame,
                        text = "New Game"
                    )
                    NeonRoundedButton(
                        modifier = Modifier.testTag("GameScreen_Button_GameOverExit"),
                        onClick = onExit,
                        text = "Exit"
                    )
                }
            }
        }
    }
}

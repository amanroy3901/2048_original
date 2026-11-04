package com.avfusionapps.game_2048.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.ui.sprites.ElementalSprites
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.ui.sprites.SpriteTile
import com.avfusionapps.game_2048.ui.theme.LocalGameTheme

/** All 11 sprites; the ones the player hasn't made yet show as dark silhouettes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpriteCollectionSheet(seen: Set<Int>, onDismiss: () -> Unit) {
    val theme = LocalGameTheme.current
    val found = ElementalSprites.all.indices.count { (2 shl it) in seen }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = theme.surfaceColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Collection", color = theme.textColor, fontSize = 24.sp, fontWeight = FontWeight.Black)
            Text(
                "$found of ${ElementalSprites.all.size} sprites found. New ones join the first time you merge them.",
                color = theme.textColor.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxWidth().height(430.dp)
            ) {
                itemsIndexed(ElementalSprites.all) { i, spec ->
                    val value = 2 shl i
                    val isFound = value in seen
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "No.${(i + 1).toString().padStart(2, '0')}",
                            color = theme.textColor.copy(alpha = 0.6f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        SpriteTile(
                            value = value,
                            locked = !isFound,
                            emotion = if (isFound) SpriteEmotion.HAPPY else SpriteEmotion.IDLE,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (isFound) spec.name else "???",
                            color = if (isFound) theme.textColor else theme.textColor.copy(alpha = 0.5f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                "The Shadow Rock is not a sprite. It drops in after 10 moves without a merge or 3 bonks in a row.",
                color = Color(0xFFC9B8F0),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }
    }
}

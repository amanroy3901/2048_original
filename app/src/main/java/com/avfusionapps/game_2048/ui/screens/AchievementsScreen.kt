package com.avfusionapps.game_2048.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.avfusionapps.game_2048.data.progress.Achievement
import com.avfusionapps.game_2048.data.progress.Achievements
import com.avfusionapps.game_2048.data.progress.PlayerStats
import com.avfusionapps.game_2048.data.progress.Progress
import com.avfusionapps.game_2048.ui.sprites.SpriteDisplayFont
import com.avfusionapps.game_2048.ui.sprites.SpriteNumberFont

private val Muted = Color(0xFFC9B8F0)
private val Line = Color(0x52BE96FF)
private val Gold = Color(0xFFFFC93A)

/** Medals for everything worth celebrating, plus the lifetime numbers behind them. */
@Composable
fun AchievementsScreen(navController: NavController) {
    val stats by Progress.stats.collectAsState()
    val unlocked by Progress.unlocked.collectAsState()
    val list = remember(unlocked) { Achievements.all.sortedBy { if (it.id in unlocked) 0 else 1 } }
    Box(Modifier.fillMaxSize()) {
        SpriteNightBackground(Modifier.fillMaxSize(), scene = false)
        BoxWithConstraints(Modifier.fillMaxSize().safeDrawingPadding()) {
            val cols = when {
                maxWidth >= 900.dp -> 5
                maxWidth >= 600.dp -> 4
                else -> 3
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(cols),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RoundButton(size = 40.dp, description = "Back", onClick = { navController.navigateUp() }) {
                                val c = center
                                val r = size.minDimension / 2f
                                drawLine(Color.White, Offset(c.x + r * 0.4f, c.y), Offset(c.x - r * 0.5f, c.y), strokeWidth = r * 0.28f, cap = StrokeCap.Round)
                                drawLine(Color.White, Offset(c.x - r * 0.5f, c.y), Offset(c.x - r * 0.05f, c.y - r * 0.5f), strokeWidth = r * 0.28f, cap = StrokeCap.Round)
                                drawLine(Color.White, Offset(c.x - r * 0.5f, c.y), Offset(c.x - r * 0.05f, c.y + r * 0.5f), strokeWidth = r * 0.28f, cap = StrokeCap.Round)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Achievements", color = Color.White, fontFamily = SpriteDisplayFont, fontSize = 28.sp)
                                Text("${unlocked.size} of ${Achievements.all.size} medals", color = Muted, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        StatsStrip(stats)
                        Spacer(Modifier.height(4.dp))
                    }
                }
                items(list, key = { it.id }) { a -> AchievementCell(a, a.id in unlocked, stats) }
            }
        }
    }
}

@Composable
private fun StatsStrip(s: PlayerStats) {
    val items = listOf(
        "Games" to s.gamesPlayed,
        "Merges" to s.merges,
        "Best combo" to s.bestCombo,
        "Biggest" to s.biggestTile,
        "Best chain" to s.bestChain,
        "Streak" to s.bestDailyStreak
    )
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color(0xB80C0522)).border(1.dp, Line, RoundedCornerShape(20.dp)).padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { (label, v) ->
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("%,d".format(v), color = Color.White, fontFamily = SpriteNumberFont, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, lineHeight = 22.sp)
                        Text(label, color = Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementCell(a: Achievement, unlocked: Boolean, stats: PlayerStats) {
    val clock by com.avfusionapps.game_2048.ui.sprites.spriteTime()
    Column(
        Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.verticalGradient(if (unlocked) listOf(Color(0xCC3A1C80), Color(0xE61A0E45)) else listOf(Color(0x990C0522), Color(0xB80C0522))))
            .border(if (unlocked) 2.dp else 1.dp, if (unlocked) Gold.copy(alpha = 0.6f) else Line, RoundedCornerShape(18.dp))
            .drawBehind {
                if (unlocked) {
                    val shine = (clock % 2.8f) / 2.8f
                    val x = -size.width * 0.5f + shine * size.width * 2f
                    drawRect(Brush.linearGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.12f), Color.Transparent), start = Offset(x - 40f, 0f), end = Offset(x + 40f, size.height)))
                }
            }
            .padding(horizontal = 6.dp, vertical = 10.dp)
            .semantics { contentDescription = "${a.title}: ${a.description}. ${if (unlocked) "Unlocked" else "Locked"}" },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Medal(a, unlocked, size = 58.dp, modifier = Modifier.graphicsLayer { alpha = if (unlocked) 1f else 0.85f })
        Spacer(Modifier.height(6.dp))
        Text(a.title, color = if (unlocked) Color.White else Color(0xFFE0D6F7), fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 15.sp, maxLines = 2)
        Text(a.description, color = Muted, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 13.sp, maxLines = 3)
        Spacer(Modifier.weight(1f))
        if (unlocked) {
            Text("+${a.tier.coins} coins", color = Gold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        } else {
            Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(6.dp)).background(Color.Black.copy(alpha = 0.45f))) {
                Box(Modifier.fillMaxHeight().fillMaxWidth(a.progress(stats)).clip(RoundedCornerShape(6.dp)).background(Brush.horizontalGradient(listOf(Color(0xFFFF9A1F), Color(0xFFFFD25A)))))
            }
        }
    }
}

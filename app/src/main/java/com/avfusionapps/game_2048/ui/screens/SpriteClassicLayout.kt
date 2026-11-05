package com.avfusionapps.game_2048.ui.screens

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.avfusionapps.game_2048.ui.components.GameBoard
import com.avfusionapps.game_2048.ui.sprites.ElementalSprites
import com.avfusionapps.game_2048.ui.sprites.SpriteEmotion
import com.avfusionapps.game_2048.viewmodel.Direction
import com.avfusionapps.game_2048.viewmodel.GameState
import com.avfusionapps.game_2048.viewmodel.GameViewModel
import com.avfusionapps.game_2048.viewmodel.PowerUps
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.random.Random

/** What Blaze is saying right now, and how he looks saying it. */
private data class Line(val text: String, val emotion: SpriteEmotion, val key: Int)

private const val INTRO = "Blaze: Swipe to slide every sprite. Twins merge into the next element."

/**
 * The Classic screen in the Elemental Sprites theme, laid out like the v3 prototype: header, hero banner,
 * Blaze's narration, the board, the power-ups and the bottom menu, over the night-sky background.
 */
@Composable
internal fun SpriteClassicLayout(
    viewModel: GameViewModel,
    gameState: GameState,
    best: Int,
    coins: Int,
    soundEnabled: Boolean,
    onMenu: () -> Unit,
    onHelp: () -> Unit,
    onCollection: () -> Unit,
    onProfile: () -> Unit,
    onThemes: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()

    // ── Blaze's narration: a base line from the game state, overridden by short reactions ──
    var keyCounter by remember { mutableIntStateOf(1) }
    var said by remember { mutableStateOf<Line?>(null) }
    var hopKey by remember { mutableIntStateOf(0) }
    var clearJob by remember { mutableStateOf<Job?>(null) }
    fun say(text: String, emotion: SpriteEmotion, ms: Long) {
        said = Line("Blaze: $text", emotion, ++keyCounter)
        if (emotion != SpriteEmotion.SLEEPY && emotion != SpriteEmotion.SAD && emotion != SpriteEmotion.THINKING) hopKey++
        clearJob?.cancel()
        clearJob = scope.launch { delay(ms); said = null }
    }
    var idle by remember { mutableIntStateOf(0) }
    LaunchedEffect(gameState.moveCount) {
        if (idle >= 2) say("Whoa! We are up, we are up!", SpriteEmotion.SURPRISED, 1400)
        idle = 0
        delay(6_000)
        idle = 1
        delay(8_000)
        idle = 2
    }
    LaunchedEffect(gameState.notice?.key) {
        val n = gameState.notice ?: return@LaunchedEffect
        val t = n.text
        when {
            t.startsWith("Bonk") -> say(if (t.contains("Combo")) "Bonk! Nothing can slide that way, and the combo is gone." else "Bonk! Nothing can slide that way.", SpriteEmotion.DIZZY, 1500)
            t.contains("fell in") -> say(t, SpriteEmotion.WORRIED, 2200)
            t.contains("crumbled") -> say("The Shadow Rock crumbled. Room to breathe!", SpriteEmotion.HAPPY, 1800)
            t.startsWith("New friend") -> {
                val name = t.removePrefix("New friend: ").removeSuffix("!")
                val no = ElementalSprites.all.indexOfFirst { it.name == name } + 1
                say("New friend: $name${if (no > 0) ", No.%02d".format(no) else ""}! Check your collection.", SpriteEmotion.PROUD, 1800)
            }
            t.startsWith("Smashed") -> say("Smashed the Shadow Rock!", SpriteEmotion.SURPRISED, 1500)
            else -> say(t, SpriteEmotion.HAPPY, 1600)
        }
    }
    var reactValue by remember { mutableIntStateOf(0) }
    var reactKey by remember { mutableIntStateOf(0) }
    LaunchedEffect(gameState.lastGain?.key) {
        val gain = gameState.lastGain ?: return@LaunchedEffect
        val merged = gameState.tileAnimationInfo.filterValues { it.isMerged }.keys
            .mapNotNull { (r, c) -> gameState.grid.getOrNull(r)?.getOrNull(c) }
        val biggest = merged.maxOrNull() ?: 0
        if (biggest > 0) { reactValue = biggest; reactKey++ }
        when {
            gain.multiplier >= 2 -> say("Combo x${gain.multiplier}! Keep merging every move.", SpriteEmotion.EXCITED, 1400)
            biggest >= 16 && Random.nextFloat() < 0.5f -> {
                val a = ElementalSprites.specFor(biggest / 2).name
                say("$a + $a = ${ElementalSprites.specFor(biggest).name}!", SpriteEmotion.HAPPY, 1500)
            }
        }
    }
    var wasArmed by remember { mutableStateOf(gameState.hammerArmed) }
    LaunchedEffect(gameState.hammerArmed) {
        if (gameState.hammerArmed && !wasArmed) say("Tap a sprite or a Shadow Rock to break it. Tap the hammer again to cancel.", SpriteEmotion.THINKING, 60_000)
        else if (!gameState.hammerArmed && wasArmed && gameState.breakingCell == null) say("Hammer put away.", SpriteEmotion.IDLE, 900)
        wasArmed = gameState.hammerArmed
    }
    LaunchedEffect(gameState.breakingCell) {
        val cell = gameState.breakingCell ?: return@LaunchedEffect
        val v = gameState.grid.getOrNull(cell.first)?.getOrNull(cell.second) ?: 0
        say(if (v < 0) "Smashed the Shadow Rock!" else "Bye, ${ElementalSprites.specFor(v).name}. That made some room.", SpriteEmotion.SURPRISED, 1500)
    }
    var wasFrozen by remember { mutableStateOf(gameState.freezeMoves > 0) }
    LaunchedEffect(gameState.freezeMoves) {
        val frozen = gameState.freezeMoves > 0
        if (frozen && !wasFrozen) say("Freeze! No new sprites for the next 3 moves.", SpriteEmotion.SURPRISED, 1800)
        else if (!frozen && wasFrozen) say("The freeze wore off. New sprites are back.", SpriteEmotion.SURPRISED, 1500)
        wasFrozen = frozen
    }
    LaunchedEffect(gameState.rewindKey) { if (gameState.rewindKey > 0) say("Rewound one move.", SpriteEmotion.SURPRISED, 1400) }
    LaunchedEffect(gameState.moveCount) {
        if (gameState.tileAnimationInfo.values.any { it.shuffled }) say("Shuffle! Everyone swapped places.", SpriteEmotion.DIZZY, 1500)
    }
    val free = gameState.grid.sumOf { row -> row.count { it == 0 } }
    var warned by remember { mutableStateOf(false) }
    LaunchedEffect(free <= 2 && !gameState.isGameOver) {
        if (free <= 2 && !gameState.isGameOver && !warned) { warned = true; say("Careful, we are almost out of room!", SpriteEmotion.WORRIED, 1800) }
        if (free > 2) warned = false
    }
    val base = when {
        gameState.isGameOver -> Line("Blaze: Oh no, nobody can move. Use a power-up, or start again.", SpriteEmotion.SAD, -1)
        idle >= 2 -> Line("Blaze: Zzz… swipe to wake everyone up.", SpriteEmotion.SLEEPY, -2)
        idle == 1 -> Line("Blaze: Hmm… try keeping your biggest sprite in a corner.", SpriteEmotion.THINKING, -3)
        else -> Line(INTRO, SpriteEmotion.IDLE, -4)
    }
    val line = said?.takeIf { !gameState.isGameOver } ?: base

    // ── Level bar: the board's total working toward the next level's tile ──
    val level = gameState.currentLevel
    val nextTile = 128 shl (level - 1).coerceIn(0, 4)
    val progress = gameState.grid.sumOf { row -> row.sumOf { it.coerceAtLeast(0) } }.toFloat() / nextTile

    val safeTop = with(density) { WindowInsets.safeDrawing.getTop(this).toDp() }
    Box(Modifier.fillMaxSize().testTag("GameScreen_Root")) {
        SpriteNightBackground(Modifier.fillMaxSize(), scene = true, hillsTop = safeTop + 8.dp + 106.dp)
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .pointerInput(gameState.isGameOver) {
                    var totalX = 0f
                    var totalY = 0f
                    detectDragGestures(
                        onDragEnd = {
                            if (!gameState.isGameOver) {
                                val min = 50
                                when {
                                    abs(totalX) > abs(totalY) && abs(totalX) > min -> viewModel.move(if (totalX > 0) Direction.RIGHT else Direction.LEFT)
                                    abs(totalY) > abs(totalX) && abs(totalY) > min -> viewModel.move(if (totalY > 0) Direction.DOWN else Direction.UP)
                                }
                            }
                            totalX = 0f; totalY = 0f
                        }
                    ) { change, drag ->
                        change.consume()
                        totalX += drag.x
                        totalY += drag.y
                    }
                }
        ) {
            // Tablets: keep the phone proportions in a centred column.
            val w = minOf(maxWidth, 600.dp)
            val h = maxHeight
            val gap = 10.dp
            // header, two-line bubble, power-ups (with badges), bottom menu, gaps
            val fixed = 46.dp + 52.dp + 78.dp + 72.dp + gap * 5
            val heroH = (h - fixed - w).coerceIn(170.dp, 224.dp)
            val board = minOf(w, h - fixed - heroH)
            Column(Modifier.align(Alignment.TopCenter).fillMaxHeight().width(w), horizontalAlignment = Alignment.CenterHorizontally) {
                SpriteClassicHeader(
                    level = level,
                    progress = progress,
                    coins = coins,
                    soundOn = soundEnabled,
                    onSound = { viewModel.updateSoundEnabled(!soundEnabled) }
                )
                Spacer(Modifier.height(gap))
                SpriteHero(
                    score = gameState.score,
                    best = maxOf(best, gameState.score),
                    heroEmotion = line.emotion,
                    hopKey = hopKey,
                    reactValue = reactValue,
                    reactKey = reactKey,
                    height = heroH
                )
                Spacer(Modifier.height(gap))
                SpriteNarratorRow(
                    text = line.text,
                    textKey = line.key,
                    combo = gameState.combo,
                    freezeMoves = gameState.freezeMoves,
                    rockTtl = gameState.rockTtl
                )
                Spacer(Modifier.height(gap))
                Box(Modifier.size(board)) { GameBoard(viewModel = viewModel) }
                Spacer(Modifier.height(gap))
                val p = gameState.powerUps
                SpritePowerRow(
                    cost = PowerUps.COST,
                    buttons = listOf(
                        SpritePowerButton("Undo", p.undo, Color(0xFFB57BFF), Color(0xFF6B35D6), Color(0xB3B57BFF), onClick = { viewModel.undoMove(context) }) { drawUndoIcon() },
                        SpritePowerButton("Hammer", p.hammer, Color(0xFFC9A8FF), Color(0xFF7E4FE0), Color(0x99C9A8FF), active = gameState.hammerArmed, onClick = { viewModel.toggleHammer(context) }) { drawHammerIcon() },
                        SpritePowerButton("Shuffle", p.shuffle, Color(0xFFFFB25A), Color(0xFFF2601E), Color(0xB3FF963C), onClick = { viewModel.shuffle(context) }) { drawShuffleIcon() },
                        SpritePowerButton("Freeze", p.freeze, Color(0xFF7FD8FF), Color(0xFF2E7BE0), Color(0xB36EC8FF), active = gameState.freezeMoves > 0, onClick = { viewModel.freeze(context) }) { drawFreezeIcon() },
                        SpritePowerButton("Hint", p.hint, Color(0xFFFFE07A), Color(0xFFE59A12), Color(0xB3FFD25A), onClick = { viewModel.showHint(context) }) { drawHintIcon() }
                    )
                )
                Spacer(Modifier.weight(1f))
                SpriteBottomNav(
                    items = listOf(
                        SpriteNavItem("Menu", onMenu) { navMenuIcon() },
                        SpriteNavItem("Help", onHelp) { navHelpIcon() },
                        SpriteNavItem("Collection", onCollection) { navCollectionIcon() },
                        SpriteNavItem("Profile", onProfile) { navProfileIcon() },
                        SpriteNavItem("Themes", onThemes) { navThemesIcon() }
                    )
                )
            }
        }
    }
}

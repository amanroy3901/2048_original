package com.avfusionapps.game_2048.shared.ui

import androidx.compose.foundation.layout.fillMaxSize

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.math.abs
import com.avfusionapps.game_2048.shared.game.Direction
import com.avfusionapps.game_2048.shared.game.SoundManager
import com.avfusionapps.game_2048.shared.game.SpriteSfx
import com.avfusionapps.game_2048.shared.game.TimeAttackState
import com.avfusionapps.game_2048.shared.game.TimeAttackViewModel
import com.avfusionapps.game_2048.shared.platform.hapticTap
import com.avfusionapps.game_2048.shared.platform.nowMillis
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avfusionapps.game_2048.shared.sprites.SpriteEmotion
import com.avfusionapps.game_2048.shared.sprites.SpriteTile
import kotlin.math.sin

private val Muted = Color(0xFFC9B8F0)

/** Time Attack header: back, a big clock panel with Frosty watching the seconds, help and pause. */
@Composable
internal fun SpriteTimeAttackHeader(
    timeRemainingMillis: Long,
    isPaused: Boolean,
    onPauseToggle: () -> Unit,
    onHelp: () -> Unit,
    onBack: () -> Unit,
    bonuses: List<FloatingBonus>,
    onBonusDone: (Long) -> Unit
) {
    val minutes = (timeRemainingMillis / 60000).toInt()
    val seconds = ((timeRemainingMillis % 60000) / 1000).toInt()
    val tenths = ((timeRemainingMillis % 1000) / 100).toInt()
    val urgent = timeRemainingMillis < 10_000L
    val color by animateColorAsState(
        when {
            urgent -> Color(0xFFFF4D6D)
            timeRemainingMillis < 30_000L -> Color(0xFFFFC93A)
            else -> Color.White
        }, label = "taColor"
    )
    val beat by rememberInfiniteTransition(label = "beat").animateFloat(0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing)), label = "beatT")
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        RoundButton(44.dp, "Back", onBack, Modifier.testTag("TimeAttackTopBar_Button_Back")) { backArrow() }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
            Row(
                Modifier
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xB80C0522))
                    .border(2.dp, if (urgent) Color(0xFFFF4D6D).copy(alpha = 0.5f + 0.5f * beat) else Color(0x8047C6FF), RoundedCornerShape(22.dp))
                    .padding(start = 6.dp, end = 16.dp, top = 4.dp, bottom = 4.dp)
                    .semantics { contentDescription = "$minutes minutes $seconds seconds left" },
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteTile(16, Modifier.size(52.dp), showCard = false, showNumber = false, showEffects = false, emotion = if (urgent) SpriteEmotion.WORRIED else if (isPaused) SpriteEmotion.SLEEPY else SpriteEmotion.EXCITED)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$minutes:${pad2(seconds)}.$tenths",
                        color = color, fontFamily = numberFont(), fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 36.sp,
                        modifier = Modifier.graphicsLayer { if (urgent) { val s = 1f + 0.06f * sin(beat * kotlin.math.PI.toFloat()); scaleX = s; scaleY = s } }
                    )
                    Text(if (isPaused) "PAUSED" else "TIME LEFT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                }
            }
            bonuses.forEach { b -> key(b.id) { FloatingBonusText(text = b.text, onAnimationFinished = { onBonusDone(b.id) }) } }
        }
        Spacer(Modifier.width(10.dp))
        RoundButton(44.dp, "Help", onHelp) { questionMark() }
        Spacer(Modifier.width(8.dp))
        RoundButton(44.dp, if (isPaused) "Resume" else "Pause", onPauseToggle, Modifier.testTag("TimeAttackTopBar_Button_Pause")) { if (isPaused) playIcon() else pauseIcon() }
    }
}

/** Score and best, as the prototype's crowned panels. */
@Composable
internal fun SpriteScoreRow(score: Int, best: Int) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        ScoreBox("Score", score, Modifier)
        ScoreBox("Best", best, Modifier)
    }
}

/** Undo, new game and hint as the shooter's labelled square buttons. */
@Composable
internal fun SpriteTimeAttackActions(onUndo: () -> Unit, onNewGame: () -> Unit, onHint: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color(0x990A041E)).border(1.dp, Color(0x52BE96FF), RoundedCornerShape(24.dp)).padding(8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        ActionSquare("Undo", Color(0xFFB57BFF), Color(0xFF6B35D6), onUndo) { drawUndoIcon() }
        ActionSquare("New game", Color(0xFFFFB25A), Color(0xFFF2601E), onNewGame) { drawShuffleIcon() }
        ActionSquare("Hint", Color(0xFFFFE07A), Color(0xFFE59A12), onHint) { drawHintIcon() }
    }
}

@Composable
private fun ActionSquare(label: String, top: Color, bottom: Color, onClick: () -> Unit, icon: DrawScope.() -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick).semantics { contentDescription = label }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(58.dp).scale(if (pressed) 0.88f else 1f).clip(RoundedCornerShape(18.dp))
                .background(Brush.linearGradient(listOf(top, bottom), start = Offset.Zero, end = Offset(110f, 170f)))
                .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center
        ) { Canvas(Modifier.size(32.dp)) { icon() } }
        Spacer(Modifier.height(4.dp))
        Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

private fun DrawScope.backArrow() {
    val c = center
    val r = size.minDimension / 2f
    val w = r * 0.28f
    drawLine(Color.White, Offset(c.x + r * 0.4f, c.y), Offset(c.x - r * 0.5f, c.y), strokeWidth = w, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(c.x - r * 0.5f, c.y), Offset(c.x - r * 0.05f, c.y - r * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
    drawLine(Color.White, Offset(c.x - r * 0.5f, c.y), Offset(c.x - r * 0.05f, c.y + r * 0.5f), strokeWidth = w, cap = StrokeCap.Round)
}

private fun DrawScope.pauseIcon() {
    val w = size.width
    drawRoundRect(Color.White, topLeft = Offset(w * 0.22f, w * 0.15f), size = Size(w * 0.2f, w * 0.7f), cornerRadius = CornerRadius(w * 0.08f))
    drawRoundRect(Color.White, topLeft = Offset(w * 0.58f, w * 0.15f), size = Size(w * 0.2f, w * 0.7f), cornerRadius = CornerRadius(w * 0.08f))
}

private fun DrawScope.playIcon() {
    val w = size.width
    val p = androidx.compose.ui.graphics.Path().apply { moveTo(w * 0.28f, w * 0.15f); lineTo(w * 0.85f, w * 0.5f); lineTo(w * 0.28f, w * 0.85f); close() }
    drawPath(p, Color.White)
}

private fun DrawScope.questionMark() {
    val w = size.width
    drawArc(Color.White, 200f, 250f, false, topLeft = Offset(w * 0.25f, w * 0.08f), size = Size(w * 0.5f, w * 0.46f), style = Stroke(w * 0.14f, cap = StrokeCap.Round))
    drawLine(Color.White, Offset(w * 0.5f, w * 0.52f), Offset(w * 0.5f, w * 0.64f), strokeWidth = w * 0.14f, cap = StrokeCap.Round)
    drawCircle(Color.White, w * 0.08f, Offset(w * 0.5f, w * 0.86f))
}


data class FloatingBonus(
    val id: Long,
    val text: String
)

@Composable
fun FloatingBonusText(text: String, onAnimationFinished: () -> Unit) {
    val animatableY = remember { Animatable(40f) }
    val animatableAlpha = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.coroutineScope {
            launch { animatableY.animateTo(-50f, tween(durationMillis = 1200, easing = FastOutLinearInEasing)) }
            launch { animatableAlpha.animateTo(0f, tween(durationMillis = 1200, easing = FastOutLinearInEasing)) }
        }
        onAnimationFinished()
    }
    Text(
        text = text,
        color = Color(0xFFFFD25A),
        fontSize = 24.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.offset(y = animatableY.value.dp).graphicsLayer(alpha = animatableAlpha.value)
    )
}

/** Time Attack (sprite theme). Android: TimeAttackScreen's sprite branch. */
@Composable
fun TimeAttackScreen(vm: TimeAttackViewModel) {
    val viewModel = vm
    val gameState by viewModel.gameState.collectAsState()
    val highScore by viewModel.highScore.collectAsState()
    val hasSeenTimeAttackTutorial by viewModel.hasSeenTimeAttackTutorial.collectAsState()
    var forceShowTutorial by remember { mutableStateOf(false) }
    val vibrationEnabled by viewModel.vibrationEnabled.collectAsState()
    val soundEnabled by viewModel.soundEnabled.collectAsState()
    var floatingBonuses by remember { mutableStateOf(listOf<FloatingBonus>()) }

    LaunchedEffect(viewModel, soundEnabled) {
        viewModel.soundEvent.collect { soundId ->
            if (!soundEnabled) return@collect
            val s = viewModel.gameState.value
            when (soundId) {
                SoundManager.SOUND_MOVE -> { SpriteSfx.play(SpriteSfx.Sound.SLIDE); SpriteSfx.play(SpriteSfx.Sound.POP, delayMs = 335) }
                SoundManager.SOUND_MERGE -> {
                    val top = s.tileAnimationInfo.filterValues { it.isMerged }.keys.maxOfOrNull { (r, c) -> s.grid.getOrNull(r)?.getOrNull(c) ?: 0 } ?: 4
                    SpriteSfx.play(SpriteSfx.Sound.SLIDE)
                    SpriteSfx.merge(top, delayMs = 130)
                    SpriteSfx.play(SpriteSfx.Sound.POP, delayMs = 335)
                }
                SoundManager.SOUND_LEVEL_UP -> SpriteSfx.play(SpriteSfx.Sound.FANFARE, delayMs = 130)
                SoundManager.SOUND_GAME_OVER -> SpriteSfx.play(SpriteSfx.Sound.SAD)
                else -> {}
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.timeBonusEvent.collectLatest { bonusText ->
            floatingBonuses = floatingBonuses + FloatingBonus(id = nowMillis() + (0..1000).random(), text = bonusText)
        }
    }
    LaunchedEffect(viewModel, vibrationEnabled) {
        viewModel.mergeEvent.collect { if (vibrationEnabled) hapticTap() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("TimeAttackScreen_Root")
            .background(NightBrush)
            .safeDrawingPadding()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                var totalX = 0f
                var totalY = 0f
                detectDragGestures(
                    onDragEnd = {
                        if (!viewModel.gameState.value.isGameOver) {
                            val minSwipeDistance = 50
                            when {
                                abs(totalX) > abs(totalY) && abs(totalX) > minSwipeDistance ->
                                    viewModel.onSwipe(if (totalX > 0) Direction.RIGHT else Direction.LEFT)
                                abs(totalY) > abs(totalX) && abs(totalY) > minSwipeDistance ->
                                    viewModel.onSwipe(if (totalY > 0) Direction.DOWN else Direction.UP)
                            }
                        }
                        totalX = 0f; totalY = 0f
                    }
                ) { change, dragAmount ->
                    change.consume()
                    totalX += dragAmount.x
                    totalY += dragAmount.y
                }
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Column(
            modifier = Modifier.widthIn(max = 600.dp).fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SpriteTimeAttackHeader(
                timeRemainingMillis = gameState.timeRemainingMillis,
                isPaused = gameState.isPaused,
                onPauseToggle = { viewModel.togglePause() },
                onHelp = { forceShowTutorial = true; viewModel.setPaused(true) },
                onBack = { Nav.back() },
                bonuses = floatingBonuses,
                onBonusDone = { id -> floatingBonuses = floatingBonuses.filter { it.id != id } }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SpriteScoreRow(score = gameState.score, best = highScore)
            Spacer(modifier = Modifier.height(14.dp))
            GameBoard(
                grid = gameState.grid,
                tileAnimationInfo = gameState.tileAnimationInfo,
                moveCount = gameState.moveCount,
                onAnimationsComplete = { viewModel.clearAnimationInfo() },
                isGameOver = gameState.isGameOver
            )
            Spacer(modifier = Modifier.weight(1f))
            SpriteTimeAttackActions(
                onUndo = { viewModel.undoMove() },
                onNewGame = { viewModel.startNewGame() },
                onHint = { viewModel.showHint() }
            )
        }
    }

    val isTutorialShowing = (hasSeenTimeAttackTutorial == false || forceShowTutorial)
    if (gameState.isPaused && !isTutorialShowing && !gameState.isGameOver) {
        SpritePauseDialog(
            score = gameState.score,
            onResume = { viewModel.togglePause() },
            onRestart = { viewModel.startNewGame() },
            onQuit = { Nav.back() },
            onDismiss = { viewModel.togglePause() }
        )
    }
    if (gameState.isGameOver) {
        val survived = gameState.timeSurvivedMillis
        val minutes = (survived / 60000).toInt()
        val seconds = ((survived % 60000) / 1000).toInt()
        SpriteGameOverDialog(
            score = gameState.score,
            title = if (gameState.timeRemainingMillis <= 0) "Time's up!" else "No moves left",
            body = "You kept the sprites going for $minutes:${pad2(seconds)}. Merge faster to earn more time.",
            primary = "Play again",
            onNewGame = { viewModel.startNewGame() },
            onExit = { Nav.back() }
        )
    }
    if (hasSeenTimeAttackTutorial == false || forceShowTutorial) {
        SpriteTutorial(isTimeAttack = true, onDismiss = {
            viewModel.setHasSeenTimeAttackTutorial(true)
            forceShowTutorial = false
            viewModel.setPaused(false)
        })
    }
}

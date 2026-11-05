package com.avfusionapps.game_2048.ui.sprites

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

/**
 * One clock for every idle loop on screen: sprites breathing and blinking, the sky twinkling, the hero's friends
 * bobbing, the shooter's beam. It ticks at [FPS] rather than every display frame (60–120 Hz), and it stops while a
 * sprite dialog covers the screen, so a menu that nobody touches costs far less battery.
 *
 * One-shot animations (merges, spawns, hops, pokes) keep their own full-rate timelines.
 */
object SpriteClock {
    /** Idle loops are slow (breaths, blinks, twinkles); 30 updates a second look the same and halve the work. */
    const val FPS = 30
    private const val SECONDS = 3600f
    internal val covers = mutableIntStateOf(0)

    @Composable
    internal fun Provide(honourCovers: Boolean, content: @Composable () -> Unit) {
        val time = remember { mutableFloatStateOf(0f) }
        LaunchedEffect(honourCovers) {
            val step = 1_000_000_000L / FPS
            var prev = -1L
            var t = time.floatValue
            while (true) {
                if (honourCovers && covers.intValue > 0) {
                    snapshotFlow { covers.intValue }.first { it == 0 }
                    prev = -1L
                }
                withFrameNanos { now ->
                    // Advance by real elapsed time (capped, so a pause or a stall doesn't jump the loops).
                    if (prev >= 0) t = (t + ((now - prev) / 1e9f).coerceAtMost(0.1f)) % SECONDS
                    prev = now
                    time.floatValue = t
                }
                delay(step / 1_000_000L - 4)
            }
        }
        CompositionLocalProvider(LocalSpriteTime provides time, content = content)
    }
}

internal val LocalSpriteTime = staticCompositionLocalOf<State<Float>?> { null }

/** The app-wide sprite clock; it pauses while a sprite dialog is up. */
@Composable
fun ProvideSpriteClock(content: @Composable () -> Unit) = SpriteClock.Provide(honourCovers = true, content = content)

/**
 * Marks the screen underneath as covered (its sprites hold still) and gives this content its own running clock.
 * Use it as the body of a dialog that hides most of the screen.
 */
@Composable
fun SpriteCover(content: @Composable () -> Unit) {
    DisposableEffect(Unit) {
        SpriteClock.covers.intValue++
        onDispose { SpriteClock.covers.intValue-- }
    }
    SpriteClock.Provide(honourCovers = false, content = content)
}

/** Seconds on the shared clock (read it in a draw or layer lambda, not in composition). */
@Composable
fun spriteTime(): State<Float> {
    LocalSpriteTime.current?.let { return it }
    // Previews and screens outside the app's clock fall back to a private one.
    return rememberInfiniteTransition(label = "spriteFallback")
        .animateFloat(0f, 3600f, infiniteRepeatable(tween(3_600_000, easing = LinearEasing)), label = "spriteFallbackT")
}

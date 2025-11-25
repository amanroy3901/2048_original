package com.avfusionapps.game_2048.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

data class CelebrationParticle(
    val id: Int,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var color: Color,
    var alpha: Float,
    var rotation: Float,
    var rotationSpeed: Float,
    var life: Float,
    val maxLife: Float,
    val type: ParticleType
) {
    enum class ParticleType {
        STAR, DIAMOND, CIRCLE, SQUARE, TRIANGLE
    }
}

@Composable
fun LevelCelebrationEffect(
    isVisible: Boolean,
    level: Int,
    modifier: Modifier = Modifier
) {
    var particles by remember { mutableStateOf(listOf<CelebrationParticle>()) }
    val coroutineScope = rememberCoroutineScope()
    
    LaunchedEffect(isVisible, level) {
        if (isVisible) {
            val particleCount = when (level) {
                2 -> 500
                3 -> 1000
                4 -> 2000
                else -> 5000
            }
            
            particles = generateParticles(particleCount, level)
            
            // Animate particles
            coroutineScope.launch {
                while (particles.any { it.life > 0 }) {
                    particles = particles.map { particle ->
                        particle.copy(
                            x = particle.x + particle.vx,
                            y = particle.y + particle.vy,
                            vy = particle.vy + 0.1f, // gravity
                            vx = particle.vx * 0.99f, // air resistance
                            alpha = particle.alpha * 0.98f,
                            life = particle.life - 1f,
                            rotation = particle.rotation + particle.rotationSpeed
                        )
                    }.filter { it.life > 0 }
                    delay(16) // ~60fps
                }
            }
        } else {
            particles = emptyList()
        }
    }
    
    if (particles.isNotEmpty()) {
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { particle ->
                drawParticle(particle)
            }
        }
    }
}

private fun generateParticles(count: Int, level: Int): List<CelebrationParticle> {
    val colors = getLevelColors(level)
    
    return List(count) { index ->
        val angle = Random.nextFloat() * 360f
        val speed = Random.nextFloat() * 8f + 4f
        val centerX = 0f
        val centerY = 0f
        
        CelebrationParticle(
            id = index,
            x = centerX,
            y = centerY,
            vx = cos(Math.toRadians(angle.toDouble())).toFloat() * speed,
            vy = sin(Math.toRadians(angle.toDouble())).toFloat() * speed - 2f,
            size = Random.nextFloat() * 12f + 6f,
            color = colors.random(),
            alpha = 1f,
            rotation = Random.nextFloat() * 360f,
            rotationSpeed = Random.nextFloat() * 10f - 5f,
            life = Random.nextFloat() * 120f + 60f,
            maxLife = 180f,
            type = CelebrationParticle.ParticleType.values().random()
        )
    }
}

private fun getLevelColors(level: Int): List<Color> {
    return when (level) {
        2 -> listOf(
            Color(0xFFFFD700), // Gold
            Color(0xFFFFA500), // Orange
            Color(0xFFFF6347)  // Tomato
        )
        3 -> listOf(
            Color(0xFF00CED1), // Dark Turquoise
            Color(0xFF4169E1), // Royal Blue
            Color(0xFF9370DB)  // Medium Purple
        )
        4 -> listOf(
            Color(0xFF32CD32), // Lime Green
            Color(0xFF00FF7F), // Spring Green
            Color(0xFFADFF2F)  // Green Yellow
        )
        else -> listOf(
            Color(0xFFFF1493), // Deep Pink
            Color(0xFFFF69B4), // Hot Pink
            Color(0xFFFFC0CB)  // Pink
        )
    }
}

private fun DrawScope.drawParticle(particle: CelebrationParticle) {
    val alpha = (particle.life / particle.maxLife).coerceIn(0f, 1f) * particle.alpha
    
    when (particle.type) {
        CelebrationParticle.ParticleType.STAR -> drawStar(particle, alpha)
        CelebrationParticle.ParticleType.DIAMOND -> drawDiamond(particle, alpha)
        CelebrationParticle.ParticleType.CIRCLE -> drawCircle(particle, alpha)
        CelebrationParticle.ParticleType.SQUARE -> drawSquare(particle, alpha)
        CelebrationParticle.ParticleType.TRIANGLE -> drawTriangle(particle, alpha)
    }
}

private fun DrawScope.drawStar(particle: CelebrationParticle, alpha: Float) {
    val color = particle.color.copy(alpha = alpha)
    val center = Offset(particle.x, particle.y)
    val radius = particle.size / 2f
    
    // Draw a 5-pointed star
    val points = mutableListOf<Offset>()
    for (i in 0 until 10) {
        val angle = Math.toRadians(particle.rotation + i * 36.0)
        val r = if (i % 2 == 0) radius else radius * 0.5f
        val x = center.x + cos(angle).toFloat() * r
        val y = center.y + sin(angle).toFloat() * r
        points.add(Offset(x, y))
    }
    
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(points[0].x, points[0].y)
            for (i in 1 until points.size) {
                lineTo(points[i].x, points[i].y)
            }
            close()
        },
        color = color
    )
}

private fun DrawScope.drawDiamond(particle: CelebrationParticle, alpha: Float) {
    val color = particle.color.copy(alpha = alpha)
    val center = Offset(particle.x, particle.y)
    val size = particle.size / 2f
    
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x, center.y - size)
            lineTo(center.x + size, center.y)
            lineTo(center.x, center.y + size)
            lineTo(center.x - size, center.y)
            close()
        },
        color = color
    )
}

private fun DrawScope.drawCircle(particle: CelebrationParticle, alpha: Float) {
    drawCircle(
        color = particle.color.copy(alpha = alpha),
        center = Offset(particle.x, particle.y),
        radius = particle.size / 2f
    )
}

private fun DrawScope.drawSquare(particle: CelebrationParticle, alpha: Float) {
    drawRect(
        color = particle.color.copy(alpha = alpha),
        topLeft = Offset(particle.x - particle.size / 2f, particle.y - particle.size / 2f),
        size = androidx.compose.ui.geometry.Size(particle.size, particle.size)
    )
}

private fun DrawScope.drawTriangle(particle: CelebrationParticle, alpha: Float) {
    val color = particle.color.copy(alpha = alpha)
    val center = Offset(particle.x, particle.y)
    val size = particle.size / 2f
    
    drawPath(
        path = androidx.compose.ui.graphics.Path().apply {
            moveTo(center.x, center.y - size)
            lineTo(center.x + size, center.y + size)
            lineTo(center.x - size, center.y + size)
            close()
        },
        color = color
    )
}

private fun Math.toRadians(degrees: Double): Double = degrees * Math.PI / 180.0
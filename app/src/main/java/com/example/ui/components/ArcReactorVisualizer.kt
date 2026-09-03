package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.JarvisCyan
import com.example.ui.theme.JarvisCyanBright
import com.example.ui.theme.JarvisCyanDark
import com.example.ui.theme.JarvisGold
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ArcReactorVisualizer(
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRms: Float,
    modifier: Modifier = Modifier,
    size: Dp = 190.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reactor_rotation")

    // Outer ring rotation
    val rotationOuter by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isListening) 4000 else 12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    // Inner ring reverse rotation
    val rotationInner by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (isSpeaking) 3000 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    // Core pulsing animation
    val pulseAnim = remember { Animatable(1f) }
    LaunchedEffect(isListening, isSpeaking, audioRms) {
        val target = when {
            isListening -> 1f + (audioRms * 0.45f).coerceIn(0.1f, 0.5f)
            isSpeaking -> 1.18f
            else -> 1.0f
        }
        pulseAnim.animateTo(target, tween(120, easing = LinearEasing))
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(this.size.width / 2f, this.size.height / 2f)
            val radius = (this.size.minDimension / 2f) - 12f
            val pulse = pulseAnim.value

            // 1. Ambient Glow behind reactor
            val glowColor = when {
                isListening -> JarvisCyan.copy(alpha = 0.35f * pulse)
                isSpeaking -> JarvisGold.copy(alpha = 0.35f)
                else -> JarvisCyan.copy(alpha = 0.15f)
            }
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(glowColor, Color.Transparent),
                    center = center,
                    radius = radius * 1.3f
                ),
                radius = radius * 1.3f,
                center = center
            )

            // 2. Outermost static guide ring
            drawCircle(
                color = JarvisCyanDark.copy(alpha = 0.5f),
                radius = radius,
                center = center,
                style = Stroke(width = 2f)
            )

            // 3. Rotating Outer Segmented Power Ring
            rotate(rotationOuter, pivot = center) {
                drawOuterSegments(center, radius, isListening)
            }

            // 4. Middle Arc Reactor Coils (10 coils)
            val middleRadius = radius * 0.72f
            rotate(rotationInner, pivot = center) {
                drawCoils(center, middleRadius, isSpeaking)
            }

            // 5. Inner Core Ring
            val innerRingRadius = radius * 0.44f * pulse
            drawCircle(
                color = if (isListening) JarvisCyanBright else if (isSpeaking) JarvisGold else JarvisCyan,
                radius = innerRingRadius,
                center = center,
                style = Stroke(width = 3.5f)
            )

            // 6. Central Arc Reactor Core Triangle & Energy Pearl
            drawCoreTriangle(center, innerRingRadius * 0.65f, isListening, isSpeaking)
        }
    }
}

private fun DrawScope.drawOuterSegments(center: Offset, radius: Float, isListening: Boolean) {
    val segmentCount = 12
    val anglePerSegment = 360f / segmentCount
    val segmentLength = anglePerSegment * 0.65f
    val strokeWidth = 3.5f
    val segmentColor = if (isListening) JarvisCyanBright else JarvisCyan

    for (i in 0 until segmentCount) {
        val startAngle = i * anglePerSegment
        drawArc(
            color = segmentColor.copy(alpha = if (i % 2 == 0) 0.9f else 0.4f),
            startAngle = startAngle,
            sweepAngle = segmentLength,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawCoils(center: Offset, radius: Float, isSpeaking: Boolean) {
    val coilCount = 10
    val coilColor = if (isSpeaking) JarvisGold else JarvisCyan

    for (i in 0 until coilCount) {
        val angleRad = Math.toRadians((i * (360.0 / coilCount))).toFloat()
        val startX = center.x + (radius - 9f) * cos(angleRad)
        val startY = center.y + (radius - 9f) * sin(angleRad)
        val endX = center.x + (radius + 9f) * cos(angleRad)
        val endY = center.y + (radius + 9f) * sin(angleRad)

        drawLine(
            color = coilColor,
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = 4.5f,
            cap = StrokeCap.Round
        )
    }

    // Circular guideline
    drawCircle(
        color = coilColor.copy(alpha = 0.35f),
        radius = radius,
        center = center,
        style = Stroke(width = 1.5f)
    )
}

private fun DrawScope.drawCoreTriangle(
    center: Offset,
    size: Float,
    isListening: Boolean,
    isSpeaking: Boolean
) {
    val trianglePath = Path().apply {
        val top = Offset(center.x, center.y - size)
        val right = Offset(center.x + size * 0.866f, center.y + size * 0.5f)
        val left = Offset(center.x - size * 0.866f, center.y + size * 0.5f)
        moveTo(top.x, top.y)
        lineTo(right.x, right.y)
        lineTo(left.x, left.y)
        close()
    }

    val fillColor = when {
        isListening -> JarvisCyanBright.copy(alpha = 0.4f)
        isSpeaking -> JarvisGold.copy(alpha = 0.4f)
        else -> JarvisCyan.copy(alpha = 0.25f)
    }
    val strokeColor = when {
        isListening -> JarvisCyanBright
        isSpeaking -> JarvisGold
        else -> Color.White
    }

    drawPath(trianglePath, color = fillColor)
    drawPath(trianglePath, color = strokeColor, style = Stroke(width = 2.5f))

    // Central Energy Pearl
    drawCircle(
        color = Color.White,
        radius = size * 0.32f,
        center = center
    )
}

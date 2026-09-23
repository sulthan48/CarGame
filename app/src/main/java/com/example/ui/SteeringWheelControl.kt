package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.atan2

@Composable
fun SteeringWheelControl(
    modifier: Modifier = Modifier,
    onSteerChanged: (Float) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val wheelAngle = remember { Animatable(0f) }
    var touchCenter by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .size(150.dp)
            .testTag("steering_wheel_control")
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        touchCenter = Offset(size.width / 2f, size.height / 2f)
                        val angle = Math.toDegrees(
                            atan2((offset.y - touchCenter.y).toDouble(), (offset.x - touchCenter.x).toDouble())
                        ).toFloat()
                        val clamped = (angle - 90f).coerceIn(-140f, 140f)
                        coroutineScope.launch {
                            wheelAngle.snapTo(clamped)
                            onSteerChanged((clamped / 140f).coerceIn(-1f, 1f))
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        val dx = (pos.x - touchCenter.x).toDouble()
                        val dy = (pos.y - touchCenter.y).toDouble()
                        var angle = Math.toDegrees(atan2(dy, dx)).toFloat() - 90f
                        if (angle < -180f) angle += 360f
                        if (angle > 180f) angle -= 360f
                        val clamped = angle.coerceIn(-140f, 140f)
                        coroutineScope.launch {
                            wheelAngle.snapTo(clamped)
                            onSteerChanged((clamped / 140f).coerceIn(-1f, 1f))
                        }
                    },
                    onDragEnd = {
                        // Smoothly spring return to center (0 degrees)
                        coroutineScope.launch {
                            wheelAngle.animateTo(0f, animationSpec = tween(180))
                            onSteerChanged(0f)
                        }
                    },
                    onDragCancel = {
                        coroutineScope.launch {
                            wheelAngle.animateTo(0f, animationSpec = tween(180))
                            onSteerChanged(0f)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 10.dp.toPx()

            rotate(wheelAngle.value, pivot = center) {
                // Outer Rim Grip
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2A2E39), Color(0xFF131720), Color(0xFF090B0E)),
                        center = center,
                        radius = radius + 8.dp.toPx()
                    ),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )

                // Top Red Racing Center Line
                drawLine(
                    color = Color(0xFFE50914),
                    start = Offset(center.x, center.y - radius - 9.dp.toPx()),
                    end = Offset(center.x, center.y - radius + 9.dp.toPx()),
                    strokeWidth = 6.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // 3 Metal Spokes (Left, Right, Bottom)
                val spokeColor = Color(0xFF5A6275)
                val spokeStroke = 8.dp.toPx()

                // Left spoke
                drawLine(
                    color = spokeColor,
                    start = center,
                    end = Offset(center.x - radius * 0.88f, center.y + 4.dp.toPx()),
                    strokeWidth = spokeStroke,
                    cap = StrokeCap.Round
                )
                // Right spoke
                drawLine(
                    color = spokeColor,
                    start = center,
                    end = Offset(center.x + radius * 0.88f, center.y + 4.dp.toPx()),
                    strokeWidth = spokeStroke,
                    cap = StrokeCap.Round
                )
                // Bottom spoke
                drawLine(
                    color = spokeColor,
                    start = center,
                    end = Offset(center.x, center.y + radius * 0.88f),
                    strokeWidth = spokeStroke,
                    cap = StrokeCap.Round
                )

                // Center Horn Button / Hub
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF323846), Color(0xFF1A1D24)),
                        center = center,
                        radius = 24.dp.toPx()
                    ),
                    radius = 24.dp.toPx(),
                    center = center
                )
                // Yellow Shield / Logo
                drawCircle(
                    color = Color(0xFFFFB300),
                    radius = 8.dp.toPx(),
                    center = center
                )
            }
        }
    }
}

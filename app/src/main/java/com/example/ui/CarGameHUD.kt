package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.renderer.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CarGameHUD(
    uiState: GameUIState,
    onThrottle: (Float) -> Unit,
    onBrake: (Float) -> Unit,
    onSteer: (Float) -> Unit,
    onHandbrake: (Boolean) -> Unit,
    onBoost: (Boolean) -> Unit,
    onSwitchCamera: () -> Unit,
    onToggleTimeOfDay: () -> Unit,
    onToggleWeather: () -> Unit,
    onToggleHeadlights: () -> Unit,
    onToggleAbs: () -> Unit,
    onToggleTcs: () -> Unit,
    onShiftUp: () -> Unit,
    onShiftDown: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenSettings: () -> Unit,
    onRepair: () -> Unit,
    onReset: () -> Unit,
    onToggleMute: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        // 1. TOP STATUS & CONTROLS BAR
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Top: Car Info & Health
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .background(Color(0x9910141E), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "Car",
                    tint = Color(0xFFFFD54F),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = uiState.cameraMode.name.replace("_", " "),
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                // Health & Damage indicator
                if (uiState.damagePercent > 5f) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "DMG ${uiState.damagePercent.toInt()}%",
                        color = if (uiState.damagePercent > 50f) Color(0xFFFF5252) else Color(0xFFFFB74D),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(
                        onClick = onRepair,
                        modifier = Modifier
                            .size(24.dp)
                            .testTag("repair_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = "Repair",
                            tint = Color(0xFF64B5F6),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Keyboard Controls Active badge
                if (uiState.isKeyboardActive) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        modifier = Modifier
                            .background(Color(0xFF00E5FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                            .testTag("keyboard_active_badge")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "Keyboard Active",
                            tint = Color.Black,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            text = "WASD",
                            color = Color.Black,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            // Top Center: Drift Banner
            AnimatedVisibility(
                visible = uiState.driftScore > 10f || uiState.isDrifting,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFFFF3D00), Color(0xFFFF9100))),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 14.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "🔥 DRIFT ${uiState.driftScore.toInt()}  x${"%.1f".format(uiState.driftMultiplier)}",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Top Right: Quick Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Camera Switch
                HudIconButton(
                    icon = Icons.Default.Videocam,
                    label = "Cam",
                    testTag = "camera_button",
                    onClick = onSwitchCamera
                )

                // Time of Day
                HudIconButton(
                    icon = when (uiState.timeOfDay) {
                        TimeOfDay.DAY -> Icons.Default.WbSunny
                        TimeOfDay.SUNSET -> Icons.Default.Brightness6
                        TimeOfDay.NIGHT -> Icons.Default.NightlightRound
                    },
                    label = uiState.timeOfDay.name,
                    testTag = "time_button",
                    onClick = onToggleTimeOfDay
                )

                // Weather
                HudIconButton(
                    icon = when (uiState.weatherMode) {
                        WeatherMode.CLEAR -> Icons.Default.CloudQueue
                        WeatherMode.RAIN -> Icons.Default.WaterDrop
                        WeatherMode.FOG -> Icons.Default.FilterDrama
                    },
                    label = uiState.weatherMode.name,
                    testTag = "weather_button",
                    onClick = onToggleWeather
                )

                // Headlights
                HudIconButton(
                    icon = if (uiState.headlightsOn) Icons.Default.Highlight else Icons.Outlined.Highlight,
                    label = "Light",
                    tint = if (uiState.headlightsOn) Color(0xFF80D8FF) else Color.White,
                    testTag = "headlights_button",
                    onClick = onToggleHeadlights
                )

                // Sound Mute
                HudIconButton(
                    icon = if (uiState.isMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                    label = "Sound",
                    testTag = "sound_button",
                    onClick = onToggleMute
                )

                // Reset Vehicle
                HudIconButton(
                    icon = Icons.Default.Refresh,
                    label = "Reset",
                    testTag = "reset_button",
                    onClick = onReset
                )

                // Garage
                HudIconButton(
                    icon = Icons.Default.Tune,
                    label = "Garage",
                    tint = Color(0xFFFFD54F),
                    testTag = "garage_button",
                    onClick = onOpenGarage
                )
            }
        }

        // 2. CENTER-BOTTOM INSTRUMENT CLUSTER (Speedometer, Tachometer, Gear, ABS/TCS)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        ) {
            InstrumentCluster(
                speedKmh = uiState.speedKmh,
                rpm = uiState.rpm,
                maxRpm = uiState.maxRpm,
                gear = uiState.currentGear,
                turboBoost = uiState.turboBoost,
                absActive = uiState.absActive,
                tcsActive = uiState.tcsActive,
                absEnabled = uiState.absEnabled,
                tcsEnabled = uiState.tcsEnabled,
                isAutomatic = uiState.isAutomatic,
                onToggleAbs = onToggleAbs,
                onToggleTcs = onToggleTcs,
                onShiftUp = onShiftUp,
                onShiftDown = onShiftDown
            )
        }

        // 3. BOTTOM-LEFT: STEERING CONTROLS (Steering Wheel OR Arrows)
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 8.dp)
        ) {
            when (uiState.controlType) {
                ControlType.STEERING_WHEEL -> {
                    SteeringWheelControl(onSteerChanged = onSteer)
                }
                ControlType.ARROWS -> {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ArrowPedalButton(
                            label = "◀",
                            testTag = "steer_left_button",
                            onPressed = { onSteer(-1f) },
                            onReleased = { onSteer(0f) }
                        )
                        ArrowPedalButton(
                            label = "▶",
                            testTag = "steer_right_button",
                            onPressed = { onSteer(1f) },
                            onReleased = { onSteer(0f) }
                        )
                    }
                }
                ControlType.TILT -> {
                    Box(
                        modifier = Modifier
                            .background(Color(0x9910141E), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "📲 TILT TO STEER",
                            color = Color(0xFF64B5F6),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 4. BOTTOM-RIGHT: DRIVING PEDALS (Brake, Handbrake, Boost, Throttle)
        Row(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // Handbrake & Boost column
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Boost / NOS Button
                HoldButton(
                    label = "⚡ NOS",
                    color = Color(0xFF00E5FF),
                    testTag = "boost_button",
                    width = 72.dp,
                    height = 54.dp,
                    onStateChange = onBoost
                )

                // Handbrake Button
                HoldButton(
                    label = "(P) DRIFT",
                    color = Color(0xFFFF5252),
                    testTag = "handbrake_button",
                    width = 72.dp,
                    height = 54.dp,
                    onStateChange = onHandbrake
                )
            }

            // Brake / Reverse Pedal
            Pedal(
                label = "BRAKE\nREV",
                color = Color(0xFFD32F2F),
                testTag = "brake_pedal",
                width = 70.dp,
                height = 135.dp,
                onValueChange = onBrake
            )

            // Throttle / Accelerate Pedal
            Pedal(
                label = "GAS",
                color = Color(0xFF00C853),
                testTag = "throttle_pedal",
                width = 80.dp,
                height = 150.dp,
                onValueChange = onThrottle
            )
        }
    }
}

@Composable
fun HudIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    testTag: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(38.dp)
            .background(Color(0x9910141E), CircleShape)
            .border(1.dp, Color(0x33FFFFFF), CircleShape)
            .testTag(testTag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun HoldButton(
    label: String,
    color: Color,
    testTag: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onStateChange: (Boolean) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isPressed) color else color.copy(alpha = 0.35f))
            .border(1.5.dp, color, RoundedCornerShape(12.dp))
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onStateChange(true)
                        tryAwaitRelease()
                        isPressed = false
                        onStateChange(false)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isPressed) Color.Black else Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun ArrowPedalButton(
    label: String,
    testTag: String,
    onPressed: () -> Unit,
    onReleased: () -> Unit
) {
    var isDown by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(68.dp)
            .clip(CircleShape)
            .background(if (isDown) Color(0xFF00E5FF) else Color(0xAA161C28))
            .border(2.dp, Color(0xFF00E5FF), CircleShape)
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isDown = true
                        onPressed()
                        tryAwaitRelease()
                        isDown = false
                        onReleased()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (isDown) Color.Black else Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
    }
}

@Composable
fun Pedal(
    label: String,
    color: Color,
    testTag: String,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onValueChange: (Float) -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(width, height)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (isPressed) color.copy(alpha = 0.95f) else Color(0xDD1B2232),
                        if (isPressed) color.copy(alpha = 0.75f) else Color(0xDD0D111A)
                    )
                )
            )
            .border(2.dp, if (isPressed) color else Color(0x44FFFFFF), RoundedCornerShape(12.dp))
            .testTag(testTag)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        onValueChange(1.0f)
                        tryAwaitRelease()
                        isPressed = false
                        onValueChange(0.0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Pedal grip treads
            repeat(4) {
                Box(
                    modifier = Modifier
                        .width(width * 0.65f)
                        .height(3.dp)
                        .background(if (isPressed) Color.White.copy(alpha = 0.8f) else Color.White.copy(alpha = 0.25f), RoundedCornerShape(2.dp))
                        .padding(vertical = 1.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Text(
                text = label,
                color = if (isPressed) Color.White else color,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp
            )
        }
    }
}

@Composable
fun InstrumentCluster(
    speedKmh: Float,
    rpm: Float,
    maxRpm: Float,
    gear: Int,
    turboBoost: Float,
    absActive: Boolean,
    tcsActive: Boolean,
    absEnabled: Boolean,
    tcsEnabled: Boolean,
    isAutomatic: Boolean,
    onToggleAbs: () -> Unit,
    onToggleTcs: () -> Unit,
    onShiftUp: () -> Unit,
    onShiftDown: () -> Unit
) {
    val gearLabel = when (gear) {
        -1 -> "R"
        0 -> "N"
        else -> gear.toString()
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .background(Color(0xBB0D121D), RoundedCornerShape(20.dp))
            .border(1.5.dp, Color(0x33FFFFFF), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Paddle Down (Manual mode)
        if (!isAutomatic) {
            IconButton(
                onClick = onShiftDown,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0x44FFFFFF), CircleShape)
                    .testTag("paddle_down")
            ) {
                Text("-", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Circular Speedometer Arc
        Box(
            modifier = Modifier.size(90.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = size.minDimension / 2f - 8.dp.toPx()
                val startAngle = 135f
                val sweepTotal = 270f
                val speedFraction = (speedKmh / 360f).coerceIn(0f, 1f)

                // Background track
                drawArc(
                    color = Color(0x33FFFFFF),
                    startAngle = startAngle,
                    sweepAngle = sweepTotal,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )

                // Speed fill arc
                val speedColor = if (speedKmh > 260f) Color(0xFFFF1744)
                else if (speedKmh > 160f) Color(0xFFFF9100)
                else Color(0xFF00E5FF)

                drawArc(
                    color = speedColor,
                    startAngle = startAngle,
                    sweepAngle = sweepTotal * speedFraction,
                    useCenter = false,
                    style = Stroke(width = 7.dp.toPx(), cap = StrokeCap.Round)
                )

                // Needle
                val needleAngle = startAngle + sweepTotal * speedFraction
                val needleRad = Math.toRadians(needleAngle.toDouble())
                val needleEnd = Offset(
                    center.x + (radius - 4.dp.toPx()) * cos(needleRad).toFloat(),
                    center.y + (radius - 4.dp.toPx()) * sin(needleRad).toFloat()
                )
                drawLine(
                    color = Color.White,
                    start = center,
                    end = needleEnd,
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = speedKmh.toInt().toString(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    text = "KM/H",
                    color = Color(0xFF80D8FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Center: Gear & RPM Bar & Assists
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Gear Display
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(Color(0xFFE50914), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = gearLabel,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // RPM Linear Bar
            val rpmFrac = (rpm / maxRpm).coerceIn(0f, 1f)
            val rpmColor = if (rpmFrac > 0.88f) Color(0xFFFF1744) else if (rpmFrac > 0.75f) Color(0xFFFFC400) else Color(0xFF00E676)

            Box(
                modifier = Modifier
                    .width(76.dp)
                    .height(6.dp)
                    .background(Color(0x44FFFFFF), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(rpmFrac)
                        .background(rpmColor, RoundedCornerShape(3.dp))
                )
            }

            // ABS & TCS Indicator Lamps
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AssistPill(
                    label = "ABS",
                    enabled = absEnabled,
                    active = absActive,
                    onClick = onToggleAbs
                )
                AssistPill(
                    label = "TCS",
                    enabled = tcsEnabled,
                    active = tcsActive,
                    onClick = onToggleTcs
                )
            }
        }

        // Paddle Up (Manual mode)
        if (!isAutomatic) {
            IconButton(
                onClick = onShiftUp,
                modifier = Modifier
                    .size(34.dp)
                    .background(Color(0x44FFFFFF), CircleShape)
                    .testTag("paddle_up")
            ) {
                Text("+", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AssistPill(
    label: String,
    enabled: Boolean,
    active: Boolean,
    onClick: () -> Unit
) {
    val bgColor = when {
        active -> Color(0xFFFF9100)
        enabled -> Color(0x4400E676)
        else -> Color(0x33FFFFFF)
    }
    val textColor = when {
        active -> Color.Black
        enabled -> Color(0xFF00E676)
        else -> Color(0x66FFFFFF)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { onClick() })
            }
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontSize = 9.sp,
            fontWeight = FontWeight.Black
        )
    }
}

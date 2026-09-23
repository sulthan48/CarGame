package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.physics.CarConfig
import com.example.renderer.GameRenderer

@Composable
fun GarageScreen(
    uiState: GameUIState,
    renderer: GameRenderer,
    onSelectCar: (Int) -> Unit,
    onSelectColor: (Long) -> Unit,
    onSelectEngineStage: (Int) -> Unit,
    onSelectSuspensionPreset: (Int) -> Unit,
    onSelectTireCompound: (Int) -> Unit,
    onSelectControlType: (ControlType) -> Unit,
    onToggleTransmission: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentCar = CarConfig.ALL_CARS[uiState.selectedCarIndex]

    val colorPalette = listOf(
        0xFFE50914L, // Crimson Red
        0xFF00E5FFL, // Cyber Cyan
        0xFFFFB300L, // Racing Gold
        0xFF1A1D24L, // Stealth Black
        0xFF2E7D32L, // Forest Green
        0xFFF5F5F5L, // Pearl White
        0xFF7B1FA2L, // Midnight Violet
        0xFFFF5722L  // Sunset Orange
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xE60A0D14))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "GARAGE & TUNING",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = "${currentCar.name} • ${currentCar.category}",
                        color = Color(0xFFFFD54F),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0x33FFFFFF), CircleShape)
                        .testTag("close_garage_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // Middle Content: 3-column layout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Column 1: Car Selection & Specs
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "SELECT VEHICLE",
                            color = Color(0xFF80D8FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        CarConfig.ALL_CARS.forEachIndexed { index, car ->
                            val isSelected = index == uiState.selectedCarIndex
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) Color(0xFFE50914) else Color(0x22FFFFFF))
                                    .border(1.dp, if (isSelected) Color.White else Color.Transparent, RoundedCornerShape(10.dp))
                                    .clickable { onSelectCar(index) }
                                    .padding(horizontal = 10.dp, vertical = 7.dp)
                                    .testTag("car_item_$index")
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = car.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${car.maxPowerHp.toInt()} HP • ${car.driveType}",
                                            color = if (isSelected) Color(0xFFFFD54F) else Color(0xFFAAAAAA),
                                            fontSize = 11.sp
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Stats overview
                        StatBar("Top Speed", "${currentCar.topSpeedKmh.toInt()} km/h", currentCar.topSpeedKmh / 360f)
                        StatBar("Horsepower", "${currentCar.maxPowerHp.toInt()} HP", currentCar.maxPowerHp / 800f)
                        StatBar("Weight", "${currentCar.massKg.toInt()} kg", (2000f - currentCar.massKg) / 1000f)
                    }
                }

                // Column 2: Custom Paint & Performance Upgrades
                Card(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "PAINT COLOR",
                            color = Color(0xFF80D8FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Color swatches
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            colorPalette.forEach { colorHex ->
                                val isSelected = uiState.customPaintColor == colorHex
                                val color = Color(colorHex)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(2.dp, if (isSelected) Color.White else Color(0x33FFFFFF), CircleShape)
                                        .clickable { onSelectColor(colorHex) }
                                        .testTag("color_${colorHex.toString(16)}")
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = if (colorHex == 0xFFF5F5F5L) Color.Black else Color.White,
                                            modifier = Modifier
                                                .size(16.dp)
                                                .align(Alignment.Center)
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = Color(0x33FFFFFF), thickness = 1.dp)

                        Text(
                            text = "PERFORMANCE TUNING",
                            color = Color(0xFF80D8FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Engine Stage
                        TuningSelector(
                            title = "Engine ECU Tune",
                            options = listOf("Stock", "Stage 1", "Stage 2 (+35% HP)"),
                            selectedIndex = uiState.engineStage - 1,
                            onSelect = { onSelectEngineStage(it + 1) }
                        )

                        // Suspension
                        TuningSelector(
                            title = "Suspension Setup",
                            options = listOf("Comfort", "Sport", "Track Stiff"),
                            selectedIndex = uiState.suspensionPreset,
                            onSelect = onSelectSuspensionPreset
                        )

                        // Tires
                        TuningSelector(
                            title = "Tire Compound",
                            options = listOf("Street", "Sport Grip", "Drift Slicks"),
                            selectedIndex = uiState.tireCompound,
                            onSelect = onSelectTireCompound
                        )
                    }
                }

                // Column 3: Controls & Transmission
                Card(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF141923)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "CONTROLS & TRANSMISSION",
                            color = Color(0xFF80D8FF),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )

                        // Steering Control mode
                        TuningSelector(
                            title = "Steering Mode",
                            options = listOf("Wheel", "Buttons", "Tilt"),
                            selectedIndex = when (uiState.controlType) {
                                ControlType.STEERING_WHEEL -> 0
                                ControlType.ARROWS -> 1
                                ControlType.TILT -> 2
                            },
                            onSelect = {
                                val mode = when (it) {
                                    0 -> ControlType.STEERING_WHEEL
                                    1 -> ControlType.ARROWS
                                    else -> ControlType.TILT
                                }
                                onSelectControlType(mode)
                            }
                        )

                        // Transmission mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Gearbox Mode",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Button(
                                onClick = onToggleTransmission,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isAutomatic) Color(0xFF0288D1) else Color(0xFFE50914)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("transmission_toggle")
                            ) {
                                Text(
                                    text = if (uiState.isAutomatic) "AUTOMATIC" else "MANUAL (+/-)",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        }

                        // Keyboard Shortcuts Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x2200E5FF), RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0x4400E5FF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "⌨️ KEYBOARD SUPPORT",
                                    color = Color(0xFF80D8FF),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "W / ⬆ : Throttle  •  S / ⬇ : Progressive Brake\nA / D : Steer  •  Space : Drift  •  Shift : NOS",
                                    color = Color(0xFFDDDDDD),
                                    fontSize = 9.sp,
                                    lineHeight = 12.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        // Large DRIVE NOW button
                        Button(
                            onClick = onClose,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("drive_now_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "DRIVE NOW",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatBar(label: String, valueStr: String, fraction: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, color = Color(0xFFAAAAAA), fontSize = 10.sp)
            Text(text = valueStr, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(3.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .background(Color(0x33FFFFFF), RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction.coerceIn(0.05f, 1f))
                    .background(Color(0xFF00E5FF), RoundedCornerShape(3.dp))
            )
        }
    }
}

@Composable
fun TuningSelector(
    title: String,
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, color = Color(0xFFAAAAAA), fontSize = 11.sp)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEachIndexed { idx, opt ->
                val isSel = idx == selectedIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) Color(0xFF00E5FF) else Color(0x22FFFFFF))
                        .clickable { onSelect(idx) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = opt,
                        color = if (isSel) Color.Black else Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

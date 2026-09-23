package com.example.physics

import kotlin.math.abs
import kotlin.random.Random

enum class TrafficVehicleType {
    SEDAN,
    TRUCK,
    AUTO_RICKSHAW,
    SUV
}

data class TrafficVehicle(
    val id: Int,
    val type: TrafficVehicleType,
    var posX: Float,
    var posZ: Float,
    var speedMps: Float,
    val laneIndex: Int,
    val colorHex: Long,
    var brakeLightsOn: Boolean = false,
    var hitByPlayer: Boolean = false
)

class TrafficSystem {
    // 4 lanes: lane 0: -5.2m, lane 1: -1.8m, lane 2: 1.8m, lane 3: 5.2m
    private val laneXPositions = floatArrayOf(-5.2f, -1.8f, 1.8f, 5.2f)
    val vehicles = mutableListOf<TrafficVehicle>()
    private var nextId = 1

    init {
        // Pre-populate some traffic ahead
        for (i in 0 until 12) {
            spawnVehicle(aheadDistance = 40f + i * 45f)
        }
    }

    private fun spawnVehicle(aheadDistance: Float) {
        val lane = Random.nextInt(4)
        val x = laneXPositions[lane]
        val type = when (Random.nextInt(4)) {
            0 -> TrafficVehicleType.SEDAN
            1 -> TrafficVehicleType.TRUCK
            2 -> TrafficVehicleType.AUTO_RICKSHAW
            else -> TrafficVehicleType.SUV
        }
        val speedKmh = when (type) {
            TrafficVehicleType.TRUCK -> Random.nextFloat() * 20f + 60f
            TrafficVehicleType.AUTO_RICKSHAW -> Random.nextFloat() * 15f + 45f
            TrafficVehicleType.SEDAN -> Random.nextFloat() * 30f + 85f
            TrafficVehicleType.SUV -> Random.nextFloat() * 25f + 90f
        }
        val colors = listOf(
            0xFFFFFFFFL, // White
            0xFF212121L, // Black
            0xFF1976D2L, // Blue
            0xFFC62828L, // Red
            0xFFF57F17L, // Yellow/Amber
            0xFF757575L  // Silver
        )

        vehicles.add(
            TrafficVehicle(
                id = nextId++,
                type = type,
                posX = x,
                posZ = aheadDistance,
                speedMps = speedKmh / 3.6f,
                laneIndex = lane,
                colorHex = colors.random()
            )
        )
    }

    fun update(playerZ: Float, playerX: Float, dt: Float, onCollision: () -> Unit) {
        val it = vehicles.iterator()
        while (it.hasNext()) {
            val v = it.next()
            v.posZ += v.speedMps * dt

            // Check collision with player
            val dz = abs(v.posZ - playerZ)
            val dx = abs(v.posX - playerX)
            val vehicleLength = if (v.type == TrafficVehicleType.TRUCK) 7.5f else 4.2f
            val vehicleWidth = if (v.type == TrafficVehicleType.TRUCK) 2.4f else 1.9f

            if (dz < (vehicleLength * 0.5f + 2.1f) && dx < (vehicleWidth * 0.5f + 0.95f)) {
                if (!v.hitByPlayer) {
                    v.hitByPlayer = true
                    v.speedMps *= 0.5f
                    onCollision()
                }
            }

            // Remove vehicles that are too far behind or ahead
            if (v.posZ < playerZ - 80f) {
                it.remove()
            }
        }

        // Maintain traffic density ahead of player
        val furthestZ = vehicles.maxOfOrNull { it.posZ } ?: playerZ
        if (furthestZ < playerZ + 450f && vehicles.size < 16) {
            spawnVehicle(furthestZ + Random.nextFloat() * 40f + 25f)
        }
    }

    fun reset(playerZ: Float) {
        vehicles.clear()
        for (i in 0 until 12) {
            spawnVehicle(aheadDistance = playerZ + 50f + i * 45f)
        }
    }
}

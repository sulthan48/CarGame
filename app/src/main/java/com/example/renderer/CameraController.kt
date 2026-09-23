package com.example.renderer

import com.example.physics.CarPhysics
import kotlin.math.*
import kotlin.random.Random

enum class CameraMode {
    THIRD_PERSON,
    FIRST_PERSON_COCKPIT,
    HOOD
}

class CameraController {
    var mode = CameraMode.THIRD_PERSON

    // Camera world position
    var camX = 0f
    var camY = 3f
    var camZ = -7f

    // Camera look target
    var targetX = 0f
    var targetY = 1f
    var targetZ = 5f

    // Up vector
    var upX = 0f
    var upY = 1f
    var upZ = 0f

    // Dynamic FOV
    var fov = 60f

    fun switchMode(): CameraMode {
        mode = when (mode) {
            CameraMode.THIRD_PERSON -> CameraMode.FIRST_PERSON_COCKPIT
            CameraMode.FIRST_PERSON_COCKPIT -> CameraMode.HOOD
            CameraMode.HOOD -> CameraMode.THIRD_PERSON
        }
        return mode
    }

    fun update(car: CarPhysics, dt: Float) {
        val safeDt = dt.coerceIn(0.001f, 0.05f)
        val speed = car.speedKmh
        val sinYaw = sin(car.yaw)
        val cosYaw = cos(car.yaw)

        // Camera shake on high speed or collisions
        val shakeAmount = (car.collisionImpulse * 0.25f) + if (speed > 200f) (speed - 200f) * 0.0003f else 0f
        val shakeX = if (shakeAmount > 0f) (Random.nextFloat() * 2f - 1f) * shakeAmount else 0f
        val shakeY = if (shakeAmount > 0f) (Random.nextFloat() * 2f - 1f) * shakeAmount else 0f

        when (mode) {
            CameraMode.THIRD_PERSON -> {
                // Chase camera behind and above car
                val distBehind = 6.2f + (speed * 0.012f)
                val heightAbove = 2.2f + (speed * 0.003f)

                val desiredCamX = car.posX - sinYaw * distBehind + shakeX
                val desiredCamY = car.posY + heightAbove + shakeY
                val desiredCamZ = car.posZ - cosYaw * distBehind

                // Smooth lag/spring interpolation
                val lagSpeed = 12f
                camX += (desiredCamX - camX) * safeDt * lagSpeed
                camY += (desiredCamY - camY) * safeDt * lagSpeed
                camZ += (desiredCamZ - camZ) * safeDt * lagSpeed

                // Look at point slightly ahead of car
                val lookAheadDist = 6.0f + (speed * 0.02f)
                val desiredTargetX = car.posX + sinYaw * lookAheadDist
                val desiredTargetY = car.posY + 1.1f
                val desiredTargetZ = car.posZ + cosYaw * lookAheadDist

                targetX += (desiredTargetX - targetX) * safeDt * 15f
                targetY += (desiredTargetY - targetY) * safeDt * 15f
                targetZ += (desiredTargetZ - targetZ) * safeDt * 15f

                // Dynamic FOV expands at higher speeds
                val targetFov = (60f + (speed / 300f) * 16f).coerceIn(60f, 80f)
                fov += (targetFov - fov) * safeDt * 5f

                upX = 0f; upY = 1f; upZ = 0f
            }

            CameraMode.FIRST_PERSON_COCKPIT -> {
                // Driver eye position inside cabin (left seat)
                // Local offset: driver seat is at x = -0.38m, y = 0.88m, z = -0.22m
                val driverLocalX = -0.36f
                val driverLocalY = 0.85f + car.pitch * 0.5f - car.roll * 0.3f
                val driverLocalZ = -0.20f

                // Transform driver eye to world coordinates
                camX = car.posX + (cosYaw * driverLocalX + sinYaw * driverLocalZ) + shakeX * 0.5f
                camY = car.posY + driverLocalY + shakeY * 0.5f
                camZ = car.posZ + (-sinYaw * driverLocalX + cosYaw * driverLocalZ)

                // Look through windshield ahead
                val lookDist = 20.0f
                targetX = camX + sinYaw * lookDist
                targetY = camY + car.pitch * 2.0f
                targetZ = camZ + cosYaw * lookDist

                // Subtle head tilt with body roll
                upX = -sin(car.roll)
                upY = cos(car.roll)
                upZ = 0f

                fov = 68f
            }

            CameraMode.HOOD -> {
                // Hood / Bumper camera at the front of the car
                val hoodOffsetZ = 1.8f
                val hoodOffsetY = 0.65f

                camX = car.posX + sinYaw * hoodOffsetZ + shakeX * 0.3f
                camY = car.posY + hoodOffsetY + shakeY * 0.3f
                camZ = car.posZ + cosYaw * hoodOffsetZ

                val lookDist = 25.0f
                targetX = camX + sinYaw * lookDist
                targetY = camY + car.pitch * 2.5f
                targetZ = camZ + cosYaw * lookDist

                upX = 0f; upY = 1f; upZ = 0f
                fov = 72f
            }
        }
    }
}

package com.example.physics

import androidx.compose.ui.graphics.Color

enum class CarBodyType {
    SUPERCAR,
    MUSCLE,
    OFFROAD,
    DRIFT_GT
}

enum class DriveType {
    RWD,
    AWD,
    FWD
}

data class CarConfig(
    val id: String,
    val name: String,
    val category: String,
    val bodyType: CarBodyType,
    val massKg: Float = 1420f,
    val maxPowerHp: Float = 650f,
    val maxTorqueNm: Float = 750f,
    val idleRpm: Float = 850f,
    val redlineRpm: Float = 8500f,
    val gearRatios: List<Float> = listOf(-3.5f, 3.82f, 2.36f, 1.68f, 1.31f, 1.00f, 0.79f), // [0]=R, 1..6
    val finalDriveRatio: Float = 3.65f,
    val wheelRadius: Float = 0.34f,
    val wheelbase: Float = 2.65f,
    val trackWidth: Float = 1.85f,
    val maxSteerAngleDeg: Float = 35f,
    val steerSpeedDegPerSec: Float = 180f,
    val brakeTorqueMax: Float = 4200f,
    val handbrakeTorque: Float = 5500f,
    val suspensionRestLength: Float = 0.35f,
    val suspensionStiffness: Float = 36000f,
    val suspensionDamping: Float = 3800f,
    val driveType: DriveType = DriveType.AWD,
    val primaryColor: Long = 0xFFD32F2FL, // Crimson Red
    val accentColor: Long = 0xFF1E1E1EL,
    val topSpeedKmh: Float = 330f
) {
    companion object {
        val ALL_CARS = listOf(
            CarConfig(
                id = "apex_gt",
                name = "Apex GT-V10",
                category = "Supercar",
                bodyType = CarBodyType.SUPERCAR,
                massKg = 1380f,
                maxPowerHp = 710f,
                maxTorqueNm = 780f,
                driveType = DriveType.AWD,
                primaryColor = 0xFFE50914L,
                topSpeedKmh = 345f
            ),
            CarConfig(
                id = "thunder_v8",
                name = "Thunder V8",
                category = "Muscle Car",
                bodyType = CarBodyType.MUSCLE,
                massKg = 1680f,
                maxPowerHp = 620f,
                maxTorqueNm = 840f,
                driveType = DriveType.RWD,
                primaryColor = 0xFFF9A825L, // Amber Yellow
                topSpeedKmh = 295f
            ),
            CarConfig(
                id = "thar_beast",
                name = "Safari Thar 4x4",
                category = "Indian Rugged SUV",
                bodyType = CarBodyType.OFFROAD,
                massKg = 1850f,
                maxPowerHp = 420f,
                maxTorqueNm = 620f,
                driveType = DriveType.AWD,
                suspensionRestLength = 0.45f,
                suspensionStiffness = 28000f,
                primaryColor = 0xFF2E7D32L, // Forest Green
                topSpeedKmh = 220f
            ),
            CarConfig(
                id = "drift_spec",
                name = "Kaze GT-R Drift",
                category = "Tuner Drift",
                bodyType = CarBodyType.DRIFT_GT,
                massKg = 1290f,
                maxPowerHp = 580f,
                maxTorqueNm = 670f,
                driveType = DriveType.RWD,
                maxSteerAngleDeg = 44f,
                primaryColor = 0xFF0288D1L, // Electric Blue
                topSpeedKmh = 310f
            )
        )
    }
}

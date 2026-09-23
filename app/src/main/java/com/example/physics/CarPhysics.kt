package com.example.physics

import kotlin.math.*

class CarPhysics(var config: CarConfig = CarConfig.ALL_CARS.first()) {

    // World state
    var posX = 0f
    var posY = 0.35f
    var posZ = 0f

    // Orientation in radians
    var yaw = 0f       // Heading (0 is along +Z axis, or along road direction)
    var pitch = 0f     // Forward/backward tilt (dive/squat)
    var roll = 0f      // Lateral tilt (body roll)

    // Velocities (m/s)
    var velForward = 0f
    var velLateral = 0f
    var velVertical = 0f
    var yawRate = 0f    // rad/s

    // Engine & Transmission
    var rpm = config.idleRpm
    var currentGear = 1 // -1: Reverse, 0: Neutral, 1..6: Forward
    var isAutomatic = true
    var clutchEngaged = 1.0f
    var shiftTimer = 0f
    var turboBoost = 0f // 0.0 to 1.0

    // Steering
    var steerAngleDeg = 0f

    // Driving aids
    var absEnabled = true
    var tcsEnabled = true
    var absActive = false
    var tcsActive = false

    // Wheels & Slip
    var frontSlipAngle = 0f
    var rearSlipAngle = 0f
    var frontWheelRpm = 0f
    var rearWheelRpm = 0f
    var isDrifting = false
    var driftAngle = 0f
    var driftScore = 0f
    var currentDriftMultiplier = 1f
    var driftComboTime = 0f

    // Suspension 4 corners (displacement from rest in meters)
    var suspFL = 0f
    var suspFR = 0f
    var suspRL = 0f
    var suspRR = 0f

    // Damage & Collision
    var damagePercent = 0f
    var collisionImpulse = 0f

    // Inputs (from touch/tilt)
    var inputThrottle = 0f   // 0 to 1
    var inputBrake = 0f      // 0 to 1
    var inputSteer = 0f      // -1 (left) to +1 (right)
    var inputHandbrake = false
    var inputBoost = false

    // Tuning multipliers
    var engineTuneMultiplier = 1.0f
    var suspensionStiffnessMultiplier = 1.0f
    var tireGripMultiplier = 1.0f

    val speedKmh: Float
        get() = abs(velForward) * 3.6f

    fun reset(startZ: Float = 0f, startX: Float = 0f) {
        posX = startX
        posY = config.suspensionRestLength
        posZ = startZ
        yaw = 0f
        pitch = 0f
        roll = 0f
        velForward = 0f
        velLateral = 0f
        velVertical = 0f
        yawRate = 0f
        rpm = config.idleRpm
        currentGear = 1
        steerAngleDeg = 0f
        damagePercent = 0f
        driftScore = 0f
        turboBoost = 0f
    }

    fun repair() {
        damagePercent = 0f
        collisionImpulse = 0f
    }

    fun shiftUp() {
        if (currentGear < config.gearRatios.size - 1) {
            currentGear++
            shiftTimer = 0.15f
        }
    }

    fun shiftDown() {
        if (currentGear > -1) {
            currentGear--
            shiftTimer = 0.15f
        }
    }

    fun update(dt: Float) {
        val safeDt = dt.coerceIn(0.001f, 0.05f)

        // 1. Steering dynamics (smooth interpolation towards target)
        val targetSteerDeg = inputSteer * config.maxSteerAngleDeg
        val steerDelta = config.steerSpeedDegPerSec * safeDt
        steerAngleDeg = if (abs(targetSteerDeg - steerAngleDeg) <= steerDelta) {
            targetSteerDeg
        } else {
            steerAngleDeg + sign(targetSteerDeg - steerAngleDeg) * steerDelta
        }
        val steerRad = Math.toRadians(steerAngleDeg.toDouble()).toFloat()

        // 2. Transmission & Gear shifting
        if (shiftTimer > 0f) {
            shiftTimer -= safeDt
            clutchEngaged = (1f - (shiftTimer / 0.15f)).coerceIn(0f, 1f)
        } else {
            clutchEngaged = 1.0f
        }

        // Automatic shift logic
        if (isAutomatic && shiftTimer <= 0f) {
            if (velForward < -0.5f && currentGear >= 0 && inputBrake > 0.2f && speedKmh < 5f) {
                // Shift to reverse
                currentGear = -1
                shiftTimer = 0.2f
            } else if (currentGear == -1 && inputThrottle > 0.2f && velForward > -0.5f) {
                currentGear = 1
                shiftTimer = 0.2f
            } else if (currentGear > 0) {
                if (rpm > config.redlineRpm * 0.88f && currentGear < config.gearRatios.size - 1) {
                    currentGear++
                    shiftTimer = 0.18f
                } else if (rpm < config.idleRpm * 2.2f && currentGear > 1) {
                    currentGear--
                    shiftTimer = 0.18f
                }
            }
        }

        // 3. Engine RPM & Torque
        val gearRatio = if (currentGear == -1) config.gearRatios[0]
        else if (currentGear == 0) 0f
        else config.gearRatios.getOrElse(currentGear) { 1.0f }

        val totalDriveRatio = abs(gearRatio * config.finalDriveRatio)

        // Turbo boost buildup
        if (inputBoost) {
            turboBoost = (turboBoost + safeDt * 1.5f).coerceAtMost(1.0f)
        } else {
            turboBoost = (turboBoost - safeDt * 1.0f).coerceAtLeast(0.0f)
        }

        // Calculate theoretical RPM from wheel speed
        val wheelAngularSpeed = abs(velForward) / config.wheelRadius
        val targetRpmFromWheels = (wheelAngularSpeed * totalDriveRatio * (60f / (2f * Math.PI.toFloat())))

        // Actual RPM response to throttle
        val effectiveThrottle = if (tcsActive) inputThrottle * 0.4f else inputThrottle
        val powerBoost = 1.0f + (turboBoost * 0.35f)

        if (currentGear == 0 || clutchEngaged < 0.3f) {
            val targetIdle = if (effectiveThrottle > 0.1f) config.redlineRpm * effectiveThrottle else config.idleRpm
            rpm += (targetIdle - rpm) * safeDt * 8f
        } else {
            val mechanicalRpm = targetRpmFromWheels.coerceIn(config.idleRpm, config.redlineRpm + 400f)
            rpm += (mechanicalRpm - rpm) * safeDt * 15f
            if (effectiveThrottle > 0.1f && rpm < config.idleRpm * 1.2f) {
                rpm += (config.idleRpm * 1.5f - rpm) * safeDt * 5f
            }
        }
        rpm = rpm.coerceIn(config.idleRpm, config.redlineRpm + 500f)

        // Torque calculation from torque curve peak
        val rpmNormalized = ((rpm - config.idleRpm) / (config.redlineRpm - config.idleRpm)).coerceIn(0f, 1f)
        val torqueCurveFactor = (0.5f + 0.5f * sin(rpmNormalized * Math.PI).toFloat())
        val engineTorque = config.maxTorqueNm * torqueCurveFactor * effectiveThrottle * powerBoost * engineTuneMultiplier

        // Drive force at wheels
        var driveForce = 0f
        if (currentGear != 0) {
            val directionSign = if (currentGear == -1) -1f else 1f
            driveForce = directionSign * (engineTorque * totalDriveRatio * 0.88f * clutchEngaged) / config.wheelRadius
        }

        // 4. Braking & Handbrake
        var brakeForce = 0f
        if (inputBrake > 0.05f) {
            if (currentGear == -1) {
                // If in reverse, brake opposes negative velocity
                brakeForce = inputBrake * config.brakeTorqueMax / config.wheelRadius
            } else {
                brakeForce = inputBrake * config.brakeTorqueMax / config.wheelRadius
            }
        }

        // ABS simulation
        absActive = false
        if (absEnabled && brakeForce > 0f && abs(velForward) > 5f) {
            val estimatedLockup = brakeForce / (config.massKg * 9.81f)
            if (estimatedLockup > 0.85f) {
                absActive = true
                // Pulse brake force
                val pulse = (sin(System.currentTimeMillis() * 0.035) * 0.25f + 0.75f).toFloat()
                brakeForce *= pulse
            }
        }

        // 5. Tire Grip & Slip Angles
        val lf = config.wheelbase * 0.52f
        val lr = config.wheelbase * 0.48f
        val vFwdSafe = max(abs(velForward), 1.0f)

        frontSlipAngle = atan2((velLateral + yawRate * lf), vFwdSafe) - steerRad * sign(vFwdSafe)
        rearSlipAngle = atan2((velLateral - yawRate * lr), vFwdSafe)

        // Handbrake locked rear wheels
        var rearGripFactor = 1.0f
        if (inputHandbrake) {
            rearGripFactor = 0.28f // dynamic sliding friction
            rearSlipAngle += sign(yawRate.takeIf { it != 0f } ?: steerRad) * 0.25f
        }

        // TCS simulation
        tcsActive = false
        if (tcsEnabled && driveForce > 0f && rearSlipAngle.absoluteValue > 0.18f) {
            tcsActive = true
        }

        // Lateral tire forces (Pacejka simplified)
        val baseGrip = config.massKg * 9.81f * 0.5f * tireGripMultiplier
        val frontLatForce = -baseGrip * 1.15f * sin(1.8f * atan(6.5f * frontSlipAngle)).coerceIn(-1.2f, 1.2f)
        val rearLatForce = -baseGrip * 1.10f * rearGripFactor * sin(1.8f * atan(6.5f * rearSlipAngle)).coerceIn(-1.2f, 1.2f)

        // Drifting detection & Score tally
        val rawDriftAngleDeg = abs(atan2(velLateral, vFwdSafe)) * (180f / Math.PI.toFloat())
        driftAngle = rawDriftAngleDeg
        if (rawDriftAngleDeg > 12f && speedKmh > 25f) {
            isDrifting = true
            driftComboTime += safeDt
            currentDriftMultiplier = 1f + (driftComboTime * 0.2f).coerceAtMost(5.0f)
            driftScore += (rawDriftAngleDeg * speedKmh * 0.04f * currentDriftMultiplier) * safeDt
        } else {
            isDrifting = false
            if (driftComboTime > 0f) {
                driftComboTime = (driftComboTime - safeDt * 0.5f).coerceAtLeast(0f)
            }
        }

        // 6. Longitudinal forces (Accel, Brake, Rolling resistance, Air Drag)
        val rollingResistance = config.massKg * 9.81f * 0.015f * sign(velForward)
        val airDrag = 0.5f * 1.225f * 0.32f * 2.2f * velForward * abs(velForward)
        val totalBraking = brakeForce * sign(velForward)

        val netLongitudinalForce = driveForce - totalBraking - rollingResistance - airDrag

        // Linear accelerations in car frame
        val accelForward = netLongitudinalForce / config.massKg
        val accelLateral = (frontLatForce + rearLatForce) / config.massKg

        velForward += accelForward * safeDt
        velLateral += accelLateral * safeDt

        // Angular acceleration (yaw torque)
        val yawTorque = (frontLatForce * cos(steerRad) * lf) - (rearLatForce * lr)
        val carMomentOfInertia = config.massKg * (config.wheelbase.pow(2) + config.trackWidth.pow(2)) / 12f
        val yawAccel = yawTorque / carMomentOfInertia
        yawRate += yawAccel * safeDt
        yawRate *= 0.94f // natural yaw damping

        // Update heading
        yaw += yawRate * safeDt

        // 7. World velocity & position update
        val cosYaw = cos(yaw)
        val sinYaw = sin(yaw)
        val worldVelX = sinYaw * velForward + cosYaw * velLateral
        val worldVelZ = cosYaw * velForward - sinYaw * velLateral

        posX += worldVelX * safeDt
        posZ += worldVelZ * safeDt

        // 8. Suspension & Weight Transfer
        // Pitch: squat under acceleration, dive under braking
        val targetPitch = (-accelForward * 0.007f).coerceIn(-0.08f, 0.08f)
        pitch += (targetPitch - pitch) * safeDt * 10f * suspensionStiffnessMultiplier

        // Roll: tilt outward when cornering
        val targetRoll = (accelLateral * 0.005f).coerceIn(-0.10f, 0.10f)
        roll += (targetRoll - roll) * safeDt * 12f * suspensionStiffnessMultiplier

        // Individual suspension corner displacement
        val springRest = config.suspensionRestLength
        suspFL = springRest + pitch * 0.6f - roll * 0.5f
        suspFR = springRest + pitch * 0.6f + roll * 0.5f
        suspRL = springRest - pitch * 0.6f - roll * 0.5f
        suspRR = springRest - pitch * 0.6f + roll * 0.5f

        // Wheel RPMs
        frontWheelRpm = (abs(velForward) / config.wheelRadius) * (60f / (2f * Math.PI.toFloat()))
        rearWheelRpm = if (inputHandbrake) 0f else frontWheelRpm * (if (tcsActive) 1.0f else 1.05f)

        // Road boundaries & Collision reaction (Road width is approx 14 meters, +/- 7m from center)
        val roadHalfWidth = 8.5f
        if (abs(posX) > roadHalfWidth) {
            val penalty = (abs(posX) - roadHalfWidth)
            if (penalty > 0.8f) {
                // Barrier bounce
                posX = sign(posX) * (roadHalfWidth + 0.8f)
                velLateral = -velLateral * 0.5f
                velForward *= 0.82f
                damagePercent = (damagePercent + 1.5f).coerceAtMost(100f)
                collisionImpulse = 1.0f
            }
        }
        if (collisionImpulse > 0f) {
            collisionImpulse = (collisionImpulse - safeDt * 3f).coerceAtLeast(0f)
        }
    }
}

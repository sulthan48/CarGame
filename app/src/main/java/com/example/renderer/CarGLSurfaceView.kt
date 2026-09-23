package com.example.renderer

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import com.example.audio.CarAudioEngine
import com.example.physics.CarPhysics
import com.example.physics.TrafficSystem
import com.example.physics.VehicleInputHandler
import kotlin.math.absoluteValue

class CarGLSurfaceView(
    context: Context,
    val carPhysics: CarPhysics,
    val trafficSystem: TrafficSystem,
    val cameraController: CameraController,
    val audioEngine: CarAudioEngine,
    val inputHandler: VehicleInputHandler? = null
) : GLSurfaceView(context) {

    val gameRenderer: GameRenderer
    private val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    private var lastUpdateNanos = System.nanoTime()

    init {
        setEGLContextClientVersion(2)
        gameRenderer = GameRenderer(carPhysics, trafficSystem, cameraController)
        setRenderer(gameRenderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }

    fun updateGameSimulation() {
        val now = System.nanoTime()
        val dt = ((now - lastUpdateNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        lastUpdateNanos = now

        // Process smooth keyboard inputs if handler is present
        inputHandler?.let { handler ->
            handler.update(dt, carPhysics.speedKmh)
            if (handler.isKeyboardActive) {
                handler.applyTo(carPhysics)
            }
        }

        // Physics step
        carPhysics.update(dt)

        // Traffic step
        trafficSystem.update(carPhysics.posZ, carPhysics.posX, dt) {
            // Collision with traffic vehicle
            audioEngine.triggerCrash = true
            carPhysics.collisionImpulse = 1.0f
            carPhysics.damagePercent = (carPhysics.damagePercent + 8f).coerceAtMost(100f)
            vibrateImpact()
        }

        // Check barrier collision vibration
        if (carPhysics.collisionImpulse > 0.5f) {
            vibrateImpact()
        }

        // Update Audio Engine parameters
        audioEngine.targetRpm = carPhysics.rpm
        audioEngine.targetThrottle = carPhysics.inputThrottle
        audioEngine.isShifting = carPhysics.shiftTimer > 0.05f
        audioEngine.isBoostActive = carPhysics.inputBoost

        val slipFactor = (carPhysics.rearSlipAngle.absoluteValue * 3.5f) +
                (if (carPhysics.inputHandbrake && carPhysics.speedKmh > 10f) 0.85f else 0.0f) +
                (if (carPhysics.absActive) 0.45f else 0.0f)
        audioEngine.tireScreechAmount = slipFactor.coerceIn(0f, 1f)
    }

    private fun vibrateImpact() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(120)
            }
        } catch (_: Exception) {}
    }

    fun vibrateGearShift() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(30)
            }
        } catch (_: Exception) {}
    }
}

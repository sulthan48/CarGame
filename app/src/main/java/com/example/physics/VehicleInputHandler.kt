package com.example.physics

import android.view.KeyEvent
import kotlin.math.*

/**
 * VehicleInputHandler maps hardware keyboard controls (WASD, Arrow keys, Spacebar, Shift, etc.)
 * to vehicle movement parameters with smooth non-linear acceleration, braking, and steering curves.
 */
class VehicleInputHandler(
    var throttleRiseRate: Float = 2.2f,    // Reaches 100% in ~0.45s
    var throttleFallRate: Float = 5.5f,    // Returns to 0 in ~0.18s
    var brakeRiseRate: Float = 2.8f,       // Reaches 100% in ~0.35s
    var brakeFallRate: Float = 6.5f,       // Returns to 0 in ~0.15s
    var steerRiseRate: Float = 3.8f,       // Steer deflection rate
    var steerReturnRate: Float = 5.2f,     // Steer auto-center rate
    var highSpeedSteerDamping: Boolean = true
) {
    // Raw binary key states
    var keyThrottle = false
        private set
    var keyBrake = false
        private set
    var keySteerLeft = false
        private set
    var keySteerRight = false
        private set
    var keyHandbrake = false
        private set
    var keyBoost = false
        private set

    // Internal normalized accumulator values [0..1] or [-1..1]
    private var rawThrottle = 0f
    private var rawBrake = 0f
    private var rawSteer = 0f

    // Smoothed, curved outputs applied to physics
    var throttle: Float = 0f
        private set
    var brake: Float = 0f
        private set
    var steer: Float = 0f
        private set
    var handbrake: Boolean = false
        private set
    var boost: Boolean = false
        private set

    // Tracks if keyboard input was recently used
    var isKeyboardActive: Boolean = false
        private set
    private var activeTimeout = 0f

    // Action listeners for discrete key presses
    var onCameraSwitch: (() -> Unit)? = null
    var onResetVehicle: (() -> Unit)? = null
    var onShiftUp: (() -> Unit)? = null
    var onShiftDown: (() -> Unit)? = null
    var onToggleHeadlights: (() -> Unit)? = null
    var onToggleMute: (() -> Unit)? = null
    var onToggleGarage: (() -> Unit)? = null

    /**
     * Process an Android KeyEvent (handles both ACTION_DOWN and ACTION_UP).
     * Returns true if the key event was consumed by the driving controls.
     */
    fun onKeyEvent(event: KeyEvent): Boolean {
        val isDown = event.action == KeyEvent.ACTION_DOWN

        return when (event.keyCode) {
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> {
                keyThrottle = isDown
                markActive()
                true
            }
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> {
                keyBrake = isDown
                markActive()
                true
            }
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> {
                keySteerLeft = isDown
                markActive()
                true
            }
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                keySteerRight = isDown
                markActive()
                true
            }
            KeyEvent.KEYCODE_SPACE -> {
                keyHandbrake = isDown
                markActive()
                true
            }
            KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_SHIFT_RIGHT,
            KeyEvent.KEYCODE_B, KeyEvent.KEYCODE_N -> {
                keyBoost = isDown
                markActive()
                true
            }
            // Discrete action triggers (fire only on ACTION_DOWN, ignore auto-repeats)
            KeyEvent.KEYCODE_C -> {
                if (isDown && event.repeatCount == 0) {
                    onCameraSwitch?.invoke()
                    markActive()
                }
                true
            }
            KeyEvent.KEYCODE_R -> {
                if (isDown && event.repeatCount == 0) {
                    onResetVehicle?.invoke()
                    markActive()
                }
                true
            }
            KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_PAGE_UP -> {
                if (isDown && event.repeatCount == 0) {
                    onShiftUp?.invoke()
                    markActive()
                }
                true
            }
            KeyEvent.KEYCODE_Q, KeyEvent.KEYCODE_PAGE_DOWN -> {
                if (isDown && event.repeatCount == 0) {
                    onShiftDown?.invoke()
                    markActive()
                }
                true
            }
            KeyEvent.KEYCODE_H -> {
                if (isDown && event.repeatCount == 0) {
                    onToggleHeadlights?.invoke()
                    markActive()
                }
                true
            }
            KeyEvent.KEYCODE_M -> {
                if (isDown && event.repeatCount == 0) {
                    onToggleMute?.invoke()
                    markActive()
                }
                true
            }
            KeyEvent.KEYCODE_G -> {
                if (isDown && event.repeatCount == 0) {
                    onToggleGarage?.invoke()
                    markActive()
                }
                true
            }
            else -> false
        }
    }

    private fun markActive() {
        isKeyboardActive = true
        activeTimeout = 2.0f // Keep active flag for 2 seconds after last key activity
    }

    /**
     * Advance the smooth input curves by dt seconds.
     * @param dt delta time in seconds (e.g. 0.016s)
     * @param currentSpeedKmh current vehicle speed in km/h for speed-sensitive steering
     */
    fun update(dt: Float, currentSpeedKmh: Float = 0f) {
        val safeDt = dt.coerceIn(0.001f, 0.1f)

        if (activeTimeout > 0f) {
            activeTimeout -= safeDt
            if (activeTimeout <= 0f && !hasAnyKeyPressed()) {
                isKeyboardActive = false
            }
        }

        // 1. Smooth Acceleration Curve
        // Linear accumulator ramp
        if (keyThrottle) {
            rawThrottle = (rawThrottle + throttleRiseRate * safeDt).coerceAtMost(1.0f)
        } else {
            rawThrottle = (rawThrottle - throttleFallRate * safeDt).coerceAtLeast(0.0f)
        }
        // Apply smooth progressive curve:
        // Initial soft tip-in (x^1.35) combined with cubic smoothing ensures gentle low-speed throttle modulation
        // and full engine punch when held.
        throttle = calculateAccelerationCurve(rawThrottle)

        // 2. Smooth Braking Curve
        // Linear accumulator ramp
        if (keyBrake) {
            rawBrake = (rawBrake + brakeRiseRate * safeDt).coerceAtMost(1.0f)
        } else {
            rawBrake = (rawBrake - brakeFallRate * safeDt).coerceAtLeast(0.0f)
        }
        // Progressive hydraulic brake pedal curve:
        // Soft initial bite transitioning smoothly into firm threshold braking.
        brake = calculateBrakingCurve(rawBrake)

        // 3. Smooth Steering Curve
        val targetSteer = when {
            keySteerLeft && !keySteerRight -> -1.0f
            keySteerRight && !keySteerLeft -> 1.0f
            else -> 0.0f
        }

        if (targetSteer != 0.0f) {
            val delta = steerRiseRate * safeDt
            rawSteer = if (targetSteer > rawSteer) {
                (rawSteer + delta).coerceAtMost(targetSteer)
            } else {
                (rawSteer - delta).coerceAtLeast(targetSteer)
            }
        } else {
            // Auto-center spring return
            val delta = steerReturnRate * safeDt
            rawSteer = if (rawSteer > 0f) {
                (rawSteer - delta).coerceAtLeast(0.0f)
            } else if (rawSteer < 0f) {
                (rawSteer + delta).coerceAtMost(0.0f)
            } else {
                0.0f
            }
        }

        // Speed-sensitive steering damping (prevents uncontrollable fishtailing at high speeds on keyboard)
        val speedDampingFactor = if (highSpeedSteerDamping) {
            calculateSpeedSteeringDamping(currentSpeedKmh)
        } else {
            1.0f
        }

        // Progressive steering response
        val absRaw = abs(rawSteer)
        val curvedMagnitude = (absRaw * absRaw * 0.4f + absRaw * 0.6f) * speedDampingFactor
        steer = sign(rawSteer) * curvedMagnitude.coerceIn(0f, 1f)

        // 4. Handbrake & Boost
        handbrake = keyHandbrake
        boost = keyBoost
    }

    /**
     * Progressive non-linear acceleration curve.
     * Provides featherable tip-in at low throttle values, smoothly surging to 100% full power.
     */
    fun calculateAccelerationCurve(input: Float): Float {
        val clamped = input.coerceIn(0f, 1f)
        // Blended Hermite S-curve and power curve for natural automotive throttle feel
        val hermite = clamped * clamped * (3f - 2f * clamped)
        val power = clamped.pow(1.3f)
        return (hermite * 0.4f + power * 0.6f).coerceIn(0f, 1f)
    }

    /**
     * Progressive hydraulic braking curve.
     * Prevents instant wheel lockup/ABS panic on initial key tap, but firmly ramps to max deceleration.
     */
    fun calculateBrakingCurve(input: Float): Float {
        val clamped = input.coerceIn(0f, 1f)
        // Quadratic-cubic progressive curve
        val progressive = clamped.pow(1.25f)
        return progressive.coerceIn(0f, 1f)
    }

    /**
     * Speed-sensitive steering scale factor.
     * At 0-40 km/h: 1.0 (full maneuverability for parking / tight turns)
     * At 120 km/h: ~0.70
     * At 250+ km/h: ~0.45 (prevents high-speed rollover on binary key taps)
     */
    fun calculateSpeedSteeringDamping(speedKmh: Float): Float {
        if (speedKmh <= 40f) return 1.0f
        val factor = 1.0f - ((speedKmh - 40f) / 220f) * 0.55f
        return factor.coerceIn(0.45f, 1.0f)
    }

    /**
     * Applies the calculated parameters to the target CarPhysics instance.
     */
    fun applyTo(carPhysics: CarPhysics) {
        carPhysics.inputThrottle = throttle
        carPhysics.inputBrake = brake
        carPhysics.inputSteer = steer
        carPhysics.inputHandbrake = handbrake
        carPhysics.inputBoost = boost
    }

    fun hasAnyKeyPressed(): Boolean {
        return keyThrottle || keyBrake || keySteerLeft || keySteerRight || keyHandbrake || keyBoost
    }

    /**
     * Reset all keys and accumulator values (e.g. when app loses focus or window hides).
     */
    fun reset() {
        keyThrottle = false
        keyBrake = false
        keySteerLeft = false
        keySteerRight = false
        keyHandbrake = false
        keyBoost = false
        rawThrottle = 0f
        rawBrake = 0f
        rawSteer = 0f
        throttle = 0f
        brake = 0f
        steer = 0f
        handbrake = false
        boost = false
        isKeyboardActive = false
        activeTimeout = 0f
    }
}

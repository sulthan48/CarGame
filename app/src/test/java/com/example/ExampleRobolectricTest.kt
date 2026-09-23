package com.example

import android.content.Context
import android.view.KeyEvent
import androidx.test.core.app.ApplicationProvider
import com.example.physics.CarConfig
import com.example.physics.CarPhysics
import com.example.physics.VehicleInputHandler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Apex Drive 3D", appName)
    }

    @Test
    fun `car physics accelerates and builds rpm`() {
        val physics = CarPhysics(CarConfig.ALL_CARS.first())
        assertEquals(0f, physics.speedKmh, 0.01f)
        assertEquals(physics.config.idleRpm, physics.rpm, 1f)

        physics.inputThrottle = 1.0f
        for (i in 0 until 60) {
            physics.update(0.016f)
        }

        assertTrue("Car should accelerate with throttle", physics.speedKmh > 5f)
        assertTrue("RPM should increase above idle", physics.rpm > physics.config.idleRpm)
    }

    @Test
    fun `handbrake locks rear wheels and induces drift`() {
        val physics = CarPhysics(CarConfig.ALL_CARS.first())
        physics.velForward = 25f // ~90 km/h
        physics.inputHandbrake = true
        physics.inputSteer = 0.5f

        for (i in 0 until 20) {
            physics.update(0.016f)
        }

        assertTrue("Rear wheels should slip with handbrake", physics.rearSlipAngle != 0f)
    }

    @Test
    fun `vehicle input handler maps WASD and Space keys correctly`() {
        val handler = VehicleInputHandler()

        // Press W
        val eventWDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_W)
        assertTrue(handler.onKeyEvent(eventWDown))
        assertTrue(handler.keyThrottle)
        assertTrue(handler.isKeyboardActive)

        // Release W
        val eventWUp = KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_W)
        assertTrue(handler.onKeyEvent(eventWUp))
        assertFalse(handler.keyThrottle)

        // Press Space (Handbrake)
        val eventSpaceDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SPACE)
        assertTrue(handler.onKeyEvent(eventSpaceDown))
        assertTrue(handler.keyHandbrake)

        // Press S (Brake)
        val eventSDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_S)
        assertTrue(handler.onKeyEvent(eventSDown))
        assertTrue(handler.keyBrake)

        // Press A (Steer Left)
        val eventADown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_A)
        assertTrue(handler.onKeyEvent(eventADown))
        assertTrue(handler.keySteerLeft)

        // Press D (Steer Right)
        val eventDDown = KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_D)
        assertTrue(handler.onKeyEvent(eventDDown))
        assertTrue(handler.keySteerRight)
    }

    @Test
    fun `vehicle input handler produces smooth acceleration and braking curves`() {
        val handler = VehicleInputHandler()
        val physics = CarPhysics(CarConfig.ALL_CARS.first())

        // Simulate pressing W
        handler.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_W))

        // Initial tick: gentle tip-in, not instant 1.0
        handler.update(0.016f, 0f)
        assertTrue("Initial throttle should be gentle tip-in", handler.throttle > 0f && handler.throttle < 0.1f)

        // Accumulate ticks over 0.5s
        for (i in 0 until 35) {
            handler.update(0.016f, 0f)
        }
        assertTrue("Throttle should smoothly ramp to full power", handler.throttle > 0.95f)

        // Apply to physics
        handler.applyTo(physics)
        assertEquals(handler.throttle, physics.inputThrottle, 0.001f)

        // Release W and press S (Brake)
        handler.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_W))
        handler.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_S))

        // Update a few ticks (approx 80ms)
        for (i in 0 until 5) {
            handler.update(0.016f, 50f)
        }
        assertTrue("Throttle should quickly fall off", handler.throttle < 0.85f)
        assertTrue("Brake should begin progressive curve", handler.brake > 0f)

        // Brake builds up smoothly
        for (i in 0 until 30) {
            handler.update(0.016f, 50f)
        }
        assertTrue("Brake reaches maximum threshold braking", handler.brake > 0.95f)
    }

    @Test
    fun `vehicle input handler steering returns to center`() {
        val handler = VehicleInputHandler()

        // Turn right with D
        handler.onKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_D))
        for (i in 0 until 15) {
            handler.update(0.016f, 30f)
        }
        assertTrue("Steering should be positive (right)", handler.steer > 0.4f)

        // Release D -> auto-centers
        handler.onKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_D))
        for (i in 0 until 25) {
            handler.update(0.016f, 30f)
        }
        assertEquals("Steering should smoothly return to center", 0f, handler.steer, 0.05f)
    }
}

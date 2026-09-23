package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.CarGameHUD
import com.example.ui.GameUIState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun greeting_screenshot() {
        val testState = GameUIState(
            speedKmh = 184f,
            rpm = 7400f,
            currentGear = 4,
            driftScore = 4820f,
            driftMultiplier = 2.4f,
            isDrifting = true,
            absEnabled = true,
            tcsEnabled = true
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                CarGameHUD(
                    uiState = testState,
                    onThrottle = {},
                    onBrake = {},
                    onSteer = {},
                    onHandbrake = {},
                    onBoost = {},
                    onSwitchCamera = {},
                    onToggleTimeOfDay = {},
                    onToggleWeather = {},
                    onToggleHeadlights = {},
                    onToggleAbs = {},
                    onToggleTcs = {},
                    onShiftUp = {},
                    onShiftDown = {},
                    onOpenGarage = {},
                    onOpenSettings = {},
                    onRepair = {},
                    onReset = {},
                    onToggleMute = {}
                )
            }
        }

        composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
    }
}

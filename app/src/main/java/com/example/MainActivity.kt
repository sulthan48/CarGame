package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.renderer.CarGLSurfaceView
import com.example.ui.*
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.isActive

class MainActivity : ComponentActivity() {

    private val viewModel: CarGameViewModel by viewModels()
    private var glSurfaceView: CarGLSurfaceView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep screen on while driving
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        hideSystemUI()

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CarGameScreen(viewModel = viewModel) { surface ->
                        glSurfaceView = surface
                    }
                }
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (viewModel.inputHandler.onKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    private fun hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            or View.SYSTEM_UI_FLAG_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    )
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUI()
        glSurfaceView?.onResume()
    }

    override fun onPause() {
        super.onPause()
        viewModel.inputHandler.reset()
        glSurfaceView?.onPause()
    }
}

@Composable
fun CarGameScreen(
    viewModel: CarGameViewModel,
    onSurfaceCreated: (CarGLSurfaceView) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var surfaceViewRef by remember { mutableStateOf<CarGLSurfaceView?>(null) }

    // Connect surface-dependent keyboard action triggers
    LaunchedEffect(surfaceViewRef) {
        viewModel.inputHandler.onShiftUp = {
            surfaceViewRef?.let { viewModel.shiftUp(it) }
        }
        viewModel.inputHandler.onShiftDown = {
            surfaceViewRef?.let { viewModel.shiftDown(it) }
        }
        viewModel.inputHandler.onToggleHeadlights = {
            surfaceViewRef?.gameRenderer?.let { viewModel.toggleHeadlights(it) }
        }
    }

    // Game simulation loop running at display frame rate
    LaunchedEffect(surfaceViewRef) {
        val surface = surfaceViewRef ?: return@LaunchedEffect
        while (isActive) {
            withFrameNanos {
                surface.updateGameSimulation()
                viewModel.syncUiState()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. OpenGL ES 3D Surface View
        AndroidView(
            factory = { context ->
                CarGLSurfaceView(
                    context = context,
                    carPhysics = viewModel.carPhysics,
                    trafficSystem = viewModel.trafficSystem,
                    cameraController = viewModel.cameraController,
                    audioEngine = viewModel.audioEngine,
                    inputHandler = viewModel.inputHandler
                ).also {
                    surfaceViewRef = it
                    onSurfaceCreated(it)
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. HUD & Touch Driving Controls Overlay
        CarGameHUD(
            uiState = uiState,
            onThrottle = { viewModel.setThrottle(it) },
            onBrake = { viewModel.setBrake(it) },
            onSteer = { viewModel.setSteer(it) },
            onHandbrake = { viewModel.setHandbrake(it) },
            onBoost = { viewModel.setBoost(it) },
            onSwitchCamera = { viewModel.switchCamera() },
            onToggleTimeOfDay = {
                surfaceViewRef?.gameRenderer?.let { r -> viewModel.toggleTimeOfDay(r) }
            },
            onToggleWeather = {
                surfaceViewRef?.gameRenderer?.let { r -> viewModel.toggleWeather(r) }
            },
            onToggleHeadlights = {
                surfaceViewRef?.gameRenderer?.let { r -> viewModel.toggleHeadlights(r) }
            },
            onToggleAbs = { viewModel.toggleAbs() },
            onToggleTcs = { viewModel.toggleTcs() },
            onShiftUp = { surfaceViewRef?.let { viewModel.shiftUp(it) } },
            onShiftDown = { surfaceViewRef?.let { viewModel.shiftDown(it) } },
            onOpenGarage = { viewModel.openGarage(true) },
            onOpenSettings = { viewModel.openSettings(true) },
            onRepair = { viewModel.repairVehicle() },
            onReset = { viewModel.resetVehicle() },
            onToggleMute = { viewModel.toggleMute() }
        )

        // 3. Garage & Tuning Modal
        AnimatedVisibility(
            visible = uiState.isGarageOpen,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            surfaceViewRef?.gameRenderer?.let { renderer ->
                GarageScreen(
                    uiState = uiState,
                    renderer = renderer,
                    onSelectCar = { index -> viewModel.selectCar(index, renderer) },
                    onSelectColor = { color -> viewModel.setCustomPaintColor(color, renderer) },
                    onSelectEngineStage = { stage -> viewModel.setEngineStage(stage) },
                    onSelectSuspensionPreset = { preset -> viewModel.setSuspensionPreset(preset) },
                    onSelectTireCompound = { compound -> viewModel.setTireCompound(compound) },
                    onSelectControlType = { type -> viewModel.setControlType(type) },
                    onToggleTransmission = { viewModel.toggleTransmission() },
                    onClose = { viewModel.openGarage(false) }
                )
            }
        }
    }
}

package com.example.ui

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.audio.CarAudioEngine
import com.example.physics.*
import com.example.renderer.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ControlType {
    STEERING_WHEEL,
    ARROWS,
    TILT
}

data class GameUIState(
    val speedKmh: Float = 0f,
    val rpm: Float = 850f,
    val maxRpm: Float = 8500f,
    val currentGear: Int = 1,
    val isAutomatic: Boolean = true,
    val driftScore: Float = 0f,
    val driftMultiplier: Float = 1f,
    val isDrifting: Boolean = false,
    val damagePercent: Float = 0f,
    val turboBoost: Float = 0f,
    val cameraMode: CameraMode = CameraMode.THIRD_PERSON,
    val timeOfDay: TimeOfDay = TimeOfDay.DAY,
    val weatherMode: WeatherMode = WeatherMode.CLEAR,
    val headlightsOn: Boolean = true,
    val absActive: Boolean = false,
    val tcsActive: Boolean = false,
    val absEnabled: Boolean = true,
    val tcsEnabled: Boolean = true,
    val controlType: ControlType = ControlType.STEERING_WHEEL,
    val selectedCarIndex: Int = 0,
    val customPaintColor: Long = 0xFFD32F2FL,
    val isGarageOpen: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val isMuted: Boolean = false,
    val isKeyboardActive: Boolean = false,
    val engineStage: Int = 2,
    val suspensionPreset: Int = 1, // 0: Soft, 1: Sport, 2: Track
    val tireCompound: Int = 1      // 0: Street, 1: Sport, 2: Drift
)

class CarGameViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    val carPhysics = CarPhysics(CarConfig.ALL_CARS[0])
    val trafficSystem = TrafficSystem()
    val cameraController = CameraController()
    val audioEngine = CarAudioEngine()
    val inputHandler = VehicleInputHandler()

    private val sensorManager = application.getSystemService(Application.SENSOR_SERVICE) as? SensorManager
    private val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _uiState = MutableStateFlow(GameUIState(customPaintColor = carPhysics.config.primaryColor))
    val uiState = _uiState.asStateFlow()

    private var tiltSteerInput = 0f

    init {
        audioEngine.start()
        startSensor()

        // Bind keyboard action shortcuts
        inputHandler.onCameraSwitch = { switchCamera() }
        inputHandler.onResetVehicle = { resetVehicle() }
        inputHandler.onToggleMute = { toggleMute() }
        inputHandler.onToggleGarage = { openGarage(!_uiState.value.isGarageOpen) }
    }

    private fun startSensor() {
        accelerometer?.let {
            sensorManager?.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || _uiState.value.controlType != ControlType.TILT) return
        // In landscape orientation: event.values[1] reflects left/right tilt
        val tiltY = event.values[1]
        tiltSteerInput = (-tiltY * 0.22f).coerceIn(-1.0f, 1.0f)
        if (_uiState.value.controlType == ControlType.TILT) {
            carPhysics.inputSteer = tiltSteerInput
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    fun setThrottle(value: Float) {
        carPhysics.inputThrottle = value.coerceIn(0f, 1f)
    }

    fun setBrake(value: Float) {
        carPhysics.inputBrake = value.coerceIn(0f, 1f)
    }

    fun setSteer(value: Float) {
        if (_uiState.value.controlType != ControlType.TILT) {
            carPhysics.inputSteer = value.coerceIn(-1f, 1f)
        }
    }

    fun setHandbrake(active: Boolean) {
        carPhysics.inputHandbrake = active
    }

    fun setBoost(active: Boolean) {
        carPhysics.inputBoost = active
    }

    fun switchCamera() {
        val newMode = cameraController.switchMode()
        _uiState.update { it.copy(cameraMode = newMode) }
    }

    fun toggleTimeOfDay(renderer: GameRenderer) {
        val next = when (renderer.timeOfDay) {
            TimeOfDay.DAY -> TimeOfDay.SUNSET
            TimeOfDay.SUNSET -> TimeOfDay.NIGHT
            TimeOfDay.NIGHT -> TimeOfDay.DAY
        }
        renderer.timeOfDay = next
        _uiState.update { it.copy(timeOfDay = next) }
    }

    fun toggleWeather(renderer: GameRenderer) {
        val next = when (renderer.weatherMode) {
            WeatherMode.CLEAR -> WeatherMode.RAIN
            WeatherMode.RAIN -> WeatherMode.FOG
            WeatherMode.FOG -> WeatherMode.CLEAR
        }
        renderer.weatherMode = next
        _uiState.update { it.copy(weatherMode = next) }
    }

    fun toggleHeadlights(renderer: GameRenderer) {
        val newState = !renderer.headlightsOn
        renderer.headlightsOn = newState
        _uiState.update { it.copy(headlightsOn = newState) }
    }

    fun toggleAbs() {
        val newState = !carPhysics.absEnabled
        carPhysics.absEnabled = newState
        _uiState.update { it.copy(absEnabled = newState) }
    }

    fun toggleTcs() {
        val newState = !carPhysics.tcsEnabled
        carPhysics.tcsEnabled = newState
        _uiState.update { it.copy(tcsEnabled = newState) }
    }

    fun shiftUp(surfaceView: CarGLSurfaceView) {
        carPhysics.shiftUp()
        surfaceView.vibrateGearShift()
    }

    fun shiftDown(surfaceView: CarGLSurfaceView) {
        carPhysics.shiftDown()
        surfaceView.vibrateGearShift()
    }

    fun toggleTransmission() {
        val next = !carPhysics.isAutomatic
        carPhysics.isAutomatic = next
        _uiState.update { it.copy(isAutomatic = next) }
    }

    fun setControlType(type: ControlType) {
        _uiState.update { it.copy(controlType = type) }
        carPhysics.inputSteer = 0f
    }

    fun toggleMute() {
        val next = !audioEngine.isMuted
        audioEngine.isMuted = next
        _uiState.update { it.copy(isMuted = next) }
    }

    fun repairVehicle() {
        carPhysics.repair()
        _uiState.update { it.copy(damagePercent = 0f) }
    }

    fun resetVehicle() {
        carPhysics.reset(startZ = carPhysics.posZ, startX = 0f)
        trafficSystem.reset(carPhysics.posZ)
        _uiState.update { it.copy(speedKmh = 0f, driftScore = 0f, damagePercent = 0f) }
    }

    fun openGarage(open: Boolean) {
        _uiState.update { it.copy(isGarageOpen = open) }
    }

    fun openSettings(open: Boolean) {
        _uiState.update { it.copy(isSettingsOpen = open) }
    }

    fun selectCar(index: Int, renderer: GameRenderer) {
        val newConfig = CarConfig.ALL_CARS[index]
        carPhysics.config = newConfig
        carPhysics.reset(startZ = carPhysics.posZ, startX = 0f)
        renderer.reloadCarMesh(newConfig.primaryColor)
        _uiState.update {
            it.copy(
                selectedCarIndex = index,
                customPaintColor = newConfig.primaryColor,
                maxRpm = newConfig.redlineRpm
            )
        }
    }

    fun setCustomPaintColor(colorHex: Long, renderer: GameRenderer) {
        renderer.reloadCarMesh(colorHex)
        _uiState.update { it.copy(customPaintColor = colorHex) }
    }

    fun setEngineStage(stage: Int) {
        val mult = when (stage) {
            1 -> 1.0f
            2 -> 1.18f
            3 -> 1.35f
            else -> 1.0f
        }
        carPhysics.engineTuneMultiplier = mult
        _uiState.update { it.copy(engineStage = stage) }
    }

    fun setSuspensionPreset(preset: Int) {
        val mult = when (preset) {
            0 -> 0.85f // Comfort
            1 -> 1.15f // Sport
            2 -> 1.45f // Track
            else -> 1.0f
        }
        carPhysics.suspensionStiffnessMultiplier = mult
        _uiState.update { it.copy(suspensionPreset = preset) }
    }

    fun setTireCompound(compound: Int) {
        val mult = when (compound) {
            0 -> 0.92f // Street
            1 -> 1.15f // Sport
            2 -> 0.78f // Drift Slicks (easier slides)
            else -> 1.0f
        }
        carPhysics.tireGripMultiplier = mult
        _uiState.update { it.copy(tireCompound = compound) }
    }

    fun syncUiState() {
        _uiState.update {
            it.copy(
                speedKmh = carPhysics.speedKmh,
                rpm = carPhysics.rpm,
                maxRpm = carPhysics.config.redlineRpm,
                currentGear = carPhysics.currentGear,
                driftScore = carPhysics.driftScore,
                driftMultiplier = carPhysics.currentDriftMultiplier,
                isDrifting = carPhysics.isDrifting,
                damagePercent = carPhysics.damagePercent,
                turboBoost = carPhysics.turboBoost,
                absActive = carPhysics.absActive,
                tcsActive = carPhysics.tcsActive,
                isKeyboardActive = inputHandler.isKeyboardActive
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager?.unregisterListener(this)
        audioEngine.stop()
    }
}

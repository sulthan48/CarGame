package com.example.renderer

import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.physics.*
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.*
import kotlin.random.Random

enum class TimeOfDay {
    DAY,
    SUNSET,
    NIGHT
}

enum class WeatherMode {
    CLEAR,
    RAIN,
    FOG
}

class GameRenderer(
    val carPhysics: CarPhysics,
    val trafficSystem: TrafficSystem,
    val cameraController: CameraController
) : GLSurfaceView.Renderer {

    // Shader handles
    private var programId = 0
    private var uMVPMatrixHandle = 0
    private var uModelMatrixHandle = 0
    private var uCameraPosHandle = 0
    private var uLightDirHandle = 0
    private var uLightColorHandle = 0
    private var uAmbientColorHandle = 0
    private var uHeadlightPosHandle = 0
    private var uHeadlightDirHandle = 0
    private var uHeadlightsEnabledHandle = 0
    private var uIsWetHandle = 0
    private var uFogColorHandle = 0
    private var uEmissionHandle = 0

    private var aPositionHandle = 0
    private var aNormalHandle = 0
    private var aTexCoordHandle = 0
    private var aColorHandle = 0

    // Matrices
    private val projectionMatrix = FloatArray(16)
    private val viewMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val scratchMatrix = FloatArray(16)

    // Meshes
    private var carBodyMesh: Mesh? = null
    private var steeringWheelMesh: Mesh? = null
    private var wheelMesh: Mesh? = null
    private var highwayMesh: Mesh? = null
    private var gantryMesh: Mesh? = null
    private var fuelStationMesh: Mesh? = null
    private var bridgeMesh: Mesh? = null
    private var treeMesh: Mesh? = null
    private var buildingMesh1: Mesh? = null
    private var buildingMesh2: Mesh? = null
    private val trafficMeshes = mutableMapOf<TrafficVehicleType, Mesh>()

    // Environment state
    var timeOfDay = TimeOfDay.DAY
    var weatherMode = WeatherMode.CLEAR
    var headlightsOn = true
    var wipersOn = false

    // Timing
    private var lastTimeNanos = 0L
    private var wheelSpinAngleDeg = 0f
    private var rainParticleMesh: Mesh? = null

    // Highway segments tracking
    private val segmentLength = 120f
    private val numSegments = 6

    // Nitro flame mesh
    private var nitroFlameMesh: Mesh? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)
        GLES20.glDepthFunc(GLES20.GL_LEQUAL)
        GLES20.glEnable(GLES20.GL_CULL_FACE)
        GLES20.glCullFace(GLES20.GL_BACK)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // Compile shaders
        programId = ShaderHelper.createProgram(ShaderHelper.VERTEX_SHADER, ShaderHelper.FRAGMENT_SHADER)
        GLES20.glUseProgram(programId)

        // Cache uniform & attribute locations
        uMVPMatrixHandle = GLES20.glGetUniformLocation(programId, "uMVPMatrix")
        uModelMatrixHandle = GLES20.glGetUniformLocation(programId, "uModelMatrix")
        uCameraPosHandle = GLES20.glGetUniformLocation(programId, "uCameraPos")
        uLightDirHandle = GLES20.glGetUniformLocation(programId, "uLightDir")
        uLightColorHandle = GLES20.glGetUniformLocation(programId, "uLightColor")
        uAmbientColorHandle = GLES20.glGetUniformLocation(programId, "uAmbientColor")
        uHeadlightPosHandle = GLES20.glGetUniformLocation(programId, "uHeadlightPos")
        uHeadlightDirHandle = GLES20.glGetUniformLocation(programId, "uHeadlightDir")
        uHeadlightsEnabledHandle = GLES20.glGetUniformLocation(programId, "uHeadlightsEnabled")
        uIsWetHandle = GLES20.glGetUniformLocation(programId, "uIsWet")
        uFogColorHandle = GLES20.glGetUniformLocation(programId, "uFogColor")
        uEmissionHandle = GLES20.glGetUniformLocation(programId, "uEmission")

        aPositionHandle = GLES20.glGetAttribLocation(programId, "aPosition")
        aNormalHandle = GLES20.glGetAttribLocation(programId, "aNormal")
        aTexCoordHandle = GLES20.glGetAttribLocation(programId, "aTexCoord")
        aColorHandle = GLES20.glGetAttribLocation(programId, "aColor")

        // Build procedural meshes
        reloadCarMesh()
        steeringWheelMesh = MeshFactory.createSteeringWheelMesh()
        wheelMesh = MeshFactory.createWheelMesh()
        highwayMesh = MeshFactory.createHighwaySegment(segmentLength)
        gantryMesh = MeshFactory.createHighwayGantry()
        fuelStationMesh = MeshFactory.createFuelStation()
        bridgeMesh = MeshFactory.createBridge()
        treeMesh = MeshFactory.createTree()
        buildingMesh1 = MeshFactory.createBuilding(18f, 38f, 22f, 0xFF37474FL)
        buildingMesh2 = MeshFactory.createBuilding(24f, 52f, 26f, 0xFF263238L)

        for (type in TrafficVehicleType.values()) {
            trafficMeshes[type] = MeshFactory.createTrafficMesh(type, 0xFFFFFFFFL)
        }

        buildRainMesh()
        buildNitroMesh()

        lastTimeNanos = System.nanoTime()
    }

    fun reloadCarMesh(customColorHex: Long? = null) {
        carBodyMesh = MeshFactory.createCarBodyMesh(carPhysics.config, customColorHex)
    }

    private fun buildRainMesh() {
        val b = MeshBuilder()
        val rainColor = floatArrayOf(0.75f, 0.85f, 1.0f, 0.45f)
        for (i in 0 until 120) {
            val rx = Random.nextFloat() * 40f - 20f
            val ry = Random.nextFloat() * 15f + 1f
            val rz = Random.nextFloat() * 40f - 20f
            b.addBox(rx - 0.02f, ry, rz - 0.02f, rx + 0.02f, ry + 1.2f, rz + 0.02f, rainColor)
        }
        rainParticleMesh = b.build()
    }

    private fun buildNitroMesh() {
        val b = MeshBuilder()
        val flameColor = floatArrayOf(0.1f, 0.85f, 1.0f, 0.9f)
        b.addBox(-0.1f, -0.1f, -0.6f, 0.1f, 0.1f, 0f, flameColor)
        nitroFlameMesh = b.build()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val aspect = width.toFloat() / height.toFloat().coerceAtLeast(1f)
        Matrix.perspectiveM(projectionMatrix, 0, cameraController.fov, aspect, 0.2f, 400.0f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val dt = ((now - lastTimeNanos) / 1_000_000_000.0f).coerceIn(0.001f, 0.05f)
        lastTimeNanos = now

        // Update wheel spin
        wheelSpinAngleDeg += (carPhysics.velForward / carPhysics.config.wheelRadius) * (180f / Math.PI.toFloat()) * dt
        wheelSpinAngleDeg %= 360f

        // Update camera position
        cameraController.update(carPhysics, dt)

        // Clear color based on sky / time of day
        val (skyR, skyG, skyB) = when (timeOfDay) {
            TimeOfDay.DAY -> floatArrayOf(0.42f, 0.65f, 0.88f)
            TimeOfDay.SUNSET -> floatArrayOf(0.82f, 0.42f, 0.25f)
            TimeOfDay.NIGHT -> floatArrayOf(0.04f, 0.06f, 0.12f)
        }
        val fogColor = if (weatherMode == WeatherMode.FOG) floatArrayOf(0.65f, 0.68f, 0.72f) else floatArrayOf(skyR, skyG, skyB)

        GLES20.glClearColor(fogColor[0], fogColor[1], fogColor[2], 1.0f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        GLES20.glUseProgram(programId)

        // Set Camera View Matrix
        Matrix.setLookAtM(
            viewMatrix, 0,
            cameraController.camX, cameraController.camY, cameraController.camZ,
            cameraController.targetX, cameraController.targetY, cameraController.targetZ,
            cameraController.upX, cameraController.upY, cameraController.upZ
        )

        // Update FOV if changed
        val aspect = 16f / 9f // Default widescreen aspect
        Matrix.perspectiveM(projectionMatrix, 0, cameraController.fov, aspect, 0.2f, 400.0f)

        // Upload environment uniforms
        GLES20.glUniform3f(uCameraPosHandle, cameraController.camX, cameraController.camY, cameraController.camZ)
        GLES20.glUniform3f(uFogColorHandle, fogColor[0], fogColor[1], fogColor[2])
        GLES20.glUniform1f(uIsWetHandle, if (weatherMode == WeatherMode.RAIN) 1.0f else 0.0f)

        // Directional Light & Ambient
        when (timeOfDay) {
            TimeOfDay.DAY -> {
                GLES20.glUniform3f(uLightDirHandle, -0.4f, -0.8f, -0.4f)
                GLES20.glUniform3f(uLightColorHandle, 0.95f, 0.95f, 0.90f)
                GLES20.glUniform3f(uAmbientColorHandle, 0.45f, 0.48f, 0.52f)
            }
            TimeOfDay.SUNSET -> {
                GLES20.glUniform3f(uLightDirHandle, -0.8f, -0.25f, -0.5f)
                GLES20.glUniform3f(uLightColorHandle, 0.98f, 0.55f, 0.25f)
                GLES20.glUniform3f(uAmbientColorHandle, 0.35f, 0.25f, 0.30f)
            }
            TimeOfDay.NIGHT -> {
                GLES20.glUniform3f(uLightDirHandle, -0.2f, -0.9f, -0.3f)
                GLES20.glUniform3f(uLightColorHandle, 0.25f, 0.30f, 0.45f)
                GLES20.glUniform3f(uAmbientColorHandle, 0.12f, 0.14f, 0.20f)
            }
        }

        // Headlights uniform
        val sinYaw = sin(carPhysics.yaw)
        val cosYaw = cos(carPhysics.yaw)
        val hlX = carPhysics.posX + sinYaw * 2.1f
        val hlY = carPhysics.posY + 0.5f
        val hlZ = carPhysics.posZ + cosYaw * 2.1f
        GLES20.glUniform3f(uHeadlightPosHandle, hlX, hlY, hlZ)
        GLES20.glUniform3f(uHeadlightDirHandle, sinYaw, carPhysics.pitch, cosYaw)
        val enableHl = if (headlightsOn && (timeOfDay == TimeOfDay.NIGHT || weatherMode != WeatherMode.CLEAR)) 1.0f else 0.0f
        GLES20.glUniform1f(uHeadlightsEnabledHandle, enableHl)

        // 1. Draw Highway Segments
        renderHighwayAndEnvironment()

        // 2. Draw Traffic Vehicles
        renderTraffic()

        // 3. Draw Player Car & Wheels
        renderPlayerCar()

        // 4. Draw Rain streaks if rain weather
        if (weatherMode == WeatherMode.RAIN) {
            renderRain()
        }
    }

    private fun drawMesh(mesh: Mesh?, model: FloatArray, emission: Float = 0f) {
        if (mesh == null) return
        Matrix.multiplyMM(mvpMatrix, 0, viewMatrix, 0, model, 0)
        Matrix.multiplyMM(scratchMatrix, 0, projectionMatrix, 0, mvpMatrix, 0)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, scratchMatrix, 0)
        GLES20.glUniformMatrix4fv(uModelMatrixHandle, 1, false, model, 0)
        GLES20.glUniform1f(uEmissionHandle, emission)

        mesh.draw(aPositionHandle, aNormalHandle, aTexCoordHandle, aColorHandle)
    }

    private fun renderHighwayAndEnvironment() {
        val playerZ = carPhysics.posZ
        val startSegmentIdx = floor((playerZ - segmentLength) / segmentLength).toInt()

        for (i in startSegmentIdx until startSegmentIdx + numSegments) {
            val segZ = i * segmentLength

            // Road surface
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, 0f, 0f, segZ)
            drawMesh(highwayMesh, modelMatrix)

            // Overhead Gantry Signboards every 240m
            if (i % 2 == 0) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, 0f, 0f, segZ + 60f)
                drawMesh(gantryMesh, modelMatrix)
            }

            // Fuel Station every 360m
            if (i % 3 == 0) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, 0f, 0f, segZ + 30f)
                drawMesh(fuelStationMesh, modelMatrix)
            }

            // Suspension Bridge every 5 segments
            if (i % 5 == 0) {
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, 0f, 0f, segZ + 50f)
                drawMesh(bridgeMesh, modelMatrix)
            }

            // Roadside Buildings and Trees along the margins
            for (side in floatArrayOf(-25f, 25f)) {
                // Trees
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, side + (if (side < 0) -8f else 8f), 0f, segZ + 25f)
                drawMesh(treeMesh, modelMatrix)

                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, side + (if (side < 0) -15f else 15f), 0f, segZ + 85f)
                drawMesh(treeMesh, modelMatrix)

                // Skyscrapers
                Matrix.setIdentityM(modelMatrix, 0)
                Matrix.translateM(modelMatrix, 0, side + (if (side < 0) -30f else 30f), 0f, segZ + 45f)
                val bMesh = if (i % 2 == 0) buildingMesh1 else buildingMesh2
                drawMesh(bMesh, modelMatrix, emission = if (timeOfDay == TimeOfDay.NIGHT) 0.35f else 0.0f)
            }
        }
    }

    private fun renderTraffic() {
        val nightGlow = if (timeOfDay == TimeOfDay.NIGHT) 0.6f else 0.0f
        for (v in trafficSystem.vehicles) {
            val mesh = trafficMeshes[v.type] ?: continue
            Matrix.setIdentityM(modelMatrix, 0)
            Matrix.translateM(modelMatrix, 0, v.posX, 0.2f, v.posZ)
            drawMesh(mesh, modelMatrix, emission = nightGlow)
        }
    }

    private fun renderPlayerCar() {
        // Car Body Base Matrix
        val carBaseMatrix = FloatArray(16)
        Matrix.setIdentityM(carBaseMatrix, 0)
        Matrix.translateM(carBaseMatrix, 0, carPhysics.posX, carPhysics.posY, carPhysics.posZ)
        Matrix.rotateM(carBaseMatrix, 0, Math.toDegrees(carPhysics.yaw.toDouble()).toFloat(), 0f, 1f, 0f)
        Matrix.rotateM(carBaseMatrix, 0, Math.toDegrees(carPhysics.pitch.toDouble()).toFloat(), 1f, 0f, 0f)
        Matrix.rotateM(carBaseMatrix, 0, Math.toDegrees(carPhysics.roll.toDouble()).toFloat(), 0f, 0f, 1f)

        // Draw Car Chassis & Cockpit
        val brakeEmission = if (carPhysics.inputBrake > 0.1f) 0.95f else (if (timeOfDay == TimeOfDay.NIGHT) 0.45f else 0.0f)
        drawMesh(carBodyMesh, carBaseMatrix, emission = brakeEmission)

        // Draw 3D Rotating Steering Wheel (inside cockpit)
        val steerMatrix = FloatArray(16)
        System.arraycopy(carBaseMatrix, 0, steerMatrix, 0, 16)
        // Position at driver console: x = -0.36m, y = 0.68m, z = 0.08m
        Matrix.translateM(steerMatrix, 0, -0.36f, 0.68f, 0.08f)
        Matrix.rotateM(steerMatrix, 0, -22f, 1f, 0f, 0f) // Angled towards driver
        Matrix.rotateM(steerMatrix, 0, -carPhysics.steerAngleDeg * 3.5f, 0f, 0f, 1f) // Steer rotation!
        drawMesh(steeringWheelMesh, steerMatrix)

        // Draw Boost Nitro Flames if active
        if (carPhysics.inputBoost) {
            for (exhaustX in floatArrayOf(-0.28f, 0.28f)) {
                val flameMatrix = FloatArray(16)
                System.arraycopy(carBaseMatrix, 0, flameMatrix, 0, 16)
                Matrix.translateM(flameMatrix, 0, exhaustX, 0.22f, -2.3f)
                val scale = 0.8f + Random.nextFloat() * 0.4f
                Matrix.scaleM(flameMatrix, 0, scale, scale, scale * 1.5f)
                drawMesh(nitroFlameMesh, flameMatrix, emission = 1.0f)
            }
        }

        // Draw 4 Wheels with suspension & steering
        val halfTrack = carPhysics.config.trackWidth * 0.48f
        val frontAxleZ = carPhysics.config.wheelbase * 0.52f
        val rearAxleZ = -carPhysics.config.wheelbase * 0.48f

        // Front-Left Wheel
        renderWheel(carBaseMatrix, -halfTrack, -carPhysics.suspFL + 0.34f, frontAxleZ, carPhysics.steerAngleDeg, wheelSpinAngleDeg)
        // Front-Right Wheel
        renderWheel(carBaseMatrix, halfTrack, -carPhysics.suspFR + 0.34f, frontAxleZ, carPhysics.steerAngleDeg, wheelSpinAngleDeg)
        // Rear-Left Wheel
        renderWheel(carBaseMatrix, -halfTrack, -carPhysics.suspRL + 0.34f, rearAxleZ, 0f, wheelSpinAngleDeg)
        // Rear-Right Wheel
        renderWheel(carBaseMatrix, halfTrack, -carPhysics.suspRR + 0.34f, rearAxleZ, 0f, wheelSpinAngleDeg)
    }

    private fun renderWheel(baseMatrix: FloatArray, offsetX: Float, offsetY: Float, offsetZ: Float, steerDeg: Float, spinDeg: Float) {
        val wMatrix = FloatArray(16)
        System.arraycopy(baseMatrix, 0, wMatrix, 0, 16)
        Matrix.translateM(wMatrix, 0, offsetX, offsetY, offsetZ)
        if (steerDeg != 0f) {
            Matrix.rotateM(wMatrix, 0, steerDeg, 0f, 1f, 0f)
        }
        Matrix.rotateM(wMatrix, 0, spinDeg, 1f, 0f, 0f)
        drawMesh(wheelMesh, wMatrix)
    }

    private fun renderRain() {
        val rainMatrix = FloatArray(16)
        Matrix.setIdentityM(rainMatrix, 0)
        Matrix.translateM(rainMatrix, 0, carPhysics.posX, 0f, carPhysics.posZ)
        drawMesh(rainParticleMesh, rainMatrix, emission = 0.5f)
    }
}

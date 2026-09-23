package com.example.renderer

import android.opengl.GLES20
import com.example.physics.CarBodyType
import com.example.physics.CarConfig
import com.example.physics.TrafficVehicleType
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.math.*

class Mesh(
    val vertexBuffer: FloatBuffer,
    val vertexCount: Int,
    val drawMode: Int = GLES20.GL_TRIANGLES
) {
    fun draw(posHandle: Int, normHandle: Int, uvHandle: Int, colorHandle: Int) {
        val stride = (3 + 3 + 2 + 4) * 4 // 12 floats = 48 bytes

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(posHandle)
        GLES20.glVertexAttribPointer(posHandle, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(3)
        GLES20.glEnableVertexAttribArray(normHandle)
        GLES20.glVertexAttribPointer(normHandle, 3, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(6)
        GLES20.glEnableVertexAttribArray(uvHandle)
        GLES20.glVertexAttribPointer(uvHandle, 2, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        vertexBuffer.position(8)
        GLES20.glEnableVertexAttribArray(colorHandle)
        GLES20.glVertexAttribPointer(colorHandle, 4, GLES20.GL_FLOAT, false, stride, vertexBuffer)

        GLES20.glDrawArrays(drawMode, 0, vertexCount)

        GLES20.glDisableVertexAttribArray(posHandle)
        GLES20.glDisableVertexAttribArray(normHandle)
        GLES20.glDisableVertexAttribArray(uvHandle)
        GLES20.glDisableVertexAttribArray(colorHandle)
    }
}

class MeshBuilder {
    private val data = mutableListOf<Float>()

    fun addVertex(
        x: Float, y: Float, z: Float,
        nx: Float, ny: Float, nz: Float,
        u: Float, v: Float,
        r: Float, g: Float, b: Float, a: Float = 1.0f
    ) {
        data.add(x); data.add(y); data.add(z)
        data.add(nx); data.add(ny); data.add(nz)
        data.add(u); data.add(v)
        data.add(r); data.add(g); data.add(b); data.add(a)
    }

    fun addQuad(
        v1: FloatArray, v2: FloatArray, v3: FloatArray, v4: FloatArray,
        normal: FloatArray,
        color: FloatArray,
        uvs: FloatArray = floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)
    ) {
        // Triangle 1: v1, v2, v3
        addVertex(v1[0], v1[1], v1[2], normal[0], normal[1], normal[2], uvs[0], uvs[1], color[0], color[1], color[2], color[3])
        addVertex(v2[0], v2[1], v2[2], normal[0], normal[1], normal[2], uvs[2], uvs[3], color[0], color[1], color[2], color[3])
        addVertex(v3[0], v3[1], v3[2], normal[0], normal[1], normal[2], uvs[4], uvs[5], color[0], color[1], color[2], color[3])

        // Triangle 2: v1, v3, v4
        addVertex(v1[0], v1[1], v1[2], normal[0], normal[1], normal[2], uvs[0], uvs[1], color[0], color[1], color[2], color[3])
        addVertex(v3[0], v3[1], v3[2], normal[0], normal[1], normal[2], uvs[4], uvs[5], color[0], color[1], color[2], color[3])
        addVertex(v4[0], v4[1], v4[2], normal[0], normal[1], normal[2], uvs[6], uvs[7], color[0], color[1], color[2], color[3])
    }

    fun addBox(
        minX: Float, minY: Float, minZ: Float,
        maxX: Float, maxY: Float, maxZ: Float,
        color: FloatArray
    ) {
        // Front (+Z)
        addQuad(
            floatArrayOf(minX, minY, maxZ), floatArrayOf(maxX, minY, maxZ),
            floatArrayOf(maxX, maxY, maxZ), floatArrayOf(minX, maxY, maxZ),
            floatArrayOf(0f, 0f, 1f), color
        )
        // Back (-Z)
        addQuad(
            floatArrayOf(maxX, minY, minZ), floatArrayOf(minX, minY, minZ),
            floatArrayOf(minX, maxY, minZ), floatArrayOf(maxX, maxY, minZ),
            floatArrayOf(0f, 0f, -1f), color
        )
        // Top (+Y)
        addQuad(
            floatArrayOf(minX, maxY, maxZ), floatArrayOf(maxX, maxY, maxZ),
            floatArrayOf(maxX, maxY, minZ), floatArrayOf(minX, maxY, minZ),
            floatArrayOf(0f, 1f, 0f), color
        )
        // Bottom (-Y)
        addQuad(
            floatArrayOf(minX, minY, minZ), floatArrayOf(maxX, minY, minZ),
            floatArrayOf(maxX, minY, maxZ), floatArrayOf(minX, minY, maxZ),
            floatArrayOf(0f, -1f, 0f), color
        )
        // Right (+X)
        addQuad(
            floatArrayOf(maxX, minY, maxZ), floatArrayOf(maxX, minY, minZ),
            floatArrayOf(maxX, maxY, minZ), floatArrayOf(maxX, maxY, maxZ),
            floatArrayOf(1f, 0f, 0f), color
        )
        // Left (-X)
        addQuad(
            floatArrayOf(minX, minY, minZ), floatArrayOf(minX, minY, maxZ),
            floatArrayOf(minX, maxY, maxZ), floatArrayOf(minX, maxY, minZ),
            floatArrayOf(-1f, 0f, 0f), color
        )
    }

    fun addCylinder(
        centerX: Float, centerY: Float, centerZ: Float,
        radius: Float, length: Float,
        segments: Int, color: FloatArray,
        axisX: Boolean = true
    ) {
        val halfL = length * 0.5f
        for (i in 0 until segments) {
            val a1 = (i.toFloat() / segments) * 2f * Math.PI.toFloat()
            val a2 = ((i + 1).toFloat() / segments) * 2f * Math.PI.toFloat()
            val cos1 = cos(a1); val sin1 = sin(a1)
            val cos2 = cos(a2); val sin2 = sin(a2)

            if (axisX) {
                // Cylinder along X-axis (for wheels)
                val p1 = floatArrayOf(centerX - halfL, centerY + cos1 * radius, centerZ + sin1 * radius)
                val p2 = floatArrayOf(centerX + halfL, centerY + cos1 * radius, centerZ + sin1 * radius)
                val p3 = floatArrayOf(centerX + halfL, centerY + cos2 * radius, centerZ + sin2 * radius)
                val p4 = floatArrayOf(centerX - halfL, centerY + cos2 * radius, centerZ + sin2 * radius)
                val norm = floatArrayOf(0f, (cos1 + cos2) * 0.5f, (sin1 + sin2) * 0.5f)
                addQuad(p1, p2, p3, p4, norm, color)

                // Wheel caps
                addVertex(centerX + halfL, centerY, centerZ, 1f, 0f, 0f, 0.5f, 0.5f, color[0], color[1], color[2], color[3])
                addVertex(p2[0], p2[1], p2[2], 1f, 0f, 0f, 0.5f + cos1 * 0.5f, 0.5f + sin1 * 0.5f, color[0], color[1], color[2], color[3])
                addVertex(p3[0], p3[1], p3[2], 1f, 0f, 0f, 0.5f + cos2 * 0.5f, 0.5f + sin2 * 0.5f, color[0], color[1], color[2], color[3])

                addVertex(centerX - halfL, centerY, centerZ, -1f, 0f, 0f, 0.5f, 0.5f, color[0], color[1], color[2], color[3])
                addVertex(p4[0], p4[1], p4[2], -1f, 0f, 0f, 0.5f + cos2 * 0.5f, 0.5f + sin2 * 0.5f, color[0], color[1], color[2], color[3])
                addVertex(p1[0], p1[1], p1[2], -1f, 0f, 0f, 0.5f + cos1 * 0.5f, 0.5f + sin1 * 0.5f, color[0], color[1], color[2], color[3])
            }
        }
    }

    fun build(drawMode: Int = GLES20.GL_TRIANGLES): Mesh {
        val buffer = ByteBuffer.allocateDirect(data.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        for (f in data) {
            buffer.put(f)
        }
        buffer.position(0)
        return Mesh(buffer, data.size / 12, drawMode)
    }
}

object MeshFactory {
    fun hexToFloatColor(hex: Long): FloatArray {
        val r = ((hex shr 16) and 0xFF) / 255.0f
        val g = ((hex shr 8) and 0xFF) / 255.0f
        val b = (hex and 0xFF) / 255.0f
        val a = ((hex shr 24) and 0xFF).let { if (it == 0L) 1.0f else it / 255.0f }
        return floatArrayOf(r, g, b, a)
    }

    // 1. Detailed Car Mesh (Chassis, Cockpit, Glass, Spoiler, Lights)
    fun createCarBodyMesh(config: CarConfig, customColorHex: Long? = null): Mesh {
        val builder = MeshBuilder()
        val bodyColor = hexToFloatColor(customColorHex ?: config.primaryColor)
        val blackTrim = floatArrayOf(0.08f, 0.08f, 0.09f, 1.0f)
        val chromeColor = floatArrayOf(0.85f, 0.88f, 0.92f, 1.0f)
        val glassColor = floatArrayOf(0.12f, 0.18f, 0.25f, 0.75f)
        val headLightColor = floatArrayOf(1.0f, 1.0f, 0.85f, 1.0f)
        val tailLightColor = floatArrayOf(0.95f, 0.05f, 0.05f, 1.0f)
        val interiorColor = floatArrayOf(0.15f, 0.15f, 0.16f, 1.0f)
        val seatColor = floatArrayOf(0.22f, 0.08f, 0.08f, 1.0f)

        val halfW = config.trackWidth * 0.52f

        when (config.bodyType) {
            CarBodyType.SUPERCAR -> {
                // Sleek wedge supercar: low front hood, curved cabin, aggressive diffuser & rear wing
                // Lower chassis & side skirts
                builder.addBox(-halfW, 0.12f, -2.1f, halfW, 0.38f, 2.2f, bodyColor)
                // Front hood (sloping forward)
                builder.addBox(-halfW * 0.92f, 0.38f, 0.6f, halfW * 0.92f, 0.58f, 2.05f, bodyColor)
                // Front bumper splitter
                builder.addBox(-halfW * 0.95f, 0.06f, 1.95f, halfW * 0.95f, 0.16f, 2.3f, blackTrim)
                // Cabin & Roof (curved cockpit)
                builder.addBox(-halfW * 0.78f, 0.58f, -0.95f, halfW * 0.78f, 1.05f, 0.45f, bodyColor)
                // Windshield (front glass)
                builder.addQuad(
                    floatArrayOf(-halfW * 0.74f, 0.58f, 0.58f), floatArrayOf(halfW * 0.74f, 0.58f, 0.58f),
                    floatArrayOf(halfW * 0.72f, 1.03f, 0.38f), floatArrayOf(-halfW * 0.72f, 1.03f, 0.38f),
                    floatArrayOf(0f, 0.5f, 0.86f), glassColor
                )
                // Rear window glass
                builder.addQuad(
                    floatArrayOf(halfW * 0.72f, 1.03f, -0.92f), floatArrayOf(-halfW * 0.72f, 1.03f, -0.92f),
                    floatArrayOf(-halfW * 0.74f, 0.65f, -1.55f), floatArrayOf(halfW * 0.74f, 0.65f, -1.55f),
                    floatArrayOf(0f, 0.5f, -0.86f), glassColor
                )
                // Cockpit Interior - Dashboard
                builder.addBox(-halfW * 0.70f, 0.52f, 0.05f, halfW * 0.70f, 0.74f, 0.50f, interiorColor)
                // Sport Seats
                builder.addBox(-0.55f, 0.38f, -0.55f, -0.15f, 0.85f, -0.20f, seatColor) // Driver
                builder.addBox(0.15f, 0.38f, -0.55f, 0.55f, 0.85f, -0.20f, seatColor)  // Passenger
                // Rear Deck & Engine cover vents
                builder.addBox(-halfW * 0.85f, 0.48f, -2.15f, halfW * 0.85f, 0.65f, -0.95f, bodyColor)
                // Rear GT Wing / Spoiler
                builder.addBox(-halfW * 0.98f, 0.92f, -2.18f, halfW * 0.98f, 0.98f, -1.85f, blackTrim) // Wing blade
                builder.addBox(-0.45f, 0.65f, -2.05f, -0.38f, 0.92f, -1.95f, blackTrim) // Left pillar
                builder.addBox(0.38f, 0.65f, -2.05f, 0.45f, 0.92f, -1.95f, blackTrim)  // Right pillar
                // Headlights (glowing LED strips)
                builder.addBox(-halfW * 0.85f, 0.42f, 2.12f, -halfW * 0.45f, 0.52f, 2.22f, headLightColor)
                builder.addBox(halfW * 0.45f, 0.42f, 2.12f, halfW * 0.85f, 0.52f, 2.22f, headLightColor)
                // Taillights (continuous red LED bar)
                builder.addBox(-halfW * 0.85f, 0.52f, -2.22f, halfW * 0.85f, 0.60f, -2.14f, tailLightColor)
                // Dual Exhaust tips
                builder.addBox(-0.35f, 0.18f, -2.25f, -0.18f, 0.28f, -2.18f, chromeColor)
                builder.addBox(0.18f, 0.18f, -2.25f, 0.35f, 0.28f, -2.18f, chromeColor)
            }
            CarBodyType.MUSCLE -> {
                // Muscular angular muscle car: long wide hood with supercharger scoop, fastback roof
                builder.addBox(-halfW, 0.15f, -2.2f, halfW, 0.52f, 2.3f, bodyColor)
                // Hood scoop
                builder.addBox(-0.32f, 0.52f, 0.8f, 0.32f, 0.65f, 1.8f, blackTrim)
                // Roof & Cabin
                builder.addBox(-halfW * 0.85f, 0.52f, -1.1f, halfW * 0.85f, 1.15f, 0.4f, bodyColor)
                // Windshield
                builder.addQuad(
                    floatArrayOf(-halfW * 0.80f, 0.52f, 0.5f), floatArrayOf(halfW * 0.80f, 0.52f, 0.5f),
                    floatArrayOf(halfW * 0.78f, 1.12f, 0.35f), floatArrayOf(-halfW * 0.78f, 1.12f, 0.35f),
                    floatArrayOf(0f, 0.6f, 0.8f), glassColor
                )
                // Interior
                builder.addBox(-halfW * 0.75f, 0.48f, -0.05f, halfW * 0.75f, 0.75f, 0.45f, interiorColor)
                builder.addBox(-0.55f, 0.42f, -0.55f, -0.15f, 0.90f, -0.20f, seatColor)
                builder.addBox(0.15f, 0.42f, -0.55f, 0.55f, 0.90f, -0.20f, seatColor)
                // Front Grille
                builder.addBox(-halfW * 0.85f, 0.25f, 2.25f, halfW * 0.85f, 0.48f, 2.32f, blackTrim)
                // Quad round headlights
                builder.addBox(-halfW * 0.75f, 0.32f, 2.31f, -halfW * 0.55f, 0.44f, 2.34f, headLightColor)
                builder.addBox(halfW * 0.55f, 0.32f, 2.31f, halfW * 0.75f, 0.44f, 2.34f, headLightColor)
                // Taillights
                builder.addBox(-halfW * 0.85f, 0.40f, -2.25f, halfW * 0.85f, 0.52f, -2.21f, tailLightColor)
                // Ducktail spoiler
                builder.addBox(-halfW * 0.92f, 0.55f, -2.25f, halfW * 0.92f, 0.68f, -2.10f, blackTrim)
            }
            CarBodyType.OFFROAD -> {
                // High ground clearance 4x4 rugged SUV / Thar style
                builder.addBox(-halfW, 0.28f, -2.0f, halfW, 0.72f, 2.1f, bodyColor)
                // Cabin roll cage & hardtop
                builder.addBox(-halfW * 0.88f, 0.72f, -1.8f, halfW * 0.88f, 1.48f, 0.2f, blackTrim)
                // Flat windshield
                builder.addQuad(
                    floatArrayOf(-halfW * 0.82f, 0.72f, 0.25f), floatArrayOf(halfW * 0.82f, 0.72f, 0.25f),
                    floatArrayOf(halfW * 0.80f, 1.42f, 0.15f), floatArrayOf(-halfW * 0.80f, 1.42f, 0.15f),
                    floatArrayOf(0f, 0.2f, 0.98f), glassColor
                )
                // Roof rack with auxiliary lights
                builder.addBox(-halfW * 0.82f, 1.48f, -1.6f, halfW * 0.82f, 1.58f, 0.1f, blackTrim)
                builder.addBox(-0.6f, 1.58f, 0.05f, 0.6f, 1.68f, 0.15f, headLightColor) // Light bar
                // Heavy front bull bar
                builder.addBox(-halfW * 0.95f, 0.15f, 2.05f, halfW * 0.95f, 0.48f, 2.25f, blackTrim)
                // Spare wheel mounted on rear door
                builder.addCylinder(0f, 0.78f, -2.15f, 0.38f, 0.25f, 14, blackTrim, axisX = false)
                // Round headlights & fog lamps
                builder.addBox(-halfW * 0.72f, 0.42f, 2.12f, -halfW * 0.48f, 0.62f, 2.16f, headLightColor)
                builder.addBox(halfW * 0.48f, 0.42f, 2.12f, halfW * 0.72f, 0.62f, 2.16f, headLightColor)
                builder.addBox(-halfW * 0.82f, 0.45f, -2.05f, -halfW * 0.60f, 0.68f, -2.01f, tailLightColor)
                builder.addBox(halfW * 0.60f, 0.45f, -2.05f, halfW * 0.82f, 0.68f, -2.01f, tailLightColor)
            }
            CarBodyType.DRIFT_GT -> {
                // Widebody GT tuner with carbon diffusers, flared fenders, vented hood
                builder.addBox(-halfW * 1.05f, 0.10f, -2.1f, halfW * 1.05f, 0.42f, 2.2f, bodyColor)
                builder.addBox(-halfW * 0.82f, 0.42f, -1.05f, halfW * 0.82f, 1.08f, 0.4f, bodyColor)
                // Glass
                builder.addQuad(
                    floatArrayOf(-halfW * 0.76f, 0.42f, 0.5f), floatArrayOf(halfW * 0.76f, 0.42f, 0.5f),
                    floatArrayOf(halfW * 0.74f, 1.05f, 0.35f), floatArrayOf(-halfW * 0.74f, 1.05f, 0.35f),
                    floatArrayOf(0f, 0.55f, 0.83f), glassColor
                )
                // Interior
                builder.addBox(-halfW * 0.72f, 0.45f, 0.05f, halfW * 0.72f, 0.72f, 0.48f, interiorColor)
                builder.addBox(-0.52f, 0.35f, -0.50f, -0.15f, 0.85f, -0.18f, seatColor)
                // Huge High-Downforce GT Carbon Wing
                builder.addBox(-halfW * 1.10f, 1.15f, -2.15f, halfW * 1.10f, 1.22f, -1.75f, blackTrim)
                builder.addBox(-0.55f, 0.55f, -2.0f, -0.48f, 1.15f, -1.9f, blackTrim)
                builder.addBox(0.48f, 0.55f, -2.0f, 0.55f, 1.15f, -1.9f, blackTrim)
                // Front and Rear lights
                builder.addBox(-halfW * 0.88f, 0.38f, 2.15f, -halfW * 0.42f, 0.48f, 2.24f, headLightColor)
                builder.addBox(halfW * 0.42f, 0.38f, 2.15f, halfW * 0.88f, 0.48f, 2.24f, headLightColor)
                builder.addBox(-halfW * 0.88f, 0.45f, -2.22f, halfW * 0.88f, 0.55f, -2.16f, tailLightColor)
            }
        }

        return builder.build()
    }

    // 2. 3D Steering Wheel (drawn inside cockpit, rotates with steering angle!)
    fun createSteeringWheelMesh(): Mesh {
        val builder = MeshBuilder()
        val wheelColor = floatArrayOf(0.12f, 0.12f, 0.13f, 1.0f)
        val emblemColor = floatArrayOf(0.9f, 0.75f, 0.1f, 1.0f)

        // Outer ring (radius ~ 0.18m)
        val radius = 0.18f
        val segs = 16
        for (i in 0 until segs) {
            val a1 = (i.toFloat() / segs) * 2f * Math.PI.toFloat()
            val a2 = ((i + 1).toFloat() / segs) * 2f * Math.PI.toFloat()
            val x1 = cos(a1) * radius; val y1 = sin(a1) * radius
            val x2 = cos(a2) * radius; val y2 = sin(a2) * radius
            builder.addBox(
                min(x1, x2) - 0.015f, min(y1, y2) - 0.015f, -0.015f,
                max(x1, x2) + 0.015f, max(y1, y2) + 0.015f, 0.015f,
                wheelColor
            )
        }
        // Center hub and spokes
        builder.addBox(-0.045f, -0.045f, -0.02f, 0.045f, 0.045f, 0.01f, wheelColor)
        builder.addBox(-0.02f, -0.02f, 0.01f, 0.02f, 0.02f, 0.025f, emblemColor)
        // Horizontal spokes
        builder.addBox(-radius * 0.9f, -0.025f, -0.015f, radius * 0.9f, 0.025f, 0.015f, wheelColor)
        // Bottom vertical spoke
        builder.addBox(-0.025f, -radius * 0.9f, -0.015f, 0.025f, 0f, 0.015f, wheelColor)

        return builder.build()
    }

    // 3. 3D Wheel Mesh (Tire rubber + alloy rim + brake disc)
    fun createWheelMesh(wheelRadius: Float = 0.34f, tireWidth: Float = 0.26f): Mesh {
        val builder = MeshBuilder()
        val tireColor = floatArrayOf(0.12f, 0.12f, 0.13f, 1.0f)
        val rimColor = floatArrayOf(0.82f, 0.85f, 0.88f, 1.0f)
        val brakeColor = floatArrayOf(0.92f, 0.12f, 0.12f, 1.0f) // Red Brembo caliper

        // Outer tire
        builder.addCylinder(0f, 0f, 0f, wheelRadius, tireWidth, 20, tireColor, axisX = true)
        // Inner rim
        builder.addCylinder(0f, 0f, 0f, wheelRadius * 0.72f, tireWidth * 0.95f, 16, rimColor, axisX = true)
        // Brake caliper
        builder.addBox(0.01f, wheelRadius * 0.35f, -0.06f, tireWidth * 0.42f, wheelRadius * 0.65f, 0.06f, brakeColor)

        return builder.build()
    }

    // 4. Highway Segment (Asphalt, road markings, barriers, streetlights, overhead gantry)
    fun createHighwaySegment(length: Float = 120f, width: Float = 16f): Mesh {
        val builder = MeshBuilder()
        val asphaltColor = floatArrayOf(0.15f, 0.16f, 0.18f, 1.0f)
        val lineWhite = floatArrayOf(0.95f, 0.95f, 0.95f, 1.0f)
        val lineYellow = floatArrayOf(0.98f, 0.85f, 0.15f, 1.0f)
        val shoulderColor = floatArrayOf(0.25f, 0.26f, 0.27f, 1.0f)
        val grassColor = floatArrayOf(0.18f, 0.32f, 0.16f, 1.0f)
        val barrierColor = floatArrayOf(0.68f, 0.72f, 0.76f, 1.0f)
        val poleColor = floatArrayOf(0.45f, 0.48f, 0.52f, 1.0f)
        val lightGlowColor = floatArrayOf(1.0f, 0.95f, 0.7f, 1.0f)

        val halfW = width * 0.5f

        // Asphalt main surface
        builder.addQuad(
            floatArrayOf(-halfW, 0f, length), floatArrayOf(halfW, 0f, length),
            floatArrayOf(halfW, 0f, 0f), floatArrayOf(-halfW, 0f, 0f),
            floatArrayOf(0f, 1f, 0f), asphaltColor
        )

        // Road shoulders (curb edges)
        builder.addQuad(
            floatArrayOf(-halfW - 2.5f, 0.05f, length), floatArrayOf(-halfW, 0.05f, length),
            floatArrayOf(-halfW, 0.05f, 0f), floatArrayOf(-halfW - 2.5f, 0.05f, 0f),
            floatArrayOf(0f, 1f, 0f), shoulderColor
        )
        builder.addQuad(
            floatArrayOf(halfW, 0.05f, length), floatArrayOf(halfW + 2.5f, 0.05f, length),
            floatArrayOf(halfW + 2.5f, 0.05f, 0f), floatArrayOf(halfW, 0.05f, 0f),
            floatArrayOf(0f, 1f, 0f), shoulderColor
        )

        // Outer grass terrain banks
        builder.addQuad(
            floatArrayOf(-halfW - 35f, -0.4f, length), floatArrayOf(-halfW - 2.5f, 0.05f, length),
            floatArrayOf(-halfW - 2.5f, 0.05f, 0f), floatArrayOf(-halfW - 35f, -0.4f, 0f),
            floatArrayOf(0f, 1f, 0f), grassColor
        )
        builder.addQuad(
            floatArrayOf(halfW + 2.5f, 0.05f, length), floatArrayOf(halfW + 35f, -0.4f, length),
            floatArrayOf(halfW + 35f, -0.4f, 0f), floatArrayOf(halfW + 2.5f, 0.05f, 0f),
            floatArrayOf(0f, 1f, 0f), grassColor
        )

        // Solid Yellow Shoulder lines
        builder.addQuad(
            floatArrayOf(-halfW + 0.3f, 0.01f, length), floatArrayOf(-halfW + 0.55f, 0.01f, length),
            floatArrayOf(-halfW + 0.55f, 0.01f, 0f), floatArrayOf(-halfW + 0.3f, 0.01f, 0f),
            floatArrayOf(0f, 1f, 0f), lineYellow
        )
        builder.addQuad(
            floatArrayOf(halfW - 0.55f, 0.01f, length), floatArrayOf(halfW - 0.3f, 0.01f, length),
            floatArrayOf(halfW - 0.3f, 0.01f, 0f), floatArrayOf(halfW - 0.55f, 0.01f, 0f),
            floatArrayOf(0f, 1f, 0f), lineYellow
        )

        // Dashed White Lane Markings (3 lane dividers for 4 lanes)
        val laneDividersX = floatArrayOf(-3.5f, 0.0f, 3.5f)
        for (lx in laneDividersX) {
            var z = 0f
            while (z < length) {
                builder.addQuad(
                    floatArrayOf(lx - 0.12f, 0.01f, z + 6.0f), floatArrayOf(lx + 0.12f, 0.01f, z + 6.0f),
                    floatArrayOf(lx + 0.12f, 0.01f, z), floatArrayOf(lx - 0.12f, 0.01f, z),
                    floatArrayOf(0f, 1f, 0f), lineWhite
                )
                z += 12.0f // 6m mark, 6m gap
            }
        }

        // Metal Guard Rails on shoulders
        builder.addBox(-halfW - 1.8f, 0.2f, 0f, -halfW - 1.6f, 0.85f, length, barrierColor)
        builder.addBox(halfW + 1.6f, 0.2f, 0f, halfW + 1.8f, 0.85f, length, barrierColor)

        // Street Lights with posts and lanterns every 60 meters
        var lightZ = 20f
        while (lightZ < length) {
            // Left light
            builder.addBox(-halfW - 2.8f, 0f, lightZ - 0.15f, -halfW - 2.5f, 7.5f, lightZ + 0.15f, poleColor)
            builder.addBox(-halfW - 2.8f, 7.2f, lightZ - 0.15f, -halfW - 0.5f, 7.5f, lightZ + 0.15f, poleColor)
            builder.addBox(-halfW - 1.0f, 7.0f, lightZ - 0.3f, -halfW - 0.4f, 7.2f, lightZ + 0.3f, lightGlowColor)

            // Right light
            builder.addBox(halfW + 2.5f, 0f, lightZ - 0.15f, halfW + 2.8f, 7.5f, lightZ + 0.15f, poleColor)
            builder.addBox(halfW + 0.5f, 7.2f, lightZ - 0.15f, halfW + 2.8f, 7.5f, lightZ + 0.15f, poleColor)
            builder.addBox(halfW + 0.4f, 7.0f, lightZ - 0.3f, halfW + 1.0f, 7.2f, lightZ + 0.3f, lightGlowColor)

            lightZ += 60f
        }

        return builder.build()
    }

    // 5. Overhead Highway Gantry Sign ("NH 48", "PUNE 110 KM", etc.)
    fun createHighwayGantry(roadWidth: Float = 16f): Mesh {
        val builder = MeshBuilder()
        val metalTruss = floatArrayOf(0.35f, 0.38f, 0.42f, 1.0f)
        val signGreen = floatArrayOf(0.08f, 0.45f, 0.22f, 1.0f) // Indian highway green signboard
        val signTextWhite = floatArrayOf(0.95f, 0.95f, 0.95f, 1.0f)
        val halfW = roadWidth * 0.5f + 2.5f

        // Left and right pillars
        builder.addBox(-halfW - 0.4f, 0f, -0.4f, -halfW + 0.4f, 7.8f, 0.4f, metalTruss)
        builder.addBox(halfW - 0.4f, 0f, -0.4f, halfW + 0.4f, 7.8f, 0.4f, metalTruss)
        // Crossbeam
        builder.addBox(-halfW, 7.2f, -0.4f, halfW, 7.8f, 0.4f, metalTruss)
        // Green Signboards
        builder.addBox(-halfW * 0.75f, 5.2f, 0.42f, -halfW * 0.05f, 7.2f, 0.55f, signGreen)
        builder.addBox(halfW * 0.05f, 5.2f, 0.42f, halfW * 0.75f, 7.2f, 0.55f, signGreen)
        // Stylized text lines
        builder.addBox(-halfW * 0.70f, 6.4f, 0.56f, -halfW * 0.15f, 6.8f, 0.58f, signTextWhite)
        builder.addBox(-halfW * 0.70f, 5.6f, 0.56f, -halfW * 0.30f, 6.0f, 0.58f, signTextWhite)

        builder.addBox(halfW * 0.12f, 6.4f, 0.56f, halfW * 0.68f, 6.8f, 0.58f, signTextWhite)
        builder.addBox(halfW * 0.12f, 5.6f, 0.56f, halfW * 0.45f, 6.0f, 0.58f, signTextWhite)

        return builder.build()
    }

    // 6. Fuel Station (IndianOil / Bharat Petroleum style canopy & pumps)
    fun createFuelStation(): Mesh {
        val builder = MeshBuilder()
        val canopyColor = floatArrayOf(0.95f, 0.55f, 0.05f, 1.0f) // Vibrant orange canopy
        val columnColor = floatArrayOf(0.92f, 0.92f, 0.95f, 1.0f)
        val pumpColor = floatArrayOf(0.12f, 0.48f, 0.85f, 1.0f)
        val buildingWall = floatArrayOf(0.85f, 0.86f, 0.88f, 1.0f)
        val glassDoor = floatArrayOf(0.15f, 0.25f, 0.35f, 0.9f)

        // Canopy roof
        builder.addBox(12f, 5.5f, -15f, 26f, 6.2f, 15f, canopyColor)
        // 4 Canopy support pillars
        builder.addBox(14f, 0f, -10f, 15f, 5.5f, -9f, columnColor)
        builder.addBox(23f, 0f, -10f, 24f, 5.5f, -9f, columnColor)
        builder.addBox(14f, 0f, 9f, 15f, 5.5f, 10f, columnColor)
        builder.addBox(23f, 0f, 9f, 24f, 5.5f, 10f, columnColor)

        // 2 Fuel pump islands
        builder.addBox(17.5f, 0.2f, -8f, 20.5f, 2.2f, -6f, pumpColor)
        builder.addBox(17.5f, 0.2f, 6f, 20.5f, 2.2f, 8f, pumpColor)

        // Convenience store building
        builder.addBox(28f, 0f, -12f, 40f, 4.5f, 12f, buildingWall)
        // Store glass entrance
        builder.addBox(27.8f, 0f, -4f, 28.1f, 3.2f, 4f, glassDoor)

        return builder.build()
    }

    // 7. Suspension Bridge with arch trusses
    fun createBridge(): Mesh {
        val builder = MeshBuilder()
        val concretePillar = floatArrayOf(0.72f, 0.74f, 0.76f, 1.0f)
        val steelRed = floatArrayOf(0.85f, 0.22f, 0.18f, 1.0f)

        // Main Bridge Arch Trusses
        val bridgeLength = 80f
        val archHeight = 18f
        val segs = 16
        for (side in floatArrayOf(-9.5f, 9.5f)) {
            for (i in 0 until segs) {
                val z1 = (i.toFloat() / segs) * bridgeLength - bridgeLength * 0.5f
                val z2 = ((i + 1).toFloat() / segs) * bridgeLength - bridgeLength * 0.5f
                val y1 = sin((i.toFloat() / segs) * Math.PI.toFloat()) * archHeight + 1.0f
                val y2 = sin(((i + 1).toFloat() / segs) * Math.PI.toFloat()) * archHeight + 1.0f

                builder.addBox(
                    side - 0.25f, min(y1, y2) - 0.25f, min(z1, z2),
                    side + 0.25f, max(y1, y2) + 0.25f, max(z1, z2),
                    steelRed
                )
                // Vertical suspension cables
                builder.addBox(side - 0.08f, 0.5f, z1 - 0.08f, side + 0.08f, y1, z1 + 0.08f, concretePillar)
            }
        }
        return builder.build()
    }

    // 8. 3D Trees
    fun createTree(): Mesh {
        val builder = MeshBuilder()
        val trunkColor = floatArrayOf(0.38f, 0.25f, 0.18f, 1.0f)
        val foliageColor = floatArrayOf(0.12f, 0.45f, 0.18f, 1.0f)

        // Trunk
        builder.addBox(-0.35f, 0f, -0.35f, 0.35f, 3.2f, 0.35f, trunkColor)
        // 3 tiers of foliage cones/boxes
        builder.addBox(-2.2f, 2.8f, -2.2f, 2.2f, 5.0f, 2.2f, foliageColor)
        builder.addBox(-1.7f, 4.8f, -1.7f, 1.7f, 6.8f, 1.7f, foliageColor)
        builder.addBox(-1.0f, 6.6f, -1.0f, 1.0f, 8.2f, 1.0f, foliageColor)

        return builder.build()
    }

    // 9. 3D Buildings for Skyline & Roadside
    fun createBuilding(width: Float, height: Float, depth: Float, wallHex: Long): Mesh {
        val builder = MeshBuilder()
        val wallColor = hexToFloatColor(wallHex)
        val windowColor = floatArrayOf(0.92f, 0.88f, 0.65f, 1.0f) // Warm night window light

        // Main tower body
        builder.addBox(-width * 0.5f, 0f, -depth * 0.5f, width * 0.5f, height, depth * 0.5f, wallColor)

        // Rows of windows
        var y = 3f
        while (y < height - 2f) {
            var x = -width * 0.5f + 1.2f
            while (x < width * 0.5f - 1.2f) {
                // Front window
                builder.addBox(x, y, depth * 0.5f + 0.05f, x + 1.2f, y + 1.6f, depth * 0.5f + 0.15f, windowColor)
                // Back window
                builder.addBox(x, y, -depth * 0.5f - 0.15f, x + 1.2f, y + 1.6f, -depth * 0.5f - 0.05f, windowColor)
                x += 2.4f
            }
            y += 3.2f
        }

        return builder.build()
    }

    // 10. Traffic Vehicles (Sedans, Trucks, Indian Auto-rickshaws, SUVs)
    fun createTrafficMesh(type: TrafficVehicleType, colorHex: Long): Mesh {
        val builder = MeshBuilder()
        val mainColor = hexToFloatColor(colorHex)
        val blackTrim = floatArrayOf(0.1f, 0.1f, 0.1f, 1.0f)
        val yellowAuto = floatArrayOf(0.98f, 0.82f, 0.12f, 1.0f)
        val greenAuto = floatArrayOf(0.12f, 0.55f, 0.25f, 1.0f) // Traditional Indian CNG auto-rickshaw green
        val glassColor = floatArrayOf(0.2f, 0.25f, 0.3f, 0.8f)
        val headLightColor = floatArrayOf(1f, 1f, 0.9f, 1f)
        val tailLightColor = floatArrayOf(0.95f, 0.08f, 0.08f, 1f)

        when (type) {
            TrafficVehicleType.SEDAN -> {
                builder.addBox(-0.95f, 0.15f, -2.1f, 0.95f, 0.55f, 2.1f, mainColor)
                builder.addBox(-0.82f, 0.55f, -1.0f, 0.82f, 1.15f, 0.5f, glassColor)
                builder.addBox(-0.75f, 0.42f, 2.05f, -0.45f, 0.52f, 2.15f, headLightColor)
                builder.addBox(0.45f, 0.42f, 2.05f, 0.75f, 0.52f, 2.15f, headLightColor)
                builder.addBox(-0.80f, 0.45f, -2.15f, 0.80f, 0.55f, -2.05f, tailLightColor)
            }
            TrafficVehicleType.TRUCK -> {
                // Indian heavy transport truck with colorful decorated cabin & cargo container
                builder.addBox(-1.25f, 0.25f, 1.1f, 1.25f, 2.4f, 3.8f, mainColor) // Cabin
                builder.addBox(-1.18f, 1.4f, 2.2f, 1.18f, 2.2f, 3.7f, glassColor) // Windshield
                builder.addBox(-1.35f, 0.35f, -3.8f, 1.35f, 2.8f, 1.0f, floatArrayOf(0.85f, 0.45f, 0.15f, 1.0f)) // Cargo box
                builder.addBox(-1.0f, 0.5f, 3.85f, -0.6f, 0.75f, 3.9f, headLightColor)
                builder.addBox(0.6f, 0.5f, 3.85f, 1.0f, 0.75f, 3.9f, headLightColor)
                builder.addBox(-1.2f, 0.4f, -3.85f, 1.2f, 0.6f, -3.75f, tailLightColor)
            }
            TrafficVehicleType.AUTO_RICKSHAW -> {
                // Indian 3-wheeler auto-rickshaw: yellow canvas roof, green lower body, open sides
                builder.addBox(-0.65f, 0.15f, -1.3f, 0.65f, 0.65f, 1.1f, greenAuto) // Lower body
                builder.addBox(-0.68f, 0.65f, -1.35f, 0.68f, 1.45f, 0.9f, yellowAuto) // Yellow roof
                builder.addBox(-0.55f, 0.65f, 0.7f, 0.55f, 1.25f, 1.05f, glassColor) // Front windshield
                builder.addBox(-0.18f, 0.38f, 1.12f, 0.18f, 0.58f, 1.2f, headLightColor) // Single center headlight
                builder.addBox(-0.58f, 0.35f, -1.35f, 0.58f, 0.50f, -1.25f, tailLightColor)
            }
            TrafficVehicleType.SUV -> {
                builder.addBox(-1.05f, 0.22f, -2.2f, 1.05f, 0.75f, 2.2f, mainColor)
                builder.addBox(-0.95f, 0.75f, -1.8f, 0.95f, 1.38f, 0.8f, glassColor)
                builder.addBox(-0.85f, 0.48f, 2.18f, -0.45f, 0.65f, 2.25f, headLightColor)
                builder.addBox(0.45f, 0.48f, 2.18f, 0.85f, 0.65f, 2.25f, headLightColor)
                builder.addBox(-0.90f, 0.55f, -2.25f, 0.90f, 0.70f, -2.15f, tailLightColor)
            }
        }
        return builder.build()
    }
}

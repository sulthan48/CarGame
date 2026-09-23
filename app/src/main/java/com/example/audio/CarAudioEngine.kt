package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.*
import kotlin.random.Random

class CarAudioEngine {
    private var audioTrack: AudioTrack? = null
    private val isRunning = AtomicBoolean(false)
    private var audioThread: Thread? = null

    // Real-time parameters passed from game loop
    @Volatile var targetRpm: Float = 900f
    @Volatile var targetThrottle: Float = 0f
    @Volatile var tireScreechAmount: Float = 0f // 0 to 1
    @Volatile var isShifting: Boolean = false
    @Volatile var isBoostActive: Boolean = false
    @Volatile var triggerCrash: Boolean = false
    @Volatile var isMuted: Boolean = false

    private val sampleRate = 22050
    private val bufferSize = AudioTrack.getMinBufferSize(
        sampleRate,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_16BIT
    ).coerceAtLeast(2048)

    fun start() {
        if (isRunning.get()) return
        isRunning.set(true)

        try {
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()

            audioTrack?.play()
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }

        audioThread = Thread({
            synthesizeAudioLoop()
        }, "CarAudioSynthesisThread")
        audioThread?.priority = Thread.MAX_PRIORITY
        audioThread?.start()
    }

    private fun synthesizeAudioLoop() {
        val shortBuffer = ShortArray(1024)
        var phase1 = 0.0
        var phase2 = 0.0
        var subPhase = 0.0
        var currentPitch = 40.0
        var smoothThrottle = 0.0f
        var smoothScreech = 0.0f
        var shiftPopDuration = 0
        var crashDuration = 0
        var prevShifting = false

        while (isRunning.get()) {
            if (isMuted) {
                shortBuffer.fill(0)
                audioTrack?.write(shortBuffer, 0, shortBuffer.size)
                try { Thread.sleep(20) } catch (_: InterruptedException) {}
                continue
            }

            // Check triggers
            if (isShifting && !prevShifting) {
                shiftPopDuration = 2200 // samples of blow-off / pop
            }
            prevShifting = isShifting

            if (triggerCrash) {
                triggerCrash = false
                crashDuration = 6000
            }

            // Smoothing
            smoothThrottle += (targetThrottle - smoothThrottle) * 0.05f
            smoothScreech += (tireScreechAmount - smoothScreech) * 0.08f

            // Target base frequency: RPM to Hz (for an 8-cylinder / 10-cylinder engine, ~4 to 5 pulses per revolution)
            val baseFreq = ((targetRpm / 60.0) * 4.2).coerceIn(40.0, 750.0)
            currentPitch += (baseFreq - currentPitch) * 0.06

            for (i in shortBuffer.indices) {
                val dt = 1.0 / sampleRate

                phase1 = (phase1 + currentPitch * dt) % 1.0
                phase2 = (phase2 + currentPitch * 2.0 * dt) % 1.0
                subPhase = (subPhase + currentPitch * 0.5 * dt) % 1.0

                // Engine wave: combination of saw wave + distorted sine + sub-harmonic rumble
                val saw1 = (2.0 * phase1 - 1.0)
                val harm2 = sin(2.0 * Math.PI * phase2) * 0.4
                val sub = sin(2.0 * Math.PI * subPhase) * 0.5

                // Growl distortion when throttle is applied
                val rawEngine = (saw1 * 0.6 + harm2 + sub)
                val drive = 1.0 + smoothThrottle * 1.8
                val saturatedEngine = tanh(rawEngine * drive)

                val engineVol = (0.28 + smoothThrottle * 0.35 + (targetRpm / 9000.0) * 0.25).coerceIn(0.15, 0.85)
                var sample = saturatedEngine * engineVol

                // Turbo whine
                if (isBoostActive) {
                    val turboFreq = 1800.0 + (targetRpm / 9000.0) * 1600.0
                    val turbo = sin(2.0 * Math.PI * (phase1 * (turboFreq / currentPitch))) * 0.15
                    sample += turbo
                }

                // Tire screech (band-limited noise)
                if (smoothScreech > 0.05f) {
                    val whiteNoise = (Random.nextFloat() * 2f - 1f)
                    val screech = whiteNoise * smoothScreech * 0.45
                    sample += screech
                }

                // Shift pop / blow-off valve
                if (shiftPopDuration > 0) {
                    shiftPopDuration--
                    val popNoise = (Random.nextFloat() * 2f - 1f) * (shiftPopDuration / 2200f) * 0.45
                    sample += popNoise
                }

                // Crash noise
                if (crashDuration > 0) {
                    crashDuration--
                    val crashNoise = (Random.nextFloat() * 2f - 1f) * (crashDuration / 6000f) * 0.8
                    sample += crashNoise
                }

                val clamped = (sample.coerceIn(-1.0, 1.0) * 32767.0).toInt().toShort()
                shortBuffer[i] = clamped
            }

            audioTrack?.write(shortBuffer, 0, shortBuffer.size)
        }
    }

    fun stop() {
        isRunning.set(false)
        try {
            audioThread?.join(500)
        } catch (_: Exception) {}
        try {
            audioTrack?.stop()
            audioTrack?.release()
        } catch (_: Exception) {}
        audioTrack = null
    }
}

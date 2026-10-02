package com.example.util

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Robust Sound and Haptic feedback controller for Royal Ludo Empire.
 * Utilizes low-latency native ToneGenerator synthesis and system Vibrator.
 * Safely wraps all audio/vibration calls to ensure zero crashes on any device.
 */
class LudoSoundManager private constructor(private val context: Context) {

    private var toneGenerator: ToneGenerator? = null
    private var vibrator: Vibrator? = null

    var isSoundEnabled: Boolean = true
    var isHapticsEnabled: Boolean = true

    init {
        try {
            toneGenerator = ToneGenerator(AudioManager.STREAM_MUSIC, 75)
        } catch (t: Throwable) {
            Log.w("LudoSoundManager", "ToneGenerator init failed, sound will be muted: ${t.message}")
            toneGenerator = null
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
        } catch (t: Throwable) {
            Log.w("LudoSoundManager", "Vibrator init failed: ${t.message}")
            vibrator = null
        }
    }

    fun playDiceRoll() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 35)
        } catch (_: Throwable) {}
        vibrate(20)
    }

    fun playTokenStep() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 40)
        } catch (_: Throwable) {}
        vibrate(15)
    }

    fun playTokenUnlocked() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_PIP, 120)
        } catch (_: Throwable) {}
        vibrate(45)
    }

    fun playRolledSix() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_PROMPT, 180)
        } catch (_: Throwable) {}
        vibratePattern(longArrayOf(0, 40, 50, 80))
    }

    fun playCapture() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_NETWORK_LITE, 220)
        } catch (_: Throwable) {}
        vibratePattern(longArrayOf(0, 60, 40, 100))
    }

    fun playVictory() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_CDMA_HIGH_L, 350)
        } catch (_: Throwable) {}
        vibratePattern(longArrayOf(0, 80, 60, 120, 60, 200))
    }

    fun playButtonClick() {
        if (!isSoundEnabled) return
        try {
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP2, 25)
        } catch (_: Throwable) {}
        vibrate(12)
    }

    private fun vibrate(durationMs: Long) {
        if (!isHapticsEnabled) return
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(durationMs)
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    private fun vibratePattern(pattern: LongArray) {
        if (!isHapticsEnabled) return
        try {
            vibrator?.let { v ->
                if (v.hasVibrator()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        v.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(pattern, -1)
                    }
                }
            }
        } catch (_: Throwable) {}
    }

    companion object {
        @Volatile
        private var instance: LudoSoundManager? = null

        fun getInstance(context: Context): LudoSoundManager {
            return instance ?: synchronized(this) {
                instance ?: LudoSoundManager(context.applicationContext).also { instance = it }
            }
        }
    }
}

package com.example.sound

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.*
import kotlin.random.Random

/**
 * Premium sound effects engine for Royal Ludo Empire.
 * Features dedicated synthesized high-fidelity PCM audio for:
 * - Dice Roll (realistic wooden tumble & rattle)
 * - Piece Step (crisp tactile wooden/crystal hop per square)
 * - Piece Unlocked (regal ascending arpeggio chime from home base)
 * - Piece Capture (heavy impact blade clash & knockback)
 * - Goal Scored (shimmering victory harp chime)
 * - Lucky Six (bright twin bell sparkle)
 * - Turn Pass (soft descending forfeit cue)
 * - Grand Victory (imperial royal brass fanfare)
 */
object LudoSoundManager {

    private const val SAMPLE_RATE = 22050

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .setUsage(AudioAttributes.USAGE_GAME)
        .build()

    private val audioFormat = AudioFormat.Builder()
        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
        .setSampleRate(SAMPLE_RATE)
        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
        .build()

    private var soundDiceRoll: AudioTrack? = null
    private var soundPieceStep: AudioTrack? = null
    private var soundPieceUnlocked: AudioTrack? = null
    private var soundPieceCapture: AudioTrack? = null
    private var soundGoalScored: AudioTrack? = null
    private var soundLuckySix: AudioTrack? = null
    private var soundTurnPass: AudioTrack? = null
    private var soundGrandVictory: AudioTrack? = null

    private var isInitialized = false
    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            initSounds()
        }
    }

    private fun initSounds() {
        try {
            soundDiceRoll = createTrack(generateDiceRollSound())
            soundPieceStep = createTrack(generatePieceStepSound())
            soundPieceUnlocked = createTrack(generatePieceUnlockedSound())
            soundPieceCapture = createTrack(generatePieceCaptureSound())
            soundGoalScored = createTrack(generateGoalScoredSound())
            soundLuckySix = createTrack(generateLuckySixSound())
            soundTurnPass = createTrack(generateTurnPassSound())
            soundGrandVictory = createTrack(generateGrandVictorySound())
            isInitialized = true
        } catch (e: Exception) {
            // Audio initialization fallback
        }
    }

    private fun createTrack(pcmBytes: ByteArray): AudioTrack {
        val track = AudioTrack(
            audioAttributes,
            audioFormat,
            pcmBytes.size,
            AudioTrack.MODE_STATIC,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
        track.write(pcmBytes, 0, pcmBytes.size)
        return track
    }

    private fun playTrack(track: AudioTrack?, fallbackGenerator: () -> ByteArray) {
        scope.launch {
            try {
                if (track != null && track.state == AudioTrack.STATE_INITIALIZED) {
                    track.pause()
                    track.reloadStaticData()
                    track.play()
                } else {
                    val fallback = createTrack(fallbackGenerator())
                    fallback.play()
                }
            } catch (e: Exception) {
                try {
                    val fallback = createTrack(fallbackGenerator())
                    fallback.play()
                } catch (ignored: Exception) {}
            }
        }
    }

    // --- SOUND TRIGGERS ---

    fun playDiceRoll() {
        playTrack(soundDiceRoll) { generateDiceRollSound() }
    }

    fun playPieceStep() {
        playTrack(soundPieceStep) { generatePieceStepSound() }
    }

    fun playPieceUnlocked() {
        playTrack(soundPieceUnlocked) { generatePieceUnlockedSound() }
    }

    fun playPieceCapture() {
        playTrack(soundPieceCapture) { generatePieceCaptureSound() }
    }

    fun playGoalScored() {
        playTrack(soundGoalScored) { generateGoalScoredSound() }
    }

    fun playLuckySix() {
        playTrack(soundLuckySix) { generateLuckySixSound() }
    }

    fun playTurnPass() {
        playTrack(soundTurnPass) { generateTurnPassSound() }
    }

    fun playGrandVictory() {
        playTrack(soundGrandVictory) { generateGrandVictorySound() }
    }

    // --- SYNTHESIZED SOUND WAVEFORM GENERATORS ---

    /**
     * Realistic dice roll: 6 randomized wooden knocks with decaying rumble
     */
    private fun generateDiceRollSound(): ByteArray {
        val durationMs = 520
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        val knockTimes = listOf(0.04, 0.11, 0.19, 0.28, 0.38, 0.47)
        val knockFreqs = listOf(380.0, 440.0, 310.0, 520.0, 350.0, 410.0)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            var sample = 0.0

            for (k in knockTimes.indices) {
                val dt = t - knockTimes[k]
                if (dt in 0.0..0.08) {
                    val env = exp(-dt * 65.0)
                    val freq = knockFreqs[k]
                    val tone = sin(2.0 * Math.PI * freq * dt) + 0.3 * sin(4.0 * Math.PI * freq * dt)
                    val woodNoise = (Random.nextFloat() * 2.0 - 1.0) * 0.25 * env
                    sample += (tone * 0.75 + woodNoise) * env
                }
            }

            // Light board friction background
            val frictionEnv = (1.0 - (t / 0.52)).coerceIn(0.0, 1.0)
            sample += (Random.nextFloat() * 2.0 - 1.0) * 0.04 * frictionEnv

            writePcmSample(pcm, i, sample * 0.8)
        }
        return pcm
    }

    /**
     * Tactile piece step: clean crisp pop / tap per cell hop
     */
    private fun generatePieceStepSound(): ByteArray {
        val durationMs = 85
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 55.0)
            val freq = 520.0 - (t * 2200.0) // pitch glide downward
            val tone = sin(2.0 * Math.PI * max(100.0, freq) * t)
            val click = if (i < 40) (Random.nextFloat() * 2.0 - 1.0) * 0.4 else 0.0
            val sample = (tone * 0.8 + click) * env
            writePcmSample(pcm, i, sample * 0.85)
        }
        return pcm
    }

    /**
     * Piece unlocked: regal ascending 4-note chime
     */
    private fun generatePieceUnlockedSound(): ByteArray {
        val durationMs = 380
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        val notes = listOf(523.25, 659.25, 783.99, 1046.50) // C5, E5, G5, C6
        val noteDur = 0.085

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val noteIdx = min(notes.size - 1, (t / noteDur).toInt())
            val noteTime = t - (noteIdx * noteDur)
            val freq = notes[noteIdx]

            val env = exp(-noteTime * 14.0)
            val tone = sin(2.0 * Math.PI * freq * t) +
                    0.4 * sin(4.0 * Math.PI * freq * t) +
                    0.2 * sin(6.0 * Math.PI * freq * t)
            val sample = tone * env * 0.7
            writePcmSample(pcm, i, sample)
        }
        return pcm
    }

    /**
     * Piece capture: explosive impact, low thump + metallic sword slash
     */
    private fun generatePieceCaptureSound(): ByteArray {
        val durationMs = 340
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            // Low bass punch
            val bassEnv = exp(-t * 25.0)
            val bass = sin(2.0 * Math.PI * (120.0 - t * 250.0) * t) * bassEnv

            // Sword slice metallic ring
            val metallicEnv = exp(-t * 18.0)
            val metallicFreq = 1600.0 - t * 2800.0
            val metallic = sin(2.0 * Math.PI * max(200.0, metallicFreq) * t) * metallicEnv

            // Slash impact noise
            val noiseEnv = exp(-t * 40.0)
            val noise = (Random.nextFloat() * 2.0 - 1.0) * noiseEnv * 0.45

            val sample = (bass * 0.6 + metallic * 0.5 + noise) * 0.85
            writePcmSample(pcm, i, sample)
        }
        return pcm
    }

    /**
     * Goal scored: sparkling royal harp triad chime
     */
    private fun generateGoalScoredSound(): ByteArray {
        val durationMs = 620
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        val chord = listOf(880.0, 1174.66, 1760.0, 2349.32) // A5, D6, A6, D7

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = exp(-t * 5.5)
            var sample = 0.0

            chord.forEachIndexed { idx, freq ->
                val delay = idx * 0.035
                if (t >= delay) {
                    val dt = t - delay
                    val noteEnv = exp(-dt * 6.0)
                    sample += (sin(2.0 * Math.PI * freq * dt) + 0.3 * sin(4.0 * Math.PI * freq * dt)) * noteEnv
                }
            }

            writePcmSample(pcm, i, sample * 0.32 * env)
        }
        return pcm
    }

    /**
     * Lucky 6: bright twin golden bells
     */
    private fun generateLuckySixSound(): ByteArray {
        val durationMs = 450
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val env1 = exp(-t * 9.0)
            val bell1 = sin(2.0 * Math.PI * 1318.51 * t) * env1 // E6

            val t2 = t - 0.08
            val bell2 = if (t2 > 0) sin(2.0 * Math.PI * 1567.98 * t2) * exp(-t2 * 9.0) else 0.0 // G6

            val sparkle = (sin(2.0 * Math.PI * 2637.0 * t) * 0.15) * env1
            val sample = (bell1 * 0.5 + bell2 * 0.5 + sparkle) * 0.75
            writePcmSample(pcm, i, sample)
        }
        return pcm
    }

    /**
     * Turn pass: gentle descending plink
     */
    private fun generateTurnPassSound(): ByteArray {
        val durationMs = 240
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val freq = if (t < 0.11) 440.0 else 329.63
            val noteT = if (t < 0.11) t else (t - 0.11)
            val env = exp(-noteT * 22.0)
            val sample = sin(2.0 * Math.PI * freq * t) * env * 0.55
            writePcmSample(pcm, i, sample)
        }
        return pcm
    }

    /**
     * Grand victory: triumphant imperial arpeggio fanfare
     */
    private fun generateGrandVictorySound(): ByteArray {
        val durationMs = 950
        val numSamples = (SAMPLE_RATE * durationMs) / 1000
        val pcm = ByteArray(numSamples * 2)

        val notes = listOf(261.63, 329.63, 392.00, 523.25, 659.25, 783.99, 1046.50) // C4..C6
        val stepTime = 0.09

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val idx = min(notes.size - 1, (t / stepTime).toInt())
            val noteT = t - (idx * stepTime)
            val freq = notes[idx]
            val env = if (idx == notes.size - 1) exp(-noteT * 3.5) else exp(-noteT * 12.0)

            // Trumpet-like brass harmonic series
            val brass = sin(2.0 * Math.PI * freq * t) +
                    0.55 * sin(4.0 * Math.PI * freq * t) +
                    0.35 * sin(6.0 * Math.PI * freq * t) +
                    0.20 * sin(8.0 * Math.PI * freq * t)

            writePcmSample(pcm, i, brass * env * 0.45)
        }
        return pcm
    }

    private fun writePcmSample(pcm: ByteArray, index: Int, sampleValue: Double) {
        val clamped = sampleValue.coerceIn(-1.0, 1.0)
        val shortVal = (clamped * 32767.0).toInt().toShort()
        val byteIndex = index * 2
        if (byteIndex + 1 < pcm.size) {
            pcm[byteIndex] = (shortVal.toInt() and 0xFF).toByte()
            pcm[byteIndex + 1] = ((shortVal.toInt() shr 8) and 0xFF).toByte()
        }
    }
}

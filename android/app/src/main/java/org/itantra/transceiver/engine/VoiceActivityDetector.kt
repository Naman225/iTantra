package org.itantra.transceiver.engine

import android.os.Handler
import android.os.Looper
import android.util.Log
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * On-Device Real-Time Voice Activity Detector (VAD).
 * Implements adaptive energy-based silence & speech boundary detection.
 * Enables:
 * 1. Automatic STT triggering when user finishes speaking (pause/stop detection).
 * 2. Full-duplex hands-free "Phone Mode" without holding PTT.
 */
class VoiceActivityDetector(
    private val sampleRate: Int = 16000,
    private val silenceThresholdMillis: Long = 450L,
    private val pcmDbfsThreshold: Float = -36.0f,
    private val recognizerThresholdDb: Float = 2.5f // Android onRmsChanged scale (-2dB to +10dB)
) {
    companion object {
        const val TAG = "iTantra-VAD"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isSpeaking = false
    private var lastSpeechTime = 0L
    private var speechStartTime = 0L
    private var adaptiveNoiseFloorDbfs = -55.0f

    var onSpeechStart: (() -> Unit)? = null
    var onSpeechPause: (() -> Unit)? = null
    var onRmsUpdate: ((Float) -> Unit)? = null

    /**
     * Processes live audio level from SpeechRecognizer onRmsChanged (scale: -2dB to +10dB).
     * Silence is typically -2dB to +0.5dB, speech is typically > 2.5dB.
     */
    fun processRecognizerRms(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        onRmsUpdate?.invoke(normalized)

        val isVoiceActive = rmsdB > recognizerThresholdDb
        handleVoiceState(isVoiceActive, "RecognizerRms ($rmsdB dB)")
    }

    /**
     * Processes audio level for backward compatibility.
     */
    fun processRms(rmsdB: Float) {
        processRecognizerRms(rmsdB)
    }

    private fun handleVoiceState(isVoiceActive: Boolean, sourceTag: String) {
        val currentTime = System.currentTimeMillis()
        if (isVoiceActive) {
            lastSpeechTime = currentTime
            if (!isSpeaking) {
                isSpeaking = true
                speechStartTime = currentTime
                Log.d(TAG, "Speech onset detected via $sourceTag")
                mainHandler.post { onSpeechStart?.invoke() }
            }
        } else {
            if (isSpeaking && (currentTime - lastSpeechTime) >= silenceThresholdMillis) {
                val utteranceDuration = currentTime - speechStartTime
                if (utteranceDuration >= 300) {
                    Log.d(TAG, "Speech pause detected after ${utteranceDuration}ms utterance")
                    isSpeaking = false
                    mainHandler.post { onSpeechPause?.invoke() }
                } else {
                    isSpeaking = false
                }
            }
        }
    }

    /**
     * Processes a buffer of 16-bit PCM audio samples directly using dBFS scale (-90 dBFS to 0 dBFS).
     */
    fun processPcmBuffer(buffer: ShortArray, readSize: Int) {
        if (readSize <= 0) return

        var sumSquares = 0.0
        for (i in 0 until readSize) {
            val sample = buffer[i].toDouble()
            sumSquares += sample * sample
        }
        val meanSquare = sumSquares / readSize
        val rms = sqrt(meanSquare)
        val dbfs = if (rms > 0) (20.0 * log10(rms / 32767.0)).toFloat() else -90.0f

        val normalized = ((dbfs + 60f) / 60f).coerceIn(0f, 1f)
        onRmsUpdate?.invoke(normalized)

        // Adapt noise floor during silence
        if (dbfs < pcmDbfsThreshold) {
            adaptiveNoiseFloorDbfs = adaptiveNoiseFloorDbfs * 0.95f + dbfs * 0.05f
        }
        val dynamicThreshold = maxOf(pcmDbfsThreshold, adaptiveNoiseFloorDbfs + 10.0f)
        val isVoiceActive = dbfs > dynamicThreshold

        handleVoiceState(isVoiceActive, "PcmDbfs ($dbfs dBFS)")
    }

    fun reset() {
        isSpeaking = false
        lastSpeechTime = 0L
        speechStartTime = 0L
    }
}

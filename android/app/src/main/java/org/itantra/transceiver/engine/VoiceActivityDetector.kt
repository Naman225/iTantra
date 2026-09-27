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
    private val speechThresholdDb: Float = -36.0f
) {
    companion object {
        const val TAG = "iTantra-VAD"
    }

    private val mainHandler = Handler(Looper.getMainLooper())

    private var isSpeaking = false
    private var lastSpeechTime = 0L
    private var speechStartTime = 0L

    var onSpeechStart: (() -> Unit)? = null
    var onSpeechPause: (() -> Unit)? = null
    var onRmsUpdate: ((Float) -> Unit)? = null

    /**
     * Processes live audio level in dB (from AudioRecord or SpeechRecognizer onRmsChanged).
     */
    fun processRms(rmsdB: Float) {
        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
        onRmsUpdate?.invoke(normalized)

        val currentTime = System.currentTimeMillis()
        val isVoiceActive = rmsdB > speechThresholdDb

        if (isVoiceActive) {
            lastSpeechTime = currentTime
            if (!isSpeaking) {
                isSpeaking = true
                speechStartTime = currentTime
                Log.d(TAG, "Speech onset detected at $rmsdB dB")
                mainHandler.post { onSpeechStart?.invoke() }
            }
        } else {
            if (isSpeaking && (currentTime - lastSpeechTime) >= silenceThresholdMillis) {
                val utteranceDuration = currentTime - speechStartTime
                if (utteranceDuration >= 300) {
                    Log.d(TAG, "Speech pause/stop detected after ${utteranceDuration}ms utterance")
                    isSpeaking = false
                    mainHandler.post { onSpeechPause?.invoke() }
                } else {
                    isSpeaking = false
                }
            }
        }
    }

    /**
     * Processes a buffer of 16-bit PCM audio samples directly.
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
        val db = if (rms > 0) (20.0 * log10(rms / 32767.0)).toFloat() else -100.0f

        processRms(db)
    }

    fun reset() {
        isSpeaking = false
        lastSpeechTime = 0L
        speechStartTime = 0L
    }
}

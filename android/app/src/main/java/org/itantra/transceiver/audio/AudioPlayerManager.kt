package org.itantra.transceiver.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.sin

/**
 * Low-latency AudioTrack Player.
 * Plays raw PCM synthesized speech, Roger beeps, and squelch audio feedback.
 */
class AudioPlayerManager {
    companion object {
        const val TAG = "iTantra-AudioPlayer"
    }

    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Plays raw 16-bit PCM audio samples.
     */
    fun playPcm(
        pcmData: ByteArray,
        sampleRate: Int = 16000,
        isAlarmPriority: Boolean = false,
        onComplete: (() -> Unit)? = null
    ) {
        scope.launch {
            try {
                val usage = if (isAlarmPriority) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_MEDIA
                val contentType = if (isAlarmPriority) AudioAttributes.CONTENT_TYPE_SONIFICATION else AudioAttributes.CONTENT_TYPE_SPEECH

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(usage)
                    .setContentType(contentType)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val minBufferSize = AudioTrack.getMinBufferSize(
                    sampleRate,
                    AudioFormat.CHANNEL_OUT_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )

                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(maxOf(minBufferSize, pcmData.size))
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(pcmData, 0, pcmData.size)
                audioTrack.play()

                // Wait for playback to finish
                val durationMs = (pcmData.size.toDouble() / (sampleRate * 2)) * 1000
                Thread.sleep(durationMs.toLong() + 50)

                audioTrack.stop()
                audioTrack.release()
                onComplete?.invoke()
            } catch (e: Exception) {
                Log.e(TAG, "Audio playback error: ${e.message}")
            }
        }
    }

    /**
     * Generates and plays a classic Walkie-Talkie Roger Beep (1000Hz tone, 120ms).
     */
    fun playRogerBeep() {
        scope.launch {
            val sampleRate = 16000
            val durationMs = 120
            val numSamples = (sampleRate * (durationMs / 1000.0)).toInt()
            val pcmData = ByteArray(numSamples * 2)

            val freq = 1000.0 // 1 kHz tone
            for (i in 0 until numSamples) {
                val angle = 2.0 * PI * i / (sampleRate / freq)
                // Apply subtle envelope fade-in/out to avoid speaker pop
                val envelope = when {
                    i < 100 -> i / 100.0
                    i > numSamples - 100 -> (numSamples - i) / 100.0
                    else -> 1.0
                }
                val sample = (sin(angle) * 32767.0 * 0.6 * envelope).toInt().toShort()
                pcmData[i * 2] = (sample.toInt() and 0xFF).toByte()
                pcmData[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()
            }
            playPcm(pcmData, sampleRate = sampleRate)
        }
    }
}

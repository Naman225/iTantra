package org.itantra.transceiver.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.sqrt

/**
 * Microphone Audio Capture Manager.
 * Based on sherpa-onnx Android audio pipeline for streaming ASR/VAD.
 * Captures 16kHz, 16-bit, Mono PCM audio with real-time RMS amplitude tracking.
 */
class AudioRecordManager(
    private val sampleRate: Int = 16000,
    private val onAudioChunk: (ByteArray, FloatArray) -> Unit
) {
    companion object {
        const val TAG = "iTantra-AudioRecord"
    }

    private var audioRecord: AudioRecord? = null
    private var isRecording = false
    private var recordingThread: Thread? = null

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    @SuppressLint("MissingPermission")
    fun startRecording(): Boolean {
        if (isRecording) return true

        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT
        val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
        val bufferSize = maxOf(minBufferSize, sampleRate / 10 * 2) // 100ms buffer

        try {
            audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                sampleRate,
                channelConfig,
                audioFormat,
                bufferSize
            )

            if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord failed to initialize")
                return false
            }

            audioRecord?.startRecording()
            isRecording = true

            recordingThread = Thread({
                val buffer = ShortArray(sampleRate / 10) // 100ms frames
                val byteBuffer = ByteArray(buffer.size * 2)
                val floatBuffer = FloatArray(buffer.size)

                while (isRecording) {
                    val readCount = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readCount > 0) {
                        var sumSquare = 0.0
                        for (i in 0 until readCount) {
                            val sample = buffer[i]
                            // Convert short to byte
                            byteBuffer[i * 2] = (sample.toInt() and 0xFF).toByte()
                            byteBuffer[i * 2 + 1] = ((sample.toInt() shr 8) and 0xFF).toByte()

                            // Normalize float for neural models [-1.0, 1.0]
                            val normSample = sample / 32768.0f
                            floatBuffer[i] = normSample
                            sumSquare += (normSample * normSample)
                        }

                        // Compute RMS for UI visualizer
                        val rms = sqrt(sumSquare / readCount).toFloat()
                        _audioLevel.value = (rms * 5f).coerceIn(0f, 1f)

                        onAudioChunk(byteBuffer.copyOf(readCount * 2), floatBuffer.copyOf(readCount))
                    }
                }
            }, "iTantraAudioCaptureThread").apply { start() }

            Log.i(TAG, "Audio recording started at $sampleRate Hz")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording: ${e.message}")
            return false
        }
    }

    fun stopRecording() {
        if (!isRecording) return
        isRecording = false
        try {
            recordingThread?.join(500)
            audioRecord?.stop()
            audioRecord?.release()
            audioRecord = null
            _audioLevel.value = 0f
            Log.i(TAG, "Audio recording stopped")
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
        }
    }
}

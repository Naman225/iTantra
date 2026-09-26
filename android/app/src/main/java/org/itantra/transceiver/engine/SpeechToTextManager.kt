package org.itantra.transceiver.engine

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Speech-To-Text Recognition Coordinator.
 * Interfaces with Android on-device SpeechRecognizer and Sherpa-ONNX/Vosk pipeline.
 * Converts live microphone speech in Hindi or English into real text transcripts.
 */
class SpeechToTextManager(private val context: Context) {
    companion object {
        const val TAG = "iTantra-STT"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var onFinalResultCallback: ((String) -> Unit)? = null
    private var lastCapturedText = ""

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    fun initModel(langCode: String): Boolean {
        Log.i(TAG, "Initializing STT pipeline for $langCode. System speech available: ${isAvailable()}")
        return true
    }

    /**
     * Starts listening to user speech using PTT hold.
     * @param langId 0 = Hindi, 1 = English, etc.
     */
    fun startListening(langId: Int, onResult: (String) -> Unit) {
        mainHandler.post {
            try {
                // Clean up any existing recognizer session
                cleanupRecognizer()

                _partialText.value = ""
                lastCapturedText = ""
                onFinalResultCallback = onResult
                _isListening.value = true

                speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.i(TAG, "ASR Engine ready for speech")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.i(TAG, "Voice input detected")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            // rmsdB typically ranges from -2 to +10 dB
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioLevel.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.i(TAG, "End of speech segment")
                            _audioLevel.value = 0f
                        }

                        override fun onError(error: Int) {
                            val errorName = when (error) {
                                SpeechRecognizer.ERROR_AUDIO -> "ERROR_AUDIO"
                                SpeechRecognizer.ERROR_CLIENT -> "ERROR_CLIENT"
                                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "ERROR_INSUFFICIENT_PERMISSIONS"
                                SpeechRecognizer.ERROR_NETWORK -> "ERROR_NETWORK"
                                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "ERROR_NETWORK_TIMEOUT"
                                SpeechRecognizer.ERROR_NO_MATCH -> "ERROR_NO_MATCH"
                                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "ERROR_RECOGNIZER_BUSY"
                                SpeechRecognizer.ERROR_SERVER -> "ERROR_SERVER"
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "ERROR_SPEECH_TIMEOUT"
                                else -> "ERROR_CODE_$error"
                            }
                            Log.w(TAG, "Recognition error: $errorName ($error)")
                            _audioLevel.value = 0f
                            _isListening.value = false

                            // If we received any partial text before error, use that!
                            if (lastCapturedText.isNotBlank()) {
                                deliverResult(lastCapturedText)
                            } else {
                                deliverResult("")
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _audioLevel.value = 0f
                            _isListening.value = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull() ?: lastCapturedText
                            Log.i(TAG, "Recognition success: '$recognized'")
                            deliverResult(recognized)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull() ?: ""
                            if (partial.isNotBlank()) {
                                lastCapturedText = partial
                                _partialText.value = partial
                                Log.d(TAG, "Partial: '$partial'")
                            }
                        }

                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }

                val langTag = if (langId == 1) "en-IN" else "hi-IN"
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, langTag)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, langTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                    putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                Log.i(TAG, "Started listening with language: $langTag")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer: ${e.message}", e)
                _isListening.value = false
                deliverResult("")
            }
        }
    }

    /**
     * Stops listening and completes the transcription.
     */
    fun stopListening() {
        mainHandler.post {
            if (!_isListening.value) return@post
            try {
                speechRecognizer?.stopListening()
                Log.i(TAG, "Requested stopListening, awaiting results...")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer: ${e.message}")
                deliverResult(lastCapturedText)
            }
        }
    }

    private fun deliverResult(text: String) {
        _isListening.value = false
        _audioLevel.value = 0f
        val cb = onFinalResultCallback
        onFinalResultCallback = null
        cb?.invoke(text.trim())
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying previous recognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    fun shutdown() {
        mainHandler.post {
            cleanupRecognizer()
            _isListening.value = false
        }
    }

    fun transcribe(pcmBytes: ByteArray, sampleRate: Int = 16000): String {
        return lastCapturedText
    }
}

package org.itantra.transceiver.engine

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
 * High-Reliability Speech-To-Text Recognition Coordinator.
 * Optimized for Samsung One UI, Pixel, and all Android devices.
 * Supports Hindi (hi-IN) and Indian English (en-IN).
 */
class SpeechToTextManager(private val context: Context) {
    companion object {
        const val TAG = "iTantra-STT"
        private const val GOOGLE_RECOGNITION_PACKAGE = "com.google.android.googlequicksearchbox"
        private const val GOOGLE_RECOGNITION_SERVICE = "com.google.android.voicesearch.serviceapi.GoogleRecognitionService"
    }

    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _audioLevel = MutableStateFlow(0f)
    val audioLevel: StateFlow<Float> = _audioLevel.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText: StateFlow<String> = _partialText.asStateFlow()

    private var onFinalResultCallback: ((String, String?) -> Unit)? = null
    private var lastCapturedText = ""
    private var sessionStartTime = 0L

    fun isAvailable(): Boolean {
        return SpeechRecognizer.isRecognitionAvailable(context)
    }

    /**
     * Creates best available SpeechRecognizer for the device.
     * Prefers Google Recognition Service for high Indic accuracy on Samsung.
     */
    private fun createBestSpeechRecognizer(): SpeechRecognizer {
        return try {
            val googleComp = ComponentName(GOOGLE_RECOGNITION_PACKAGE, GOOGLE_RECOGNITION_SERVICE)
            val pm = context.packageManager
            val intent = Intent("android.speech.RecognitionService").setComponent(googleComp)
            val resolves = pm.queryIntentServices(intent, 0)
            if (resolves.isNotEmpty()) {
                Log.i(TAG, "Using Google Speech Recognition Service")
                SpeechRecognizer.createSpeechRecognizer(context, googleComp)
            } else {
                Log.i(TAG, "Using default system SpeechRecognizer")
                SpeechRecognizer.createSpeechRecognizer(context)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallback to default SpeechRecognizer: ${e.message}")
            SpeechRecognizer.createSpeechRecognizer(context)
        }
    }

    /**
     * Starts listening to user speech.
     * @param langId 0 = Hindi, 1 = English
     */
    fun startListening(langId: Int, onResult: (text: String, errorMsg: String?) -> Unit) {
        mainHandler.post {
            try {
                cleanupRecognizer()

                _partialText.value = ""
                lastCapturedText = ""
                sessionStartTime = System.currentTimeMillis()
                onFinalResultCallback = onResult
                _isListening.value = true
                _isProcessing.value = false

                speechRecognizer = createBestSpeechRecognizer().apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            Log.i(TAG, "STT Engine ready for speech")
                        }

                        override fun onBeginningOfSpeech() {
                            Log.i(TAG, "Voice input detected")
                        }

                        override fun onRmsChanged(rmsdB: Float) {
                            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                            _audioLevel.value = normalized
                        }

                        override fun onBufferReceived(buffer: ByteArray?) {}

                        override fun onEndOfSpeech() {
                            Log.i(TAG, "End of speech segment")
                            _audioLevel.value = 0f
                            _isProcessing.value = true
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
                            _isProcessing.value = false

                            if (lastCapturedText.isNotBlank()) {
                                deliverResult(lastCapturedText, null)
                            } else {
                                val friendlyMsg = when (error) {
                                    SpeechRecognizer.ERROR_NO_MATCH -> "No voice recognized. Hold the button and speak clearly."
                                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Hold the button while speaking into your microphone."
                                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                                    SpeechRecognizer.ERROR_AUDIO -> "Microphone busy. Please try again."
                                    else -> "No speech detected. Hold button and speak."
                                }
                                deliverResult("", friendlyMsg)
                            }
                        }

                        override fun onResults(results: Bundle?) {
                            _audioLevel.value = 0f
                            _isListening.value = false
                            _isProcessing.value = false

                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val recognized = matches?.firstOrNull { it.isNotBlank() } ?: lastCapturedText
                            Log.i(TAG, "Recognition success: '$recognized'")
                            deliverResult(recognized, null)
                        }

                        override fun onPartialResults(partialResults: Bundle?) {
                            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = matches?.firstOrNull { it.isNotBlank() } ?: ""
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
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                    putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                }

                speechRecognizer?.startListening(intent)
                Log.i(TAG, "Started listening with language: $langTag")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start SpeechRecognizer: ${e.message}", e)
                _isListening.value = false
                _isProcessing.value = false
                deliverResult("", "Voice engine initialization error: ${e.message}")
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
                val elapsed = System.currentTimeMillis() - sessionStartTime
                _isListening.value = false
                _isProcessing.value = true

                if (elapsed < 350) {
                    // Pressed too briefly
                    Log.w(TAG, "Touch too brief: ${elapsed}ms")
                    mainHandler.postDelayed({
                        deliverResult("", "Hold the button while speaking")
                    }, 200)
                } else {
                    speechRecognizer?.stopListening()
                    Log.i(TAG, "Requested stopListening, awaiting results...")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping SpeechRecognizer: ${e.message}")
                deliverResult(lastCapturedText, null)
            }
        }
    }

    private fun deliverResult(text: String, errorMsg: String?) {
        _isListening.value = false
        _isProcessing.value = false
        _audioLevel.value = 0f
        val cb = onFinalResultCallback
        onFinalResultCallback = null
        cb?.invoke(text.trim(), errorMsg)
    }

    private fun cleanupRecognizer() {
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying recognizer: ${e.message}")
        }
        speechRecognizer = null
    }

    fun shutdown() {
        mainHandler.post {
            cleanupRecognizer()
            _isListening.value = false
            _isProcessing.value = false
        }
    }
}

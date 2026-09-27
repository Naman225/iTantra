package org.itantra.transceiver.engine

import android.content.Context
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale

/**
 * High-reliability Offline Text-To-Speech Manager.
 * Uses Android's built-in offline Indic voice packs with USAGE_ALARM override support.
 */
class TextToSpeechManager(
    private val context: Context,
    private val onInitComplete: ((Boolean) -> Unit)? = null
) : TextToSpeech.OnInitListener {

    companion object {
        const val TAG = "iTantra-TTS"
    }

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val LOCALE_MAP = mapOf(
        "hi" to Locale("hi", "IN"),
        "en" to Locale("en", "IN"),
        "gu" to Locale("gu", "IN"),
        "mr" to Locale("mr", "IN"),
        "kn" to Locale("kn", "IN"),
        "ml" to Locale("ml", "IN"),
        "ta" to Locale("ta", "IN"),
        "te" to Locale("te", "IN"),
        "or" to Locale("or", "IN"),
        "bn" to Locale("bn", "IN")
    )

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            tts?.language = Locale("hi", "IN")
            Log.i(TAG, "Offline TTS Engine Initialized successfully")
            onInitComplete?.invoke(true)
        } else {
            Log.e(TAG, "Failed to initialize TTS Engine, code: $status")
            onInitComplete?.invoke(false)
        }
    }

    /**
     * Speaks text with language selection and emergency alarm override.
     */
    fun speak(
        text: String,
        langCode: String,
        isEmergency: Boolean = false,
        onDone: (() -> Unit)? = null
    ) {
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not ready yet")
            return
        }

        val targetLocale = LOCALE_MAP[langCode] ?: Locale("hi", "IN")
        val available = tts?.isLanguageAvailable(targetLocale)
        if (available != TextToSpeech.LANG_NOT_SUPPORTED && available != TextToSpeech.LANG_MISSING_DATA) {
            tts?.language = targetLocale
            // Select on-device offline voice pack (strictly zero internet / no cloud synthesis)
            try {
                tts?.voices?.filter { !it.isNetworkConnectionRequired }?.firstOrNull {
                    it.locale.language.equals(targetLocale.language, ignoreCase = true)
                }?.let { offlineVoice ->
                    tts?.voice = offlineVoice
                    Log.i(TAG, "Using 100% offline on-device voice: ${offlineVoice.name}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Voice selection note: ${e.message}")
            }
        } else {
            // Fallback to on-device English/Hindi if specific dialect is unavailable
            tts?.language = if (langCode == "en") Locale.ENGLISH else Locale("hi", "IN")
        }

        val utteranceId = "iTantra_msg_${System.currentTimeMillis()}"

        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) {
                if (id == utteranceId) onDone?.invoke()
            }
            override fun onError(id: String?) {
                Log.e(TAG, "TTS playback error on utterance $id")
            }
        })

        val params = Bundle().apply {
            // Explicitly block cloud/network synthesis to guarantee 100% offline compliance
            putString(TextToSpeech.Engine.KEY_FEATURE_NETWORK_SYNTHESIS, "false")
        }

        if (isEmergency) {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, android.media.AudioManager.STREAM_ALARM)
        } else {
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
            tts?.setAudioAttributes(audioAttributes)
        }

        // Emergency alerts play with QUEUE_FLUSH (immediate override)
        val queueMode = if (isEmergency) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        tts?.speak(text, queueMode, params, utteranceId)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}

package org.itantra.transceiver.emergency

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log

/**
 * Emergency & Distress Audio Controller.
 * Overrides device volume to maximum, acquires USAGE_ALARM, and triggers haptic alert.
 */
class EmergencyAlertManager(private val context: Context) {
    companion object {
        const val TAG = "iTantra-Emergency"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val vibrator: Vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    /**
     * Enforces maximum alarm volume for life-safety distress broadcasts.
     */
    fun overrideVolumeToMax() {
        try {
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                maxVolume,
                AudioManager.FLAG_SHOW_UI
            )
            Log.w(TAG, "EMERGENCY: Device alarm volume forced to MAX ($maxVolume)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to override volume: ${e.message}")
        }
    }

    /**
     * Triggers tactical pulsing distress vibration pattern.
     */
    fun triggerDistressVibration() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = longArrayOf(0, 200, 100, 200, 100, 400)
                val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255)
                val effect = VibrationEffect.createWaveform(timings, amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 400), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Vibration failed: ${e.message}")
        }
    }
}

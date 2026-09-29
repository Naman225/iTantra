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

    private var isVibrating = false

    /**
     * Enforces silent operation as requested: Alarm volume override is disabled.
     */
    fun overrideVolumeToMax() {
        // Disabled: do NOT force alarm volume to max (sound-free tactical mode)
        Log.d(TAG, "overrideVolumeToMax skipped - tactical vibration-only mode")
    }

    /**
     * Triggers heavy, continuous tactical vibration that repeats indefinitely
     * until the emergency message is read or acknowledged.
     */
    @Synchronized
    fun startContinuousDistressVibration() {
        if (isVibrating) return
        isVibrating = true
        try {
            // Intense tactical vibration pattern: 500ms on, 100ms off, 500ms on, 100ms off, 800ms on, 300ms pause
            val timings = longArrayOf(0, 500, 100, 500, 100, 800, 300)
            val amplitudes = intArrayOf(0, 255, 0, 255, 0, 255, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // repeat index 0 loops indefinitely
                val effect = VibrationEffect.createWaveform(timings, amplitudes, 0)
                vibrator.vibrate(effect)
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(timings, 0)
            }
            Log.w(TAG, "EMERGENCY: Started continuous heavy distress vibration")
        } catch (e: Exception) {
            Log.e(TAG, "Continuous vibration failed: ${e.message}")
        }
    }

    /**
     * Stops the continuous distress vibration immediately.
     * Called when the operator acknowledges or reads the SOS message.
     */
    @Synchronized
    fun stopDistressVibration() {
        if (!isVibrating) return
        isVibrating = false
        try {
            vibrator.cancel()
            Log.i(TAG, "EMERGENCY: Distress vibration stopped (acknowledged/read)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel vibration: ${e.message}")
        }
    }

    fun isDistressVibrating(): Boolean = isVibrating

    /**
     * Triggers one-shot tactical pulsing distress vibration pattern (for yellow tactical warnings).
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

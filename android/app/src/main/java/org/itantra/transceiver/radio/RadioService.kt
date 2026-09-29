package org.itantra.transceiver.radio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import org.itantra.transceiver.MainActivity

/**
 * Foreground Service (I-10) for iTantra.
 * Keeps UDP radio listening, Bluetooth sockets, and wake locks alive when the screen turns off.
 */
class RadioService : Service() {

    companion object {
        const val TAG = "iTantra-RadioService"
        const val CHANNEL_ID = "itantra_radio_service_channel"
        const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, RadioService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, RadioService::class.java)
            context.stopService(intent)
        }
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private var wifiLock: WifiManager.WifiLock? = null
    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getService(): RadioService = this@RadioService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Radio Active", "Listening on offline mesh..."))
        acquireLocks()
        Log.i(TAG, "RadioService started in foreground")
    }

    private fun acquireLocks() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "iTantra:RadioWakeLock")?.apply {
                acquire(10 * 60 * 1000L /*10 mins fallback*/)
            }

            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            wifiLock = wifiManager?.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "iTantra:RadioWifiLock")?.apply {
                acquire()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not acquire locks: ${e.message}")
        }
    }

    fun updateStatus(status: String) {
        val notification = buildNotification("iTantra Active", status)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(title: String, content: String): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "iTantra Tactical Mesh Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Maintains offline mesh connectivity and background emergency monitoring"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
            if (wifiLock?.isHeld == true) wifiLock?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing locks: ${e.message}")
        }
        Log.i(TAG, "RadioService stopped")
    }
}

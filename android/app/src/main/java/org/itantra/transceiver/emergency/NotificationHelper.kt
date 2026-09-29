package org.itantra.transceiver.emergency

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import org.itantra.transceiver.MainActivity

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_MESSAGES = "itantra_messages"
        const val CHANNEL_ALERT = "itantra_alert"
        const val CHANNEL_EMERGENCY = "itantra_emergency_v2"
        const val NOTIF_ID_MESSAGE = 1001
        const val NOTIF_ID_ALERT = 5001
        const val NOTIF_ID_EMERGENCY = 9999
    }

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Delete legacy channel that may have cached alarm sound
            try {
                notificationManager.deleteNotificationChannel("itantra_emergency")
            } catch (_: Exception) {}

            // Channel 1: Normal Transceiver Messages (Green tier)
            val messageChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Radio Messages (Normal)",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Incoming walkie-talkie voice notes and routine field messages"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
            }

            // Channel 2: Tactical Alerts (Yellow tier)
            val alertChannel = NotificationChannel(
                CHANNEL_ALERT,
                "Tactical Warning Alerts (Yellow)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Tactical danger warnings and urgent alerts"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                lightColor = 0xFFF59E0B.toInt()
                enableLights(true)
            }

            // Channel 3: Emergency SOS Alerts (Tactical Vibration Only - NO Alarm Sound / Voice)
            val emergencyChannel = NotificationChannel(
                CHANNEL_EMERGENCY,
                "Emergency SOS Distress (Vibrate Only)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Critical life-safety distress broadcasts (Tactical Vibration)"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 100, 500, 100, 800, 300)
                setSound(null, null) // Pure vibration only, NO alarm sound or voice
                lightColor = 0xFFEA4335.toInt()
                enableLights(true)
            }

            notificationManager.createNotificationChannel(messageChannel)
            notificationManager.createNotificationChannel(alertChannel)
            notificationManager.createNotificationChannel(emergencyChannel)
        }
    }

    fun showMessageNotification(
        sender: String,
        message: String,
        isEmergency: Boolean = false,
        isAlert: Boolean = false
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("action", if (isEmergency) "dismiss_sos" else "open")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            if (isEmergency) NOTIF_ID_EMERGENCY else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = when {
            isEmergency -> CHANNEL_EMERGENCY
            isAlert -> CHANNEL_ALERT
            else -> CHANNEL_MESSAGES
        }

        val title = when {
            isEmergency -> "🚨 RED SOS: EMERGENCY DISTRESS"
            isAlert -> "⚠️ YELLOW ALERT: TACTICAL WARNING"
            else -> "📻 $sender"
        }

        val content = when {
            isEmergency -> "SOS DISTRESS: $message"
            isAlert -> "TACTICAL ALERT: $message"
            else -> "$sender: $message"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(
                when {
                    isEmergency -> NotificationCompat.PRIORITY_MAX
                    isAlert -> NotificationCompat.PRIORITY_HIGH
                    else -> NotificationCompat.PRIORITY_DEFAULT
                }
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        if (isEmergency) {
            builder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
            builder.setColor(0xFFEA4335.toInt())
            builder.setSound(null)
            builder.setVibrate(longArrayOf(0, 500, 100, 500, 100, 800, 300))

            // Action button to acknowledge & stop continuous vibration directly
            val ackIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("action", "dismiss_sos")
            }
            val ackPendingIntent = PendingIntent.getActivity(
                context,
                NOTIF_ID_EMERGENCY + 1,
                ackIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Acknowledge & Silence",
                ackPendingIntent
            )
        } else if (isAlert) {
            builder.setCategory(NotificationCompat.CATEGORY_EVENT)
            builder.setColor(0xFFF59E0B.toInt())
        }

        val notifId = when {
            isEmergency -> NOTIF_ID_EMERGENCY
            isAlert -> NOTIF_ID_ALERT + (1..1000).random()
            else -> NOTIF_ID_MESSAGE + (1..1000).random()
        }
        notificationManager.notify(notifId, builder.build())
    }
}

package com.example.medicationreminder.reminder

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.medicationreminder.MainActivity
import com.example.medicationreminder.R
import com.example.medicationreminder.domain.DoseOccurrence

object ReminderNotifications {
    const val VOICE_CHANNEL_ID = "medication_alarm"
    const val FALLBACK_CHANNEL_ID = "medication_alarm_fallback_v1"
    const val NOTIFICATION_ID = 9001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        val voiceChannel = NotificationChannel(
            VOICE_CHANNEL_ID,
            context.getString(R.string.alarm_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.alarm_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            // Spoken audio is primary; a channel sound would overlap it.
            setSound(null, null)
        }
        val fallbackSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val fallbackChannel = NotificationChannel(
            FALLBACK_CHANNEL_ID,
            context.getString(R.string.fallback_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.fallback_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            setSound(
                fallbackSound,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        manager.createNotificationChannels(listOf(voiceChannel, fallbackChannel))
    }

    fun reminder(
        context: Context,
        occurrences: List<DoseOccurrence>,
        message: String,
        useFallbackChannel: Boolean = false,
    ): Notification = reminder(
        context = context,
        eventIds = occurrences.map { it.eventId }.toLongArray(),
        medicationNames = occurrences.map { it.medicationName },
        message = message,
        useFallbackChannel = useFallbackChannel,
    )

    fun reminder(
        context: Context,
        eventIds: LongArray,
        medicationNames: List<String>,
        message: String,
        useFallbackChannel: Boolean = false,
    ): Notification {
        val title = when {
            useFallbackChannel -> "用药提醒：请打开应用确认"
            medicationNames.size == 1 -> "温馨提醒：该服用 ${medicationNames.first()} 了"
            else -> "现在有 ${medicationNames.size} 项用药提醒"
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            8100,
            Intent(context, ReminderActivity::class.java)
                .putExtra(AlarmPlaybackService.EXTRA_EVENT_IDS, eventIds),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return baseBuilder(
            context = context,
            channelId = if (useFallbackChannel) FALLBACK_CHANNEL_ID else VOICE_CHANNEL_ID,
            title = title,
            message = message,
        )
            .setOnlyAlertOnce(!useFallbackChannel)
            .setContentIntent(contentIntent)
            .setFullScreenIntent(contentIntent, true)
            .addAction(0, "全部已服", actionIntent(context, ReminderActionReceiver.ACTION_TAKEN, eventIds, 8101))
            .addAction(0, "稍后提醒", actionIntent(context, ReminderActionReceiver.ACTION_SNOOZE, eventIds, 8102))
            .addAction(0, "跳过", actionIntent(context, ReminderActionReceiver.ACTION_SKIPPED, eventIds, 8103))
            .build()
    }

    fun test(context: Context, message: String): Notification {
        val stopIntent = PendingIntent.getBroadcast(
            context,
            8201,
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderActionReceiver.ACTION_STOP_TEST
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val contentIntent = PendingIntent.getActivity(
            context,
            8200,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return baseBuilder(context, VOICE_CHANNEL_ID, "语音风格试听", message)
            .setContentIntent(contentIntent)
            .addAction(0, "停止试听", stopIntent)
            .build()
    }

    fun confirmation(context: Context, message: String): Notification = baseBuilder(
        context, VOICE_CHANNEL_ID, "操作已确认", message,
    ).build()

    fun loading(context: Context, isTest: Boolean): Notification = baseBuilder(
        context = context,
        channelId = VOICE_CHANNEL_ID,
        title = if (isTest) "正在准备语音试听" else "正在准备用药提醒",
        message = "正在加载语音…",
    ).build()

    @SuppressLint("MissingPermission")
    fun showFallback(
        context: Context,
        occurrences: List<DoseOccurrence>,
        message: String,
    ): Boolean {
        if (!canPostNotifications(context)) return false
        createChannels(context)
        val channel = context.getSystemService(NotificationManager::class.java)
            .getNotificationChannel(FALLBACK_CHANNEL_ID)
        if (channel?.importance == NotificationManager.IMPORTANCE_NONE) return false
        NotificationManagerCompat.from(context).notify(
            NOTIFICATION_ID,
            reminder(context, occurrences, message, useFallbackChannel = true),
        )
        return true
    }

    fun cancel(context: Context) {
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun baseBuilder(
        context: Context,
        channelId: String,
        title: String,
        message: String,
    ): NotificationCompat.Builder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_medication)
        .setContentTitle(title)
        .setContentText(message)
        .setStyle(NotificationCompat.BigTextStyle().bigText(message))
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

    private fun actionIntent(
        context: Context,
        action: String,
        ids: LongArray,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getBroadcast(
        context,
        requestCode,
        Intent(context, ReminderActionReceiver::class.java).apply {
            this.action = action
            putExtra(ReminderActionReceiver.EXTRA_EVENT_IDS, ids)
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun canPostNotifications(context: Context): Boolean {
        val runtimePermissionGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return runtimePermissionGranted &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}

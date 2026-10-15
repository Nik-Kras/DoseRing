package com.nikkras.dosering.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import com.nikkras.dosering.R
import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.ui.ActionActivity
import com.nikkras.dosering.ui.Format
import com.nikkras.dosering.ui.MainActivity
import com.nikkras.dosering.ui.Tab

object Notifications {

    private const val CHANNEL_READY = "ready"

    fun canPost(context: Context): Boolean =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED &&
            context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    fun showReady(context: Context, med: Medication, last: Intake) {
        if (!canPost(context)) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_READY,
                context.getString(R.string.channel_ready_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply { description = context.getString(R.string.channel_ready_description) }
        )

        val open = PendingIntent.getActivity(
            context,
            med.id.toInt(),
            MainActivity.intent(context, Tab.Medicines),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val take = PendingIntent.getActivity(
            context,
            med.id.toInt(),
            ActionActivity.intent(context, ActionActivity.MODE_TAP, med.id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val notification = Notification.Builder(context, CHANNEL_READY)
            .setSmallIcon(R.drawable.ic_stat_dose)
            .setColor(med.color)
            .setContentTitle(context.getString(R.string.notification_ready_title, med.name))
            .setContentText(
                context.getString(
                    R.string.notification_ready_text,
                    DoseLogic.formatMinutes(med.intervalMinutes),
                    Doses.format(last.doseAmount, last.unit),
                    Format.time(context, last.takenAt),
                )
            )
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(null, context.getString(R.string.action_take_now), take).build())
            .build()

        manager.notify(notificationId(med.id), notification)
    }

    fun cancelReady(context: Context, medicationId: Long) {
        context.getSystemService(NotificationManager::class.java).cancel(notificationId(medicationId))
    }

    private fun notificationId(medicationId: Long): Int = 1_000 + medicationId.toInt()
}

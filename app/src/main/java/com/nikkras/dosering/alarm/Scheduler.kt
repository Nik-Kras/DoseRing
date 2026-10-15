package com.nikkras.dosering.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.widget.DoseWidgetProvider
import com.nikkras.dosering.widget.WidgetUpdater

/**
 * Two kinds of alarms:
 *  - a per-medicine "ready" alarm that fires the moment its ring completes (wakes the device);
 *  - a once-a-minute widget tick while any ring is still filling. It is a non-wakeup alarm,
 *    so it costs nothing while the screen is off and catches up as soon as the phone wakes.
 */
object Scheduler {

    const val ACTION_TICK = "com.nikkras.dosering.action.TICK"
    const val ACTION_READY = "com.nikkras.dosering.action.READY"
    const val EXTRA_MEDICATION_ID = "medication_id"

    fun rescheduleAll(context: Context) {
        val repo = DoseRepository.get(context)
        val alarms = context.getSystemService(AlarmManager::class.java)
        val latest = repo.latestIntakes()
        val now = System.currentTimeMillis()
        var anyFilling = false

        for (med in repo.medications()) {
            val readyAt = DoseLogic.readyAt(med, latest[med.id])
            val intent = readyIntent(context, med.id)
            if (readyAt != null && readyAt > now) {
                anyFilling = true
                setExact(alarms, AlarmManager.RTC_WAKEUP, readyAt, intent, whileIdle = true)
            } else {
                alarms.cancel(intent)
            }
        }

        val tick = tickIntent(context)
        if (anyFilling && WidgetUpdater.widgetIds(context).isNotEmpty()) {
            val nextMinute = (now / 60_000L + 1) * 60_000L
            setExact(alarms, AlarmManager.RTC, nextMinute, tick, whileIdle = false)
        } else {
            alarms.cancel(tick)
        }
    }

    fun cancelReady(context: Context, medicationId: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(readyIntent(context, medicationId))
    }

    private fun setExact(alarms: AlarmManager, type: Int, at: Long, intent: PendingIntent, whileIdle: Boolean) {
        when {
            !alarms.canScheduleExactAlarms() ->
                if (whileIdle) alarms.setAndAllowWhileIdle(type, at, intent) else alarms.set(type, at, intent)
            whileIdle -> alarms.setExactAndAllowWhileIdle(type, at, intent)
            else -> alarms.setExact(type, at, intent)
        }
    }

    private fun readyIntent(context: Context, medicationId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReadyReceiver::class.java)
                .setAction(ACTION_READY)
                .setData(Uri.parse("dosering://medication/$medicationId"))
                .putExtra(EXTRA_MEDICATION_ID, medicationId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    private fun tickIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, DoseWidgetProvider::class.java).setAction(ACTION_TICK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}

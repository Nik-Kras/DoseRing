package com.nikkras.dosering.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.nikkras.dosering.DoseSync
import com.nikkras.dosering.alarm.Scheduler

class DoseWidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Scheduler.ACTION_TICK) DoseSync.refresh(context) else super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        DoseSync.refresh(context)
    }

    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) {
        WidgetUpdater.updateAll(context)
    }

    override fun onDisabled(context: Context) {
        Scheduler.rescheduleAll(context)
    }
}

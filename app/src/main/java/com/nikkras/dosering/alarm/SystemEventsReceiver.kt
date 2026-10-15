package com.nikkras.dosering.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikkras.dosering.DoseSync

/** Alarms do not survive reboots, updates or clock changes, so re-arm them. */
class SystemEventsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        DoseSync.refresh(context)
    }
}

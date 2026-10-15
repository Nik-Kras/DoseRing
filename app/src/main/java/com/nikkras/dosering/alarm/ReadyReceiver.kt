package com.nikkras.dosering.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikkras.dosering.DoseSync
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.notify.Notifications

/** Fires when a medicine's ring completes: notify, then redraw the widget. */
class ReadyReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repo = DoseRepository.get(context)
        val med = repo.medication(intent.getLongExtra(Scheduler.EXTRA_MEDICATION_ID, -1L))
        val last = med?.let { repo.latestIntake(it.id) }
        // Small tolerance: an exact alarm can be delivered a few milliseconds early.
        if (med != null && last != null && DoseLogic.status(med, last, System.currentTimeMillis() + 1_000L).isReady) {
            Notifications.showReady(context, med, last)
        }
        DoseSync.refresh(context)
    }
}

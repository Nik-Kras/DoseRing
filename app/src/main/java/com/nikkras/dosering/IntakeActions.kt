package com.nikkras.dosering

import android.content.Context
import com.nikkras.dosering.alarm.Scheduler
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.TapCheck
import com.nikkras.dosering.notify.Notifications

/** User actions shared by the widget, the app screens and notifications. */
object IntakeActions {

    fun check(context: Context, med: Medication, now: Long = System.currentTimeMillis()): TapCheck {
        val repo = DoseRepository.get(context)
        return DoseLogic.checkTap(
            med = med,
            last = repo.latestIntake(med.id),
            recent = repo.intakesFor(med.id, now - DoseLogic.DAY_MILLIS),
            now = now,
        )
    }

    fun log(context: Context, med: Medication): Intake {
        val intake = DoseRepository.get(context).logIntake(med, System.currentTimeMillis())
        Notifications.cancelReady(context, med.id)
        DoseSync.changed(context)
        return intake
    }

    fun delete(context: Context, intake: Intake) {
        DoseRepository.get(context).deleteIntake(intake.id)
        DoseSync.changed(context)
    }

    fun saveMedication(context: Context, med: Medication): Long {
        val id = DoseRepository.get(context).saveMedication(med)
        DoseSync.changed(context)
        return id
    }

    fun deleteMedication(context: Context, med: Medication) {
        Scheduler.cancelReady(context, med.id)
        Notifications.cancelReady(context, med.id)
        DoseRepository.get(context).deleteMedication(med.id)
        DoseSync.changed(context)
    }
}

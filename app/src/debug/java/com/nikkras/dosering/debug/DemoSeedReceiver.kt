package com.nikkras.dosering.debug

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nikkras.dosering.DoseSync
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.ui.theme.Palette
import java.time.LocalDate
import java.time.ZoneId

/**
 * Replaces all data with a week of a cold, so screenshots are reproducible.
 * Pass `--ez empty true` to wipe everything instead.
 */
class DemoSeedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val repo = DoseRepository.get(context)
        repo.clearAll()
        if (intent.getBooleanExtra("empty", false)) {
            DoseSync.changed(context)
            return
        }

        val meds = listOf(
            Medication(name = "Difflam Spray", intervalMinutes = 90, doseAmount = 4.0, unit = "spray", maxPer24h = 40.0, color = Palette.colors[0]),
            Medication(name = "Strepsils", intervalMinutes = 120, doseAmount = 1.0, unit = "lozenge", maxPer24h = 12.0, color = Palette.colors[1]),
            Medication(name = "Lemsip", intervalMinutes = 240, doseAmount = 1.0, unit = "sachet", maxPer24h = 4.0, color = Palette.colors[2]),
            Medication(name = "Ibuprofen", intervalMinutes = 240, doseAmount = 400.0, unit = "mg", maxPer24h = 1200.0, color = Palette.colors[3]),
        ).map { it.copy(id = repo.saveMedication(it)) }
        val (difflam, strepsils, lemsip, ibuprofen) = meds

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        // Doses per day for the past six days, oldest first: the cold builds up, then eases off.
        val history = mapOf(
            difflam to listOf(0, 2, 4, 5, 3, 2),
            strepsils to listOf(2, 4, 6, 5, 3, 2),
            lemsip to listOf(1, 2, 3, 3, 2, 1),
            ibuprofen to listOf(0, 2, 3, 3, 2, 1),
        )
        val slots = listOf(8 * 60 + 10, 10 * 60 + 25, 12 * 60 + 40, 14 * 60 + 55, 17 * 60 + 15, 19 * 60 + 35, 21 * 60 + 50)
        history.entries.forEachIndexed { medIndex, (med, counts) ->
            counts.forEachIndexed { dayIndex, count ->
                val date = today.minusDays(6L - dayIndex)
                repeat(count) { n ->
                    val minuteOfDay = slots[(n * slots.size / count.coerceAtLeast(1) + medIndex) % slots.size] + medIndex * 7
                    val at = date.atStartOfDay(zone).plusMinutes(minuteOfDay.toLong()).toInstant().toEpochMilli()
                    repo.insertIntake(intakeOf(med, at))
                }
            }
        }

        // Today: rings at different stages so the widget shows every state.
        val now = System.currentTimeMillis()
        val minute = 60_000L
        listOf(
            lemsip to now - 300 * minute,
            ibuprofen to now - 310 * minute,
            strepsils to now - 260 * minute,
            difflam to now - 20 * minute,
            strepsils to now - 85 * minute,
            ibuprofen to now - 62 * minute,
        ).forEach { (med, at) -> repo.insertIntake(intakeOf(med, at)) }

        DoseSync.changed(context)
    }

    private fun intakeOf(med: Medication, at: Long) =
        Intake(medicationId = med.id, takenAt = at, name = med.name, doseAmount = med.doseAmount, unit = med.unit, color = med.color)
}

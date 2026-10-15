package com.nikkras.dosering.domain

import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication

/** Where a medicine is in its cycle. A full ring (progress 1) means it can be taken again. */
data class DoseStatus(
    val progress: Float,
    val remainingMillis: Long,
    val lastTakenAt: Long?,
) {
    val isReady: Boolean get() = remainingMillis <= 0L
}

/** What happens if the user takes a dose right now. */
data class TapCheck(
    val status: DoseStatus,
    val amountAfter: Double,
    val maxPer24h: Double?,
) {
    val isEarly: Boolean get() = !status.isReady
    val exceedsMax: Boolean get() = maxPer24h != null && amountAfter > maxPer24h + 1e-9
    val needsConfirmation: Boolean get() = isEarly || exceedsMax
}

object DoseLogic {

    const val DAY_MILLIS = 24 * 60 * 60 * 1000L

    fun status(med: Medication, last: Intake?, now: Long): DoseStatus {
        if (last == null || med.intervalMillis <= 0) return DoseStatus(1f, 0L, last?.takenAt)
        val elapsed = (now - last.takenAt).coerceAtLeast(0L)
        val remaining = (med.intervalMillis - elapsed).coerceAtLeast(0L)
        val progress = (elapsed.toDouble() / med.intervalMillis).coerceIn(0.0, 1.0).toFloat()
        return DoseStatus(progress, remaining, last.takenAt)
    }

    fun readyAt(med: Medication, last: Intake?): Long? = last?.let { it.takenAt + med.intervalMillis }

    /** Total amount of [medicationId] taken in the rolling 24 hours before [now]. */
    fun amountInLast24h(intakes: List<Intake>, medicationId: Long, now: Long): Double =
        intakes
            .filter { it.medicationId == medicationId && it.takenAt > now - DAY_MILLIS && it.takenAt <= now }
            .sumOf { it.doseAmount }

    fun checkTap(med: Medication, last: Intake?, recent: List<Intake>, now: Long): TapCheck =
        TapCheck(
            status = status(med, last, now),
            amountAfter = amountInLast24h(recent, med.id, now) + med.doseAmount,
            maxPer24h = med.maxPer24h,
        )

    /** "Difflam Spray" -> "DS", "Ibuprofen" -> "I". */
    fun initials(name: String): String {
        val words = name.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when (words.size) {
            0 -> ""
            1 -> words[0].take(1).uppercase()
            else -> (words[0].take(1) + words[1].take(1)).uppercase()
        }
    }

    /** Time left, rounded up to the minute so a countdown never shows 0m before it is done. */
    fun formatRemaining(millis: Long): String =
        formatMinutes(((millis + 59_999L) / 60_000L).toInt())

    /** Time passed, rounded down to the minute. */
    fun formatElapsed(millis: Long): String =
        formatMinutes((millis / 60_000L).toInt())

    fun formatMinutes(minutes: Int): String {
        val m = minutes.coerceAtLeast(0)
        val hours = m / 60
        val rest = m % 60
        return when {
            hours >= 48 -> "${hours / 24}d ${hours % 24}h"
            hours == 0 -> "${rest}m"
            rest == 0 -> "${hours}h"
            else -> "${hours}h ${rest}m"
        }
    }
}

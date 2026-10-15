package com.nikkras.dosering.domain

import com.nikkras.dosering.data.Intake
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class MedTotal(
    val medicationId: Long,
    val name: String,
    val color: Int,
    val unit: String,
    val count: Int,
    val amount: Double,
)

data class DaySummary(val date: LocalDate, val totals: List<MedTotal>) {
    val doseCount: Int get() = totals.sumOf { it.count }
}

object Stats {

    fun localDate(millis: Long, zone: ZoneId): LocalDate = Instant.ofEpochMilli(millis).atZone(zone).toLocalDate()

    /** Per-medicine totals, most frequent first. */
    fun totals(intakes: List<Intake>): List<MedTotal> =
        intakes
            .groupBy { it.medicationId to it.unit }
            .map { (key, list) ->
                val latest = list.maxBy { it.takenAt }
                MedTotal(key.first, latest.name, latest.color, key.second, list.size, list.sumOf { it.doseAmount })
            }
            .sortedWith(compareByDescending<MedTotal> { it.count }.thenBy { it.name })

    /** One summary per calendar day, oldest first, including days with no intakes. */
    fun lastDays(intakes: List<Intake>, zone: ZoneId, today: LocalDate, days: Int): List<DaySummary> {
        val byDate = intakes.groupBy { localDate(it.takenAt, zone) }
        return (days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            DaySummary(date, totals(byDate[date].orEmpty()))
        }
    }
}

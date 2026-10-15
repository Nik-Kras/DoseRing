package com.nikkras.dosering.data

/** A medicine shown as one tile on the widget. */
data class Medication(
    val id: Long = 0,
    val name: String,
    val intervalMinutes: Int,
    val doseAmount: Double,
    val unit: String,
    val maxPer24h: Double? = null,
    val color: Int,
    val position: Int = 0,
) {
    val intervalMillis: Long get() = intervalMinutes * 60_000L
}

/**
 * One logged dose. Name, dose and colour are snapshotted so the journal stays
 * accurate after the medicine is edited or deleted.
 */
data class Intake(
    val id: Long = 0,
    val medicationId: Long,
    val takenAt: Long,
    val name: String,
    val doseAmount: Double,
    val unit: String,
    val color: Int,
)

package com.nikkras.dosering.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor

/**
 * All reads and writes go through here. The data set is tiny (a handful of
 * medicines, a few intakes a day), so queries run synchronously.
 */
class DoseRepository private constructor(context: Context) {

    private val helper = DoseDatabase(context.applicationContext)

    fun medications(): List<Medication> =
        query("SELECT * FROM medications ORDER BY position, id").map(::readMedication)

    fun medication(id: Long): Medication? =
        query("SELECT * FROM medications WHERE id = ?", id).map(::readMedication).firstOrNull()

    fun saveMedication(med: Medication): Long {
        val db = helper.writableDatabase
        val values = ContentValues().apply {
            put("name", med.name.trim())
            put("interval_minutes", med.intervalMinutes)
            put("dose_amount", med.doseAmount)
            put("unit", med.unit)
            if (med.maxPer24h != null) put("max_per_24h", med.maxPer24h) else putNull("max_per_24h")
            put("color", med.color)
        }
        if (med.id != 0L) {
            db.update("medications", values, "id = ?", arrayOf(med.id.toString()))
            return med.id
        }
        val nextPosition = db.rawQuery("SELECT COALESCE(MAX(position), -1) + 1 FROM medications", null)
            .use { it.moveToFirst(); it.getInt(0) }
        values.put("position", nextPosition)
        return db.insertOrThrow("medications", null, values)
    }

    fun deleteMedication(id: Long) {
        helper.writableDatabase.delete("medications", "id = ?", arrayOf(id.toString()))
    }

    fun logIntake(med: Medication, takenAt: Long): Intake =
        insertIntake(
            Intake(
                medicationId = med.id,
                takenAt = takenAt,
                name = med.name,
                doseAmount = med.doseAmount,
                unit = med.unit,
                color = med.color,
            )
        )

    fun insertIntake(intake: Intake): Intake {
        val values = ContentValues().apply {
            put("medication_id", intake.medicationId)
            put("taken_at", intake.takenAt)
            put("name", intake.name)
            put("dose_amount", intake.doseAmount)
            put("unit", intake.unit)
            put("color", intake.color)
        }
        val id = helper.writableDatabase.insertOrThrow("intakes", null, values)
        return intake.copy(id = id)
    }

    fun deleteIntake(id: Long) {
        helper.writableDatabase.delete("intakes", "id = ?", arrayOf(id.toString()))
    }

    fun latestIntake(medicationId: Long): Intake? =
        query(
            "SELECT * FROM intakes WHERE medication_id = ? ORDER BY taken_at DESC, id DESC LIMIT 1",
            medicationId,
        ).map(::readIntake).firstOrNull()

    /** Most recent intake per medication, keyed by medication id. */
    fun latestIntakes(): Map<Long, Intake> =
        query(
            """
            SELECT * FROM intakes i WHERE i.id = (
                SELECT id FROM intakes WHERE medication_id = i.medication_id
                ORDER BY taken_at DESC, id DESC LIMIT 1
            )
            """.trimIndent()
        ).map(::readIntake).associateBy { it.medicationId }

    fun intakesSince(since: Long): List<Intake> =
        query("SELECT * FROM intakes WHERE taken_at >= ? ORDER BY taken_at DESC, id DESC", since)
            .map(::readIntake)

    fun intakesFor(medicationId: Long, since: Long): List<Intake> =
        query(
            "SELECT * FROM intakes WHERE medication_id = ? AND taken_at >= ? ORDER BY taken_at DESC",
            medicationId, since,
        ).map(::readIntake)

    fun allIntakes(): List<Intake> =
        query("SELECT * FROM intakes ORDER BY taken_at DESC, id DESC").map(::readIntake)

    fun clearAll() {
        helper.writableDatabase.apply {
            delete("intakes", null, null)
            delete("medications", null, null)
        }
    }

    private fun query(sql: String, vararg args: Any): Rows =
        Rows(helper.readableDatabase.rawQuery(sql, args.map { it.toString() }.toTypedArray()))

    private class Rows(private val cursor: Cursor) {
        fun <T> map(read: (Cursor) -> T): List<T> = cursor.use { c ->
            buildList(c.count) { while (c.moveToNext()) add(read(c)) }
        }
    }

    private fun readMedication(c: Cursor) = Medication(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        name = c.getString(c.getColumnIndexOrThrow("name")),
        intervalMinutes = c.getInt(c.getColumnIndexOrThrow("interval_minutes")),
        doseAmount = c.getDouble(c.getColumnIndexOrThrow("dose_amount")),
        unit = c.getString(c.getColumnIndexOrThrow("unit")),
        maxPer24h = c.getColumnIndexOrThrow("max_per_24h").let { if (c.isNull(it)) null else c.getDouble(it) },
        color = c.getInt(c.getColumnIndexOrThrow("color")),
        position = c.getInt(c.getColumnIndexOrThrow("position")),
    )

    private fun readIntake(c: Cursor) = Intake(
        id = c.getLong(c.getColumnIndexOrThrow("id")),
        medicationId = c.getLong(c.getColumnIndexOrThrow("medication_id")),
        takenAt = c.getLong(c.getColumnIndexOrThrow("taken_at")),
        name = c.getString(c.getColumnIndexOrThrow("name")),
        doseAmount = c.getDouble(c.getColumnIndexOrThrow("dose_amount")),
        unit = c.getString(c.getColumnIndexOrThrow("unit")),
        color = c.getInt(c.getColumnIndexOrThrow("color")),
    )

    companion object {
        @Volatile private var instance: DoseRepository? = null

        fun get(context: Context): DoseRepository =
            instance ?: synchronized(this) {
                instance ?: DoseRepository(context).also { instance = it }
            }
    }
}

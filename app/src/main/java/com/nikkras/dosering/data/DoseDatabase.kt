package com.nikkras.dosering.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

internal class DoseDatabase(context: Context) :
    SQLiteOpenHelper(context, NAME, null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        db.enableWriteAheadLogging()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE medications (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                interval_minutes INTEGER NOT NULL,
                dose_amount REAL NOT NULL,
                unit TEXT NOT NULL,
                max_per_24h REAL,
                color INTEGER NOT NULL,
                position INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE intakes (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                medication_id INTEGER NOT NULL,
                taken_at INTEGER NOT NULL,
                name TEXT NOT NULL,
                dose_amount REAL NOT NULL,
                unit TEXT NOT NULL,
                color INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX idx_intakes_medication_time ON intakes(medication_id, taken_at)")
        db.execSQL("CREATE INDEX idx_intakes_time ON intakes(taken_at)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    private companion object {
        const val NAME = "dosering.db"
        const val VERSION = 1
    }
}

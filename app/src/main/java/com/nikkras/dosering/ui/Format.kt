package com.nikkras.dosering.ui

import android.content.Context
import android.text.format.DateFormat
import com.nikkras.dosering.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date

object Format {

    private val dayFormatter = DateTimeFormatter.ofPattern("EEE, d MMM")

    /** Respects the phone's 12/24-hour setting. */
    fun time(context: Context, millis: Long): String = DateFormat.getTimeFormat(context).format(Date(millis))

    fun day(context: Context, date: LocalDate, today: LocalDate = LocalDate.now()): String = when (date) {
        today -> context.getString(R.string.today)
        today.minusDays(1) -> context.getString(R.string.yesterday)
        else -> date.format(dayFormatter)
    }
}

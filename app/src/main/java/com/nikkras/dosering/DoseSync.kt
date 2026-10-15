package com.nikkras.dosering

import android.content.Context
import com.nikkras.dosering.alarm.Scheduler
import com.nikkras.dosering.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Single place that keeps every surface in step with the database:
 * open screens (via [version]), home-screen widgets and alarms.
 */
object DoseSync {

    private val _version = MutableStateFlow(0)
    val version: StateFlow<Int> = _version.asStateFlow()

    /** Call after any write. */
    fun changed(context: Context) {
        _version.update { it + 1 }
        refresh(context)
    }

    /** Re-render widgets and re-arm alarms without signalling a data change. */
    fun refresh(context: Context) {
        val app = context.applicationContext
        WidgetUpdater.updateAll(app)
        Scheduler.rescheduleAll(app)
    }
}

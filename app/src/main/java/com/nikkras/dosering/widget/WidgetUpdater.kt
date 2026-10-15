package com.nikkras.dosering.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.nikkras.dosering.R
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.ui.ActionActivity
import com.nikkras.dosering.ui.MainActivity
import com.nikkras.dosering.ui.Tab
import kotlin.math.roundToInt

object WidgetUpdater {

    private const val PLUS_ITEM_ID = Long.MAX_VALUE
    private const val RING_DP = 54f
    private const val PLUS_COLOR = 0xFF8E95A3.toInt()

    fun widgetIds(context: Context): IntArray =
        AppWidgetManager.getInstance(context)
            .getAppWidgetIds(ComponentName(context, DoseWidgetProvider::class.java))

    fun updateAll(context: Context) {
        val ids = widgetIds(context)
        if (ids.isEmpty()) return
        val repo = DoseRepository.get(context)
        val meds = repo.medications()
        val latest = repo.latestIntakes()
        val now = System.currentTimeMillis()
        val manager = AppWidgetManager.getInstance(context)
        for (id in ids) manager.updateAppWidget(id, build(context, id, meds, latest, now))
    }

    private fun build(
        context: Context,
        widgetId: Int,
        meds: List<Medication>,
        latest: Map<Long, Intake>,
        now: Long,
    ): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.widget_root)

        // Every tile opens ActionActivity; the fill-in intent on each tile says what to do.
        // It has to be an activity because early taps and cancellations ask for confirmation.
        val template = PendingIntent.getActivity(
            context,
            widgetId,
            Intent(context, ActionActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        views.setPendingIntentTemplate(R.id.grid, template)
        views.setOnClickPendingIntent(
            R.id.journal_button,
            PendingIntent.getActivity(
                context,
                0,
                MainActivity.intent(context, Tab.Journal),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            ),
        )
        views.setViewVisibility(R.id.hint, if (meds.isEmpty()) View.VISIBLE else View.GONE)

        val ringPx = (RING_DP * context.resources.displayMetrics.density).roundToInt()
        val items = RemoteViews.RemoteCollectionItems.Builder()
            .setHasStableIds(true)
            .setViewTypeCount(1)
        meds.forEach { med -> items.addItem(med.id, tile(context, med, latest[med.id], now, ringPx)) }
        items.addItem(PLUS_ITEM_ID, plusTile(context, ringPx))
        views.setRemoteAdapter(R.id.grid, items.build())
        return views
    }

    private fun tile(context: Context, med: Medication, last: Intake?, now: Long, ringPx: Int): RemoteViews {
        val status = DoseLogic.status(med, last, now)
        return RemoteViews(context.packageName, R.layout.widget_tile).apply {
            setImageViewBitmap(
                R.id.ring,
                RingRenderer.ring(ringPx, med.color, status.progress, DoseLogic.initials(med.name), status.isReady),
            )
            setTextViewText(R.id.name, med.name)
            if (status.isReady) {
                setTextViewText(R.id.status, context.getString(R.string.status_ready))
                setTextColor(R.id.status, med.color)
                setViewVisibility(R.id.cancel, View.GONE)
                setContentDescription(R.id.tile, context.getString(R.string.tile_ready_description, med.name))
            } else {
                val remaining = DoseLogic.formatRemaining(status.remainingMillis)
                setTextViewText(R.id.status, remaining)
                setColorStateList(R.id.status, "setTextColor", R.color.widget_text_secondary)
                setViewVisibility(R.id.cancel, View.VISIBLE)
                setOnClickFillInIntent(R.id.cancel, ActionActivity.fillIn(ActionActivity.MODE_CANCEL, med.id))
                setContentDescription(R.id.cancel, context.getString(R.string.cancel_intake_for, med.name))
                setContentDescription(R.id.tile, context.getString(R.string.tile_waiting_description, med.name, remaining))
            }
            setOnClickFillInIntent(R.id.tile, ActionActivity.fillIn(ActionActivity.MODE_TAP, med.id))
        }
    }

    private fun plusTile(context: Context, ringPx: Int): RemoteViews =
        RemoteViews(context.packageName, R.layout.widget_tile).apply {
            setImageViewBitmap(R.id.ring, RingRenderer.plus(ringPx, PLUS_COLOR))
            setTextViewText(R.id.name, "")
            setTextViewText(R.id.status, "")
            setViewVisibility(R.id.cancel, View.GONE)
            setContentDescription(R.id.tile, context.getString(R.string.add_medicine))
            setOnClickFillInIntent(R.id.tile, ActionActivity.fillIn(ActionActivity.MODE_ADD))
        }
}

package com.nikkras.dosering.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.nikkras.dosering.R
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.domain.DaySummary
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.domain.MedTotal
import com.nikkras.dosering.domain.Stats
import com.nikkras.dosering.ui.Format
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

private val RANGES = listOf(7, 30)

@Composable
fun StatsScreen(contentPadding: PaddingValues, version: Int, now: Long) {
    val context = LocalContext.current
    val repo = remember { DoseRepository.get(context) }
    var range by rememberSaveable { mutableIntStateOf(7) }
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val since = today.minusDays(range - 1L).atStartOfDay(zone).toInstant().toEpochMilli()

    val intakes = remember(version, range, today) { repo.intakesSince(minOf(since, now - DoseLogic.DAY_MILLIS)) }
    val meds = remember(version) { repo.medications() }
    val inRange = remember(intakes, since) { intakes.filter { it.takenAt >= since } }
    val days = remember(inRange, range, today) { Stats.lastDays(inRange, zone, today, range) }

    val hasAnyIntake = remember(version) { repo.allIntakes().isNotEmpty() }
    if (!hasAnyIntake) {
        EmptyState(
            icon = Icons.Outlined.BarChart,
            title = stringResource(R.string.empty_stats_title),
            text = stringResource(R.string.empty_stats_text),
            contentPadding = contentPadding,
        )
        return
    }

    LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item(key = "range") {
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                RANGES.forEachIndexed { index, days ->
                    SegmentedButton(
                        selected = range == days,
                        onClick = { range = days },
                        shape = SegmentedButtonDefaults.itemShape(index, RANGES.size),
                    ) { Text(stringResource(R.string.last_n_days, days)) }
                }
            }
        }

        item(key = "chart") {
            StatCard(stringResource(R.string.doses_per_day)) {
                val total = inRange.size
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    Figure(total.toString(), stringResource(R.string.total_doses))
                    Figure(String.format(Locale.getDefault(), "%.1f", total.toDouble() / range), stringResource(R.string.avg_per_day))
                    Figure(days.count { it.doseCount > 0 }.toString(), stringResource(R.string.days_with_doses))
                }
                Spacer(Modifier.height(16.dp))
                DoseChart(days)
                Spacer(Modifier.height(12.dp))
                Legend(Stats.totals(inRange))
            }
        }

        val windowStart = now - DoseLogic.DAY_MILLIS
        val last24 = meds.mapNotNull { med ->
            val amount = DoseLogic.amountInLast24h(intakes, med.id, now)
            if (amount > 0 || med.maxPer24h != null) Triple(med, amount, intakes.count { it.medicationId == med.id && it.takenAt > windowStart }) else null
        }
        if (last24.isNotEmpty()) {
            item(key = "last24") {
                StatCard(stringResource(R.string.last_24_hours)) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        last24.forEach { (med, amount, count) ->
                            val max = med.maxPer24h
                            val color = Color(med.color)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    ColorDot(color)
                                    Spacer(Modifier.width(8.dp))
                                    Text(med.name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                    Text(
                                        if (max != null) "${Doses.number(amount)} / ${Doses.format(max, med.unit)}"
                                        else pluralStringResource(R.plurals.doses_count, count, count) + " · " + Doses.format(amount, med.unit),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (max != null && amount > max) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (max != null) {
                                    LinearProgressIndicator(
                                        progress = { (amount / max).toFloat().coerceIn(0f, 1f) },
                                        color = if (amount > max) MaterialTheme.colorScheme.error else color,
                                        trackColor = color.copy(alpha = 0.18f),
                                        strokeCap = StrokeCap.Round,
                                        gapSize = 0.dp,
                                        drawStopIndicator = {},
                                        modifier = Modifier.fillMaxWidth().height(8.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item(key = "perDayTitle") { SectionTitle(stringResource(R.string.per_day)) }
        items(days.reversed().filter { it.doseCount > 0 }, key = { it.date.toString() }) { day ->
            DayCard(Format.day(context, day.date, today), day)
        }
    }
}

@Composable
private fun StatCard(title: String, content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun Figure(value: String, label: String) {
    Column {
        Text(value, style = MaterialTheme.typography.headlineSmall)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Stacked bars: one bar per day, one coloured segment per medicine. */
@Composable
private fun DoseChart(days: List<DaySummary>) {
    val grid = MaterialTheme.colorScheme.outlineVariant
    val maxCount = (days.maxOfOrNull { it.doseCount } ?: 0).coerceAtLeast(1)
    Row(verticalAlignment = Alignment.Bottom) {
        Canvas(
            Modifier
                .weight(1f)
                .height(150.dp)
        ) {
            val slot = size.width / days.size
            val barWidth = (slot * 0.62f).coerceAtMost(28.dp.toPx())
            val unit = size.height / maxCount
            for (line in 0..2) {
                val y = size.height - size.height * line / 2f
                drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
            }
            days.forEachIndexed { index, day ->
                var top = size.height
                val left = slot * index + (slot - barWidth) / 2f
                day.totals.forEach { total ->
                    val h = total.count * unit
                    drawRoundRect(
                        color = Color(total.color),
                        topLeft = Offset(left, top - h + 1.dp.toPx()),
                        size = Size(barWidth, h - 1.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx()),
                    )
                    top -= h
                }
            }
        }
        Text(
            maxCount.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 6.dp).align(Alignment.Top),
        )
    }
    Row(Modifier.fillMaxWidth().padding(end = 18.dp, top = 6.dp)) {
        days.forEachIndexed { index, day ->
            val show = days.size <= 7 || index % 5 == (days.size - 1) % 5
            Text(
                if (!show) "" else if (days.size <= 7) {
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                } else {
                    day.date.dayOfMonth.toString()
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Legend(totals: List<MedTotal>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        totals.distinctBy { it.medicationId }.forEach { total ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorDot(Color(total.color))
                Spacer(Modifier.width(6.dp))
                Text(total.name, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun DayCard(title: String, day: DaySummary) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row {
                Text(title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Text(
                    pluralStringResource(R.plurals.doses_count, day.doseCount, day.doseCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            day.totals.forEach { total ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ColorDot(Color(total.color))
                    Spacer(Modifier.width(10.dp))
                    Text(total.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    Text(
                        "${total.count}× · ${Doses.format(total.amount, total.unit)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

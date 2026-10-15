package com.nikkras.dosering.ui.screens

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikkras.dosering.IntakeActions
import com.nikkras.dosering.R
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.DoseStatus
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.domain.TapCheck
import com.nikkras.dosering.ui.components.CancelIntakeDialog
import com.nikkras.dosering.ui.components.ConfirmIntakeDialog
import com.nikkras.dosering.ui.components.DeleteMedicationDialog
import com.nikkras.dosering.ui.components.MedRing
import com.nikkras.dosering.widget.DoseWidgetProvider
import com.nikkras.dosering.widget.WidgetUpdater

@Composable
fun MedicinesScreen(
    contentPadding: PaddingValues,
    version: Int,
    now: Long,
    resumeCount: Int,
    onEdit: (Medication) -> Unit,
) {
    val context = LocalContext.current
    val repo = remember { DoseRepository.get(context) }
    val meds = remember(version) { repo.medications() }
    val latest = remember(version) { repo.latestIntakes() }
    val hasWidget = remember(version, resumeCount) { WidgetUpdater.widgetIds(context).isNotEmpty() }
    val clock = maxOf(now, remember(version) { System.currentTimeMillis() })

    var confirmTap by remember { mutableStateOf<Pair<Medication, TapCheck>?>(null) }
    var confirmUndo by remember { mutableStateOf<Intake?>(null) }
    var confirmDelete by remember { mutableStateOf<Medication?>(null) }

    fun log(med: Medication) {
        IntakeActions.log(context, med)
        Toast.makeText(context, context.getString(R.string.toast_logged, med.name, Doses.format(med.doseAmount, med.unit)), Toast.LENGTH_SHORT).show()
    }

    if (meds.isEmpty() && hasWidget) {
        EmptyState(
            icon = Icons.Outlined.Medication,
            title = stringResource(R.string.empty_medicines_title),
            text = stringResource(R.string.empty_medicines_text),
            contentPadding = contentPadding,
        )
    } else {
        LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!hasWidget) item(key = "widget") { WidgetPromoCard() }
            items(meds, key = { it.id }) { med ->
                MedicationCard(
                    med = med,
                    status = DoseLogic.status(med, latest[med.id], clock),
                    onTake = {
                        val check = IntakeActions.check(context, med)
                        if (check.needsConfirmation) confirmTap = med to check else log(med)
                    },
                    onEdit = { onEdit(med) },
                    onUndo = { confirmUndo = latest[med.id] },
                    onDelete = { confirmDelete = med },
                )
            }
        }
    }

    confirmTap?.let { (med, check) ->
        ConfirmIntakeDialog(
            med = med,
            check = check,
            now = System.currentTimeMillis(),
            onConfirm = { log(med); confirmTap = null },
            onDismiss = { confirmTap = null },
        )
    }
    confirmUndo?.let { intake ->
        CancelIntakeDialog(intake, onConfirm = { IntakeActions.delete(context, intake); confirmUndo = null }, onDismiss = { confirmUndo = null })
    }
    confirmDelete?.let { med ->
        DeleteMedicationDialog(med, onConfirm = { IntakeActions.deleteMedication(context, med); confirmDelete = null }, onDismiss = { confirmDelete = null })
    }
}

@Composable
private fun MedicationCard(
    med: Medication,
    status: DoseStatus,
    onTake: () -> Unit,
    onEdit: () -> Unit,
    onUndo: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    val color = Color(med.color)
    var menuOpen by remember { mutableStateOf(false) }

    ElevatedCard(onClick = onEdit, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 16.dp, bottom = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            MedRing(color, status.progress, DoseLogic.initials(med.name), status.isReady, size = 56.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(med.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    Doses.format(med.doseAmount, med.unit) + " · " +
                        context.getString(R.string.every_interval, DoseLogic.formatMinutes(med.intervalMinutes)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                med.maxPer24h?.let { max ->
                    Text(
                        context.getString(R.string.max_per_day, Doses.format(max, med.unit)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    if (status.isReady) {
                        context.getString(R.string.status_ready_now)
                    } else {
                        context.getString(R.string.next_dose_in, DoseLogic.formatRemaining(status.remainingMillis))
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = if (status.isReady) color else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FilledTonalButton(onClick = onTake) { Text(stringResource(R.string.take)) }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Outlined.MoreVert, contentDescription = stringResource(R.string.more_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                        onClick = { menuOpen = false; onEdit() },
                    )
                    if (!status.isReady) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.undo_last_dose)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Outlined.Undo, contentDescription = null) },
                            onClick = { menuOpen = false; onUndo() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.delete)) },
                        leadingIcon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun WidgetPromoCard() {
    val context = LocalContext.current
    val manager = remember { AppWidgetManager.getInstance(context) }
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Outlined.Widgets, contentDescription = null)
                Text(stringResource(R.string.widget_promo_title), style = MaterialTheme.typography.titleMedium)
            }
            Text(stringResource(R.string.widget_promo_text), style = MaterialTheme.typography.bodyMedium)
            if (manager.isRequestPinAppWidgetSupported) {
                Button(
                    onClick = { manager.requestPinAppWidget(ComponentName(context, DoseWidgetProvider::class.java), null, null) },
                    modifier = Modifier.padding(top = 4.dp),
                ) { Text(stringResource(R.string.widget_promo_button)) }
            } else {
                Text(stringResource(R.string.widget_promo_manual), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

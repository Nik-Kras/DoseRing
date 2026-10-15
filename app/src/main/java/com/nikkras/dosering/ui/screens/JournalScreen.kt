package com.nikkras.dosering.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nikkras.dosering.IntakeActions
import com.nikkras.dosering.R
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.domain.Stats
import com.nikkras.dosering.ui.Format
import com.nikkras.dosering.ui.components.DeleteIntakeDialog
import com.nikkras.dosering.ui.components.MedRing
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun JournalScreen(contentPadding: PaddingValues, version: Int) {
    val context = LocalContext.current
    val intakes = remember(version) { DoseRepository.get(context).allIntakes() }
    val zone = remember { ZoneId.systemDefault() }
    val today = LocalDate.now(zone)
    val days = remember(intakes) { intakes.groupBy { Stats.localDate(it.takenAt, zone) } }
    var toDelete by remember { mutableStateOf<Intake?>(null) }

    if (intakes.isEmpty()) {
        EmptyState(
            icon = Icons.Outlined.History,
            title = stringResource(R.string.empty_journal_title),
            text = stringResource(R.string.empty_journal_text),
            contentPadding = contentPadding,
        )
    } else {
        LazyColumn(contentPadding = contentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            days.forEach { (date, list) ->
                item(key = date.toString()) {
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(Format.day(context, date, today), style = MaterialTheme.typography.titleSmall)
                            Text(
                                pluralStringResource(R.plurals.doses_count, list.size, list.size),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            list.forEachIndexed { index, intake ->
                                if (index > 0) HorizontalDivider(Modifier.padding(start = 72.dp))
                                JournalRow(intake, onDelete = { toDelete = intake })
                            }
                        }
                    }
                }
            }
        }
    }

    toDelete?.let { intake ->
        DeleteIntakeDialog(intake, onConfirm = { IntakeActions.delete(context, intake); toDelete = null }, onDismiss = { toDelete = null })
    }
}

@Composable
private fun JournalRow(intake: Intake, onDelete: () -> Unit) {
    val context = LocalContext.current
    ListItem(
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
        leadingContent = { MedRing(Color(intake.color), 1f, DoseLogic.initials(intake.name), ready = true, size = 40.dp) },
        headlineContent = { Text(intake.name) },
        supportingContent = { Text(Doses.format(intake.doseAmount, intake.unit)) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(Format.time(context, intake.takenAt), style = MaterialTheme.typography.labelLarge)
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
    )
}

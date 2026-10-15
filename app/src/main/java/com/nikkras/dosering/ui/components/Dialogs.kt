package com.nikkras.dosering.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nikkras.dosering.R
import com.nikkras.dosering.data.Intake
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.domain.TapCheck
import com.nikkras.dosering.ui.Format

/** Shown when a dose would be early or over the 24-hour limit. */
@Composable
fun ConfirmIntakeDialog(med: Medication, check: TapCheck, now: Long, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.confirm_take_title, med.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                val lastAt = check.status.lastTakenAt
                if (check.isEarly && lastAt != null) {
                    val elapsed = now - lastAt
                    Text(
                        stringResource(
                            R.string.confirm_take_early,
                            if (elapsed < 60_000L) stringResource(R.string.less_than_a_minute) else DoseLogic.formatElapsed(elapsed),
                            Format.time(context, lastAt),
                            DoseLogic.formatMinutes(med.intervalMinutes),
                            DoseLogic.formatRemaining(check.status.remainingMillis),
                        )
                    )
                }
                val max = check.maxPer24h
                if (check.exceedsMax && max != null) {
                    Text(
                        stringResource(
                            R.string.confirm_take_max,
                            Doses.format(check.amountAfter, med.unit),
                            Doses.format(max, med.unit),
                        )
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.log_anyway))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}

/** The widget's red cross: undo a dose that was logged by mistake. */
@Composable
fun CancelIntakeDialog(intake: Intake, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Close, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(R.string.cancel_title)) },
        text = {
            Text(
                stringResource(
                    R.string.cancel_text,
                    intake.name,
                    Doses.format(intake.doseAmount, intake.unit),
                    Format.time(context, intake.takenAt),
                )
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.cancel_confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } },
    )
}

@Composable
fun DeleteIntakeDialog(intake: Intake, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    DestructiveDialog(
        title = stringResource(R.string.delete_intake_title),
        text = stringResource(
            R.string.delete_intake_text,
            intake.name,
            Doses.format(intake.doseAmount, intake.unit),
            Format.time(context, intake.takenAt),
        ),
        confirm = stringResource(R.string.delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
fun DeleteMedicationDialog(med: Medication, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    DestructiveDialog(
        title = stringResource(R.string.delete_medication_title, med.name),
        text = stringResource(R.string.delete_medication_text),
        confirm = stringResource(R.string.delete),
        onConfirm = onConfirm,
        onDismiss = onDismiss,
    )
}

@Composable
private fun DestructiveDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.DeleteOutline, contentDescription = null) },
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(confirm)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

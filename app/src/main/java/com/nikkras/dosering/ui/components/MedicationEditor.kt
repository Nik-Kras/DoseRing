package com.nikkras.dosering.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.nikkras.dosering.R
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.DoseLogic
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.ui.theme.Palette

private val INTERVAL_PRESETS_HOURS = listOf(1, 2, 3, 4, 6, 8, 12)

/** Add or edit a medicine: name, time between doses, dose per tap, optional 24h limit, colour. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MedicationEditorDialog(
    initial: Medication?,
    defaultColor: Int,
    onSave: (Medication) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var hours by rememberSaveable { mutableStateOf(initial?.let { (it.intervalMinutes / 60).toString() } ?: "4") }
    var minutes by rememberSaveable { mutableStateOf(initial?.let { (it.intervalMinutes % 60).toString() } ?: "0") }
    var dose by rememberSaveable { mutableStateOf(initial?.let { Doses.number(it.doseAmount) } ?: "") }
    var unit by rememberSaveable { mutableStateOf(initial?.unit ?: "mg") }
    var max by rememberSaveable { mutableStateOf(initial?.maxPer24h?.let { Doses.number(it) } ?: "") }
    var color by rememberSaveable { mutableIntStateOf(initial?.color ?: defaultColor) }

    val intervalMinutes = (hours.toIntOrNull() ?: 0) * 60 + (minutes.toIntOrNull() ?: 0)
    val doseValue = Doses.parse(dose)
    val maxValue = if (max.isBlank()) null else Doses.parse(max)
    val maxError = max.isNotBlank() && (maxValue == null || (doseValue != null && maxValue < doseValue))
    val valid = name.isNotBlank() && intervalMinutes > 0 && doseValue != null && !maxError

    val nameFocus = remember { FocusRequester() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .imePadding(),
        ) {
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    MedRing(Color(color), 1f, DoseLogic.initials(name).ifEmpty { "+" }, ready = true, size = 48.dp)
                    Text(
                        stringResource(if (initial == null) R.string.editor_title_add else R.string.editor_title_edit),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.field_name)) },
                    placeholder = { Text(stringResource(R.string.field_name_hint)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth().focusRequester(nameFocus),
                )
                // Runs inside the dialog window, after the field above is attached.
                if (initial == null) LaunchedEffect(Unit) { runCatching { nameFocus.requestFocus() } }

                SectionLabel(stringResource(R.string.section_interval))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    NumberField(hours, { hours = it }, stringResource(R.string.field_hours), Modifier.weight(1f))
                    NumberField(minutes, { minutes = it }, stringResource(R.string.field_minutes), Modifier.weight(1f))
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    INTERVAL_PRESETS_HOURS.forEach { h ->
                        FilterChip(
                            selected = intervalMinutes == h * 60,
                            onClick = { hours = h.toString(); minutes = "0" },
                            label = { Text("${h}h") },
                        )
                    }
                }

                SectionLabel(stringResource(R.string.section_dose))
                OutlinedTextField(
                    value = dose,
                    onValueChange = { dose = it },
                    label = { Text(stringResource(R.string.field_amount)) },
                    suffix = { Text(Doses.unitLabel(doseValue ?: 2.0, unit)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Doses.UNITS.forEach { u ->
                        FilterChip(selected = unit == u, onClick = { unit = u }, label = { Text(u) })
                    }
                }

                SectionLabel(stringResource(R.string.section_limit))
                OutlinedTextField(
                    value = max,
                    onValueChange = { max = it },
                    label = { Text(stringResource(R.string.field_max)) },
                    suffix = { Text(Doses.unitLabel(maxValue ?: 2.0, unit)) },
                    isError = maxError,
                    supportingText = {
                        Text(stringResource(if (maxError) R.string.field_max_error else R.string.field_max_hint))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )

                SectionLabel(stringResource(R.string.section_colour))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Palette.colors.forEach { c -> ColorSwatch(Color(c), selected = c == color) { color = c } }
                }

                Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
                    Button(
                        enabled = valid,
                        onClick = {
                            onSave(
                                Medication(
                                    id = initial?.id ?: 0L,
                                    name = name.trim(),
                                    intervalMinutes = intervalMinutes,
                                    doseAmount = doseValue ?: return@Button,
                                    unit = unit,
                                    maxPer24h = maxValue,
                                    color = color,
                                    position = initial?.position ?: 0,
                                )
                            )
                        },
                    ) { Text(stringResource(R.string.save)) }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { text -> onChange(text.filter(Char::isDigit).take(3)) },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
        modifier = modifier,
    )
}

@Composable
private fun ColorSwatch(color: Color, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .size(32.dp)
            .background(color, CircleShape)
            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) Icon(Icons.Outlined.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
    }
}

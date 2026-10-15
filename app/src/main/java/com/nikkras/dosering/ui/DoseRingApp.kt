package com.nikkras.dosering.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Medication
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nikkras.dosering.DoseSync
import com.nikkras.dosering.IntakeActions
import com.nikkras.dosering.R
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.ui.components.MedicationEditorDialog
import com.nikkras.dosering.ui.screens.JournalScreen
import com.nikkras.dosering.ui.screens.MedicinesScreen
import com.nikkras.dosering.ui.screens.StatsScreen
import com.nikkras.dosering.ui.theme.Palette
import kotlinx.coroutines.delay

/** Which medicine the editor is open for; [medication] is null when adding. */
private data class EditorRequest(val medication: Medication?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoseRingApp(tab: Tab, onTabChange: (Tab) -> Unit, resumeCount: Int) {
    val context = LocalContext.current
    val version by DoseSync.version.collectAsState()
    val now by rememberNow()
    var editor by remember { mutableStateOf<EditorRequest?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            when (tab) {
                                Tab.Medicines -> R.string.app_name
                                Tab.Journal -> R.string.tab_journal
                                Tab.Stats -> R.string.tab_stats
                            }
                        )
                    )
                },
            )
        },
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { item ->
                    NavigationBarItem(
                        selected = item == tab,
                        onClick = { onTabChange(item) },
                        icon = {
                            Icon(
                                when (item) {
                                    Tab.Medicines -> Icons.Outlined.Medication
                                    Tab.Journal -> Icons.Outlined.History
                                    Tab.Stats -> Icons.Outlined.BarChart
                                },
                                contentDescription = null,
                            )
                        },
                        label = {
                            Text(
                                stringResource(
                                    when (item) {
                                        Tab.Medicines -> R.string.tab_medicines
                                        Tab.Journal -> R.string.tab_journal
                                        Tab.Stats -> R.string.tab_stats
                                    }
                                )
                            )
                        },
                    )
                }
            }
        },
        floatingActionButton = {
            if (tab == Tab.Medicines) {
                ExtendedFloatingActionButton(
                    onClick = { editor = EditorRequest(null) },
                    icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add_medicine)) },
                )
            }
        },
    ) { padding ->
        val content = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = padding.calculateTopPadding() + 8.dp,
            bottom = padding.calculateBottomPadding() + if (tab == Tab.Medicines) 96.dp else 16.dp,
        )
        when (tab) {
            Tab.Medicines -> MedicinesScreen(content, version, now, resumeCount, onEdit = { editor = EditorRequest(it) })
            Tab.Journal -> JournalScreen(content, version)
            Tab.Stats -> StatsScreen(content, version, now)
        }
    }

    editor?.let { request ->
        val used = remember(version) { DoseRepository.get(context).medications().map { it.color } }
        MedicationEditorDialog(
            initial = request.medication,
            defaultColor = Palette.next(used),
            onSave = { med ->
                IntakeActions.saveMedication(context, med)
                editor = null
            },
            onDismiss = { editor = null },
        )
    }
}

/** Wall-clock time, refreshed every few seconds so rings and countdowns stay live. */
@Composable
fun rememberNow(periodMillis: Long = 5_000L): State<Long> =
    produceState(System.currentTimeMillis()) {
        while (true) {
            delay(periodMillis - System.currentTimeMillis() % periodMillis)
            value = System.currentTimeMillis()
        }
    }

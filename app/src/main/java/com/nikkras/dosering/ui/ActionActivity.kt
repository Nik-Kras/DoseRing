package com.nikkras.dosering.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.nikkras.dosering.IntakeActions
import com.nikkras.dosering.R
import com.nikkras.dosering.data.DoseRepository
import com.nikkras.dosering.data.Medication
import com.nikkras.dosering.domain.Doses
import com.nikkras.dosering.ui.components.CancelIntakeDialog
import com.nikkras.dosering.ui.components.ConfirmIntakeDialog
import com.nikkras.dosering.ui.components.MedicationEditorDialog
import com.nikkras.dosering.ui.theme.DoseRingTheme
import com.nikkras.dosering.ui.theme.Palette

/**
 * Invisible activity behind every widget tap. A tap on a ready medicine logs the dose and
 * finishes straight away; anything that needs a decision shows a dialog over the home screen.
 */
class ActionActivity : ComponentActivity() {

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { finishQuietly() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = DoseRepository.get(this)
        val medicationId = intent.getLongExtra(EXTRA_MEDICATION_ID, -1L)
        when (intent.getStringExtra(EXTRA_MODE)) {
            MODE_TAP -> handleTap(repo.medication(medicationId))
            MODE_CANCEL -> handleCancel(repo.medication(medicationId))
            MODE_ADD -> showEditor(null)
            MODE_EDIT -> repo.medication(medicationId)?.let(::showEditor) ?: finishQuietly()
            else -> finishQuietly()
        }
    }

    private fun handleTap(med: Medication?) {
        if (med == null) return finishQuietly()
        val now = System.currentTimeMillis()
        val check = IntakeActions.check(this, med, now)
        if (!check.needsConfirmation) return logAndFinish(med)
        setContent {
            DoseRingTheme {
                ConfirmIntakeDialog(med, check, now, onConfirm = { logAndFinish(med) }, onDismiss = ::finishQuietly)
            }
        }
    }

    private fun logAndFinish(med: Medication) {
        IntakeActions.log(this, med)
        toast(getString(R.string.toast_logged, med.name, Doses.format(med.doseAmount, med.unit)))
        finishQuietly()
    }

    private fun handleCancel(med: Medication?) {
        val last = med?.let { DoseRepository.get(this).latestIntake(it.id) } ?: return finishQuietly()
        setContent {
            DoseRingTheme {
                CancelIntakeDialog(
                    intake = last,
                    onConfirm = {
                        IntakeActions.delete(this, last)
                        toast(getString(R.string.toast_removed, last.name))
                        finishQuietly()
                    },
                    onDismiss = ::finishQuietly,
                )
            }
        }
    }

    private fun showEditor(existing: Medication?) {
        val used = DoseRepository.get(this).medications().map { it.color }
        setContent {
            DoseRingTheme {
                MedicationEditorDialog(
                    initial = existing,
                    defaultColor = Palette.next(used),
                    onSave = { med ->
                        IntakeActions.saveMedication(this, med)
                        setContent {}
                        if (shouldAskForNotifications()) {
                            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            finishQuietly()
                        }
                    },
                    onDismiss = ::finishQuietly,
                )
            }
        }
    }

    /** Ask once, right after the first medicine is added from the widget. */
    private fun shouldAskForNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return false
        val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(KEY_ASKED_NOTIFICATIONS, false)) return false
        prefs.edit().putBoolean(KEY_ASKED_NOTIFICATIONS, true).apply()
        return true
    }

    private fun toast(text: String) {
        Toast.makeText(applicationContext, text, Toast.LENGTH_SHORT).show()
    }

    private fun finishQuietly() {
        finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    companion object {
        const val MODE_TAP = "tap"
        const val MODE_CANCEL = "cancel"
        const val MODE_ADD = "add"
        const val MODE_EDIT = "edit"

        private const val EXTRA_MODE = "mode"
        private const val EXTRA_MEDICATION_ID = "medication_id"
        const val PREFS = "dosering"
        const val KEY_ASKED_NOTIFICATIONS = "asked_notifications"

        /** Merged into the widget's pending-intent template. */
        fun fillIn(mode: String, medicationId: Long = -1L): Intent =
            Intent().putExtra(EXTRA_MODE, mode).putExtra(EXTRA_MEDICATION_ID, medicationId)

        fun intent(context: Context, mode: String, medicationId: Long = -1L): Intent =
            Intent(context, ActionActivity::class.java)
                .putExtra(EXTRA_MODE, mode)
                .putExtra(EXTRA_MEDICATION_ID, medicationId)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}

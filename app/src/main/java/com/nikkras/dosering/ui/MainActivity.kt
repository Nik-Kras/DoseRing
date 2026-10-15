package com.nikkras.dosering.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.nikkras.dosering.ui.theme.DoseRingTheme

enum class Tab { Medicines, Journal, Stats }

class MainActivity : ComponentActivity() {

    private var tab by mutableStateOf(Tab.Medicines)
    private var resumeCount by mutableIntStateOf(0)

    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        tab = savedInstanceState?.getString(KEY_TAB)?.let(Tab::valueOf) ?: tabFrom(intent) ?: Tab.Medicines
        askForNotificationsOnce()
        setContent {
            DoseRingTheme {
                DoseRingApp(tab = tab, onTabChange = { tab = it }, resumeCount = resumeCount)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        tabFrom(intent)?.let { tab = it }
    }

    override fun onResume() {
        super.onResume()
        resumeCount++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_TAB, tab.name)
    }

    private fun askForNotificationsOnce() {
        if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return
        val prefs = getSharedPreferences(ActionActivity.PREFS, MODE_PRIVATE)
        if (prefs.getBoolean(ActionActivity.KEY_ASKED_NOTIFICATIONS, false)) return
        prefs.edit().putBoolean(ActionActivity.KEY_ASKED_NOTIFICATIONS, true).apply()
        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    companion object {
        private const val EXTRA_TAB = "tab"
        private const val KEY_TAB = "tab"

        fun intent(context: Context, tab: Tab): Intent =
            Intent(context, MainActivity::class.java)
                .putExtra(EXTRA_TAB, tab.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        private fun tabFrom(intent: Intent?): Tab? =
            intent?.getStringExtra(EXTRA_TAB)?.let { name -> Tab.entries.firstOrNull { it.name == name } }
    }
}

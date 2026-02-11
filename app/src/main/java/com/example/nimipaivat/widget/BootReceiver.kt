package com.example.nimipaivat.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Update all widgets immediately
            CoroutineScope(Dispatchers.Default).launch {
                val manager = GlanceAppWidgetManager(context)
                val widget = NimipaivatWidget()
                manager.getGlanceIds(NimipaivatWidget::class.java).forEach { glanceId ->
                    widget.update(context, glanceId)
                }
            }

            // Reschedule the midnight alarm (alarms don't survive reboot)
            MidnightAlarmScheduler.schedule(context)
        }
    }
}

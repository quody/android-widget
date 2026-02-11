package com.example.nimipaivat.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MidnightUpdateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Update all widget instances
        CoroutineScope(Dispatchers.Default).launch {
            val manager = GlanceAppWidgetManager(context)
            val widget = NimipaivatWidget()
            manager.getGlanceIds(NimipaivatWidget::class.java).forEach { glanceId ->
                widget.update(context, glanceId)
            }
        }

        // Reschedule for next midnight
        MidnightAlarmScheduler.schedule(context)
    }
}

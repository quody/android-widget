package com.example.nimipaivat.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

class NimipaivatWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NimipaivatWidget()

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        MidnightAlarmScheduler.schedule(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        MidnightAlarmScheduler.cancel(context)
    }
}

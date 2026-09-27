package com.example.nimipaivat.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.state.PreferencesGlanceStateDefinition

class NimipaivatWidget : GlanceAppWidget() {

    override val stateDefinition = PreferencesGlanceStateDefinition

    // Exact: LocalSize is the widget's real size (not a responsive bucket), which
    // the wavy-edge background bitmap needs to keep its shape undistorted. The
    // layout picks small/medium/large from it with its own height breakpoints
    // (100dp and 180dp, see WidgetLayout), same as the former responsive buckets.
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetContent(context)
        }
    }
}

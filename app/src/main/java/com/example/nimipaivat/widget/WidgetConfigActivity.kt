package com.example.nimipaivat.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.glance.appwidget.GlanceAppWidgetManager
import com.example.nimipaivat.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        // Get the widget ID from the intent
        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_config)

        val radioGroup = findViewById<RadioGroup>(R.id.language_radio_group)
        val radioFinnish = findViewById<RadioButton>(R.id.radio_finnish)
        val radioSwedish = findViewById<RadioButton>(R.id.radio_swedish)
        val saveButton = findViewById<Button>(R.id.save_button)

        // Load current preference
        CoroutineScope(Dispatchers.Main).launch {
            val isSwedish = WidgetPreferences.isSwedish(this@WidgetConfigActivity)
            if (isSwedish) radioSwedish.isChecked = true else radioFinnish.isChecked = true
        }

        saveButton.setOnClickListener {
            val useSwedish = radioSwedish.isChecked

            CoroutineScope(Dispatchers.Main).launch {
                WidgetPreferences.setSwedish(this@WidgetConfigActivity, useSwedish)

                // Update the widget
                val manager = GlanceAppWidgetManager(this@WidgetConfigActivity)
                val glanceId = manager.getGlanceIdBy(appWidgetId)
                NimipaivatWidget().update(this@WidgetConfigActivity, glanceId)

                // Return success
                val resultValue = Intent().putExtra(
                    AppWidgetManager.EXTRA_APPWIDGET_ID,
                    appWidgetId
                )
                setResult(RESULT_OK, resultValue)
                finish()
            }
        }
    }
}

package com.example.nimipaivat.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.example.nimipaivat.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WidgetConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private val styleRadioMap = linkedMapOf(
        R.id.radio_style_dark to WidgetStyle.DARK,
        R.id.radio_style_material_you to WidgetStyle.MATERIAL_YOU,
        R.id.radio_style_glass_light to WidgetStyle.GLASS_LIGHT,
        R.id.radio_style_glass_dark to WidgetStyle.GLASS_DARK,
        R.id.radio_style_paper to WidgetStyle.PAPER,
    )

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

        val radioFinnish = findViewById<RadioButton>(R.id.radio_finnish)
        val radioSwedish = findViewById<RadioButton>(R.id.radio_swedish)
        val styleRadioGroup = findViewById<RadioGroup>(R.id.style_radio_group)
        val saveButton = findViewById<Button>(R.id.save_button)

        // Load current preferences
        CoroutineScope(Dispatchers.Main).launch {
            val isSwedish = WidgetPreferences.isSwedish(this@WidgetConfigActivity)
            if (isSwedish) radioSwedish.isChecked = true else radioFinnish.isChecked = true

            val currentStyle = WidgetPreferences.getStyle(this@WidgetConfigActivity)
            val radioId = styleRadioMap.entries.find { it.value == currentStyle }?.key
                ?: R.id.radio_style_material_you
            styleRadioGroup.check(radioId)
        }

        saveButton.setOnClickListener {
            val useSwedish = radioSwedish.isChecked
            val selectedStyleId = styleRadioGroup.checkedRadioButtonId
            val selectedStyle = styleRadioMap[selectedStyleId] ?: WidgetStyle.MATERIAL_YOU

            CoroutineScope(Dispatchers.Main).launch {
                WidgetPreferences.setSwedish(this@WidgetConfigActivity, useSwedish)
                WidgetPreferences.setStyle(this@WidgetConfigActivity, selectedStyle)

                // Force recomposition by touching widget state, then update
                val widget = NimipaivatWidget()
                val manager = GlanceAppWidgetManager(this@WidgetConfigActivity)
                val configChangedKey = longPreferencesKey("config_changed_at")
                manager.getGlanceIds(NimipaivatWidget::class.java).forEach { glanceId ->
                    updateAppWidgetState(this@WidgetConfigActivity, glanceId) { prefs ->
                        prefs[configChangedKey] = System.currentTimeMillis()
                    }
                    widget.update(this@WidgetConfigActivity, glanceId)
                }

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

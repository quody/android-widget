package com.example.nimipaivat.widget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Switch
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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
        R.id.radio_style_powder_puff to WidgetStyle.POWDER_PUFF,
        R.id.radio_style_lemondrop to WidgetStyle.LEMONDROP,
        R.id.radio_style_pinkie_promise to WidgetStyle.PINKIE_PROMISE,
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
        applySafeAreaInsets(findViewById(R.id.config_root))

        val radioFinnish = findViewById<RadioButton>(R.id.radio_finnish)
        val radioSwedish = findViewById<RadioButton>(R.id.radio_swedish)
        val styleRadioGroup = findViewById<RadioGroup>(R.id.style_radio_group)
        val saveButton = findViewById<Button>(R.id.save_button)
        val pastelFinishContainer = findViewById<View>(R.id.pastel_finish_container)
        val pastelFinishGroup = findViewById<RadioGroup>(R.id.pastel_finish_group)
        val wavySwitch = findViewById<Switch>(R.id.switch_wavy_edge)

        // Glass / opaque only applies to the pastel styles.
        fun updatePastelFinishVisibility(checkedId: Int) {
            val isPastel = styleRadioMap[checkedId]?.isPastel == true
            pastelFinishContainer.visibility = if (isPastel) View.VISIBLE else View.GONE
        }
        styleRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            updatePastelFinishVisibility(checkedId)
        }
        updatePastelFinishVisibility(styleRadioGroup.checkedRadioButtonId)

        // Load current preferences
        CoroutineScope(Dispatchers.Main).launch {
            val isSwedish = WidgetPreferences.isSwedish(this@WidgetConfigActivity)
            if (isSwedish) radioSwedish.isChecked = true else radioFinnish.isChecked = true

            val currentStyle = WidgetPreferences.getStyle(this@WidgetConfigActivity)
            val radioId = styleRadioMap.entries.find { it.value == currentStyle }?.key
                ?: R.id.radio_style_material_you
            styleRadioGroup.check(radioId)

            val glass = WidgetPreferences.isPastelGlass(this@WidgetConfigActivity)
            pastelFinishGroup.check(if (glass) R.id.radio_finish_glass else R.id.radio_finish_opaque)
            wavySwitch.isChecked = WidgetPreferences.isWavyEdge(this@WidgetConfigActivity)
        }

        saveButton.setOnClickListener {
            val useSwedish = radioSwedish.isChecked
            val selectedStyleId = styleRadioGroup.checkedRadioButtonId
            val selectedStyle = styleRadioMap[selectedStyleId] ?: WidgetStyle.MATERIAL_YOU
            val pastelGlass = pastelFinishGroup.checkedRadioButtonId == R.id.radio_finish_glass
            val wavyEdge = wavySwitch.isChecked

            CoroutineScope(Dispatchers.Main).launch {
                WidgetPreferences.setSwedish(this@WidgetConfigActivity, useSwedish)
                WidgetPreferences.setStyle(this@WidgetConfigActivity, selectedStyle)
                WidgetPreferences.setPastelGlass(this@WidgetConfigActivity, pastelGlass)
                WidgetPreferences.setWavyEdge(this@WidgetConfigActivity, wavyEdge)

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

    /**
     * Keeps the settings content out from under the status bar, camera cutout and
     * navigation bar. Android 15+ enforces edge-to-edge for apps targeting SDK 35,
     * so the window no longer insets content for us. On older versions the decor
     * view already consumes these insets and the values here are simply zero.
     */
    private fun applySafeAreaInsets(root: View) {
        val basePaddingLeft = root.paddingLeft
        val basePaddingTop = root.paddingTop
        val basePaddingRight = root.paddingRight
        val basePaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                basePaddingLeft + insets.left,
                basePaddingTop + insets.top,
                basePaddingRight + insets.right,
                basePaddingBottom + insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }
}

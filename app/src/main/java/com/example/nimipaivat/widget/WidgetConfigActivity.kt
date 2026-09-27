package com.example.nimipaivat.widget

import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.annotation.StringRes
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.core.widget.NestedScrollView
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.state.getAppWidgetState
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.lifecycle.lifecycleScope
import com.example.nimipaivat.R
import com.example.nimipaivat.data.NameDayRepository
import com.example.nimipaivat.util.DateUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.DynamicColors
import com.google.android.material.color.MaterialColors
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Widget settings: calendar, style (+ pastel finish) and wavy edge, with a live
 * preview of the real widget. Settings are global (shared by all widgets).
 *
 * Shown when a widget is placed and, via `widgetFeatures="reconfigurable"`,
 * from the widget's long-press menu on Android 12+. Backing out leaves the
 * result RESULT_CANCELED (placing is cancelled, reconfiguring changes nothing).
 */
class WidgetConfigActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID

    private var selection = WidgetSettings()
    private var previewNames: PreviewNames? = null
    private var darkBackdrop = false
    private var loaded = false
    /** True while the UI is being set from [selection], so listeners don't echo back. */
    private var binding = false

    private lateinit var calendarGroup: MaterialButtonToggleGroup
    private lateinit var finishGroup: MaterialButtonToggleGroup
    private lateinit var backdropGroup: MaterialButtonToggleGroup
    private lateinit var finishContainer: View
    private lateinit var wavySwitch: MaterialSwitch
    private lateinit var saveButton: MaterialButton
    private lateinit var settingsContent: ViewGroup
    private lateinit var previewBackdrop: View
    private lateinit var previewFrame: ScaledPreviewFrame
    private lateinit var previewHost: AppWidgetHostView
    private val styleCards = linkedMapOf<WidgetStyle, StyleCard>()
    private var previewJob: Job? = null

    /** Incremented each time a composed preview is shown (lets tests wait for it). */
    @VisibleForTesting
    internal var previewRenderCount = 0
        private set

    private class StyleCard(
        val card: MaterialCardView,
        val swatch: View,
        val swatchWidget: View,
        val swatchText: TextView
    )

    private data class StyleOption(
        val style: WidgetStyle,
        @StringRes val label: Int,
        @StringRes val hint: Int? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (useDynamicColors) DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)

        // Set the result to CANCELED in case the user backs out
        setResult(RESULT_CANCELED)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        setContentView(R.layout.activity_widget_config)
        applySafeAreaInsets()

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        calendarGroup = findViewById(R.id.calendar_group)
        finishGroup = findViewById(R.id.pastel_finish_group)
        backdropGroup = findViewById(R.id.backdrop_group)
        finishContainer = findViewById(R.id.pastel_finish_container)
        wavySwitch = findViewById(R.id.switch_wavy_edge)
        saveButton = findViewById(R.id.save_button)
        settingsContent = findViewById(R.id.settings_content)
        previewBackdrop = findViewById(R.id.preview_backdrop)
        previewFrame = findViewById(R.id.preview_frame)

        setupPreview()
        setupStyleCards()
        listOf(calendarGroup, finishGroup, backdropGroup).forEach(::showCheckOnSelectedSegment)

        calendarGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked && !binding) update(selection.copy(swedish = id == R.id.button_swedish))
        }
        finishGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked && !binding) update(selection.copy(pastelGlass = id == R.id.button_finish_glass))
        }
        backdropGroup.addOnButtonCheckedListener { _, id, checked ->
            if (checked && !binding) {
                darkBackdrop = id == R.id.backdrop_dark
                bindBackdrop()
            }
        }
        findViewById<View>(R.id.wavy_row).apply {
            setOnClickListener { update(selection.copy(wavyEdge = !selection.wavyEdge)) }
            ViewCompat.setAccessibilityDelegate(this, object : AccessibilityDelegateCompat() {
                override fun onInitializeAccessibilityNodeInfo(
                    host: View,
                    info: AccessibilityNodeInfoCompat
                ) {
                    super.onInitializeAccessibilityNodeInfo(host, info)
                    info.className = android.widget.Switch::class.java.name
                    info.isCheckable = true
                    info.isChecked = selection.wavyEdge
                }
            })
        }
        saveButton.isEnabled = false
        saveButton.setOnClickListener { save() }

        // Judge glass styles on a wallpaper like the user's: start from the system theme.
        darkBackdrop = isNightMode()
        bind(animate = false)

        lifecycleScope.launch {
            val context = this@WidgetConfigActivity
            selection = WidgetSettings(
                swedish = WidgetPreferences.isSwedish(context),
                style = WidgetPreferences.getStyle(context),
                pastelGlass = WidgetPreferences.isPastelGlass(context),
                wavyEdge = WidgetPreferences.isWavyEdge(context)
            )
            previewNames = withContext(Dispatchers.IO) { loadPreviewNames() }
            val reconfiguring = isAlreadyConfigured()
            saveButton.setText(if (reconfiguring) R.string.save_button else R.string.add_widget_button)
            saveButton.isEnabled = true
            loaded = true
            bind(animate = false)
        }
    }

    private fun update(newSelection: WidgetSettings) {
        val pastelShown = selection.style.isPastel
        selection = newSelection
        bind(animate = true)
        if (!pastelShown && newSelection.style.isPastel) {
            // Bring the just-revealed finish choice into view.
            finishContainer.post {
                finishContainer.requestRectangleOnScreen(
                    Rect(0, 0, finishContainer.width, finishContainer.height), false
                )
            }
        }
    }

    /** Pushes [selection] into every control and re-renders the preview. */
    private fun bind(animate: Boolean) {
        binding = true
        calendarGroup.check(if (selection.swedish) R.id.button_swedish else R.id.button_finnish)
        finishGroup.check(if (selection.pastelGlass) R.id.button_finish_glass else R.id.button_finish_opaque)
        backdropGroup.check(if (darkBackdrop) R.id.backdrop_dark else R.id.backdrop_light)
        wavySwitch.isChecked = selection.wavyEdge

        val finishVisibility = if (selection.style.isPastel) View.VISIBLE else View.GONE
        if (finishContainer.visibility != finishVisibility) {
            if (animate) TransitionManager.beginDelayedTransition(settingsContent, AutoTransition())
            finishContainer.visibility = finishVisibility
        }
        binding = false

        bindBackdrop()
        renderPreview()
    }

    /** The wallpaper behind the preview and the style swatches. */
    private fun bindBackdrop() {
        val density = resources.displayMetrics.density
        previewBackdrop.background = PreviewBackdrop.drawable(darkBackdrop, 24 * density)
        bindStyleCards()
    }

    // --- Style cards -------------------------------------------------------

    private fun setupStyleCards() {
        val groups = listOf(
            R.id.style_row_basic to listOf(
                StyleOption(WidgetStyle.DARK, R.string.style_dark),
                StyleOption(WidgetStyle.PAPER, R.string.style_paper),
                StyleOption(WidgetStyle.MATERIAL_YOU, R.string.style_material_you),
            ),
            R.id.style_row_glass to listOf(
                StyleOption(WidgetStyle.GLASS_LIGHT, R.string.style_glass_light, R.string.style_glass_light_hint),
                StyleOption(WidgetStyle.GLASS_DARK, R.string.style_glass_dark, R.string.style_glass_dark_hint),
            ),
            R.id.style_row_pastel to listOf(
                StyleOption(WidgetStyle.POWDER_PUFF, R.string.style_powder_puff),
                StyleOption(WidgetStyle.LEMONDROP, R.string.style_lemondrop),
                StyleOption(WidgetStyle.PINKIE_PROMISE, R.string.style_pinkie_promise),
            ),
        )
        val gap = (8 * resources.displayMetrics.density).toInt()
        val inflater = LayoutInflater.from(this)
        for ((rowId, options) in groups) {
            val row = findViewById<LinearLayout>(rowId)
            // Three equal columns in every group, so cards line up across groups.
            for (index in 0 until COLUMNS) {
                val option = options.getOrNull(index)
                val view = if (option == null) {
                    View(this)
                } else {
                    inflater.inflate(R.layout.item_style_option, row, false)
                }
                row.addView(view, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f).apply {
                    if (index > 0) marginStart = gap
                })
                if (option != null) setupStyleCard(view as MaterialCardView, option)
            }
        }
    }

    private fun setupStyleCard(card: MaterialCardView, option: StyleOption) {
        val label = getString(option.label)
        val hint = option.hint?.let(::getString)
        card.tag = option.style
        card.findViewById<TextView>(R.id.label).text = label
        card.findViewById<TextView>(R.id.hint).apply {
            text = hint
            visibility = if (hint != null) View.VISIBLE else View.GONE
        }
        card.contentDescription = getString(
            R.string.style_card_description,
            listOfNotNull(label, hint).joinToString(", ")
        )
        // Announced as one option of a single-choice group ("radio button, checked").
        ViewCompat.setAccessibilityDelegate(card, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = RadioButton::class.java.name
                info.isCheckable = true
                info.isChecked = card.isChecked
            }
        })
        card.setOnClickListener {
            if (selection.style != option.style) update(selection.copy(style = option.style))
        }
        styleCards[option.style] = StyleCard(
            card = card,
            swatch = card.findViewById(R.id.swatch),
            swatchWidget = card.findViewById(R.id.swatch_widget),
            swatchText = card.findViewById(R.id.swatch_text)
        )
    }

    private fun bindStyleCards() {
        val density = resources.displayMetrics.density
        val primary = MaterialColors.getColor(previewBackdrop, com.google.android.material.R.attr.colorPrimary)
        val outline = MaterialColors.getColor(previewBackdrop, com.google.android.material.R.attr.colorOutlineVariant)
        for ((style, holder) in styleCards) {
            val checked = style == selection.style
            holder.card.isChecked = checked
            holder.card.strokeWidth = ((if (checked) 2 else 1) * density).toInt()
            holder.card.setStrokeColor(ColorStateList.valueOf(if (checked) primary else outline))
            holder.swatch.background = PreviewBackdrop.drawable(darkBackdrop, 10 * density)
            // Pastel swatches follow the chosen finish, so the choice is visible here too.
            val colors = resolveStyle(style, selection.pastelGlass)
            holder.swatchWidget.background = swatchBackground(colors, density)
            holder.swatchText.setTextColor(
                colors.primaryTextColor?.toArgb()
                    ?: ContextCompat.getColor(this, androidx.glance.R.color.glance_colorOnSurface)
            )
        }
    }

    private fun swatchBackground(colors: WidgetStyleColors, density: Float): Drawable? {
        colors.backgroundDrawableRes?.let { return ContextCompat.getDrawable(this, it) }
        val fill = colors.backgroundColor?.toArgb()
            ?: ContextCompat.getColor(this, androidx.glance.R.color.glance_colorWidgetBackground)
        return GradientDrawable().apply {
            setColor(fill)
            cornerRadius = 12 * density
        }
    }

    /** Material 3 segmented buttons show a check mark on the selected segment only. */
    private fun showCheckOnSelectedSegment(group: MaterialButtonToggleGroup) {
        fun refresh() {
            for (i in 0 until group.childCount) {
                val button = group.getChildAt(i) as MaterialButton
                button.icon = if (button.isChecked) {
                    ContextCompat.getDrawable(this, R.drawable.ic_check)
                } else {
                    null
                }
            }
        }
        group.addOnButtonCheckedListener { _, _, _ -> refresh() }
        refresh()
    }

    // --- Live preview ------------------------------------------------------

    private fun setupPreview() {
        val density = resources.displayMetrics.density
        // RemoteViews bind like in a launcher when their parent is an AppWidgetHostView.
        previewHost = AppWidgetHostView(this)
        previewFrame.setChildSize(
            (PREVIEW_SIZE.width.value * density).toInt(),
            (PREVIEW_SIZE.height.value * density).toInt()
        )
        previewFrame.addView(previewHost)

        // On short (e.g. landscape) screens the pinned preview would leave too
        // little room for the settings: scroll it with them instead.
        if (resources.configuration.screenHeightDp < PINNED_PREVIEW_MIN_SCREEN_DP) {
            val section = findViewById<ViewGroup>(R.id.preview_section)
            (section.parent as ViewGroup).removeView(section)
            section.setPadding(0, 0, 0, (24 * density).toInt())
            settingsContent.addView(section, 0)
        }

        val scroll = findViewById<NestedScrollView>(R.id.settings_scroll)
        val previewDivider = findViewById<View>(R.id.preview_divider)
        val bottomDivider = findViewById<View>(R.id.bottom_divider)
        fun updateDividers() {
            previewDivider.visibility =
                if (scroll.canScrollVertically(-1)) View.VISIBLE else View.INVISIBLE
            bottomDivider.visibility =
                if (scroll.canScrollVertically(1)) View.VISIBLE else View.INVISIBLE
        }
        scroll.setOnScrollChangeListener { _: NestedScrollView, _: Int, _: Int, _: Int, _: Int ->
            updateDividers()
        }
        scroll.viewTreeObserver.addOnGlobalLayoutListener { updateDividers() }
    }

    @OptIn(ExperimentalGlanceApi::class)
    private fun renderPreview() {
        val names = previewNames ?: return
        val settings = selection
        previewJob?.cancel()
        previewJob = lifecycleScope.launch {
            val appContext = applicationContext
            val remoteViews = withContext(Dispatchers.Default) {
                SettingsPreviewWidget(settings, names).compose(
                    context = appContext,
                    options = Bundle.EMPTY,
                    size = PREVIEW_SIZE
                )
            }
            val view = remoteViews.apply(appContext, previewHost)
            previewHost.removeAllViews()
            previewHost.addView(
                view,
                FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            previewRenderCount++
        }
    }

    private fun loadPreviewNames(): PreviewNames {
        val repository = NameDayRepository(this)
        val today = repository.getNameDay(DateUtils.todayKey())
        val tomorrow = repository.getNameDay(DateUtils.tomorrowKey())
        return PreviewNames(
            dateText = DateUtils.formatDateFinnish(),
            todayFi = today.fi,
            todaySv = today.sv,
            tomorrowFi = tomorrow.fi,
            tomorrowSv = tomorrow.sv
        )
    }

    // --- Saving ------------------------------------------------------------

    /**
     * Whether this widget was set up before (reopened from its long-press menu)
     * rather than being placed now: every save stamps [CONFIG_CHANGED_AT_KEY]
     * into the state of the widgets that exist at that point.
     */
    private suspend fun isAlreadyConfigured(): Boolean = try {
        val glanceId = GlanceAppWidgetManager(this).getGlanceIdBy(appWidgetId)
        getAppWidgetState(this, PreferencesGlanceStateDefinition, glanceId)[CONFIG_CHANGED_AT_KEY] != null
    } catch (_: Exception) {
        false
    }

    private fun save() {
        if (!loaded) return
        saveButton.isEnabled = false
        val settings = selection
        lifecycleScope.launch {
            val context = this@WidgetConfigActivity
            WidgetPreferences.setSwedish(context, settings.swedish)
            WidgetPreferences.setStyle(context, settings.style)
            WidgetPreferences.setPastelGlass(context, settings.pastelGlass)
            WidgetPreferences.setWavyEdge(context, settings.wavyEdge)

            // Force recomposition by touching widget state, then update
            val widget = NimipaivatWidget()
            val manager = GlanceAppWidgetManager(context)
            val glanceIds = manager.getGlanceIds(NimipaivatWidget::class.java).toMutableSet()
            runCatching { manager.getGlanceIdBy(appWidgetId) }.getOrNull()?.let(glanceIds::add)
            glanceIds.forEach { glanceId ->
                updateAppWidgetState(context, glanceId) { prefs ->
                    prefs[CONFIG_CHANGED_AT_KEY] = System.currentTimeMillis()
                }
                widget.update(context, glanceId)
            }

            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            setResult(RESULT_OK, resultValue)
            finish()
        }
    }

    // --- Window ------------------------------------------------------------

    private fun isNightMode() =
        (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
            Configuration.UI_MODE_NIGHT_YES

    /**
     * Keeps the content out from under the status bar, camera cutout and
     * navigation bar (edge-to-edge; enforced on Android 15+ for SDK 35). The app
     * bar takes the top inset and the bottom bar the bottom one, so their
     * surface colour extends behind the system bars.
     */
    private fun applySafeAreaInsets() {
        val root = findViewById<View>(R.id.config_root)
        val toolbar = findViewById<View>(R.id.toolbar)
        val bottomBar = findViewById<View>(R.id.bottom_bar)
        val toolbarTop = toolbar.paddingTop
        val bottomBarBottom = bottomBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, windowInsets ->
            val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(insets.left, 0, insets.right, 0)
            toolbar.setPadding(toolbar.paddingLeft, toolbarTop + insets.top, toolbar.paddingRight, toolbar.paddingBottom)
            bottomBar.setPadding(
                bottomBar.paddingLeft, bottomBar.paddingTop, bottomBar.paddingRight,
                bottomBarBottom + insets.bottom
            )
            WindowInsetsCompat.CONSUMED
        }
    }

    internal companion object {
        /**
         * Wallpaper-based colours on Android 12+; the screenshot test turns them
         * off to render the app's own palette deterministically.
         */
        @VisibleForTesting
        var useDynamicColors = true
        /** A typical medium widget (the size the screenshot test calls "medium"). */
        val PREVIEW_SIZE = DpSize(250.dp, 120.dp)
        private const val COLUMNS = 3
        private const val PINNED_PREVIEW_MIN_SCREEN_DP = 600
        val CONFIG_CHANGED_AT_KEY = longPreferencesKey("config_changed_at")
    }
}

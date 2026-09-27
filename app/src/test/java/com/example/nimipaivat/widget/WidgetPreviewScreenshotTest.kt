package com.example.nimipaivat.widget

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.RadioGroup
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import com.example.nimipaivat.R
import com.example.nimipaivat.data.EtymologyRepository
import com.example.nimipaivat.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId

/**
 * Renders the real Glance widget (every [WidgetStyle], the pastel glass
 * variants and the wavy edge; names + etymology screen) to PNG files, so the
 * styles can be reviewed without an emulator. Also renders the config screen.
 *
 * The widget is composed to RemoteViews exactly like the launcher host would
 * receive it, inflated with RemoteViews.apply() and captured with the
 * Robolectric native (Skia) HardwareRenderer.
 *
 * Images are only written when a directory is given:
 *   ./gradlew :app:testDebugUnitTest --tests '*WidgetPreviewScreenshotTest' \
 *       -PwidgetPreviewDir=/abs/path/to/previews
 * Without it the test still renders everything (a smoke test for all styles).
 *
 * File names: `<style>[_glass][_wavy][_darkwp]_<size>_<screen>.png`.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class WidgetPreviewScreenshotTest {

    private val outDir: File? = System.getProperty("widgetPreviewDir")
        ?.takeIf { it.isNotBlank() }
        ?.let { File(it).apply { mkdirs() } }

    @After
    fun resetClock() {
        DateUtils.clock = Clock.systemDefaultZone()
    }

    private data class Variant(
        val style: WidgetStyle,
        val glass: Boolean = false,
        val wavy: Boolean = false,
        val darkWallpaper: Boolean = false
    ) {
        val key = buildString {
            append(style.name.lowercase())
            if (glass) append("_glass")
            if (wavy) append("_wavy")
            if (darkWallpaper) append("_darkwp")
        }
    }

    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun renderAllStyles() {
        // Juhannus 2026: "Johannes, Juhani" (both have etymologies), tomorrow "Jorma, Jarmo, ..."
        val zone = ZoneId.of("Europe/Helsinki")
        DateUtils.clock = Clock.fixed(
            LocalDate.of(2026, 6, 24).atTime(12, 0).atZone(zone).toInstant(), zone
        )
        val context = RuntimeEnvironment.getApplication()

        val medium = DpSize(250.dp, 120.dp)
        val small = DpSize(180.dp, 70.dp)
        val large = DpSize(250.dp, 190.dp)
        val shots = listOf(
            Shot("medium", medium, flipped = false),
            Shot("medium", medium, flipped = true),
            // The flip state survives resizing, so the etymology screen can show at small too.
            Shot("small", small, flipped = false),
            Shot("small", small, flipped = true),
            Shot("large", large, flipped = false),
            Shot("large", large, flipped = true),
        )
        // Real days can have up to ~9 names; the etymology list must scroll, not clip.
        val manyNameShots = listOf(
            Shot("medium", medium, flipped = true),
            Shot("small", small, flipped = true),
            Shot("large", large, flipped = true),
        )

        val pastels = WidgetStyle.entries.filter { it.isPastel }
        val base = WidgetStyle.entries.map { Variant(it) } + pastels.map { Variant(it, glass = true) }
        val fullVariants = base + base.map { it.copy(wavy = true) }
        // Glass styles on a dark wallpaper, to check text contrast.
        val darkVariants = (listOf(WidgetStyle.GLASS_LIGHT, WidgetStyle.GLASS_DARK).map { Variant(it) } +
            pastels.map { Variant(it, glass = true) } +
            pastels.map { Variant(it, glass = true, wavy = true) })
            .map { it.copy(darkWallpaper = true) }

        var rendered = 0
        fun render(variant: Variant, shot: Shot, manyNames: Boolean) {
            runBlocking {
                WidgetPreferences.setStyle(context, variant.style)
                WidgetPreferences.setPastelGlass(context, variant.glass)
                WidgetPreferences.setWavyEdge(context, variant.wavy)
            }
            val widget = if (manyNames) {
                ManyNamesWidget(resolveStyle(variant.style, variant.glass), variant.wavy)
            } else {
                NimipaivatWidget()
            }
            val remoteViews = runBlocking {
                widget.compose(
                    context = context,
                    options = Bundle.EMPTY,
                    size = shot.size,
                    state = preferencesOf(FlipAction.FLIPPED_KEY to shot.flipped),
                )
            }
            val bitmap = capture(shot.size, variant.darkWallpaper) { parent ->
                remoteViews.apply(context, parent)
            }
            val screen = when {
                manyNames -> "etymology9"
                shot.flipped -> "etymology"
                else -> "names"
            }
            outDir?.let { writePng(bitmap, File(it, "${variant.key}_${shot.label}_$screen.png")) }
            rendered++
        }

        for (variant in fullVariants) {
            shots.forEach { render(variant, it, manyNames = false) }
            manyNameShots.forEach { render(variant, it, manyNames = true) }
        }
        for (variant in darkVariants) {
            shots.take(2).forEach { render(variant, it, manyNames = false) }
        }
        runBlocking {
            WidgetPreferences.setPastelGlass(context, false)
            WidgetPreferences.setWavyEdge(context, false)
        }
        assertEquals(
            fullVariants.size * (shots.size + manyNameShots.size) + darkVariants.size * 2,
            rendered
        )
    }

    /**
     * Today's names are fitted to the space left for them: real days with 1, 2,
     * 9, 10 and 13 names at several sizes and styles, plus a 1.3x font scale.
     * Files: `autofit_<variant>[_fs130]_<size>_<case>.png`.
     */
    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun renderAutofitNames() {
        val zone = ZoneId.of("Europe/Helsinki")
        DateUtils.clock = Clock.fixed(
            LocalDate.of(2026, 6, 24).atTime(12, 0).atZone(zone).toInstant(), zone
        )
        val context = RuntimeEnvironment.getApplication()
        val sizes = listOf(
            "small" to DpSize(180.dp, 70.dp),
            "medium" to DpSize(250.dp, 120.dp),
            "large" to DpSize(250.dp, 190.dp),
            "wide" to DpSize(320.dp, 120.dp),
        )
        val variants = listOf(
            Variant(WidgetStyle.DARK),
            Variant(WidgetStyle.PAPER),
            Variant(WidgetStyle.LEMONDROP),
            Variant(WidgetStyle.PINKIE_PROMISE, glass = true, wavy = true),
        )
        var rendered = 0
        fun render(variant: Variant, fontScaleKey: String) {
            for ((label, size) in sizes) for ((case, names) in AUTOFIT_CASES) {
                val widget = NamesWidget(
                    resolveStyle(variant.style, variant.glass), variant.wavy, names
                )
                val remoteViews = runBlocking {
                    widget.compose(context = context, options = Bundle.EMPTY, size = size)
                }
                val bitmap = capture(size, darkWallpaper = false) { parent ->
                    remoteViews.apply(context, parent)
                }
                outDir?.let {
                    writePng(bitmap, File(it, "autofit_${variant.key}${fontScaleKey}_${label}_$case.png"))
                }
                rendered++
            }
        }
        variants.forEach { render(it, "") }
        RuntimeEnvironment.setFontScale(1.3f)
        try {
            listOf(variants[1], variants[3]).forEach { render(it, "_fs130") }
        } finally {
            RuntimeEnvironment.setFontScale(1f)
        }
        assertEquals(6 * sizes.size * AUTOFIT_CASES.size, rendered)
    }

    /** The config screen with a pastel style selected shows the glass/opaque toggle. */
    @Test
    @Config(qualifiers = "w411dp-h840dp-xxhdpi")
    fun renderConfigScreen() {
        val context = RuntimeEnvironment.getApplication()
        runBlocking {
            WidgetPreferences.setStyle(context, WidgetStyle.LEMONDROP)
            WidgetPreferences.setPastelGlass(context, true)
            WidgetPreferences.setWavyEdge(context, true)
        }
        val intent = Intent(context, WidgetConfigActivity::class.java)
            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, 42)
        val controller = Robolectric.buildActivity(WidgetConfigActivity::class.java, intent)
        val activity = controller.setup().get()
        // The activity loads its preferences in a coroutine that hops to DataStore's
        // IO thread; keep draining the main looper until the switch reflects them.
        val wavySwitch = activity.findViewById<android.widget.CompoundButton>(R.id.switch_wavy_edge)
        repeat(50) {
            shadowOf(Looper.getMainLooper()).idle()
            if (wavySwitch.isChecked) return@repeat
            Thread.sleep(20)
        }

        val finish = activity.findViewById<View>(R.id.pastel_finish_container)
        assertEquals(View.VISIBLE, finish.visibility)
        assertEquals(
            R.id.radio_finish_glass,
            activity.findViewById<RadioGroup>(R.id.pastel_finish_group).checkedRadioButtonId
        )
        assertTrue(activity.findViewById<android.widget.CompoundButton>(R.id.switch_wavy_edge).isChecked)

        outDir?.let { writePng(captureWindow(activity), File(it, "config_screen.png")) }

        // Non-pastel style: the toggle hides.
        activity.findViewById<RadioGroup>(R.id.style_radio_group).check(R.id.radio_style_dark)
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(View.GONE, finish.visibility)
        controller.pause().stop().destroy()
        runBlocking {
            WidgetPreferences.setPastelGlass(context, false)
            WidgetPreferences.setWavyEdge(context, false)
        }
    }

    private data class Shot(val label: String, val size: DpSize, val flipped: Boolean)

    /** The flipped widget for a synthetic 9-name day, with real etymologies. */
    private class ManyNamesWidget(
        private val styleColors: WidgetStyleColors,
        private val wavy: Boolean
    ) : GlanceAppWidget() {
        override val sizeMode = SizeMode.Exact

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            val etymologies = EtymologyRepository(context)
            provideContent {
                WidgetLayout(
                    isFlipped = true,
                    dateText = "",
                    todayNames = MANY_NAMES,
                    tomorrowNames = emptyList(),
                    styleColors = styleColors,
                    etymologyOf = etymologies::getEtymology,
                    wavyEdge = wavy
                )
            }
        }
    }

    /** The names screen for the given names (tomorrow is a fixed 6-name day). */
    private class NamesWidget(
        private val styleColors: WidgetStyleColors,
        private val wavy: Boolean,
        private val names: List<String>
    ) : GlanceAppWidget() {
        override val sizeMode = SizeMode.Exact

        override suspend fun provideGlance(context: Context, id: GlanceId) {
            provideContent {
                WidgetLayout(
                    isFlipped = false,
                    dateText = DateUtils.formatDateFinnish(),
                    todayNames = names,
                    tomorrowNames = listOf("Jorma", "Jarmo", "Jarkko", "Jarno", "Jere", "Jeremias"),
                    styleColors = styleColors,
                    etymologyOf = { null },
                    wavyEdge = wavy
                )
            }
        }
    }

    private companion object {
        /** Real days: 25.6., 4.1., 24.6. (9), 2.7. (10, "Kukka-Maaria") and 15.8. (13). */
        val AUTOFIT_CASES = listOf(
            "n01" to listOf("Uuno"),
            "n02" to listOf("Tiitus", "Ruut"),
            "n09" to listOf("Jani", "Janne", "Johannes", "Juha", "Juhana", "Juhani", "Juho", "Jukka", "Jussi"),
            "n10" to listOf(
                "Maria", "Maija", "Mari", "Meeri", "Marika", "Maiju", "Riia", "Maaria", "Maikki",
                "Kukka-Maaria"
            ),
            "n13" to listOf(
                "Marja", "Jaana", "Marjo", "Marita", "Marjatta", "Marjut", "Marianne",
                "Maritta", "Marjaana", "Marianna", "Marjukka", "Jatta", "Marju"
            ),
        )

        val MANY_NAMES = listOf(
            "Johannes", "Juhani", "Toni", "Anton", "Anttoni",
            "Heikki", "Henri", "Henrik", "Aune"
        )
        val LIGHT_WALLPAPER = intArrayOf(0xFF8FA7B8.toInt(), 0xFFB9B3C9.toInt(), 0xFFD8C3B4.toInt())
        val DARK_WALLPAPER = intArrayOf(0xFF1B2433.toInt(), 0xFF2D2440.toInt(), 0xFF3B2A26.toInt())
    }

    /** Puts the inflated widget on a soft "wallpaper" and captures it via PixelCopy. */
    private fun capture(size: DpSize, darkWallpaper: Boolean, inflate: (ViewGroup) -> View): Bitmap {
        val activityController = Robolectric.buildActivity(Activity::class.java)
        val activity = activityController.get()
        activity.setTheme(android.R.style.Theme_Material_Light_NoActionBar)
        activityController.create()
        activity.window.requestFeature(Window.FEATURE_NO_TITLE)
        val density = activity.resources.displayMetrics.density
        fun px(dp: Float) = (dp * density).toInt()

        val margin = px(20f)
        val widthPx = px(size.width.value)
        val heightPx = px(size.height.value)

        val wallpaper = FrameLayout(activity).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                if (darkWallpaper) DARK_WALLPAPER else LIGHT_WALLPAPER
            )
            setPadding(margin, margin, margin, margin)
        }
        // RemoteViews only binds collection adapters (Glance LazyColumn) when the
        // parent is an AppWidgetHostView, like in a real launcher.
        val host = AppWidgetHostView(activity)
        wallpaper.addView(host, FrameLayout.LayoutParams(widthPx, heightPx))
        host.addView(
            inflate(host),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        val root = FrameLayout(activity)
        root.addView(
            wallpaper,
            FrameLayout.LayoutParams(
                widthPx + 2 * margin,
                heightPx + 2 * margin
            )
        )
        activity.setContentView(root)
        activityController.start().resume().visible()
        shadowOf(Looper.getMainLooper()).idle()

        val loc = IntArray(2)
        wallpaper.getLocationInWindow(loc)
        val rect = Rect(loc[0], loc[1], loc[0] + wallpaper.width, loc[1] + wallpaper.height)
        val bitmap = pixelCopy(activity, rect)
        activityController.pause().stop().destroy()
        return bitmap
    }

    private fun captureWindow(activity: Activity): Bitmap {
        val decor = activity.window.decorView
        // Skip the radio button / switch check animations Robolectric would
        // otherwise capture half-way (checked state drawn as unchecked).
        decor.jumpDrawablesToCurrentState()
        shadowOf(Looper.getMainLooper()).idle()
        return pixelCopy(activity, Rect(0, 0, decor.width, decor.height))
    }

    private fun pixelCopy(activity: Activity, rect: Rect): Bitmap {
        val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        var result = -1
        PixelCopy.request(activity.window, rect, bitmap, { result = it }, Handler(Looper.getMainLooper()))
        shadowOf(Looper.getMainLooper()).idle()
        check(result == PixelCopy.SUCCESS) { "PixelCopy failed: $result" }
        return bitmap
    }

    private fun writePng(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

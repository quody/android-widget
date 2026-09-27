package com.example.nimipaivat.widget

import android.app.Activity
import android.appwidget.AppWidgetHostView
import android.content.Context
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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.preferencesOf
import androidx.glance.ExperimentalGlanceApi
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.compose
import androidx.glance.appwidget.provideContent
import com.example.nimipaivat.data.EtymologyRepository
import com.example.nimipaivat.util.DateUtils
import kotlinx.coroutines.runBlocking
import org.junit.After
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
 * Renders the real Glance widget (every [WidgetStyle], names + etymology screen)
 * to PNG files, so the styles can be reviewed without an emulator.
 *
 * The widget is composed to RemoteViews exactly like the launcher host would
 * receive it, inflated with RemoteViews.apply() and captured with the
 * Robolectric native (Skia) HardwareRenderer.
 *
 * Images are only written when a directory is given:
 *   ./gradlew :app:testDebugUnitTest --tests '*WidgetPreviewScreenshotTest' \
 *       -PwidgetPreviewDir=/abs/path/to/previews
 * Without it the test still renders everything (a smoke test for all styles).
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

    @OptIn(ExperimentalGlanceApi::class)
    @Test
    fun renderAllStyles() {
        // Juhannus 2026: "Johannes, Juhani" (both have etymologies), tomorrow "Jorma, Jarmo, ..."
        val zone = ZoneId.of("Europe/Helsinki")
        DateUtils.clock = Clock.fixed(
            LocalDate.of(2026, 6, 24).atTime(12, 0).atZone(zone).toInstant(), zone
        )
        val context = RuntimeEnvironment.getApplication()

        val shots = listOf(
            Shot("medium", DpSize(250.dp, 120.dp), flipped = false),
            Shot("medium", DpSize(250.dp, 120.dp), flipped = true),
            // The flip state survives resizing, so the etymology screen can show at small too.
            Shot("small", DpSize(180.dp, 70.dp), flipped = false),
            Shot("small", DpSize(180.dp, 70.dp), flipped = true),
            Shot("large", DpSize(250.dp, 190.dp), flipped = false),
            Shot("large", DpSize(250.dp, 190.dp), flipped = true),
        )
        // Real days can have up to ~9 names; the etymology list must scroll, not clip.
        val manyNameShots = listOf(
            Shot("medium", DpSize(250.dp, 120.dp), flipped = true),
            Shot("small", DpSize(180.dp, 70.dp), flipped = true),
            Shot("large", DpSize(250.dp, 190.dp), flipped = true),
        )

        var rendered = 0
        for (style in WidgetStyle.entries) {
            runBlocking { WidgetPreferences.setStyle(context, style) }
            for (shot in shots) {
                val remoteViews = runBlocking {
                    NimipaivatWidget().compose(
                        context = context,
                        options = Bundle.EMPTY,
                        size = shot.size,
                        state = preferencesOf(FlipAction.FLIPPED_KEY to shot.flipped),
                    )
                }
                val bitmap = capture(shot.size) { parent -> remoteViews.apply(context, parent) }
                val screen = if (shot.flipped) "etymology" else "names"
                val name = "${style.name.lowercase()}_${shot.label}_$screen.png"
                outDir?.let { writePng(bitmap, File(it, name)) }
                rendered++
            }
            for (shot in manyNameShots) {
                val remoteViews = runBlocking {
                    ManyNamesWidget(resolveStyle(style)).compose(
                        context = context,
                        options = Bundle.EMPTY,
                        size = shot.size,
                    )
                }
                val bitmap = capture(shot.size) { parent -> remoteViews.apply(context, parent) }
                val name = "${style.name.lowercase()}_${shot.label}_etymology9.png"
                outDir?.let { writePng(bitmap, File(it, name)) }
                rendered++
            }
        }
        assertTrue(rendered == WidgetStyle.entries.size * (shots.size + manyNameShots.size))
    }

    private data class Shot(val label: String, val size: DpSize, val flipped: Boolean)

    /** The flipped widget for a synthetic 9-name day, with real etymologies. */
    private class ManyNamesWidget(private val styleColors: WidgetStyleColors) : GlanceAppWidget() {
        override suspend fun provideGlance(context: Context, id: GlanceId) {
            val etymologies = EtymologyRepository(context)
            provideContent {
                WidgetLayout(
                    isFlipped = true,
                    dateText = "",
                    todayNames = MANY_NAMES,
                    tomorrowNames = emptyList(),
                    styleColors = styleColors,
                    etymologyOf = etymologies::getEtymology
                )
            }
        }
    }

    private companion object {
        val MANY_NAMES = listOf(
            "Johannes", "Juhani", "Toni", "Anton", "Anttoni",
            "Heikki", "Henri", "Henrik", "Aune"
        )
    }

    /** Puts the inflated widget on a soft "wallpaper" and captures it via PixelCopy. */
    private fun capture(size: DpSize, inflate: (ViewGroup) -> View): Bitmap {
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
                intArrayOf(0xFF8FA7B8.toInt(), 0xFFB9B3C9.toInt(), 0xFFD8C3B4.toInt())
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
        val bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Bitmap.Config.ARGB_8888)
        var result = -1
        PixelCopy.request(activity.window, rect, bitmap, { result = it }, Handler(Looper.getMainLooper()))
        shadowOf(Looper.getMainLooper()).idle()
        activityController.pause().stop().destroy()
        check(result == PixelCopy.SUCCESS) { "PixelCopy failed: $result" }
        return bitmap
    }

    private fun writePng(bitmap: Bitmap, file: File) {
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}

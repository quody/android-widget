package com.example.nimipaivat.widget

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent

/** What the settings screen previews: its current, not yet saved, selections. */
internal data class WidgetSettings(
    val swedish: Boolean = false,
    val style: WidgetStyle = WidgetStyle.MATERIAL_YOU,
    val pastelGlass: Boolean = false,
    val wavyEdge: Boolean = false
)

/** Today's / tomorrow's names in both calendars, loaded once by the settings screen. */
internal data class PreviewNames(
    val dateText: String,
    val todayFi: List<String>,
    val todaySv: List<String>,
    val tomorrowFi: List<String>,
    val tomorrowSv: List<String>
)

/**
 * The real widget layout ([WidgetLayout], names screen) for [settings] instead
 * of the saved preferences, so the settings screen can compose it to
 * RemoteViews with [GlanceAppWidget.compose] and show it before saving.
 */
internal class SettingsPreviewWidget(
    private val settings: WidgetSettings,
    private val names: PreviewNames
) : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            WidgetLayout(
                isFlipped = false,
                dateText = names.dateText,
                todayNames = if (settings.swedish) names.todaySv else names.todayFi,
                tomorrowNames = if (settings.swedish) names.tomorrowSv else names.tomorrowFi,
                styleColors = resolveStyle(settings.style, settings.pastelGlass),
                etymologyOf = { null },
                wavyEdge = settings.wavyEdge
            )
        }
    }
}

/** Wallpaper-like backdrops behind the preview (same as the screenshot test's). */
internal object PreviewBackdrop {
    private val LIGHT = intArrayOf(0xFF8FA7B8.toInt(), 0xFFB9B3C9.toInt(), 0xFFD8C3B4.toInt())
    private val DARK = intArrayOf(0xFF1B2433.toInt(), 0xFF2D2440.toInt(), 0xFF3B2A26.toInt())

    fun drawable(dark: Boolean, cornerRadiusPx: Float) =
        GradientDrawable(GradientDrawable.Orientation.TL_BR, if (dark) DARK else LIGHT).apply {
            cornerRadius = cornerRadiusPx
        }
}

/**
 * Lays its single child out at a fixed size ([setChildSize], the widget size
 * the preview is composed for) and scales it down uniformly when the available
 * width is smaller, so the preview is never re-laid out at another size.
 * Touches never reach the child: the preview's click actions are inert.
 */
class ScaledPreviewFrame @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : ViewGroup(context, attrs) {

    private var childWidthPx = 0
    private var childHeightPx = 0

    fun setChildSize(widthPx: Int, heightPx: Int) {
        childWidthPx = widthPx
        childHeightPx = heightPx
        requestLayout()
    }

    private fun scaleFor(availableWidth: Int): Float =
        if (childWidthPx <= 0 || availableWidth >= childWidthPx) 1f
        else availableWidth.toFloat() / childWidthPx

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val scale = scaleFor(width)
        for (i in 0 until childCount) {
            getChildAt(i).measure(
                MeasureSpec.makeMeasureSpec(childWidthPx, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(childHeightPx, MeasureSpec.EXACTLY)
            )
        }
        setMeasuredDimension(width, (childHeightPx * scale).toInt())
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val scale = scaleFor(width)
        for (i in 0 until childCount) {
            val child: View = getChildAt(i)
            val left = (width - childWidthPx) / 2
            val top = (height - childHeightPx) / 2
            child.layout(left, top, left + childWidthPx, top + childHeightPx)
            child.pivotX = childWidthPx / 2f
            child.pivotY = childHeightPx / 2f
            child.scaleX = scale
            child.scaleY = scale
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?) = true

    override fun shouldDelayChildPressedState() = false
}

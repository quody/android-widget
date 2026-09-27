package com.example.nimipaivat.widget

import android.content.res.Resources
import android.graphics.Typeface
import android.os.Build
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.TypedValue
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max

/**
 * Picks the font size for today's names: the largest size that still shows
 * every name inside the space the layout leaves for them.
 *
 * The text is measured with the same [StaticLayout] setup a (Glance)
 * TextView uses: default sans-serif, bold, font padding included, high
 * quality line breaking, sp converted with the device's font scale.
 */
internal object NameFit {

    /** Smallest size tried; below this the names get an ellipsis as a last resort. */
    const val MIN_SP = 9f

    /** Sizes are searched in steps of this many sp. */
    private const val STEP_SP = 0.5f

    /**
     * Launchers can render text slightly differently (and give the widget a few
     * dp less than the size it reports), so the text is fitted into this
     * fraction of the available width and height.
     */
    const val SAFETY = 0.93f

    /** U+2060 WORD JOINER: keeps "Kukka-Maaria" on one line. */
    private const val WORD_JOINER = "\u2060"

    /** Joins the names like the widget shows them, without line breaks inside a name. */
    fun joinNames(names: List<String>): String =
        names.joinToString(", ") { unbreakable(it) }.ifEmpty { "\u2014" }

    private fun unbreakable(name: String) = name.replace("-", "-$WORD_JOINER")

    data class Result(
        val sizeSp: Float,
        /** Int.MAX_VALUE (no ellipsis) when everything fits. */
        val maxLines: Int,
        val fits: Boolean
    )

    /**
     * Largest size in [minSp]..[maxSp] (in [STEP_SP] steps) for which [fits]
     * holds, assuming [fits] is monotonic (true up to some size, false above).
     * Null when not even [minSp] fits.
     */
    fun largestFitting(minSp: Float, maxSp: Float, fits: (Float) -> Boolean): Float? {
        val steps = floor((maxSp - minSp) / STEP_SP + 1e-4f).toInt()
        if (!fits(minSp)) return null
        var lo = 0 // fits
        var hi = steps + 1 // exclusive upper bound, unknown/doesn't fit
        while (hi - lo > 1) {
            val mid = (lo + hi) / 2
            if (fits(minSp + mid * STEP_SP)) lo = mid else hi = mid
        }
        return minSp + lo * STEP_SP
    }

    /**
     * Fits [text] (from [joinNames]) into [widthPx] x [heightPx]. Every name
     * must fit on a line by itself and the wrapped text must fit the height.
     */
    fun fit(
        text: String,
        widthPx: Float,
        heightPx: Float,
        maxSp: Float,
        measurer: TextMeasurer,
        minSp: Float = MIN_SP
    ): Result {
        val w = widthPx * SAFETY
        val h = heightPx * SAFETY
        val words = text.split(", ")
        val size = largestFitting(minSp, max(minSp, maxSp)) { sp ->
            words.all { measurer.width(it, sp) <= w } && measurer.height(text, sp, w) <= h
        }
        if (size != null) return Result(size, Int.MAX_VALUE, fits = true)
        // Extreme case (tiny widget / huge font scale): smallest size, as many
        // lines as fit, ellipsis for the rest.
        val lines = max(1, floor(h / measurer.lineHeight(minSp)).toInt())
        return Result(minSp, lines, fits = false)
    }

    /** Measures bold default-font text like a TextView, converting sp like the launcher does. */
    open class TextMeasurer(
        private val spToPx: (Float) -> Float,
        private val bold: Boolean = true
    ) {
        constructor(resources: Resources, bold: Boolean = true) : this(
            { sp -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics) },
            bold
        )

        private fun paint(sp: Float) = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply {
            typeface = if (bold) Typeface.create(Typeface.DEFAULT, Typeface.BOLD) else Typeface.DEFAULT
            textSize = spToPx(sp)
        }

        open fun width(text: String, sp: Float): Float = paint(sp).measureText(text)

        /** Height of [text] wrapped at [widthPx]. */
        open fun height(text: String, sp: Float, widthPx: Float): Int =
            layout(text, sp, widthPx).height

        /** Height of one line of text at [sp]. */
        open fun lineHeight(sp: Float): Float = height("Ag", sp, Float.MAX_VALUE / 4)
            .toFloat()

        private fun layout(text: String, sp: Float, widthPx: Float): StaticLayout {
            val width = max(1, ceil(minOf(widthPx, 1_000_000f)).toInt())
            return StaticLayout.Builder.obtain(text, 0, text.length, paint(sp), width)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(true)
                .setLineSpacing(0f, 1f)
                .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
                .setHyphenationFrequency(Layout.HYPHENATION_FREQUENCY_NONE)
                .apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        setUseLineSpacingFromFallbacks(true)
                    }
                }
                .build()
        }
    }
}

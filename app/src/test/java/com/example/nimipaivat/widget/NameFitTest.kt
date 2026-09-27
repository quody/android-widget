package com.example.nimipaivat.widget

import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.example.nimipaivat.data.NameDayRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h891dp-xxhdpi")
class NameFitTest {

    private val density = 3f
    private fun measurer(fontScale: Float = 1f) =
        NameFit.TextMeasurer({ sp -> sp * density * fontScale })

    @Test
    fun `largestFitting finds the largest step that fits`() {
        assertEquals(17f, NameFit.largestFitting(10f, 30f) { it <= 17.3f })
        assertEquals(30f, NameFit.largestFitting(10f, 30f) { true })
        assertEquals(10f, NameFit.largestFitting(10f, 30f) { it < 10.5f })
        assertNull(NameFit.largestFitting(10f, 30f) { false })
    }

    @Test
    fun `more names get a smaller size and still fit`() {
        val m = measurer()
        val w = 222f * density
        val h = 70f * density
        val one = NameFit.fit(NameFit.joinNames(listOf("Uuno")), w, h, 28f, m)
        val many = NameFit.fit(NameFit.joinNames(MARJA), w, h, 28f, m)
        assertEquals(28f, one.sizeSp)
        assertTrue(many.fits)
        assertEquals(Int.MAX_VALUE, many.maxLines)
        assertTrue(many.sizeSp < one.sizeSp)
        // The chosen size fits within the safety margin; half a step up would not.
        val text = NameFit.joinNames(MARJA)
        assertTrue(m.height(text, many.sizeSp, w * NameFit.SAFETY) <= h * NameFit.SAFETY)
        assertTrue(m.height(text, many.sizeSp + 0.5f, w * NameFit.SAFETY) > h * NameFit.SAFETY)
    }

    @Test
    fun `a larger font scale picks a smaller sp`() {
        val text = NameFit.joinNames(MARJA)
        val normal = NameFit.fit(text, 666f, 210f, 28f, measurer(1f))
        val scaled = NameFit.fit(text, 666f, 210f, 28f, measurer(1.3f))
        assertTrue(scaled.sizeSp < normal.sizeSp)
    }

    @Test
    fun `falls back to min size with ellipsis when nothing fits`() {
        val r = NameFit.fit(NameFit.joinNames(MARJA), 100f * density, 20f * density, 28f, measurer())
        assertFalse(r.fits)
        assertEquals(NameFit.MIN_SP, r.sizeSp)
        assertEquals(1, r.maxLines)
    }

    @Test
    fun `hyphenated names are not broken`() {
        val text = NameFit.joinNames(listOf("Maikki", "Kukka-Maaria"))
        val paint = TextPaint().apply { textSize = 60f }
        // Room for "Maikki, Kukka-" but not the whole name.
        val width = paint.measureText("Maikki, Kukka-M").toInt()
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, paint, width)
            .setBreakStrategy(Layout.BREAK_STRATEGY_HIGH_QUALITY)
            .build()
        val lines = (0 until layout.lineCount).map {
            text.substring(layout.getLineStart(it), layout.getLineEnd(it)).trim()
        }
        assertEquals(listOf("Maikki,", "Kukka-\u2060Maaria"), lines)
    }

    /**
     * Every real day (fi and sv) shows all names at the usual widget sizes, in
     * every padding setup. The one known exception: 15.8. (13 names, 108
     * characters) on a 180x70dp widget with a drawable (glass/pastel)
     * background, whose 14dp padding leaves too little room even at MIN_SP;
     * it falls back to MIN_SP with an ellipsis.
     */
    @Test
    fun `all real name days fit the supported sizes`() {
        val context = RuntimeEnvironment.getApplication()
        val repo = NameDayRepository(context)
        val sizes = listOf(
            DpSize(180.dp, 70.dp), DpSize(250.dp, 120.dp),
            DpSize(250.dp, 190.dp), DpSize(320.dp, 120.dp)
        )
        val days = generateSequence(LocalDate.of(2024, 1, 1)) { it.plusDays(1) }
            .takeWhile { it.year == 2024 }
            .map { "%02d-%02d".format(it.monthValue, it.dayOfMonth) }
            .toList()
        val failures = mutableListOf<String>()
        for (size in sizes) for (drawable in listOf(false, true)) for (wavy in listOf(false, true)) {
            val padding = contentPadding(size, drawable, wavy, isFlipped = false)
            val tier = NamesTier.of(size)
            for (day in days) {
                val nd = repo.getNameDay(day)
                for (names in listOf(nd.fi, nd.sv)) {
                    val r = fitTodayNames(context, size, padding, tier, names)
                    val knownException = day == "08-15" && names.size == 13 &&
                        size == DpSize(180.dp, 70.dp) && drawable
                    if (r.fits == knownException) {
                        failures += "$day $size drawable=$drawable wavy=$wavy fits=${r.fits} $names"
                    }
                }
            }
        }
        assertTrue(failures.joinToString("\n"), failures.isEmpty())
    }

    private companion object {
        val MARJA = listOf(
            "Marja", "Jaana", "Marjo", "Marita", "Marjatta", "Marjut", "Marianne",
            "Maritta", "Marjaana", "Marianna", "Marjukka", "Jatta", "Marju"
        )
    }
}

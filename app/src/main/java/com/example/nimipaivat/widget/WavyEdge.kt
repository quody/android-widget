package com.example.nimipaivat.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Material 3 Expressive style "scalloped" widget outline.
 *
 * A launcher's own widget clip can't be changed, so when the wavy edge is on
 * the widget paints its whole background into a bitmap at its actual size:
 * a rounded rectangle whose outline is displaced along its normal by a gentle
 * sine wave. The wave count is even and the phase starts with a crest at the
 * top centre, so the shape is symmetric on both axes, crests are evenly spaced
 * along the whole perimeter and stay smooth through the corners.
 *
 * Inner layers (glass pane, pastel panel and border) are offset copies of the
 * same wavy outline, so they follow the edge like the XML drawables follow
 * their rounded rectangles.
 */
internal object WavyEdge {

    const val AMPLITUDE_DP = 1.75f
    private const val WAVELENGTH_DP = 28f
    private const val CORNER_RADIUS_DP = 22f

    /** The base outline sits this far inside the bitmap so crests are not cut. */
    private const val EDGE_INSET_DP = AMPLITUDE_DP + 0.5f

    /**
     * Extra content padding while the edge is on. Troughs reach
     * EDGE_INSET + AMPLITUDE = 4dp into the widget (plus the panel inset for
     * pastel styles), so this keeps text as clear of the waves as it is of the
     * straight edges without the effect.
     */
    val CONTENT_EXTRA_PADDING = 3.dp

    /**
     * Upper bound for the bitmap (ARGB_8888 = 4 bytes/px, so ~2.4 MB). Launchers
     * reject RemoteViews whose bitmaps exceed ~1.5x the screen's pixel memory and
     * Glance may send one bitmap per orientation, so large widgets on dense
     * screens are rendered at a lower scale and stretched (slightly softer
     * edges) instead.
     */
    private const val MAX_PIXELS = 600_000

    fun render(sizeDp: DpSize, density: Float, painter: WavyPainter): Bitmap {
        val wDp = sizeDp.width.value
        val hDp = sizeDp.height.value
        var scale = density
        if (wDp * hDp * scale * scale > MAX_PIXELS) {
            scale = sqrt(MAX_PIXELS / (wDp * hDp))
        }
        val w = max(1, (wDp * scale).roundToInt())
        val h = max(1, (hDp * scale).roundToInt())
        val outline = Outline(w.toFloat(), h.toFloat(), scale)

        // Solid styles only need coverage: an 8-bit mask, tinted by the RemoteViews.
        val config = if (painter is WavyPainter.Solid) Bitmap.Config.ALPHA_8 else Bitmap.Config.ARGB_8888
        val bitmap = Bitmap.createBitmap(w, h, config)
        val canvas = Canvas(bitmap)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }

        when (painter) {
            is WavyPainter.Solid -> {
                fill.color = android.graphics.Color.BLACK
                canvas.drawPath(outline.path(0f), fill)
            }
            is WavyPainter.Glass -> {
                fill.color = painter.shadow
                canvas.drawPath(outline.path(0f), fill)
                val pane = outline.path(-1f * scale)
                fill.color = painter.pane
                canvas.drawPath(pane, fill)
                canvas.drawPath(pane, stroke(painter.paneStroke, 0.5f * scale))
                fill.color = android.graphics.Color.WHITE
                fill.shader = LinearGradient(
                    0f, 0f, 0f, h.toFloat(),
                    painter.highlight, painter.highlight and 0x00FFFFFF, Shader.TileMode.CLAMP
                )
                canvas.drawPath(outline.path(-2f * scale), fill)
            }
            is WavyPainter.Pastel -> paintPastel(canvas, outline, painter.palette, w, h, scale)
        }
        return bitmap
    }

    private fun paintPastel(
        canvas: Canvas,
        outline: Outline,
        p: PastelPalette,
        w: Int,
        h: Int,
        scale: Float
    ) {
        val outer = outline.path(0f)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG)
        fill.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(), p.fillStart, p.fillEnd, Shader.TileMode.CLAMP
        )
        canvas.drawPath(outer, fill)
        fill.shader = null

        canvas.save()
        canvas.clipPath(outer)
        fun dp(v: Float) = v * scale
        fill.color = p.blobTop
        canvas.drawOval(RectF(w - dp(4f + 72f), dp(4f), w - dp(4f), dp(4f + 52f)), fill)
        fill.color = p.blobBottom
        canvas.drawOval(RectF(dp(4f), h - dp(4f + 44f), dp(4f + 60f), h - dp(4f)), fill)
        canvas.restore()

        fill.color = p.panel
        canvas.drawPath(outline.path(-dp(PASTEL_PANEL_INSET_DP)), fill)

        p.highlight?.let { hl ->
            fill.shader = LinearGradient(
                0f, 0f, 0f, h.toFloat(), hl, hl and 0x00FFFFFF, Shader.TileMode.CLAMP
            )
            canvas.drawPath(outer, fill)
            fill.shader = null
        }
        // Stroke centred half a border width inside the outline: outer edge on it.
        val bw = dp(p.borderWidthDp)
        canvas.drawPath(outline.path(-bw / 2f), stroke(p.border, bw))
        p.rim?.let { rim -> canvas.drawPath(outline.path(-bw - dp(0.5f)), stroke(rim, dp(1f))) }
    }

    private fun stroke(color: Int, width: Float) = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = width
        this.color = color
    }

    /**
     * Rounded rectangle (in px) walked clockwise from the top centre. [path]
     * displaces each sample along the outward normal by
     * `offset + amplitude * cos(2π · waves · s / perimeter)`.
     */
    private class Outline(w: Float, h: Float, scale: Float) {
        private val inset = EDGE_INSET_DP * scale
        private val left = inset
        private val top = inset
        private val right = w - inset
        private val bottom = h - inset
        private val r = min(CORNER_RADIUS_DP * scale, min(right - left, bottom - top) / 2f)
        private val sw = max(0f, right - left - 2 * r)
        private val sh = max(0f, bottom - top - 2 * r)
        private val arc = (PI / 2 * r).toFloat()
        private val perimeter = 2 * sw + 2 * sh + 4 * arc
        private val waves = max(4, ((perimeter / (WAVELENGTH_DP * scale)) / 2f).roundToInt() * 2)
        private val amplitude = AMPLITUDE_DP * scale

        fun path(offset: Float): Path {
            val path = Path()
            val steps = max(64, (perimeter / 1.5f).roundToInt())
            for (i in 0..steps) {
                val s = perimeter * i / steps
                val (x, y, nx, ny) = sample(s)
                val d = offset + amplitude * cos(2 * PI * waves * s / perimeter).toFloat()
                val px = x + nx * d
                val py = y + ny * d
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            path.close()
            return path
        }

        private data class Sample(val x: Float, val y: Float, val nx: Float, val ny: Float)

        private fun sample(sIn: Float): Sample {
            var s = sIn
            val cx = (left + right) / 2f
            // 1. top edge, centre -> right
            if (s <= sw / 2) return Sample(cx + s, top, 0f, -1f)
            s -= sw / 2
            // 2. top-right corner
            if (s <= arc) return corner(right - r, top + r, -PI / 2, s)
            s -= arc
            // 3. right edge
            if (s <= sh) return Sample(right, top + r + s, 1f, 0f)
            s -= sh
            // 4. bottom-right corner
            if (s <= arc) return corner(right - r, bottom - r, 0.0, s)
            s -= arc
            // 5. bottom edge
            if (s <= sw) return Sample(right - r - s, bottom, 0f, 1f)
            s -= sw
            // 6. bottom-left corner
            if (s <= arc) return corner(left + r, bottom - r, PI / 2, s)
            s -= arc
            // 7. left edge
            if (s <= sh) return Sample(left, bottom - r - s, -1f, 0f)
            s -= sh
            // 8. top-left corner
            if (s <= arc) return corner(left + r, top + r, PI, s)
            s -= arc
            // 9. top edge, left -> centre
            return Sample(left + r + s, top, 0f, -1f)
        }

        private fun corner(ccx: Float, ccy: Float, startAngle: Double, s: Float): Sample {
            val a = startAngle + if (r > 0f) s / r else 0f
            val nx = cos(a).toFloat()
            val ny = sin(a).toFloat()
            return Sample(ccx + r * nx, ccy + r * ny, nx, ny)
        }
    }
}

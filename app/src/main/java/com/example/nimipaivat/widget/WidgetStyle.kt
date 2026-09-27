package com.example.nimipaivat.widget

import androidx.compose.ui.graphics.Color
import com.example.nimipaivat.R

enum class WidgetStyle(val displayNameKey: String, val isPastel: Boolean = false) {
    DARK("style_dark"),
    MATERIAL_YOU("style_material_you"),
    GLASS_LIGHT("style_glass_light"),
    GLASS_DARK("style_glass_dark"),
    PAPER("style_paper"),
    POWDER_PUFF("style_powder_puff", isPastel = true),
    LEMONDROP("style_lemondrop", isPastel = true),
    PINKIE_PROMISE("style_pinkie_promise", isPastel = true);
}

data class WidgetStyleColors(
    val backgroundColor: Color?,
    val primaryTextColor: Color?,
    val secondaryTextColor: Color?,
    val accentColor: Color?,
    val isMaterialYou: Boolean = false,
    val backgroundDrawableRes: Int? = null,
    /**
     * How [WavyEdge] paints this style's background into a bitmap when the wavy
     * edge is on. It mirrors the XML drawable (or solid colour) of the style, so
     * keep the two in sync when changing colours.
     */
    val wavyPainter: WavyPainter = WavyPainter.Solid(backgroundColor)
)

/** Background recipe for the wavy-edge bitmap (see [WavyEdge.render]). */
sealed interface WavyPainter {
    /**
     * A plain filled shape. It is rendered as an alpha mask and tinted in the
     * RemoteViews, so a null colour (Material You) keeps the launcher-resolved,
     * day/night aware theme colour instead of a colour baked into the bitmap.
     */
    data class Solid(val color: Color?) : WavyPainter

    /** Mirrors glass_background*.xml: shadow rim, translucent pane, top highlight. */
    data class Glass(
        val shadow: Int,
        val pane: Int,
        val paneStroke: Int,
        val highlight: Int
    ) : WavyPainter

    /** Mirrors the *_background.xml / *_glass_background.xml pastel cards. */
    data class Pastel(val palette: PastelPalette) : WavyPainter
}

/** Colours (ARGB ints) of a pastel card; the text always sits on [panel]. */
data class PastelPalette(
    val fillStart: Int,
    val fillEnd: Int,
    val blobTop: Int,
    val blobBottom: Int,
    val panel: Int,
    val border: Int,
    val borderWidthDp: Float,
    /** Glass only: top-down sheen and a thin light rim just inside the border. */
    val highlight: Int? = null,
    val rim: Int? = null
)

/** Inner panel inset (dp) of the pastel cards, shared by the XML and [WavyEdge]. */
const val PASTEL_PANEL_INSET_DP = 8f

fun resolveStyle(style: WidgetStyle, pastelGlass: Boolean = false): WidgetStyleColors {
    return when (style) {
        WidgetStyle.DARK -> WidgetStyleColors(
            backgroundColor = Color(0xFF1C1C1E),
            primaryTextColor = Color.White,
            secondaryTextColor = Color(0xFFAEAEB2),
            accentColor = Color(0xFF64D2FF)
        )
        WidgetStyle.MATERIAL_YOU -> WidgetStyleColors(
            backgroundColor = null,
            primaryTextColor = null,
            secondaryTextColor = null,
            accentColor = null,
            isMaterialYou = true
        )
        WidgetStyle.GLASS_LIGHT -> WidgetStyleColors(
            backgroundColor = null,
            primaryTextColor = Color(0xFF1C1C1E),
            secondaryTextColor = Color(0xFF3C3C43),
            accentColor = Color(0xFF007AFF),
            backgroundDrawableRes = R.drawable.glass_background_light,
            wavyPainter = WavyPainter.Glass(
                shadow = 0x15000000,
                pane = 0x66FFFFFF,
                paneStroke = 0x80FFFFFF.toInt(),
                highlight = 0x40FFFFFF
            )
        )
        WidgetStyle.GLASS_DARK -> WidgetStyleColors(
            backgroundColor = null,
            primaryTextColor = Color(0xEEFFFFFF),
            secondaryTextColor = Color(0xAAFFFFFF),
            accentColor = Color(0xFF64D2FF),
            backgroundDrawableRes = R.drawable.glass_background,
            wavyPainter = WavyPainter.Glass(
                shadow = 0x10000000,
                pane = 0x38FFFFFF,
                paneStroke = 0x50FFFFFF,
                highlight = 0x30FFFFFF
            )
        )
        WidgetStyle.PAPER -> WidgetStyleColors(
            backgroundColor = Color(0xFFFFF8F0),
            primaryTextColor = Color(0xFF5D4037),
            secondaryTextColor = Color(0xFF795548),
            accentColor = Color(0xFF8D6E63)
        )
        // Cutesy pastel styles: text always sits on the drawable's light
        // translucent-white inner panel, so dark text keeps >= 4.5:1 contrast.
        // The glass variants tint the wallpaper instead of covering it; tint +
        // panel still cover ~4/5 of it, and their secondary/accent text is a
        // shade darker, so on a dark wallpaper all text stays >= 4.5:1 (primary
        // ~6:1) and on light wallpapers it is well above that.
        WidgetStyle.POWDER_PUFF -> pastel(
            primary = Color(0xFF1F3A68),
            secondary = Color(0xFF3D5A8A),
            accent = Color(0xFF3F51B5),
            glassSecondary = Color(0xFF2E4A78),
            glassAccent = Color(0xFF33449A),
            glass = pastelGlass,
            opaqueRes = R.drawable.powder_puff_background,
            glassRes = R.drawable.powder_puff_glass_background,
            opaque = PastelPalette(
                fillStart = 0xFFBFDDF0.toInt(), fillEnd = 0xFFD3DDFB.toInt(),
                blobTop = 0x80A9C8F5.toInt(), blobBottom = 0x80C4CCF7.toInt(),
                panel = 0xA6FFFFFF.toInt(), border = 0xFF8AA4E8.toInt(),
                borderWidthDp = 4f
            ),
            glassPalette = PastelPalette(
                fillStart = 0x99BFDDF0.toInt(), fillEnd = 0x99D3DDFB.toInt(),
                blobTop = 0x66A9C8F5, blobBottom = 0x66C4CCF7,
                panel = 0x73FFFFFF, border = 0xB38AA4E8.toInt(),
                borderWidthDp = 3f,
                highlight = 0x40FFFFFF, rim = 0x80FFFFFF.toInt()
            )
        )
        WidgetStyle.LEMONDROP -> pastel(
            primary = Color(0xFF6B3A12),
            secondary = Color(0xFF8A4B1C),
            accent = Color(0xFFB4400E),
            glassSecondary = Color(0xFF74400F),
            glassAccent = Color(0xFF8A300A),
            glass = pastelGlass,
            opaqueRes = R.drawable.lemondrop_background,
            glassRes = R.drawable.lemondrop_glass_background,
            opaque = PastelPalette(
                fillStart = 0xFFFFF3B0.toInt(), fillEnd = 0xFFFFE2A6.toInt(),
                blobTop = 0x80FFC98B.toInt(), blobBottom = 0x80FFB8A3.toInt(),
                panel = 0xA6FFFFFF.toInt(), border = 0xFFF6A06B.toInt(),
                borderWidthDp = 4f
            ),
            glassPalette = PastelPalette(
                fillStart = 0x99FFF3B0.toInt(), fillEnd = 0x99FFE2A6.toInt(),
                blobTop = 0x66FFC98B, blobBottom = 0x66FFB8A3,
                panel = 0x73FFFFFF, border = 0xB3F6A06B.toInt(),
                borderWidthDp = 3f,
                highlight = 0x40FFFFFF, rim = 0x80FFFFFF.toInt()
            )
        )
        WidgetStyle.PINKIE_PROMISE -> pastel(
            primary = Color(0xFF7A1F5C),
            secondary = Color(0xFF8E3A6E),
            accent = Color(0xFFA62A6E),
            glassSecondary = Color(0xFF7A2D5E),
            glassAccent = Color(0xFF84205A),
            glass = pastelGlass,
            opaqueRes = R.drawable.pinkie_promise_background,
            glassRes = R.drawable.pinkie_promise_glass_background,
            opaque = PastelPalette(
                fillStart = 0xFFF9C6D3.toInt(), fillEnd = 0xFFE7C9F5.toInt(),
                blobTop = 0x80F7A8C4.toInt(), blobBottom = 0x80D9B8F0.toInt(),
                panel = 0x99FFFFFF.toInt(), border = 0xFFF48A8A.toInt(),
                borderWidthDp = 4f
            ),
            glassPalette = PastelPalette(
                fillStart = 0x99F9C6D3.toInt(), fillEnd = 0x99E7C9F5.toInt(),
                blobTop = 0x66F7A8C4, blobBottom = 0x66D9B8F0,
                panel = 0x73FFFFFF, border = 0xB3F48A8A.toInt(),
                borderWidthDp = 3f,
                highlight = 0x40FFFFFF, rim = 0x80FFFFFF.toInt()
            )
        )
    }
}

private fun pastel(
    primary: Color,
    secondary: Color,
    accent: Color,
    glassSecondary: Color,
    glassAccent: Color,
    glass: Boolean,
    opaqueRes: Int,
    glassRes: Int,
    opaque: PastelPalette,
    glassPalette: PastelPalette
) = WidgetStyleColors(
    backgroundColor = null,
    primaryTextColor = primary,
    secondaryTextColor = if (glass) glassSecondary else secondary,
    accentColor = if (glass) glassAccent else accent,
    backgroundDrawableRes = if (glass) glassRes else opaqueRes,
    wavyPainter = WavyPainter.Pastel(if (glass) glassPalette else opaque)
)

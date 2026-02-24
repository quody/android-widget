package com.example.nimipaivat.widget

import androidx.compose.ui.graphics.Color

enum class WidgetStyle(val displayNameKey: String) {
    DARK("style_dark"),
    MATERIAL_YOU("style_material_you"),
    FROSTED_GLASS("style_frosted_glass"),
    PAPER("style_paper");
}

data class WidgetStyleColors(
    val backgroundColor: Color?,
    val primaryTextColor: Color?,
    val secondaryTextColor: Color?,
    val accentColor: Color?,
    val isMaterialYou: Boolean = false
)

fun resolveStyle(style: WidgetStyle): WidgetStyleColors {
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
        WidgetStyle.FROSTED_GLASS -> WidgetStyleColors(
            backgroundColor = Color(0xB3FFFFFF.toInt()),
            primaryTextColor = Color(0xFF1C1C1E),
            secondaryTextColor = Color(0xFF3C3C43),
            accentColor = Color(0xFF007AFF)
        )
        WidgetStyle.PAPER -> WidgetStyleColors(
            backgroundColor = Color(0xFFFFF8F0),
            primaryTextColor = Color(0xFF5D4037),
            secondaryTextColor = Color(0xFF795548),
            accentColor = Color(0xFF8D6E63)
        )
    }
}

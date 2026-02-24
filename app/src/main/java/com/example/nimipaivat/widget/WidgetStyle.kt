package com.example.nimipaivat.widget

import androidx.compose.ui.graphics.Color
import com.example.nimipaivat.R

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
    val isMaterialYou: Boolean = false,
    val backgroundDrawableRes: Int? = null
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
            backgroundColor = null,
            primaryTextColor = Color(0xEEFFFFFF),
            secondaryTextColor = Color(0xAAFFFFFF),
            accentColor = Color(0xFF64D2FF),
            backgroundDrawableRes = R.drawable.glass_background
        )
        WidgetStyle.PAPER -> WidgetStyleColors(
            backgroundColor = Color(0xFFFFF8F0),
            primaryTextColor = Color(0xFF5D4037),
            secondaryTextColor = Color(0xFF795548),
            accentColor = Color(0xFF8D6E63)
        )
    }
}

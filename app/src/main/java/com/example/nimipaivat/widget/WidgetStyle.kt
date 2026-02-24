package com.example.nimipaivat.widget

import androidx.compose.ui.graphics.Color
import java.time.LocalDate
import java.time.LocalTime

enum class WidgetStyle(val displayNameKey: String) {
    CLASSIC("style_classic"),
    DARK("style_dark"),
    MATERIAL_YOU("style_material_you"),
    FINNISH("style_finnish"),
    SUNRISE("style_sunrise"),
    AURORA("style_aurora"),
    FROSTED_GLASS("style_frosted_glass"),
    SEASONAL("style_seasonal"),
    TIME_OF_DAY("style_time_of_day"),
    PAPER("style_paper");
}

data class WidgetStyleColors(
    val backgroundColor: Color?,
    val backgroundDrawableRes: Int?,
    val primaryTextColor: Color?,
    val secondaryTextColor: Color?,
    val accentColor: Color?,
    val isMaterialYou: Boolean = false
)

fun resolveStyle(style: WidgetStyle): WidgetStyleColors {
    return when (style) {
        WidgetStyle.CLASSIC -> WidgetStyleColors(
            backgroundColor = Color.White,
            backgroundDrawableRes = null,
            primaryTextColor = Color(0xFF1C1C1E),
            secondaryTextColor = Color(0xFF3C3C43),
            accentColor = Color(0xFF007AFF)
        )
        WidgetStyle.DARK -> WidgetStyleColors(
            backgroundColor = Color(0xFF1C1C1E),
            backgroundDrawableRes = null,
            primaryTextColor = Color.White,
            secondaryTextColor = Color(0xFFAEAEB2),
            accentColor = Color(0xFF64D2FF)
        )
        WidgetStyle.MATERIAL_YOU -> WidgetStyleColors(
            backgroundColor = null,
            backgroundDrawableRes = null,
            primaryTextColor = null,
            secondaryTextColor = null,
            accentColor = null,
            isMaterialYou = true
        )
        WidgetStyle.FINNISH -> WidgetStyleColors(
            backgroundColor = Color(0xFF003580),
            backgroundDrawableRes = null,
            primaryTextColor = Color.White,
            secondaryTextColor = Color(0xFFCCDDFF),
            accentColor = Color(0xFFFFD700)
        )
        WidgetStyle.SUNRISE -> WidgetStyleColors(
            backgroundColor = null,
            backgroundDrawableRes = com.example.nimipaivat.R.drawable.bg_sunrise,
            primaryTextColor = Color(0xFF1C1C1E),
            secondaryTextColor = Color(0xFF3C3C43),
            accentColor = Color(0xFFD84315)
        )
        WidgetStyle.AURORA -> WidgetStyleColors(
            backgroundColor = null,
            backgroundDrawableRes = com.example.nimipaivat.R.drawable.bg_aurora,
            primaryTextColor = Color.White,
            secondaryTextColor = Color(0xFFB2DFDB),
            accentColor = Color(0xFF2EC4B6)
        )
        WidgetStyle.FROSTED_GLASS -> WidgetStyleColors(
            backgroundColor = Color(0xB3FFFFFF.toInt()),
            backgroundDrawableRes = null,
            primaryTextColor = Color(0xFF1C1C1E),
            secondaryTextColor = Color(0xFF3C3C43),
            accentColor = Color(0xFF007AFF)
        )
        WidgetStyle.SEASONAL -> resolveSeasonalStyle()
        WidgetStyle.TIME_OF_DAY -> resolveTimeOfDayStyle()
        WidgetStyle.PAPER -> WidgetStyleColors(
            backgroundColor = Color(0xFFFFF8F0),
            backgroundDrawableRes = null,
            primaryTextColor = Color(0xFF5D4037),
            secondaryTextColor = Color(0xFF795548),
            accentColor = Color(0xFF8D6E63)
        )
    }
}

private fun resolveSeasonalStyle(): WidgetStyleColors {
    val month = LocalDate.now().monthValue
    val (drawableRes, primaryText, secondaryText, accent) = when (month) {
        3, 4, 5 -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_seasonal_spring,
            Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF388E3C)
        )
        6, 7, 8 -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_seasonal_summer,
            Color(0xFF01579B), Color(0xFF0277BD), Color(0xFF0288D1)
        )
        9, 10, 11 -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_seasonal_autumn,
            Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFFBF360C)
        )
        else -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_seasonal_winter,
            Color(0xFF263238), Color(0xFF37474F), Color(0xFF0277BD)
        )
    }
    return WidgetStyleColors(
        backgroundColor = null,
        backgroundDrawableRes = drawableRes,
        primaryTextColor = primaryText,
        secondaryTextColor = secondaryText,
        accentColor = accent
    )
}

private data class SeasonalColors(
    val drawableRes: Int,
    val primaryText: Color,
    val secondaryText: Color,
    val accent: Color
)

private fun resolveTimeOfDayStyle(): WidgetStyleColors {
    val hour = LocalTime.now().hour
    val (drawableRes, primaryText, secondaryText, accent) = when (hour) {
        in 6..11 -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_time_morning,
            Color(0xFF3E2723), Color(0xFF4E342E), Color(0xFFE65100)
        )
        in 12..16 -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_time_afternoon,
            Color(0xFF0D47A1), Color(0xFF1565C0), Color(0xFF1976D2)
        )
        in 17..20 -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_time_evening,
            Color.White, Color(0xFFCE93D8), Color(0xFFBA68C8)
        )
        else -> SeasonalColors(
            com.example.nimipaivat.R.drawable.bg_time_night,
            Color.White, Color(0xFFB39DDB), Color(0xFF9575CD)
        )
    }
    return WidgetStyleColors(
        backgroundColor = null,
        backgroundDrawableRes = drawableRes,
        primaryTextColor = primaryText,
        secondaryTextColor = secondaryText,
        accentColor = accent
    )
}

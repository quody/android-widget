package com.example.nimipaivat.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.itemsIndexed
import androidx.glance.ImageProvider
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.example.nimipaivat.data.EtymologyRepository
import com.example.nimipaivat.data.NameDayRepository
import com.example.nimipaivat.util.DateUtils
import kotlinx.coroutines.runBlocking

@Composable
fun WidgetContent(context: Context) {
    val prefs = currentState<Preferences>()
    val isFlipped = prefs[FlipAction.FLIPPED_KEY] ?: false

    val repository = NameDayRepository(context)
    val useSwedish = runBlocking { WidgetPreferences.isSwedish(context) }
    val widgetStyle = runBlocking { WidgetPreferences.getStyle(context) }
    val styleColors = resolveStyle(widgetStyle)

    val todayKey = DateUtils.todayKey()
    val tomorrowKey = DateUtils.tomorrowKey()
    val todayNameDay = repository.getNameDay(todayKey)
    val tomorrowNameDay = repository.getNameDay(tomorrowKey)

    val todayNames = if (useSwedish) todayNameDay.sv else todayNameDay.fi
    val tomorrowNames = if (useSwedish) tomorrowNameDay.sv else tomorrowNameDay.fi

    val dateText = DateUtils.formatDateFinnish()
    val etymologyRepo = EtymologyRepository(context)

    WidgetLayout(
        isFlipped = isFlipped,
        dateText = dateText,
        todayNames = todayNames,
        tomorrowNames = tomorrowNames,
        styleColors = styleColors,
        etymologyOf = etymologyRepo::getEtymology
    )
}

/**
 * Everything below data loading; split out so the preview test can render the
 * widget with arbitrary names (e.g. a day with many names).
 */
@Composable
internal fun WidgetLayout(
    isFlipped: Boolean,
    dateText: String,
    todayNames: List<String>,
    tomorrowNames: List<String>,
    styleColors: WidgetStyleColors,
    etymologyOf: (String) -> String?
) {
    val size = LocalSize.current

    GlanceTheme {
        val hasDrawableBackground = styleColors.backgroundDrawableRes != null
        // Below the large breakpoint the etymology screen trades 4dp of vertical
        // padding for content. The pastel drawables' inner panel is inset 8dp
        // (under a 4dp border), so 12dp still keeps all text on the panel.
        val compactEtymology = isFlipped && size.height < LARGE_MIN_HEIGHT
        val horizontalPadding = if (hasDrawableBackground) 16.dp else 12.dp
        val verticalPadding =
            if (compactEtymology) horizontalPadding - 4.dp else horizontalPadding
        val bgModifier = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
            .cornerRadius(if (hasDrawableBackground) 24.dp else 16.dp)
            .let { mod ->
                when {
                    styleColors.isMaterialYou -> mod.background(GlanceTheme.colors.widgetBackground)
                    styleColors.backgroundDrawableRes != null ->
                        mod.background(ImageProvider(styleColors.backgroundDrawableRes))
                    styleColors.backgroundColor != null ->
                        mod.background(styleColors.backgroundColor)
                    else -> mod.background(GlanceTheme.colors.widgetBackground)
                }
            }
        Column(
            modifier = bgModifier,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFlipped) {
                FlippedWidget(
                    entries = todayNames.map { it to etymologyOf(it) },
                    styleColors = styleColors,
                    compact = compactEtymology
                )
            } else {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionRunCallback<RefreshAction>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        size.height < 100.dp -> SmallWidget(dateText, todayNames, styleColors)
                        size.height < LARGE_MIN_HEIGHT -> MediumWidget(dateText, todayNames, tomorrowNames, styleColors)
                        else -> LargeWidget(dateText, todayNames, tomorrowNames, styleColors)
                    }
                }
            }
        }
    }
}

private val LARGE_MIN_HEIGHT = 180.dp

@Composable
private fun primaryTextColor(styleColors: WidgetStyleColors) =
    if (styleColors.isMaterialYou) GlanceTheme.colors.onSurface
    else ColorProvider(styleColors.primaryTextColor!!, styleColors.primaryTextColor!!)

@Composable
private fun secondaryTextColor(styleColors: WidgetStyleColors) =
    if (styleColors.isMaterialYou) GlanceTheme.colors.onSurface
    else ColorProvider(styleColors.secondaryTextColor!!, styleColors.secondaryTextColor!!)

@Composable
private fun accentColor(styleColors: WidgetStyleColors) =
    if (styleColors.isMaterialYou) GlanceTheme.colors.primary
    else ColorProvider(styleColors.accentColor!!, styleColors.accentColor!!)

@Composable
private fun SmallWidget(dateText: String, names: List<String>, styleColors: WidgetStyleColors) {
    Text(
        text = names.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = primaryTextColor(styleColors),
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 2
    )
}

@Composable
private fun MediumWidget(
    dateText: String,
    todayNames: List<String>,
    tomorrowNames: List<String>,
    styleColors: WidgetStyleColors
) {
    Text(
        text = todayNames.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = primaryTextColor(styleColors),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 2
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    Text(
        text = "Huomenna",
        style = TextStyle(
            color = secondaryTextColor(styleColors),
            fontSize = 11.sp
        )
    )
    Text(
        text = tomorrowNames.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = primaryTextColor(styleColors),
            fontSize = 14.sp
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    EtymologyButton(styleColors)
}

@Composable
private fun LargeWidget(
    dateText: String,
    todayNames: List<String>,
    tomorrowNames: List<String>,
    styleColors: WidgetStyleColors
) {
    val dayOfWeek = DateUtils.dayOfWeekFinnish()

    Text(
        text = "$dayOfWeek $dateText",
        style = TextStyle(
            color = secondaryTextColor(styleColors),
            fontSize = 12.sp
        )
    )
    Spacer(modifier = GlanceModifier.height(4.dp))
    Text(
        text = todayNames.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = primaryTextColor(styleColors),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 2
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Huomenna: ",
            style = TextStyle(
                color = secondaryTextColor(styleColors),
                fontSize = 12.sp
            )
        )
        Text(
            text = tomorrowNames.joinToString(", ").ifEmpty { "\u2014" },
            style = TextStyle(
                color = primaryTextColor(styleColors),
                fontSize = 13.sp
            ),
            maxLines = 1
        )
    }
    Spacer(modifier = GlanceModifier.height(8.dp))
    EtymologyButton(styleColors)
}

@Composable
private fun EtymologyButton(styleColors: WidgetStyleColors) {
    Text(
        text = "Etymologia \u203A",
        style = TextStyle(
            color = accentColor(styleColors),
            fontSize = 11.sp
        ),
        modifier = GlanceModifier.clickable(actionRunCallback<FlipAction>())
    )
}

/**
 * Etymology screen. The back link sits in the header row so it is always
 * visible, and the etymologies live in a LazyColumn that takes the remaining
 * height: launchers scroll it when a day has more names than fit (days can
 * have up to ~9 names) instead of clipping entries and the back link.
 */
@Composable
private fun FlippedWidget(
    entries: List<Pair<String, String?>>,
    styleColors: WidgetStyleColors,
    compact: Boolean
) {
    // Below the large breakpoint everything is a notch tighter, so a typical
    // two-name day fits a ~120dp tall widget without scrolling.
    val headerGap = if (compact) 2.dp else 6.dp
    val itemGap = if (compact) 3.dp else 6.dp
    val headerSize = if (compact) 11.sp else 12.sp
    val nameSize = if (compact) 12.sp else 13.sp
    Column(modifier = GlanceModifier.fillMaxSize()) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Etymologia",
                style = TextStyle(
                    color = primaryTextColor(styleColors),
                    fontSize = headerSize,
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 1,
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                text = "\u2039 Takaisin",
                style = TextStyle(
                    color = accentColor(styleColors),
                    fontSize = 11.sp
                ),
                maxLines = 1,
                modifier = GlanceModifier
                    .padding(start = 8.dp)
                    .clickable(actionRunCallback<FlipAction>())
            )
        }
        Spacer(modifier = GlanceModifier.height(headerGap))
        val items = entries.ifEmpty { listOf("\u2014" to null) }
        LazyColumn(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
            itemsIndexed(items) { index, (name, etymology) ->
                Column(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = if (index == 0) 0.dp else itemGap)
                ) {
                    Text(
                        text = name,
                        style = TextStyle(
                            color = primaryTextColor(styleColors),
                            fontSize = nameSize,
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1
                    )
                    Text(
                        text = etymology ?: "\u2014",
                        style = TextStyle(
                            color = secondaryTextColor(styleColors),
                            fontSize = 11.sp
                        ),
                        maxLines = 3
                    )
                }
            }
        }
    }
}

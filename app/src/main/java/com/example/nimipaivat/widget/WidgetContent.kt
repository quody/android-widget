package com.example.nimipaivat.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.ColorFilter
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
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
    val pastelGlass = runBlocking { WidgetPreferences.isPastelGlass(context) }
    val wavyEdge = runBlocking { WidgetPreferences.isWavyEdge(context) }
    val styleColors = resolveStyle(widgetStyle, pastelGlass)

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
        etymologyOf = etymologyRepo::getEtymology,
        wavyEdge = wavyEdge
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
    etymologyOf: (String) -> String?,
    wavyEdge: Boolean = false
) {
    val size = LocalSize.current
    val context = LocalContext.current

    GlanceTheme {
        val hasDrawableBackground = styleColors.backgroundDrawableRes != null
        // Below the large breakpoint the etymology screen trades 2dp of vertical
        // padding for content. The pastel drawables' inner panel is inset 8dp
        // (under a 3-4dp border) with 18dp corners, so 12dp still keeps all text
        // on the panel.
        val compactEtymology = isFlipped && size.height < LARGE_MIN_HEIGHT
        val wavyExtra = if (wavyEdge) WavyEdge.CONTENT_EXTRA_PADDING else 0.dp
        val basePadding = if (hasDrawableBackground) 14.dp else 10.dp
        val horizontalPadding = basePadding + wavyExtra
        // A small (< 100dp) widget has exactly room for two 18sp name lines, so
        // there the wave only gets extra side padding: the troughs (and the
        // pastel panel's wavy edge, ~12dp in) still stay clear of the text.
        val verticalExtra = if (size.height < SMALL_MAX_HEIGHT) 0.dp else wavyExtra
        val verticalPadding =
            (if (compactEtymology) basePadding - 2.dp else basePadding) + verticalExtra
        val base = GlanceModifier
            .fillMaxSize()
            .padding(horizontal = horizontalPadding, vertical = verticalPadding)
        val bgModifier = if (wavyEdge) {
            // The wavy outline is painted into a bitmap at the widget's real size
            // (SizeMode.Exact); no cornerRadius clip, it would cut the crests.
            val bitmap = WavyEdge.render(
                size, context.resources.displayMetrics.density, styleColors.wavyPainter
            )
            when (val painter = styleColors.wavyPainter) {
                is WavyPainter.Solid -> base.background(
                    ImageProvider(bitmap),
                    colorFilter = ColorFilter.tint(
                        painter.color?.let { ColorProvider(it, it) }
                            ?: GlanceTheme.colors.widgetBackground
                    )
                )
                else -> base.background(ImageProvider(bitmap))
            }
        } else {
            base
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
                        size.height < SMALL_MAX_HEIGHT -> SmallWidget(dateText, todayNames, styleColors)
                        size.height < LARGE_MIN_HEIGHT -> MediumWidget(dateText, todayNames, tomorrowNames, styleColors)
                        else -> LargeWidget(dateText, todayNames, tomorrowNames, styleColors)
                    }
                }
            }
        }
    }
}

private val SMALL_MAX_HEIGHT = 100.dp
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
            fontSize = 18.sp,
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
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 2
    )
    // "Huomenna: ..." and the Etymologia link share one row, so the bigger
    // names still fit a ~120dp tall widget (also with the wavy edge's padding).
    Spacer(modifier = GlanceModifier.height(6.dp))
    TomorrowRow(
        tomorrowNames, styleColors, labelSize = 11.sp, namesSize = 14.sp,
        trailingEtymologyLink = true
    )
}

@Composable
private fun TomorrowRow(
    tomorrowNames: List<String>,
    styleColors: WidgetStyleColors,
    labelSize: TextUnit,
    namesSize: TextUnit,
    trailingEtymologyLink: Boolean = false
) {
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Huomenna: ",
            style = TextStyle(
                color = secondaryTextColor(styleColors),
                fontSize = labelSize
            )
        )
        Text(
            text = tomorrowNames.joinToString(", ").ifEmpty { "\u2014" },
            style = TextStyle(
                color = primaryTextColor(styleColors),
                fontSize = namesSize
            ),
            maxLines = 1,
            modifier = if (trailingEtymologyLink) GlanceModifier.defaultWeight() else GlanceModifier
        )
        if (trailingEtymologyLink) {
            Spacer(modifier = GlanceModifier.width(8.dp))
            EtymologyButton(styleColors)
        }
    }
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
    Spacer(modifier = GlanceModifier.height(2.dp))
    Text(
        text = todayNames.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = primaryTextColor(styleColors),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 2
    )
    Spacer(modifier = GlanceModifier.height(6.dp))
    TomorrowRow(tomorrowNames, styleColors, labelSize = 12.sp, namesSize = 13.sp)
    Spacer(modifier = GlanceModifier.height(6.dp))
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
    val nameSize = if (compact) 13.sp else 15.sp
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

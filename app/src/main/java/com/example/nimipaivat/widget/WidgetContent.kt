package com.example.nimipaivat.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
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
    val size = LocalSize.current
    val prefs = currentState<Preferences>()
    val isFlipped = prefs[FlipAction.FLIPPED_KEY] ?: false

    val repository = NameDayRepository(context)
    val useSwedish = runBlocking { WidgetPreferences.isSwedish(context) }

    val todayKey = DateUtils.todayKey()
    val tomorrowKey = DateUtils.tomorrowKey()
    val todayNameDay = repository.getNameDay(todayKey)
    val tomorrowNameDay = repository.getNameDay(tomorrowKey)

    val todayNames = if (useSwedish) todayNameDay.sv else todayNameDay.fi
    val tomorrowNames = if (useSwedish) tomorrowNameDay.sv else tomorrowNameDay.fi

    val dateText = DateUtils.formatDateFinnish()

    GlanceTheme {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(12.dp)
                .cornerRadius(16.dp)
                .background(GlanceTheme.colors.widgetBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isFlipped) {
                val etymologyRepo = EtymologyRepository(context)
                FlippedWidget(todayNames, etymologyRepo)
            } else {
                Column(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .clickable(actionRunCallback<RefreshAction>()),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when {
                        size.width < 180.dp -> SmallWidget(dateText, todayNames)
                        size.width < 250.dp -> MediumWidget(dateText, todayNames, tomorrowNames)
                        else -> LargeWidget(dateText, todayNames, tomorrowNames)
                    }
                }
            }
        }
    }
}

@Composable
private fun SmallWidget(dateText: String, names: List<String>) {
    Text(
        text = dateText,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 12.sp
        )
    )
    Text(
        text = names.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
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
    tomorrowNames: List<String>
) {
    Text(
        text = dateText,
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 12.sp
        )
    )
    Text(
        text = todayNames.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        ),
        maxLines = 2
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    Text(
        text = "Huomenna",
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 11.sp
        )
    )
    Text(
        text = tomorrowNames.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 14.sp
        ),
        maxLines = 1
    )
    Spacer(modifier = GlanceModifier.height(8.dp))
    EtymologyButton()
}

@Composable
private fun LargeWidget(
    dateText: String,
    todayNames: List<String>,
    tomorrowNames: List<String>
) {
    val dayOfWeek = DateUtils.dayOfWeekFinnish()
    val weekNumber = DateUtils.weekNumber()

    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$dayOfWeek $dateText",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp
            )
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        Text(
            text = "vko $weekNumber",
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 11.sp
            )
        )
    }
    Spacer(modifier = GlanceModifier.height(4.dp))
    Text(
        text = todayNames.joinToString(", ").ifEmpty { "\u2014" },
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
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
                color = GlanceTheme.colors.onSurface,
                fontSize = 12.sp
            )
        )
        Text(
            text = tomorrowNames.joinToString(", ").ifEmpty { "\u2014" },
            style = TextStyle(
                color = GlanceTheme.colors.onSurface,
                fontSize = 13.sp
            ),
            maxLines = 1
        )
    }
    Spacer(modifier = GlanceModifier.height(8.dp))
    EtymologyButton()
}

@Composable
private fun EtymologyButton() {
    Text(
        text = "Etymologia \u203A",
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = 11.sp
        ),
        modifier = GlanceModifier.clickable(actionRunCallback<FlipAction>())
    )
}

@Composable
private fun FlippedWidget(
    names: List<String>,
    etymologyRepo: EtymologyRepository
) {
    Text(
        text = "Etymologia",
        style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    )
    Spacer(modifier = GlanceModifier.height(6.dp))
    names.forEachIndexed { index, name ->
        val etymology = etymologyRepo.getEtymology(name)
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = name,
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            Spacer(modifier = GlanceModifier.width(6.dp))
            Text(
                text = etymology ?: "\u2014",
                style = TextStyle(
                    color = GlanceTheme.colors.onSurface,
                    fontSize = 11.sp
                ),
                maxLines = 3
            )
        }
        if (index < names.size - 1) {
            Spacer(modifier = GlanceModifier.height(4.dp))
        }
    }
    Spacer(modifier = GlanceModifier.height(8.dp))
    Text(
        text = "\u2039 Takaisin",
        style = TextStyle(
            color = GlanceTheme.colors.primary,
            fontSize = 11.sp
        ),
        modifier = GlanceModifier.clickable(actionRunCallback<FlipAction>())
    )
}

package com.example.nimipaivat.util

import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale

object DateUtils {

    /**
     * Clock used for "today". Only replaced in tests (e.g. the widget
     * preview screenshot test) to render a fixed date.
     */
    @Volatile
    internal var clock: Clock = Clock.systemDefaultZone()

    private val keyFormatter = DateTimeFormatter.ofPattern("MM-dd")

    private val finnishDayNames = arrayOf(
        "Maanantai", "Tiistai", "Keskiviikko", "Torstai",
        "Perjantai", "Lauantai", "Sunnuntai"
    )

    /**
     * Returns today's date as a "MM-dd" key for JSON lookup.
     */
    fun todayKey(date: LocalDate = LocalDate.now(clock)): String {
        return date.format(keyFormatter)
    }

    /**
     * Returns tomorrow's date as a "MM-dd" key for JSON lookup.
     */
    fun tomorrowKey(date: LocalDate = LocalDate.now(clock)): String {
        return date.plusDays(1).format(keyFormatter)
    }

    /**
     * Returns a formatted date string like "10.2." (day.month.)
     */
    fun formatDateFinnish(date: LocalDate = LocalDate.now(clock)): String {
        return "${date.dayOfMonth}.${date.monthValue}."
    }

    /**
     * Returns the Finnish day-of-week name.
     */
    fun dayOfWeekFinnish(date: LocalDate = LocalDate.now(clock)): String {
        return finnishDayNames[date.dayOfWeek.value - 1]
    }

    /**
     * Returns the ISO week number.
     */
    fun weekNumber(date: LocalDate = LocalDate.now(clock)): Int {
        val weekFields = WeekFields.of(Locale("fi", "FI"))
        return date.get(weekFields.weekOfWeekBasedYear())
    }
}

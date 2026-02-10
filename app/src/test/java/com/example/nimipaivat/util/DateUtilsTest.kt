package com.example.nimipaivat.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class DateUtilsTest {

    @Test
    fun `todayKey formats date correctly`() {
        val date = LocalDate.of(2026, 2, 10)
        assertEquals("02-10", DateUtils.todayKey(date))
    }

    @Test
    fun `todayKey formats single digit month`() {
        val date = LocalDate.of(2026, 1, 5)
        assertEquals("01-05", DateUtils.todayKey(date))
    }

    @Test
    fun `todayKey formats december date`() {
        val date = LocalDate.of(2026, 12, 25)
        assertEquals("12-25", DateUtils.todayKey(date))
    }

    @Test
    fun `tomorrowKey returns next day`() {
        val date = LocalDate.of(2026, 2, 10)
        assertEquals("02-11", DateUtils.tomorrowKey(date))
    }

    @Test
    fun `tomorrowKey handles month boundary`() {
        val date = LocalDate.of(2026, 1, 31)
        assertEquals("02-01", DateUtils.tomorrowKey(date))
    }

    @Test
    fun `tomorrowKey handles year boundary`() {
        val date = LocalDate.of(2026, 12, 31)
        assertEquals("01-01", DateUtils.tomorrowKey(date))
    }

    @Test
    fun `formatDateFinnish formats correctly`() {
        val date = LocalDate.of(2026, 2, 10)
        assertEquals("10.2.", DateUtils.formatDateFinnish(date))
    }

    @Test
    fun `formatDateFinnish december`() {
        val date = LocalDate.of(2026, 12, 25)
        assertEquals("25.12.", DateUtils.formatDateFinnish(date))
    }

    @Test
    fun `dayOfWeekFinnish returns correct day`() {
        // 2026-02-10 is a Tuesday
        val date = LocalDate.of(2026, 2, 10)
        assertEquals("Tiistai", DateUtils.dayOfWeekFinnish(date))
    }

    @Test
    fun `dayOfWeekFinnish monday`() {
        val date = LocalDate.of(2026, 2, 9)
        assertEquals("Maanantai", DateUtils.dayOfWeekFinnish(date))
    }

    @Test
    fun `dayOfWeekFinnish sunday`() {
        val date = LocalDate.of(2026, 2, 15)
        assertEquals("Sunnuntai", DateUtils.dayOfWeekFinnish(date))
    }

    @Test
    fun `weekNumber returns correct week`() {
        val date = LocalDate.of(2026, 2, 10)
        val weekNum = DateUtils.weekNumber(date)
        assertEquals(7, weekNum)
    }

    @Test
    fun `weekNumber first week of year`() {
        val date = LocalDate.of(2026, 1, 1)
        val weekNum = DateUtils.weekNumber(date)
        assertEquals(1, weekNum)
    }
}

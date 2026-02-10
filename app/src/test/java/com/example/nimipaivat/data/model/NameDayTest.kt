package com.example.nimipaivat.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NameDayTest {

    @Test
    fun `default NameDay has empty lists`() {
        val nameDay = NameDay()
        assertTrue(nameDay.fi.isEmpty())
        assertTrue(nameDay.sv.isEmpty())
    }

    @Test
    fun `NameDay stores Finnish names`() {
        val nameDay = NameDay(fi = listOf("Elina", "Elena"), sv = listOf("Iris"))
        assertEquals(listOf("Elina", "Elena"), nameDay.fi)
        assertEquals(listOf("Iris"), nameDay.sv)
    }

    @Test
    fun `NameDay copy works correctly`() {
        val original = NameDay(fi = listOf("Aatos"), sv = listOf("Nyårsdagen"))
        val copy = original.copy(fi = listOf("Aatos", "Aatto"))
        assertEquals(2, copy.fi.size)
        assertEquals("Nyårsdagen", copy.sv[0])
    }
}

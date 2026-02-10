package com.example.nimipaivat.data.model

/**
 * Represents name day entries for a specific date.
 * @param fi Finnish name day names
 * @param sv Swedish-Finnish name day names
 */
data class NameDay(
    val fi: List<String> = emptyList(),
    val sv: List<String> = emptyList()
)

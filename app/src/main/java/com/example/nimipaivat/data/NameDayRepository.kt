package com.example.nimipaivat.data

import android.content.Context
import com.example.nimipaivat.data.model.NameDay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class NameDayRepository(private val context: Context) {

    private val nameDays: Map<String, NameDay> by lazy { loadNameDays() }

    private fun loadNameDays(): Map<String, NameDay> {
        val inputStream = context.assets.open("namedays.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, NameDay>>() {}.type
        return Gson().fromJson<Map<String, NameDay>>(reader, type).also {
            reader.close()
        }
    }

    /**
     * Get name day for a given date key in "MM-dd" format.
     */
    fun getNameDay(dateKey: String): NameDay {
        return nameDays[dateKey] ?: NameDay()
    }

    /**
     * Get Finnish names for a given date key.
     */
    fun getFinnishNames(dateKey: String): List<String> {
        return getNameDay(dateKey).fi
    }

    /**
     * Get Swedish-Finnish names for a given date key.
     */
    fun getSwedishNames(dateKey: String): List<String> {
        return getNameDay(dateKey).sv
    }
}

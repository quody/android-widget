package com.example.nimipaivat.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader

class EtymologyRepository(private val context: Context) {

    private val etymologies: Map<String, String> by lazy { loadEtymologies() }

    private fun loadEtymologies(): Map<String, String> {
        val inputStream = context.assets.open("etymologies.json")
        val reader = InputStreamReader(inputStream)
        val type = object : TypeToken<Map<String, String>>() {}.type
        return Gson().fromJson<Map<String, String>>(reader, type).also {
            reader.close()
        }
    }

    fun getEtymology(name: String): String? {
        return etymologies[name]
    }
}

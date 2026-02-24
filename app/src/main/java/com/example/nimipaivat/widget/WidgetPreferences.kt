package com.example.nimipaivat.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

object WidgetPreferences {

    private val USE_SWEDISH = booleanPreferencesKey("use_swedish")
    private val STYLE_KEY = stringPreferencesKey("widget_style")

    suspend fun isSwedish(context: Context): Boolean {
        return context.dataStore.data.map { prefs ->
            prefs[USE_SWEDISH] ?: false
        }.first()
    }

    suspend fun setSwedish(context: Context, useSwedish: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[USE_SWEDISH] = useSwedish
        }
    }

    suspend fun getStyle(context: Context): WidgetStyle {
        val name = context.dataStore.data.map { prefs ->
            prefs[STYLE_KEY]
        }.first()
        return try {
            if (name != null) WidgetStyle.valueOf(name) else WidgetStyle.CLASSIC
        } catch (_: IllegalArgumentException) {
            WidgetStyle.CLASSIC
        }
    }

    suspend fun setStyle(context: Context, style: WidgetStyle) {
        context.dataStore.edit { prefs ->
            prefs[STYLE_KEY] = style.name
        }
    }
}

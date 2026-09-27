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
    private val PASTEL_GLASS_KEY = booleanPreferencesKey("pastel_glass")
    private val WAVY_EDGE_KEY = booleanPreferencesKey("wavy_edge")

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
            when (name) {
                null -> WidgetStyle.MATERIAL_YOU
                "FROSTED_GLASS" -> WidgetStyle.GLASS_LIGHT
                else -> WidgetStyle.valueOf(name)
            }
        } catch (_: IllegalArgumentException) {
            WidgetStyle.MATERIAL_YOU
        }
    }

    suspend fun setStyle(context: Context, style: WidgetStyle) {
        context.dataStore.edit { prefs ->
            prefs[STYLE_KEY] = style.name
        }
    }

    /** Glass (translucent) instead of opaque card for the pastel styles. Default: opaque. */
    suspend fun isPastelGlass(context: Context): Boolean {
        return context.dataStore.data.map { prefs -> prefs[PASTEL_GLASS_KEY] ?: false }.first()
    }

    suspend fun setPastelGlass(context: Context, glass: Boolean) {
        context.dataStore.edit { prefs -> prefs[PASTEL_GLASS_KEY] = glass }
    }

    /** Scalloped Material 3 Expressive widget outline, any style. Default: off. */
    suspend fun isWavyEdge(context: Context): Boolean {
        return context.dataStore.data.map { prefs -> prefs[WAVY_EDGE_KEY] ?: false }.first()
    }

    suspend fun setWavyEdge(context: Context, wavy: Boolean) {
        context.dataStore.edit { prefs -> prefs[WAVY_EDGE_KEY] = wavy }
    }
}

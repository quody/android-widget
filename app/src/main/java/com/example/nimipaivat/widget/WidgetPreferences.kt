package com.example.nimipaivat.widget

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "widget_prefs")

object WidgetPreferences {

    private val USE_SWEDISH = booleanPreferencesKey("use_swedish")

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
}

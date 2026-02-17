package com.example.alris.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension for DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

enum class AppTheme(val value: Int) {
    SYSTEM(0),
    LIGHT(1),
    DARK(2);

    companion object {
        fun fromValue(value: Int): AppTheme = entries.find { it.value == value } ?: SYSTEM
    }
}

class ThemeManager(private val context: Context) {

    private val THEME_KEY = intPreferencesKey("app_theme")

    val themeFlow: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            val themeValue = preferences[THEME_KEY] ?: AppTheme.SYSTEM.value
            AppTheme.fromValue(themeValue)
        }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = theme.value
        }
    }
}

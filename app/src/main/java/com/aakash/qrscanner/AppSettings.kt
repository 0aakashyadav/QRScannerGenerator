package com.aakash.qrscanner

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

object AppSettings {
    private val THEME = stringPreferencesKey("theme_mode")

    fun themeFlow(context: Context): Flow<ThemeMode> = context.settingsDataStore.data.map { prefs ->
        runCatching { ThemeMode.valueOf(prefs[THEME] ?: ThemeMode.SYSTEM.name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun setTheme(context: Context, mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME] = mode.name }
    }
}

package com.athenacoree.music

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.musicDataStore by preferencesDataStore(name = "music_preferences")

internal object MusicPreferences {
    private val DARK_MODE = booleanPreferencesKey("dark_mode")
    private val DATA_SAVER = booleanPreferencesKey("data_saver")

    fun darkMode(context: Context): Flow<Boolean> =
        context.musicDataStore.data.map { preferences -> preferences[DARK_MODE] ?: false }

    fun dataSaver(context: Context): Flow<Boolean> =
        context.musicDataStore.data.map { preferences -> preferences[DATA_SAVER] ?: false }

    suspend fun setDarkMode(context: Context, enabled: Boolean) {
        context.musicDataStore.edit { it[DARK_MODE] = enabled }
    }

    suspend fun setDataSaver(context: Context, enabled: Boolean) {
        context.musicDataStore.edit { it[DATA_SAVER] = enabled }
    }
}

package com.example.healthconnect.codelab.presentation

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

const val PREFERENCES_NAME = "HealthConnectSync"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

class Preferences(private val context: Context) {

    private val startDateKey = longPreferencesKey("start_date")

    suspend fun writeToDataStore(value: Long) {
        context.dataStore.edit { preferences ->
            preferences[startDateKey] = value
        }
    }

    suspend fun readFromDataStore(): Long {
        val preferences = context.dataStore.data.first()
        return preferences[startDateKey] ?: 0L
    }
}
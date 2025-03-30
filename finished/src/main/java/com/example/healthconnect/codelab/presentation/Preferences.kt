package com.example.healthconnect.codelab.presentation

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

const val PREFERENCES_NAME = "HealthConnectSync"

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

class Preferences(private val context: Context) {

    suspend fun <T> writeToDataStore(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences ->
            Log.d(TAG, "writeToDataStore: $value")
            preferences[key] = value
        }
    }

    suspend fun <T> readFromDataStore(key: Preferences.Key<T>): T? {
        val preferences = context.dataStore.data.first()
        Log.d(TAG, "readFromDataStore: ${preferences[key]}")
        return preferences[key]
    }

    companion object {
        val START_DATE = longPreferencesKey("start_date")
        private const val TAG = "Preferences"
    }
}
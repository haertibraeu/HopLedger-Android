package com.haertibraeu.hopledger.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    companion object {
        private val BACKEND_URL_KEY = stringPreferencesKey("backend_url")
        private val API_KEY_KEY = stringPreferencesKey("api_key")
    }

    val backendUrl: Flow<String> = dataStore.data.map { it[BACKEND_URL_KEY] ?: "http://10.0.2.2:3000" }
    val apiKey: Flow<String> = dataStore.data.map { it[API_KEY_KEY] ?: "" }

    suspend fun setBackendUrl(url: String) {
        dataStore.edit { it[BACKEND_URL_KEY] = url }
    }

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[API_KEY_KEY] = key }
    }
}

package com.haertibraeu.hopledger.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.haertibraeu.hopledger.data.model.BackendProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    private val json = Json { ignoreUnknownKeys = true }

    companion object {
        private val BACKEND_URL_KEY = stringPreferencesKey("backend_url")
        private val API_KEY_KEY = stringPreferencesKey("api_key")
        private val BACKEND_PROFILES_KEY = stringPreferencesKey("backend_profiles")
        const val DEFAULT_BACKEND_URL = "http://10.0.2.2:3000"
    }

    val backendUrl: Flow<String> = dataStore.data.map { it[BACKEND_URL_KEY] ?: DEFAULT_BACKEND_URL }
    val apiKey: Flow<String> = dataStore.data.map { it[API_KEY_KEY] ?: "" }
    val backendProfiles: Flow<List<BackendProfile>> = dataStore.data.map { prefs ->
        val jsonString = prefs[BACKEND_PROFILES_KEY] ?: "[]"
        try {
            json.decodeFromString<List<BackendProfile>>(jsonString)
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun setBackendUrl(url: String) {
        dataStore.edit { it[BACKEND_URL_KEY] = url }
    }

    suspend fun setApiKey(key: String) {
        dataStore.edit { it[API_KEY_KEY] = key }
    }

    suspend fun addBackendProfile(name: String, url: String, apiKey: String) {
        dataStore.edit { prefs ->
            val currentProfiles = json.decodeFromString<List<BackendProfile>>(prefs[BACKEND_PROFILES_KEY] ?: "[]")
            val newProfile = BackendProfile(UUID.randomUUID().toString(), name, url, apiKey)
            prefs[BACKEND_PROFILES_KEY] = json.encodeToString(currentProfiles + newProfile)
        }
    }

    suspend fun deleteBackendProfile(id: String) {
        dataStore.edit { prefs ->
            val currentProfiles = json.decodeFromString<List<BackendProfile>>(prefs[BACKEND_PROFILES_KEY] ?: "[]")
            prefs[BACKEND_PROFILES_KEY] = json.encodeToString(currentProfiles.filter { it.id != id })
        }
    }

    suspend fun selectBackendProfile(profile: BackendProfile) {
        dataStore.edit { prefs ->
            prefs[BACKEND_URL_KEY] = profile.url
            prefs[API_KEY_KEY] = profile.apiKey
        }
    }
}

package com.haertibraeu.hopledger.data.api

import com.haertibraeu.hopledger.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that dynamically replaces the base URL of outgoing requests
 * with the one configured in the app settings.
 */
@Singleton
class DynamicBaseUrlInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()

        // Retrieve the current backend URL from DataStore
        // Using runBlocking is acceptable here as it's a small local read on a background thread
        val backendUrl = runBlocking { settingsRepository.backendUrl.first() }

        val newBaseUrl = backendUrl.toHttpUrlOrNull() ?: return chain.proceed(originalRequest)

        val newUrl = originalRequest.url.newBuilder()
            .scheme(newBaseUrl.scheme)
            .host(newBaseUrl.host)
            .port(newBaseUrl.port)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        return chain.proceed(newRequest)
    }
}

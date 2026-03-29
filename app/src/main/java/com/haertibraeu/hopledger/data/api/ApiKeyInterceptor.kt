package com.haertibraeu.hopledger.data.api

import com.haertibraeu.hopledger.data.repository.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interceptor that attaches the configured API key as the `x-api-key` header
 * on every outgoing request. If no key is configured the header is omitted.
 */
@Singleton
class ApiKeyInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val apiKey = runBlocking { settingsRepository.apiKey.first() }
        val request = if (apiKey.isNotBlank()) {
            chain.request().newBuilder()
                .header("x-api-key", apiKey)
                .build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

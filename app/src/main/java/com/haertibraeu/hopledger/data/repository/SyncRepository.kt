package com.haertibraeu.hopledger.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

sealed class SyncStatus {
    object Idle : SyncStatus()
    object Syncing : SyncStatus()
    object Success : SyncStatus()
    data class Error(val message: String) : SyncStatus()
}

@Singleton
class SyncRepository @Inject constructor() {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private val activeCount = AtomicInteger(0)

    fun startSync() {
        if (activeCount.incrementAndGet() == 1) _status.value = SyncStatus.Syncing
    }

    fun endSync(error: String? = null) {
        if (activeCount.decrementAndGet() <= 0) {
            activeCount.set(0)
            _status.value = if (error != null) SyncStatus.Error(error) else SyncStatus.Success
        }
    }
}

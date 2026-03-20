package com.haertibraeu.hopledger.ui

import androidx.lifecycle.ViewModel
import com.haertibraeu.hopledger.data.repository.SyncRepository
import com.haertibraeu.hopledger.data.repository.SyncStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    syncRepository: SyncRepository,
) : ViewModel() {
    val syncStatus: StateFlow<SyncStatus> = syncRepository.status
}

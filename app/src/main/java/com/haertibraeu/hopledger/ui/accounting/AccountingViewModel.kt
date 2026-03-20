package com.haertibraeu.hopledger.ui.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.*
import com.haertibraeu.hopledger.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AccountingUiState(
    val balances: List<Balance> = emptyList(),
    val entries: List<AccountEntry> = emptyList(),
    val settlements: List<Settlement> = emptyList(),
    val brewers: List<Brewer> = emptyList(),
    val selectedBrewerId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showSettlements: Boolean = false,
    val showManualEntryDialog: Boolean = false,
)

@HiltViewModel
class AccountingViewModel @Inject constructor(
    private val api: HopLedgerApi,
    private val sync: SyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountingUiState())
    val uiState: StateFlow<AccountingUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            sync.startSync()
            try {
                val balances = api.getBalances()
                val brewers = api.getBrewers()
                val entriesResponse = if (_uiState.value.selectedBrewerId != null) {
                    api.getEntries(brewerId = _uiState.value.selectedBrewerId)
                } else {
                    api.getEntries()
                }
                val entries = entriesResponse.entries
                _uiState.update { it.copy(balances = balances, brewers = brewers, entries = entries, isLoading = false, error = null) }
                sync.endSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                sync.endSync(e.message)
            }
        }
    }

    fun filterByBrewer(brewerId: String?) {
        _uiState.update { it.copy(selectedBrewerId = brewerId) }
        refresh()
    }

    fun calculateSettlements() {
        viewModelScope.launch {
            try {
                val settlements = api.getSettlements()
                _uiState.update { it.copy(settlements = settlements, showSettlements = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun dismissSettlements() { _uiState.update { it.copy(showSettlements = false) } }
    fun showManualEntryDialog() { _uiState.update { it.copy(showManualEntryDialog = true) } }
    fun dismissManualEntryDialog() { _uiState.update { it.copy(showManualEntryDialog = false) } }

    fun addManualEntry(brewerId: String, amount: Double, description: String, type: String) {
        viewModelScope.launch {
            try {
                api.createEntry(EntryRequest(brewerId = brewerId, amount = amount, description = description, type = type))
                dismissManualEntryDialog()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}

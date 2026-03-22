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
    val settlements: List<Settlement> = emptyList(),
    val entries: List<AccountEntry> = emptyList(),
    val brewers: List<Brewer> = emptyList(),
    val categories: List<Category> = emptyList(),
    val selectedBrewerId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val showManualEntryDialog: Boolean = false,
    val entryToDelete: AccountEntry? = null,
    val settlementToBook: Settlement? = null,
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
                val settlements = api.getSettlements()
                val categories = api.getCategories()
                val entriesResponse = if (_uiState.value.selectedBrewerId != null) {
                    api.getEntries(brewerId = _uiState.value.selectedBrewerId)
                } else {
                    api.getEntries()
                }
                val entries = entriesResponse.entries
                _uiState.update { it.copy(balances = balances, brewers = brewers, settlements = settlements, categories = categories, entries = entries, isLoading = false, error = null) }
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

    fun showManualEntryDialog() { _uiState.update { it.copy(showManualEntryDialog = true) } }
    fun dismissManualEntryDialog() { _uiState.update { it.copy(showManualEntryDialog = false) } }

    fun addManualEntry(brewerId: String, amount: Double, description: String, type: String, categoryId: String?) {
        viewModelScope.launch {
            try {
                api.createEntry(EntryRequest(brewerId = brewerId, amount = amount, description = description, type = type, categoryId = categoryId))
                dismissManualEntryDialog()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun confirmDeleteEntry(entry: AccountEntry) {
        _uiState.update { it.copy(entryToDelete = entry) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(entryToDelete = null) }
    }

    fun deleteEntry() {
        val entry = _uiState.value.entryToDelete ?: return
        viewModelScope.launch {
            try {
                api.deleteEntry(entry.id)
                dismissDeleteDialog()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    fun confirmBookSettlement(settlement: Settlement) {
        _uiState.update { it.copy(settlementToBook = settlement) }
    }

    fun dismissSettlementDialog() {
        _uiState.update { it.copy(settlementToBook = null) }
    }

    fun bookSettlement() {
        val s = _uiState.value.settlementToBook ?: return
        viewModelScope.launch {
            try {
                // Debit the payer (they hand over cash)
                api.createEntry(EntryRequest(
                    brewerId = s.from.id,
                    amount = -s.amount,
                    type = "settlement",
                    description = "Ausgleichszahlung an ${s.to.name}",
                ))
                // Credit the receiver (they receive cash)
                api.createEntry(EntryRequest(
                    brewerId = s.to.id,
                    amount = s.amount,
                    type = "settlement",
                    description = "Ausgleichszahlung von ${s.from.name}",
                ))
                dismissSettlementDialog()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }
}

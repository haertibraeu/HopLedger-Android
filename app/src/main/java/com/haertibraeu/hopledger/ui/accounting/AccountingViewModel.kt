package com.haertibraeu.hopledger.ui.accounting

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.AccountEntry
import com.haertibraeu.hopledger.data.model.Balance
import com.haertibraeu.hopledger.data.model.Brewer
import com.haertibraeu.hopledger.data.model.Category
import com.haertibraeu.hopledger.data.model.EntryRequest
import com.haertibraeu.hopledger.data.model.Settlement
import com.haertibraeu.hopledger.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AccountingDialogAction {
    MANUAL_ENTRY,
    DELETE_ENTRY,
    BOOK_SETTLEMENT,
}

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
    val submittingAction: AccountingDialogAction? = null,
    val dialogError: String? = null,
)

@HiltViewModel
class AccountingViewModel @Inject constructor(
    private val api: HopLedgerApi,
    private val sync: SyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountingUiState())
    val uiState: StateFlow<AccountingUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

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

    fun showManualEntryDialog() {
        _uiState.update { it.copy(showManualEntryDialog = true, dialogError = null) }
    }
    fun dismissManualEntryDialog() {
        if (_uiState.value.submittingAction != null) return
        closeManualEntryDialog()
    }

    fun addManualEntry(brewerId: String, amount: Double, description: String, type: String, categoryId: String?) {
        if (!beginDialogMutation(AccountingDialogAction.MANUAL_ENTRY)) return
        viewModelScope.launch {
            try {
                api.createEntry(EntryRequest(brewerId = brewerId, amount = amount, description = description, type = type, categoryId = categoryId))
                closeManualEntryDialog()
                completeDialogMutation()
                refresh()
            } catch (e: Exception) {
                failDialogMutation(e.message)
            }
        }
    }

    fun confirmDeleteEntry(entry: AccountEntry) {
        _uiState.update { it.copy(entryToDelete = entry, dialogError = null) }
    }

    fun dismissDeleteDialog() {
        if (_uiState.value.submittingAction != null) return
        closeDeleteDialog()
    }

    fun deleteEntry() {
        val entry = _uiState.value.entryToDelete ?: return
        if (!beginDialogMutation(AccountingDialogAction.DELETE_ENTRY)) return
        viewModelScope.launch {
            try {
                api.deleteEntry(entry.id)
                closeDeleteDialog()
                completeDialogMutation()
                refresh()
            } catch (e: Exception) {
                failDialogMutation(e.message)
            }
        }
    }

    fun confirmBookSettlement(settlement: Settlement) {
        _uiState.update { it.copy(settlementToBook = settlement, dialogError = null) }
    }

    fun dismissSettlementDialog() {
        if (_uiState.value.submittingAction != null) return
        closeSettlementDialog()
    }

    fun bookSettlement() {
        val s = _uiState.value.settlementToBook ?: return
        if (!beginDialogMutation(AccountingDialogAction.BOOK_SETTLEMENT)) return
        viewModelScope.launch {
            try {
                // Debit the payer (they hand over cash)
                api.createEntry(
                    EntryRequest(
                        brewerId = s.from.id,
                        amount = -s.amount,
                        type = "settlement",
                        description = "Ausgleichszahlung an ${s.to.name}",
                    ),
                )
                // Credit the receiver (they receive cash)
                api.createEntry(
                    EntryRequest(
                        brewerId = s.to.id,
                        amount = s.amount,
                        type = "settlement",
                        description = "Ausgleichszahlung von ${s.from.name}",
                    ),
                )
                closeSettlementDialog()
                completeDialogMutation()
                refresh()
            } catch (e: Exception) {
                failDialogMutation(e.message)
            }
        }
    }

    private fun beginDialogMutation(action: AccountingDialogAction): Boolean {
        if (_uiState.value.submittingAction != null) return false
        _uiState.update { it.copy(submittingAction = action, dialogError = null) }
        sync.startSync()
        return true
    }

    private fun completeDialogMutation() {
        _uiState.update { it.copy(submittingAction = null, dialogError = null) }
        sync.endSync()
    }

    private fun failDialogMutation(message: String?) {
        _uiState.update { it.copy(submittingAction = null, dialogError = message) }
        sync.endSync(message)
    }

    private fun closeManualEntryDialog() {
        _uiState.update { it.copy(showManualEntryDialog = false, dialogError = null) }
    }

    private fun closeDeleteDialog() {
        _uiState.update { it.copy(entryToDelete = null, dialogError = null) }
    }

    private fun closeSettlementDialog() {
        _uiState.update { it.copy(settlementToBook = null, dialogError = null) }
    }
}

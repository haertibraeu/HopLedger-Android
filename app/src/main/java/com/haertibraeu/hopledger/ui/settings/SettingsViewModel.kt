package com.haertibraeu.hopledger.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.*
import com.haertibraeu.hopledger.data.repository.SettingsRepository
import com.haertibraeu.hopledger.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val backendUrl: String = SettingsRepository.DEFAULT_BACKEND_URL,
    val apiKey: String = "",
    val healthStatus: String = "",
    val healthOk: Boolean = false,
    val brewers: List<Brewer> = emptyList(),
    val beers: List<Beer> = emptyList(),
    val locations: List<Location> = emptyList(),
    val containerTypes: List<ContainerType> = emptyList(),
    val categories: List<Category> = emptyList(),
    val showAddBrewerDialog: Boolean = false,
    val showAddBeerDialog: Boolean = false,
    val showAddLocationDialog: Boolean = false,
    val showAddContainerTypeDialog: Boolean = false,
    val showAddCategoryDialog: Boolean = false,
    val error: String? = null,
    val connectionExpanded: Boolean = false,
    val showQrDialog: Boolean = false,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: HopLedgerApi,
    private val settings: SettingsRepository,
    private val sync: SyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settings.backendUrl.collect { url -> _uiState.update { it.copy(backendUrl = url) } }
        }
        viewModelScope.launch {
            settings.apiKey.collect { key -> _uiState.update { it.copy(apiKey = key) } }
        }
        refreshAll()
    }

    fun onBackendUrlChanged(url: String) { _uiState.update { it.copy(backendUrl = url) } }
    fun onApiKeyChanged(key: String) { _uiState.update { it.copy(apiKey = key) } }

    fun saveSettings() {
        viewModelScope.launch {
            settings.setBackendUrl(_uiState.value.backendUrl)
            settings.setApiKey(_uiState.value.apiKey)
        }
    }

    fun toggleConnectionExpanded() { _uiState.update { it.copy(connectionExpanded = !it.connectionExpanded) } }
    fun showQr() { _uiState.update { it.copy(showQrDialog = true) } }
    fun dismissQr() { _uiState.update { it.copy(showQrDialog = false) } }

    fun applyQrPayload(json: String) {
        val (url, apiKey) = parseQrPayload(json) ?: return
        _uiState.update { it.copy(backendUrl = url, apiKey = apiKey) }
        viewModelScope.launch {
            settings.setBackendUrl(url)
            settings.setApiKey(apiKey)
        }
    }

    fun checkHealth() {
        viewModelScope.launch {
            sync.startSync()
            try {
                val response = api.health()
                _uiState.update { it.copy(healthStatus = "✅ ${response.status} — DB: ${response.database}", healthOk = true) }
                sync.endSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(healthStatus = "❌ ${e.message}", healthOk = false) }
                sync.endSync(e.message)
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            sync.startSync()
            try {
                val brewers = api.getBrewers()
                val beers = api.getBeers()
                val locations = api.getLocations()
                val types = api.getContainerTypes()
                val categories = api.getCategories()
                _uiState.update { it.copy(brewers = brewers, beers = beers, locations = locations, containerTypes = types, categories = categories, error = null) }
                sync.endSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                sync.endSync(e.message)
            }
        }
    }

    // Dialog visibility
    fun showAddBrewer() { _uiState.update { it.copy(showAddBrewerDialog = true) } }
    fun showAddBeer() { _uiState.update { it.copy(showAddBeerDialog = true) } }
    fun showAddLocation() { _uiState.update { it.copy(showAddLocationDialog = true) } }
    fun showAddContainerType() { _uiState.update { it.copy(showAddContainerTypeDialog = true) } }
    fun showAddCategory() { _uiState.update { it.copy(showAddCategoryDialog = true) } }
    fun dismissDialogs() { _uiState.update { it.copy(showAddBrewerDialog = false, showAddBeerDialog = false, showAddLocationDialog = false, showAddContainerTypeDialog = false, showAddCategoryDialog = false) } }

    // CRUD operations
    fun addBrewer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            sync.startSync()
            try { api.createBrewer(BrewerRequest(name)); dismissDialogs(); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun deleteBrewer(id: String) {
        viewModelScope.launch {
            sync.startSync()
            try { api.deleteBrewer(id); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun addBeer(name: String, style: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            sync.startSync()
            try { api.createBeer(BeerRequest(name, style)); dismissDialogs(); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun deleteBeer(id: String) {
        viewModelScope.launch {
            sync.startSync()
            try { api.deleteBeer(id); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun addLocation(name: String, type: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            sync.startSync()
            try { api.createLocation(LocationRequest(name, type)); dismissDialogs(); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            sync.startSync()
            try { api.deleteLocation(id); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun addContainerType(name: String, externalPrice: Double, internalPrice: Double, depositFee: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            sync.startSync()
            try { api.createContainerType(ContainerTypeRequest(name, externalPrice = externalPrice, internalPrice = internalPrice, depositFee = depositFee)); dismissDialogs(); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun deleteContainerType(id: String) {
        viewModelScope.launch {
            sync.startSync()
            try { api.deleteContainerType(id); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun addCategory(name: String, type: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            sync.startSync()
            try { api.createCategory(CategoryRequest(name, type)); dismissDialogs(); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            sync.startSync()
            try { api.deleteCategory(id); sync.endSync(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) }; sync.endSync(e.message) }
        }
    }
}

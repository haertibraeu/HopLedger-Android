package com.haertibraeu.hopledger.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.*
import com.haertibraeu.hopledger.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val backendUrl: String = "http://10.0.2.2:3000",
    val apiKey: String = "",
    val healthStatus: String = "",
    val healthOk: Boolean = false,
    val brewers: List<Brewer> = emptyList(),
    val beers: List<Beer> = emptyList(),
    val locations: List<Location> = emptyList(),
    val containerTypes: List<ContainerType> = emptyList(),
    val showAddBrewerDialog: Boolean = false,
    val showAddBeerDialog: Boolean = false,
    val showAddLocationDialog: Boolean = false,
    val showAddContainerTypeDialog: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: HopLedgerApi,
    private val settings: SettingsRepository,
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

    fun checkHealth() {
        viewModelScope.launch {
            try {
                val response = api.health()
                _uiState.update { it.copy(healthStatus = "✅ ${response.status} — DB: ${response.database}", healthOk = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(healthStatus = "❌ ${e.message}", healthOk = false) }
            }
        }
    }

    fun refreshAll() {
        viewModelScope.launch {
            try {
                val brewers = api.getBrewers()
                val beers = api.getBeers()
                val locations = api.getLocations()
                val types = api.getContainerTypes()
                _uiState.update { it.copy(brewers = brewers, beers = beers, locations = locations, containerTypes = types, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Dialog visibility
    fun showAddBrewer() { _uiState.update { it.copy(showAddBrewerDialog = true) } }
    fun showAddBeer() { _uiState.update { it.copy(showAddBeerDialog = true) } }
    fun showAddLocation() { _uiState.update { it.copy(showAddLocationDialog = true) } }
    fun showAddContainerType() { _uiState.update { it.copy(showAddContainerTypeDialog = true) } }
    fun dismissDialogs() { _uiState.update { it.copy(showAddBrewerDialog = false, showAddBeerDialog = false, showAddLocationDialog = false, showAddContainerTypeDialog = false) } }

    // CRUD operations
    fun addBrewer(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try { api.createBrewer(BrewerRequest(name)); dismissDialogs(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteBrewer(id: String) {
        viewModelScope.launch {
            try { api.deleteBrewer(id); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun addBeer(name: String, style: String?) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try { api.createBeer(BeerRequest(name, style)); dismissDialogs(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteBeer(id: String) {
        viewModelScope.launch {
            try { api.deleteBeer(id); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun addLocation(name: String, type: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try { api.createLocation(LocationRequest(name, type)); dismissDialogs(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            try { api.deleteLocation(id); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun addContainerType(name: String, externalPrice: Double, internalPrice: Double, depositFee: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            try { api.createContainerType(ContainerTypeRequest(name, externalPrice = externalPrice, internalPrice = internalPrice, depositFee = depositFee)); dismissDialogs(); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteContainerType(id: String) {
        viewModelScope.launch {
            try { api.deleteContainerType(id); refreshAll() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }
}

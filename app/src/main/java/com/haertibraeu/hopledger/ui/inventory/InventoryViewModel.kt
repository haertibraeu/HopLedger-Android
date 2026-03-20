package com.haertibraeu.hopledger.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InventoryUiState(
    val containers: List<Container> = emptyList(),
    val brewers: List<Brewer> = emptyList(),
    val beers: List<Beer> = emptyList(),
    val locations: List<Location> = emptyList(),
    val containerTypes: List<ContainerType> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    // Filters
    val filterEmpty: Boolean? = null,
    val filterReserved: Boolean? = null,
    // Dialog state
    val selectedContainer: Container? = null,
    val showActionSheet: Boolean = false,
    val showAddDialog: Boolean = false,
    val showBatchFillDialog: Boolean = false,
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val api: HopLedgerApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val state = _uiState.value
                val containers = api.getContainers(isEmpty = state.filterEmpty, isReserved = state.filterReserved)
                val brewers = api.getBrewers()
                val beers = api.getBeers()
                val locations = api.getLocations()
                val types = api.getContainerTypes()
                _uiState.update { it.copy(containers = containers, brewers = brewers, beers = beers, locations = locations, containerTypes = types, isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun setFilterEmpty(value: Boolean?) { _uiState.update { it.copy(filterEmpty = value) }; refresh() }
    fun setFilterReserved(value: Boolean?) { _uiState.update { it.copy(filterReserved = value) }; refresh() }

    fun selectContainer(container: Container) { _uiState.update { it.copy(selectedContainer = container, showActionSheet = true) } }
    fun dismissSheet() { _uiState.update { it.copy(showActionSheet = false, selectedContainer = null) } }
    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun dismissAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }
    fun showBatchFillDialog() { _uiState.update { it.copy(showBatchFillDialog = true) } }
    fun dismissBatchFillDialog() { _uiState.update { it.copy(showBatchFillDialog = false) } }

    fun addContainer(containerTypeId: String, locationId: String, beerId: String?) {
        viewModelScope.launch {
            try { api.createContainer(ContainerCreateRequest(containerTypeId, locationId, beerId)); dismissAddDialog(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun deleteContainer(id: String) {
        viewModelScope.launch {
            try { api.deleteContainer(id); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun moveContainer(id: String, locationId: String) {
        viewModelScope.launch {
            try { api.moveContainer(id, MoveRequest(locationId)); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun fillContainer(id: String, beerId: String) {
        viewModelScope.launch {
            try { api.fillContainer(id, FillRequest(beerId)); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun destroyBeer(id: String) {
        viewModelScope.launch {
            try { api.destroyBeer(id); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun reserveContainer(id: String, customerName: String) {
        viewModelScope.launch {
            try { api.reserveContainer(id, ReserveRequest(customerName)); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun unreserveContainer(id: String) {
        viewModelScope.launch {
            try { api.unreserveContainer(id); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun sell(containerId: String, brewerId: String, customerLocationId: String) {
        viewModelScope.launch {
            try { api.sell(SellRequest(containerId, brewerId, customerLocationId)); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun selfConsume(containerId: String, brewerId: String) {
        viewModelScope.launch {
            try { api.selfConsume(SelfConsumeRequest(containerId, brewerId)); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun containerReturn(containerId: String, brewerId: String, returnLocationId: String) {
        viewModelScope.launch {
            try { api.containerReturn(ContainerReturnRequest(containerId, brewerId, returnLocationId)); dismissSheet(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }

    fun batchFill(containerIds: List<String>, beerId: String) {
        viewModelScope.launch {
            try { api.batchFill(BatchFillRequest(containerIds, beerId)); dismissBatchFillDialog(); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }
}

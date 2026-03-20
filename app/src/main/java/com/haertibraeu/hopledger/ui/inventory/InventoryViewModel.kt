package com.haertibraeu.hopledger.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatusFilter { ALL, FULL, EMPTY, RESERVED }

data class ContainerGroup(
    val containerTypeId: String,
    val beerId: String?,
    val locationId: String,
    val containerType: ContainerType?,
    val beer: Beer?,
    val location: Location?,
    val count: Int,
    val reservedCount: Int,
    val sampleContainer: Container,
)

data class InventoryUiState(
    val groups: List<ContainerGroup> = emptyList(),
    val brewers: List<Brewer> = emptyList(),
    val beers: List<Beer> = emptyList(),
    val locations: List<Location> = emptyList(),
    val containerTypes: List<ContainerType> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val statusFilter: StatusFilter = StatusFilter.ALL,
    val filterLocationId: String? = null,
    val filterBeerId: String? = null,
    val selectedContainer: Container? = null,
    val showActionSheet: Boolean = false,
    val showAddDialog: Boolean = false,
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
                val s = _uiState.value
                val isEmpty = when (s.statusFilter) {
                    StatusFilter.FULL -> false
                    StatusFilter.EMPTY -> true
                    else -> null
                }
                val isReserved = if (s.statusFilter == StatusFilter.RESERVED) true else null
                val containers = api.getContainers(
                    isEmpty = isEmpty,
                    isReserved = isReserved,
                    locationId = s.filterLocationId,
                    beerId = s.filterBeerId,
                )
                val brewers = api.getBrewers()
                val beers = api.getBeers()
                val locations = api.getLocations()
                val types = api.getContainerTypes()
                _uiState.update {
                    it.copy(
                        groups = groupContainers(containers),
                        brewers = brewers,
                        beers = beers,
                        locations = locations,
                        containerTypes = types,
                        isLoading = false,
                        error = null,
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun groupContainers(containers: List<Container>): List<ContainerGroup> =
        containers
            .groupBy { Triple(it.containerTypeId, it.beerId, it.locationId) }
            .map { (_, group) ->
                val s = group.first()
                ContainerGroup(
                    containerTypeId = s.containerTypeId,
                    beerId = s.beerId,
                    locationId = s.locationId,
                    containerType = s.containerType,
                    beer = s.beer,
                    location = s.location,
                    count = group.size,
                    reservedCount = group.count { it.isReserved },
                    sampleContainer = s,
                )
            }
            .sortedWith(compareBy({ it.containerType?.name }, { it.beer?.name }))

    fun setStatusFilter(f: StatusFilter) { _uiState.update { it.copy(statusFilter = f) }; refresh() }
    fun setLocationFilter(id: String?) { _uiState.update { it.copy(filterLocationId = id) }; refresh() }
    fun setBeerFilter(id: String?) { _uiState.update { it.copy(filterBeerId = id) }; refresh() }

    fun selectGroup(group: ContainerGroup) {
        _uiState.update { it.copy(selectedContainer = group.sampleContainer, showActionSheet = true) }
    }
    fun dismissSheet() { _uiState.update { it.copy(showActionSheet = false, selectedContainer = null) } }
    fun showAddDialog() { _uiState.update { it.copy(showAddDialog = true) } }
    fun dismissAddDialog() { _uiState.update { it.copy(showAddDialog = false) } }

    fun addContainer(containerTypeId: String, locationId: String, beerId: String?, count: Int = 1) {
        viewModelScope.launch {
            try {
                repeat(count.coerceIn(1, 50)) {
                    api.createContainer(ContainerCreateRequest(containerTypeId, locationId, beerId))
                }
                dismissAddDialog()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
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
            try { api.batchFill(BatchFillRequest(containerIds, beerId)); refresh() }
            catch (e: Exception) { _uiState.update { it.copy(error = e.message) } }
        }
    }
}


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

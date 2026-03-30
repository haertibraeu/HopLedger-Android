package com.haertibraeu.hopledger.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.BatchContainerReturnRequest
import com.haertibraeu.hopledger.data.model.BatchFillRequest
import com.haertibraeu.hopledger.data.model.BatchSellRequest
import com.haertibraeu.hopledger.data.model.Beer
import com.haertibraeu.hopledger.data.model.Brewer
import com.haertibraeu.hopledger.data.model.Container
import com.haertibraeu.hopledger.data.model.ContainerCreateRequest
import com.haertibraeu.hopledger.data.model.ContainerReturnRequest
import com.haertibraeu.hopledger.data.model.ContainerType
import com.haertibraeu.hopledger.data.model.FillRequest
import com.haertibraeu.hopledger.data.model.Location
import com.haertibraeu.hopledger.data.model.LocationRequest
import com.haertibraeu.hopledger.data.model.MoveRequest
import com.haertibraeu.hopledger.data.model.ReserveRequest
import com.haertibraeu.hopledger.data.model.SelfConsumeRequest
import com.haertibraeu.hopledger.data.model.SellRequest
import com.haertibraeu.hopledger.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StatusFilter { ALL, FULL, EMPTY, RESERVED }

data class ContainerGroup(
    val containerTypeId: String,
    val beerId: String?,
    val locationId: String,
    val reservedFor: String?,
    val containerType: ContainerType?,
    val beer: Beer?,
    val location: Location?,
    val count: Int,
    val sampleContainer: Container,
    val containerIds: List<String>,
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
    val filterLocationTypes: Set<String> = setOf("brewer", "brewery"),
    val filterBeerId: String? = null,
    val selectedContainer: Container? = null,
    val selectedGroup: ContainerGroup? = null,
    val showActionSheet: Boolean = false,
    val showAddDialog: Boolean = false,
)

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val api: HopLedgerApi,
    private val sync: SyncRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            sync.startSync()
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
                // When no specific location is selected, filter groups by the active location types
                val groups = groupContainers(containers).let { all ->
                    if (s.filterLocationId != null) {
                        all
                    } else {
                        all.filter { group ->
                            val locType = group.location?.type ?: "other"
                            val canonical = when (locType) {
                                "brewer", "brewery", "customer" -> locType
                                else -> "other"
                            }
                            canonical in s.filterLocationTypes
                        }
                    }
                }
                _uiState.update {
                    it.copy(
                        groups = groups,
                        brewers = brewers,
                        beers = beers,
                        locations = locations,
                        containerTypes = types,
                        isLoading = false,
                        error = null,
                    )
                }
                sync.endSync()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
                sync.endSync(e.message)
            }
        }
    }

    // Group by type + beer + location + who reserved (each unique reservation is its own card)
    private fun groupContainers(containers: List<Container>): List<ContainerGroup> = containers
        .groupBy { Pair(Triple(it.containerTypeId, it.beerId, it.locationId), it.reservedFor) }
        .map { (_, group) ->
            val s = group.first()
            ContainerGroup(
                containerTypeId = s.containerTypeId,
                beerId = s.beerId,
                locationId = s.locationId,
                reservedFor = s.reservedFor,
                containerType = s.containerType,
                beer = s.beer,
                location = s.location,
                count = group.size,
                sampleContainer = s,
                containerIds = group.map { it.id },
            )
        }
        .sortedWith(compareBy({ it.containerType?.name }, { it.beer?.name }, { it.reservedFor }))

    fun setStatusFilter(f: StatusFilter) {
        _uiState.update { it.copy(statusFilter = f) }
        refresh()
    }
    fun setLocationFilter(id: String?) {
        _uiState.update { it.copy(filterLocationId = id) }
        refresh()
    }
    fun setLocationTypeFilter(types: Set<String>) {
        _uiState.update { it.copy(filterLocationTypes = types) }
        refresh()
    }
    fun setBeerFilter(id: String?) {
        _uiState.update { it.copy(filterBeerId = id) }
        refresh()
    }

    fun selectGroup(group: ContainerGroup) {
        _uiState.update { it.copy(selectedContainer = group.sampleContainer, selectedGroup = group, showActionSheet = true) }
    }
    fun dismissSheet() {
        _uiState.update { it.copy(showActionSheet = false, selectedContainer = null, selectedGroup = null) }
    }
    fun showAddDialog() {
        _uiState.update { it.copy(showAddDialog = true) }
    }
    fun dismissAddDialog() {
        _uiState.update { it.copy(showAddDialog = false) }
    }

    fun addContainer(containerTypeId: String, locationId: String, beerId: String?, count: Int = 1) {
        viewModelScope.launch {
            sync.startSync()
            try {
                repeat(count.coerceIn(1, 50)) {
                    api.createContainer(ContainerCreateRequest(containerTypeId, locationId, beerId))
                }
                dismissAddDialog()
                sync.endSync()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                sync.endSync(e.message)
            }
        }
    }

    private fun containerAction(action: suspend () -> Unit) {
        viewModelScope.launch {
            sync.startSync()
            try {
                action()
                sync.endSync()
                refresh()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
                sync.endSync()
            }
        }
    }

    fun deleteContainer(id: String) = containerAction {
        api.deleteContainer(id)
        dismissSheet()
    }
    fun moveContainer(id: String, locationId: String) = containerAction {
        api.moveContainer(id, MoveRequest(locationId))
        dismissSheet()
    }
    fun fillContainer(id: String, beerId: String) = containerAction {
        api.fillContainer(id, FillRequest(beerId))
        dismissSheet()
    }
    fun destroyBeer(id: String) = containerAction {
        api.destroyBeer(id)
        dismissSheet()
    }
    fun reserveContainer(id: String, customerName: String) = containerAction {
        api.reserveContainer(id, ReserveRequest(customerName))
        dismissSheet()
    }
    fun unreserveContainer(id: String) = containerAction {
        api.unreserveContainer(id)
        dismissSheet()
    }
    fun sell(containerId: String, brewerId: String, customerLocationId: String) = containerAction {
        api.sell(SellRequest(containerId, brewerId, customerLocationId))
        dismissSheet()
    }
    fun selfConsume(containerId: String, brewerId: String) = containerAction {
        api.selfConsume(SelfConsumeRequest(containerId, brewerId))
        dismissSheet()
    }
    fun containerReturn(containerId: String, brewerId: String, returnLocationId: String) = containerAction {
        api.containerReturn(ContainerReturnRequest(containerId, brewerId, returnLocationId))
        dismissSheet()
    }
    fun batchFill(containerIds: List<String>, beerId: String) = containerAction { api.batchFill(BatchFillRequest(containerIds, beerId)) }

    fun batchMove(ids: List<String>, locationId: String) = containerAction {
        ids.forEach { api.moveContainer(it, MoveRequest(locationId)) }
        dismissSheet()
    }
    fun batchFillContainers(ids: List<String>, beerId: String) = containerAction {
        ids.forEach { api.fillContainer(it, FillRequest(beerId)) }
        dismissSheet()
    }
    fun batchDestroyBeer(ids: List<String>) = containerAction {
        ids.forEach { api.destroyBeer(it) }
        dismissSheet()
    }
    fun batchDelete(ids: List<String>) = containerAction {
        ids.forEach { api.deleteContainer(it) }
        dismissSheet()
    }
    fun batchReserve(ids: List<String>, customerName: String) = containerAction {
        ids.forEach { api.reserveContainer(it, ReserveRequest(customerName)) }
        dismissSheet()
    }
    fun batchUnreserve(ids: List<String>) = containerAction {
        ids.forEach { api.unreserveContainer(it) }
        dismissSheet()
    }

    /** Find-or-create a customer location named [customerName], then sell all [ids] in a single transaction. */
    fun batchSellWithCustomer(ids: List<String>, brewerId: String, customerName: String) = containerAction {
        val s = _uiState.value
        val existing = s.locations.firstOrNull {
            it.type == "customer" && it.name.equals(customerName, ignoreCase = true)
        }
        val locationId = existing?.id ?: api.createLocation(LocationRequest(customerName, "customer")).id
        val group = s.selectedGroup
        val prefix = descriptionPrefix(group, ids.size)
        val brewerName = s.brewers.find { it.id == brewerId }?.name
        val desc = "$prefix an $customerName verkauft${brewerName?.let { " ($it)" } ?: ""}"
        api.batchSell(BatchSellRequest(ids, brewerId, locationId, desc))
        dismissSheet()
    }

    fun batchSelfConsume(ids: List<String>, brewerId: String) = containerAction {
        val s = _uiState.value
        val group = s.selectedGroup
        val prefix = descriptionPrefix(group, ids.size)
        val brewerName = s.brewers.find { it.id == brewerId }?.name
        val desc = "$prefix – Eigenverbrauch${brewerName?.let { " von $it" } ?: ""}"
        ids.forEach { api.selfConsume(SelfConsumeRequest(it, brewerId, desc)) }
        dismissSheet()
    }

    fun batchReturn(ids: List<String>, brewerId: String, returnLocationId: String) = containerAction {
        val s = _uiState.value
        val group = s.selectedGroup
        val prefix = descriptionPrefix(group, ids.size)
        val locationName = s.locations.find { it.id == returnLocationId }?.name
        val customerLocationName = group?.location?.name ?: "Kunde"
        val desc = "$prefix – Pfandrückgabe von $customerLocationName${locationName?.let { " → $it" } ?: ""}"
        api.batchContainerReturn(BatchContainerReturnRequest(ids, brewerId, returnLocationId, desc))
        dismissSheet()
    }

    private fun descriptionPrefix(group: ContainerGroup?, count: Int): String {
        val typeName = group?.containerType?.name ?: "Gebinde"
        val beerName = group?.beer?.name
        val qty = if (count > 1) "$count× " else ""
        return if (beerName != null) "$qty$typeName ($beerName)" else "$qty$typeName"
    }
}

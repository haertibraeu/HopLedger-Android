package com.haertibraeu.hopledger.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haertibraeu.hopledger.data.api.HopLedgerApi
import com.haertibraeu.hopledger.data.model.BatchContainerReturnRequest
import com.haertibraeu.hopledger.data.model.BatchFillRequest
import com.haertibraeu.hopledger.data.model.BatchSelfConsumeRequest
import com.haertibraeu.hopledger.data.model.BatchSellRequest
import com.haertibraeu.hopledger.data.model.Beer
import com.haertibraeu.hopledger.data.model.Brewer
import com.haertibraeu.hopledger.data.model.Container
import com.haertibraeu.hopledger.data.model.ContainerCreateRequest
import com.haertibraeu.hopledger.data.model.ContainerReturnRequest
import com.haertibraeu.hopledger.data.model.ContainerType
import com.haertibraeu.hopledger.data.model.ContainerTypeRequest
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

enum class StatusFilter { ALL, FULL, EMPTY, RESERVED, BYOB }

data class ContainerGroup(
    val containerTypeId: String,
    val beerId: String?,
    val locationId: String,
    val reservedFor: String?,
    val isByob: Boolean,
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
    val showSpeedDial: Boolean = false,
    val showAddExistingDialog: Boolean = false,
    val showAddNewGebindeDialog: Boolean = false,
    val showAddByobDialog: Boolean = false,
    val submittingAction: InventoryDialogAction? = null,
    val dialogError: InventoryDialogError? = null,
)

data class InventoryDialogError(
    val action: InventoryDialogAction,
    val message: String,
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
                val isByobFilter = if (s.statusFilter == StatusFilter.BYOB) true else null
                val containers = api.getContainers(
                    isEmpty = isEmpty,
                    isReserved = isReserved,
                    isByob = isByobFilter,
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

    // Group by type + beer + location + who reserved + isByob (each unique combination is its own card)
    private data class ContainerGroupKey(
        val containerTypeId: String,
        val beerId: String?,
        val locationId: String,
        val reservedFor: String?,
        val isByob: Boolean,
    )

    private fun groupContainers(containers: List<Container>): List<ContainerGroup> = containers
        .groupBy {
            ContainerGroupKey(
                containerTypeId = it.containerTypeId,
                beerId = it.beerId,
                locationId = it.locationId,
                reservedFor = it.reservedFor,
                isByob = it.isByob,
            )
        }
        .map { (_, group) ->
            val s = group.first()
            ContainerGroup(
                containerTypeId = s.containerTypeId,
                beerId = s.beerId,
                locationId = s.locationId,
                reservedFor = s.reservedFor,
                isByob = s.isByob,
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
        _uiState.update {
            it.copy(
                selectedContainer = group.sampleContainer,
                selectedGroup = group,
                showActionSheet = true,
            )
        }
    }
    fun dismissSheet() {
        if (_uiState.value.submittingAction != null) return
        closeSheet()
    }
    fun toggleSpeedDial() {
        if (_uiState.value.submittingAction != null) return
        _uiState.update { it.copy(showSpeedDial = !it.showSpeedDial) }
    }
    fun dismissSpeedDial() {
        _uiState.update { it.copy(showSpeedDial = false) }
    }
    fun showAddExistingDialog() {
        if (_uiState.value.submittingAction != null) return
        _uiState.update { it.copy(showSpeedDial = false, showAddExistingDialog = true) }
    }
    fun dismissAddExistingDialog() {
        if (_uiState.value.submittingAction != null) return
        _uiState.update { it.copy(showAddExistingDialog = false) }
    }
    fun showAddNewGebindeDialog() {
        if (_uiState.value.submittingAction != null) return
        _uiState.update { it.copy(showSpeedDial = false, showAddNewGebindeDialog = true) }
    }
    fun dismissAddNewGebindeDialog() {
        if (_uiState.value.submittingAction != null) return
        _uiState.update { it.copy(showAddNewGebindeDialog = false) }
    }
    fun showAddByobDialog() {
        if (_uiState.value.submittingAction != null) return
        _uiState.update { it.copy(showSpeedDial = false, showAddByobDialog = true) }
    }
    fun dismissAddByobDialog() {
        if (_uiState.value.submittingAction != null) return
        _uiState.update { it.copy(showAddByobDialog = false) }
    }

    fun addContainer(containerTypeId: String, locationId: String, beerId: String?, count: Int = 1) {
        if (!beginDialogAction(InventoryDialogAction.ADD_CONTAINER)) return
        viewModelScope.launch {
            try {
                repeat(count.coerceIn(1, 50)) {
                    api.createContainer(ContainerCreateRequest(containerTypeId, locationId, beerId))
                }
                completeDialogAction { copy(showAddExistingDialog = false) }
                refresh()
            } catch (e: Exception) {
                failDialogAction(e.message)
            }
        }
    }

    fun addNewGebinde(
        name: String,
        externalPrice: Double,
        internalPrice: Double,
        depositFee: Double,
        locationId: String,
        beerId: String?,
        count: Int = 1,
    ) {
        if (!beginDialogAction(InventoryDialogAction.ADD_NEW_GEBINDE)) return
        viewModelScope.launch {
            try {
                val newType = api.createContainerType(
                    ContainerTypeRequest(name, externalPrice = externalPrice, internalPrice = internalPrice, depositFee = depositFee),
                )
                repeat(count.coerceIn(1, 50)) {
                    api.createContainer(ContainerCreateRequest(newType.id, locationId, beerId))
                }
                completeDialogAction { copy(showAddNewGebindeDialog = false) }
                refresh()
            } catch (e: Exception) {
                failDialogAction(e.message)
            }
        }
    }

    fun addByobGebinde(
        containerTypeId: String?,
        newTypeName: String?,
        newTypeExternalPrice: Double,
        newTypeInternalPrice: Double,
        newTypeDepositFee: Double,
        locationId: String,
        ownerName: String,
        count: Int = 1,
    ) {
        if (!beginDialogAction(InventoryDialogAction.ADD_BYOB)) return
        viewModelScope.launch {
            try {
                val typeId = if (containerTypeId != null) {
                    containerTypeId
                } else {
                    api.createContainerType(
                        ContainerTypeRequest(
                            name = newTypeName ?: "BYOB",
                            externalPrice = newTypeExternalPrice,
                            internalPrice = newTypeInternalPrice,
                            depositFee = newTypeDepositFee,
                        ),
                    ).id
                }
                repeat(count.coerceIn(1, 50)) {
                    api.createContainer(
                        ContainerCreateRequest(
                            containerTypeId = typeId,
                            locationId = locationId,
                            isByob = true,
                            reservedFor = ownerName.trim(),
                        ),
                    )
                }
                completeDialogAction { copy(showAddByobDialog = false) }
                refresh()
            } catch (e: Exception) {
                failDialogAction(e.message)
            }
        }
    }

    private fun containerAction(
        actionId: InventoryDialogAction,
        action: suspend () -> Unit,
    ) {
        if (!beginDialogAction(actionId)) return
        viewModelScope.launch {
            try {
                action()
                completeDialogAction { copy(showActionSheet = false, selectedContainer = null, selectedGroup = null) }
                refresh()
            } catch (e: Exception) {
                failDialogAction(e.message)
            }
        }
    }

    fun deleteContainer(id: String) = containerAction(InventoryDialogAction.DELETE) {
        api.deleteContainer(id)
    }
    fun moveContainer(id: String, locationId: String) = containerAction(InventoryDialogAction.MOVE) {
        api.moveContainer(id, MoveRequest(locationId))
    }
    fun fillContainer(id: String, beerId: String) = containerAction(InventoryDialogAction.FILL) {
        api.fillContainer(id, FillRequest(beerId))
    }
    fun destroyBeer(id: String) = containerAction(InventoryDialogAction.DESTROY_BEER) {
        api.destroyBeer(id)
    }
    fun reserveContainer(id: String, customerName: String) = containerAction(InventoryDialogAction.RESERVE) {
        api.reserveContainer(id, ReserveRequest(customerName))
    }
    fun unreserveContainer(id: String) = containerAction(InventoryDialogAction.UNRESERVE) {
        api.unreserveContainer(id)
    }
    fun sell(containerId: String, brewerId: String, customerLocationId: String) = containerAction(InventoryDialogAction.SELL) {
        api.sell(SellRequest(containerId, brewerId, customerLocationId))
    }
    fun selfConsume(containerId: String, brewerId: String) = containerAction(InventoryDialogAction.SELF_CONSUME) {
        api.selfConsume(SelfConsumeRequest(containerId, brewerId))
    }
    fun containerReturn(containerId: String, brewerId: String, returnLocationId: String) = containerAction(InventoryDialogAction.RETURN) {
        api.containerReturn(ContainerReturnRequest(containerId, brewerId, returnLocationId))
    }
    fun batchFill(containerIds: List<String>, beerId: String) = containerAction(InventoryDialogAction.FILL) {
        api.batchFill(BatchFillRequest(containerIds, beerId))
    }

    fun batchMove(ids: List<String>, locationId: String) = containerAction(InventoryDialogAction.MOVE) {
        ids.forEach { api.moveContainer(it, MoveRequest(locationId)) }
    }
    fun batchFillContainers(ids: List<String>, beerId: String) = containerAction(InventoryDialogAction.FILL) {
        ids.forEach { api.fillContainer(it, FillRequest(beerId)) }
    }
    fun batchDestroyBeer(ids: List<String>) = containerAction(InventoryDialogAction.DESTROY_BEER) {
        ids.forEach { api.destroyBeer(it) }
    }
    fun batchDelete(ids: List<String>) = containerAction(InventoryDialogAction.DELETE) {
        ids.forEach { api.deleteContainer(it) }
    }
    fun batchReserve(ids: List<String>, customerName: String) = containerAction(InventoryDialogAction.RESERVE) {
        ids.forEach { api.reserveContainer(it, ReserveRequest(customerName)) }
    }
    fun batchUnreserve(ids: List<String>) = containerAction(InventoryDialogAction.UNRESERVE) {
        ids.forEach { api.unreserveContainer(it) }
    }

    /** Find-or-create a customer location named [customerName], then sell all [ids] in a single transaction. */
    fun batchSellWithCustomer(ids: List<String>, brewerId: String, customerName: String) = containerAction(InventoryDialogAction.SELL) {
        val s = _uiState.value
        val existing = s.locations.firstOrNull {
            it.type == "customer" && it.name.equals(customerName, ignoreCase = true)
        }
        val locationId = existing?.id ?: api.createLocation(LocationRequest(customerName, "customer")).id
        val group = s.selectedGroup
        val prefix = descriptionPrefix(group, ids.size)
        val brewerName = s.brewers.find { it.id == brewerId }?.name
        val byobSuffix = if (group?.isByob == true) " (BYOB – kein Pfand)" else ""
        val desc = "$prefix an $customerName verkauft${brewerName?.let { " ($it)" } ?: ""}$byobSuffix"
        api.batchSell(BatchSellRequest(ids, brewerId, locationId, desc))
    }

    fun batchSelfConsume(ids: List<String>, brewerId: String) = containerAction(InventoryDialogAction.SELF_CONSUME) {
        val s = _uiState.value
        val group = s.selectedGroup
        val prefix = descriptionPrefix(group, ids.size)
        val brewerName = s.brewers.find { it.id == brewerId }?.name
        val desc = "$prefix – Eigenverbrauch${brewerName?.let { " von $it" } ?: ""}"
        api.batchSelfConsume(BatchSelfConsumeRequest(ids, brewerId, desc))
    }

    fun batchReturn(ids: List<String>, brewerId: String, returnLocationId: String) = containerAction(InventoryDialogAction.RETURN) {
        val s = _uiState.value
        val group = s.selectedGroup
        val prefix = descriptionPrefix(group, ids.size)
        val locationName = s.locations.find { it.id == returnLocationId }?.name
        val customerLocationName = group?.location?.name ?: "Kunde"
        val desc = "$prefix – Pfandrückgabe von $customerLocationName${locationName?.let { " → $it" } ?: ""}"
        api.batchContainerReturn(BatchContainerReturnRequest(ids, brewerId, returnLocationId, desc))
    }

    private fun descriptionPrefix(group: ContainerGroup?, count: Int): String {
        val typeName = group?.containerType?.name ?: "Gebinde"
        val beerName = group?.beer?.name
        val qty = if (count > 1) "$count× " else ""
        return if (beerName != null) "$qty$typeName ($beerName)" else "$qty$typeName"
    }

    private fun beginDialogAction(action: InventoryDialogAction): Boolean {
        if (_uiState.value.submittingAction != null) return false
        _uiState.update { it.copy(submittingAction = action, dialogError = null) }
        sync.startSync()
        return true
    }

    private fun completeDialogAction(close: InventoryUiState.() -> InventoryUiState = { this }) {
        _uiState.update { it.close().copy(submittingAction = null, dialogError = null) }
        sync.endSync()
    }

    private fun failDialogAction(message: String?) {
        val failedAction = _uiState.value.submittingAction
        _uiState.update {
            it.copy(
                submittingAction = null,
                dialogError = if (failedAction != null && message != null) {
                    InventoryDialogError(action = failedAction, message = message)
                } else {
                    null
                },
            )
        }
        sync.endSync(message)
    }

    private fun closeSheet() {
        _uiState.update {
            it.copy(
                showActionSheet = false,
                selectedContainer = null,
                selectedGroup = null,
            )
        }
    }
}

package com.haertibraeu.hopledger.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.haertibraeu.hopledger.ui.components.DialogActionButton
import com.haertibraeu.hopledger.ui.components.DialogErrorMessage
import com.haertibraeu.hopledger.ui.components.DialogLoadingMessage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh every time this screen enters composition (tab switches, navigation back)
    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            FilterRow(
                statusFilter = uiState.statusFilter,
                locations = uiState.locations,
                beers = uiState.beers,
                filterLocationId = uiState.filterLocationId,
                filterLocationTypes = uiState.filterLocationTypes,
                filterBeerId = uiState.filterBeerId,
                onStatusFilter = viewModel::setStatusFilter,
                onLocationFilter = viewModel::setLocationFilter,
                onLocationTypeFilter = viewModel::setLocationTypeFilter,
                onBeerFilter = viewModel::setBeerFilter,
            )
            if (uiState.isLoading) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            uiState.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp))
            }
            if (uiState.groups.isEmpty() && !uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Keine Gebinde gefunden", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val colCount = when {
                        maxWidth < 360.dp -> 1
                        maxWidth < 600.dp -> 2
                        else -> (maxWidth / 220.dp).toInt().coerceAtLeast(1)
                    }
                    val chunkedGroups = remember(uiState.groups, colCount) {
                        uiState.groups.chunked(colCount)
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(chunkedGroups) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                for (group in rowItems) {
                                    ContainerGroupCard(
                                        group = group,
                                        modifier = Modifier.weight(1f).fillMaxHeight(),
                                        onClick = { viewModel.selectGroup(group) },
                                    )
                                }
                                // Fill remaining space in the row if it's not full
                                if (rowItems.size < colCount) {
                                    repeat(colCount - rowItems.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = viewModel::showAddDialog,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, "Gebinde hinzufügen")
        }
    }

    if (uiState.showAddDialog) {
        AddContainerDialog(
            containerTypes = uiState.containerTypes,
            locations = uiState.locations,
            beers = uiState.beers,
            submittingAction = uiState.submittingAction,
            errorMessage = uiState.dialogError
                ?.takeIf { it.action == InventoryDialogAction.ADD_CONTAINER }
                ?.message,
            onConfirm = { ctId, locId, beerId, count -> viewModel.addContainer(ctId, locId, beerId, count) },
            onDismiss = viewModel::dismissAddDialog,
        )
    }

    if (uiState.showActionSheet && uiState.selectedGroup != null) {
        ContainerActionSheet(
            group = uiState.selectedGroup!!,
            brewers = uiState.brewers,
            beers = uiState.beers,
            locations = uiState.locations,
            submittingAction = uiState.submittingAction,
            dialogError = uiState.dialogError,
            onDismiss = viewModel::dismissSheet,
            onMove = { ids, loc -> viewModel.batchMove(ids, loc) },
            onFill = { ids, beer -> viewModel.batchFillContainers(ids, beer) },
            onDestroyBeer = { ids -> viewModel.batchDestroyBeer(ids) },
            onReserve = { ids, name -> viewModel.batchReserve(ids, name.trim()) },
            onUnreserve = { ids -> viewModel.batchUnreserve(ids) },
            onSell = { ids, b, name -> viewModel.batchSellWithCustomer(ids, b, name.trim()) },
            onSelfConsume = { ids, b -> viewModel.batchSelfConsume(ids, b) },
            onContainerReturn = { ids, b, l -> viewModel.batchReturn(ids, b, l) },
            onDelete = { ids -> viewModel.batchDelete(ids) },
        )
    }
}

private val breweryLocationTypes = setOf("brewer", "brewery")

// ── Filter row ───────────────────────────────────────────────────────────────

@Composable
private fun FilterRow(
    statusFilter: StatusFilter,
    locations: List<com.haertibraeu.hopledger.data.model.Location>,
    beers: List<com.haertibraeu.hopledger.data.model.Beer>,
    filterLocationId: String?,
    filterLocationTypes: Set<String>,
    filterBeerId: String?,
    onStatusFilter: (StatusFilter) -> Unit,
    onLocationFilter: (String?) -> Unit,
    onLocationTypeFilter: (Set<String>) -> Unit,
    onBeerFilter: (String?) -> Unit,
) {
    val locationTypeOptions = listOf("brewer" to "Brauer", "brewery" to "Brauerei", "customer" to "Kunde", "other" to "Andere")
    val defaultLocationTypes = setOf("brewer", "brewery")
    val visibleLocations = locations.filter { loc ->
        val canonical = when (loc.type) {
            "brewer", "brewery", "customer" -> loc.type
            else -> "other"
        }
        canonical in filterLocationTypes
    }
    val typeIsDefault = filterLocationTypes == defaultLocationTypes

    val anyActive = statusFilter != StatusFilter.ALL || filterLocationId != null || filterBeerId != null || !typeIsDefault

    val statusEmoji = when (statusFilter) {
        StatusFilter.ALL -> "☰"
        StatusFilter.FULL -> "🍺"
        StatusFilter.EMPTY -> "🫙"
        StatusFilter.RESERVED -> "📋"
    }
    val statusLabel = when (statusFilter) {
        StatusFilter.ALL -> "Status"
        StatusFilter.FULL -> "Gefüllt"
        StatusFilter.EMPTY -> "Leer"
        StatusFilter.RESERVED -> "Reserviert"
    }
    val locationName = locations.find { it.id == filterLocationId }?.name
    val beerName = beers.find { it.id == filterBeerId }?.name

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Status filter chip
        FilterDropdownChip(
            emoji = statusEmoji,
            label = statusLabel,
            selected = statusFilter != StatusFilter.ALL,
        ) { close ->
            listOf(
                StatusFilter.ALL to ("☰" to "Alle"),
                StatusFilter.FULL to ("🍺" to "Gefüllt"),
                StatusFilter.EMPTY to ("🫙" to "Leer"),
                StatusFilter.RESERVED to ("📋" to "Reserviert"),
            ).forEach { (f, pair) ->
                val (emoji, name) = pair
                DropdownMenuItem(
                    leadingIcon = { Text(emoji) },
                    text = { Text(name) },
                    onClick = {
                        onStatusFilter(f)
                        close()
                    },
                )
            }
        }

        // Location filter chip — type checkboxes at top, then individual locations below
        FilterDropdownChip(
            emoji = "📍",
            label = locationName ?: "Standort",
            selected = filterLocationId != null || !typeIsDefault,
        ) { close ->
            // "Alle Standorte" — checks all types and clears location filter
            val allTypes = locationTypeOptions.map { it.first }.toSet()
            DropdownMenuItem(
                leadingIcon = { Text("📍") },
                text = { Text("Alle Standorte") },
                onClick = {
                    onLocationTypeFilter(allTypes)
                    onLocationFilter(null)
                    close()
                },
            )
            HorizontalDivider()
            // Type filter section (checkboxes, dropdown stays open on toggle)
            locationTypeOptions.forEach { (type, name) ->
                val checked = type in filterLocationTypes
                DropdownMenuItem(
                    leadingIcon = { Checkbox(checked = checked, onCheckedChange = null) },
                    text = { Text(name) },
                    onClick = {
                        val newTypes = if (checked) filterLocationTypes - type else filterLocationTypes + type
                        onLocationTypeFilter(newTypes)
                        val selectedLoc = locations.find { it.id == filterLocationId }
                        if (selectedLoc != null) {
                            val canonical = when (selectedLoc.type) {
                                "brewer", "brewery", "customer" -> selectedLoc.type
                                else -> "other"
                            }
                            if (canonical !in newTypes) onLocationFilter(null)
                        }
                    },
                )
            }
            HorizontalDivider()
            // Individual locations filtered by selected types
            visibleLocations.forEach { loc ->
                DropdownMenuItem(leadingIcon = { Text("📍") }, text = { Text(loc.name) }, onClick = {
                    onLocationFilter(loc.id)
                    close()
                })
            }
        }

        // Beer filter chip
        FilterDropdownChip(
            emoji = "🍺",
            label = beerName ?: "Bier",
            selected = filterBeerId != null,
        ) { close ->
            DropdownMenuItem(leadingIcon = { Text("🍺") }, text = { Text("Alle Biere") }, onClick = {
                onBeerFilter(null)
                close()
            })
            beers.forEach { beer ->
                DropdownMenuItem(leadingIcon = { Text("🍺") }, text = { Text(beer.name) }, onClick = {
                    onBeerFilter(beer.id)
                    close()
                })
            }
        }

        // Clear all — only visible when any filter is active
        if (anyActive) {
            InputChip(
                selected = false,
                onClick = {
                    onStatusFilter(StatusFilter.ALL)
                    onLocationFilter(null)
                    onLocationTypeFilter(defaultLocationTypes)
                    onBeerFilter(null)
                },
                label = { Text("Zurücksetzen") },
                leadingIcon = { Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp)) },
            )
        }
    }
}

@Composable
private fun FilterDropdownChip(
    emoji: String,
    label: String,
    selected: Boolean,
    content: @Composable (() -> Unit) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        FilterChip(
            selected = selected,
            onClick = { expanded = true },
            label = { Text(label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Text(emoji, style = MaterialTheme.typography.bodyMedium) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, null, modifier = Modifier.size(16.dp)) },
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            content { expanded = false }
        }
    }
}

// ── Container group card ──────────────────────────────────────────────────────

@Composable
private fun ContainerGroupCard(group: ContainerGroup, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val isReserved = group.reservedFor != null
    val isEmpty = group.beer == null

    val cardColor = when {
        // reserved: warm tint
        isReserved -> MaterialTheme.colorScheme.secondaryContainer

        // empty: muted neutral
        isEmpty -> MaterialTheme.colorScheme.surfaceVariant

        // full: primary tint
        else -> MaterialTheme.colorScheme.primaryContainer
    }

    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(group.containerType?.name ?: "?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)

                val countFraction = ((group.count - 1) / 15f).coerceIn(0f, 1f) // 15 and over = full red
                val badgeAccent = Color(255, 50, 50, 178)
                val badgeBackground = MaterialTheme.colorScheme.tertiaryContainer
                val badgeContainerColor = remember(badgeAccent, badgeBackground, countFraction) {
                    lerp(badgeBackground, badgeAccent, countFraction)
                }
                val badgeContentColor = if (countFraction > 0.4f) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onTertiaryContainer

                Badge(
                    containerColor = badgeContainerColor,
                    contentColor = badgeContentColor,
                ) { Text("×${group.count}") }
            }
            Spacer(Modifier.height(4.dp))
            Text(if (group.beer == null) "🫙 Leer" else "🍺 ${group.beer.name}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("📍 ${group.location?.name ?: "?"}", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (isReserved) Text("📋 ${group.reservedFor}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ── Add container dialog ──────────────────────────────────────────────────────

@Composable
private fun AddContainerDialog(
    containerTypes: List<com.haertibraeu.hopledger.data.model.ContainerType>,
    locations: List<com.haertibraeu.hopledger.data.model.Location>,
    beers: List<com.haertibraeu.hopledger.data.model.Beer>,
    submittingAction: InventoryDialogAction?,
    errorMessage: String?,
    onConfirm: (containerTypeId: String, locationId: String, beerId: String?, count: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val isSubmitting = submittingAction != null
    val defaultLocation = locations.firstOrNull { it.type == "brewery" } ?: locations.firstOrNull { it.type == "brewer" } ?: locations.firstOrNull()
    var selectedTypeId by remember { mutableStateOf(containerTypes.firstOrNull()?.id ?: "") }
    var selectedLocationId by remember { mutableStateOf(defaultLocation?.id ?: "") }
    var selectedBeerId by remember { mutableStateOf("") }
    var countText by remember { mutableStateOf("1") }

    val breweryLocations = locations.filter { it.type == "brewer" || it.type == "brewery" }
    val selectedTypeName = containerTypes.find { it.id == selectedTypeId }?.name ?: "Auswählen…"
    val selectedLocationName = breweryLocations.find { it.id == selectedLocationId }?.name ?: "Auswählen…"
    val selectedBeerName = if (selectedBeerId.isBlank()) "Leer" else beers.find { it.id == selectedBeerId }?.name ?: "Auswählen…"

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Gebinde hinzufügen") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Gebindetyp", style = MaterialTheme.typography.labelLarge)
                SpinnerField(selectedTypeName, containerTypes.map { it.name to it.id }, enabled = !isSubmitting) { selectedTypeId = it }
                Text("Standort", style = MaterialTheme.typography.labelLarge)
                SpinnerField(selectedLocationName, breweryLocations.map { it.name to it.id }, enabled = !isSubmitting) { selectedLocationId = it }
                Text("Bier (optional)", style = MaterialTheme.typography.labelLarge)
                SpinnerField(selectedBeerName, listOf("Leer" to "") + beers.map { it.name to it.id }, enabled = !isSubmitting) { selectedBeerId = it }
                Text("Anzahl", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = countText,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) countText = it },
                    label = { Text("Stück (max. 50)") },
                    singleLine = true,
                    enabled = !isSubmitting,
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let { DialogErrorMessage(it) }
            }
        },
        confirmButton = {
            DialogActionButton(
                label = "Hinzufügen",
                onClick = { onConfirm(selectedTypeId, selectedLocationId, selectedBeerId.ifBlank { null }, countText.toIntOrNull() ?: 1) },
                enabled = selectedTypeId.isNotBlank() && selectedLocationId.isNotBlank(),
                isLoading = submittingAction == InventoryDialogAction.ADD_CONTAINER,
            )
        },
        dismissButton = { DialogActionButton(label = "Abbrechen", onClick = onDismiss, enabled = !isSubmitting) },
    )
}

@Composable
private fun SpinnerField(
    value: String,
    options: List<Pair<String, String>>,
    enabled: Boolean = true,
    onSelect: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            Text(value, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (label, id) ->
                DropdownMenuItem(text = { Text(label) }, onClick = {
                    onSelect(id)
                    expanded = false
                })
            }
        }
    }
}

// ── Action bottom sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerActionSheet(
    group: ContainerGroup,
    brewers: List<com.haertibraeu.hopledger.data.model.Brewer>,
    beers: List<com.haertibraeu.hopledger.data.model.Beer>,
    locations: List<com.haertibraeu.hopledger.data.model.Location>,
    submittingAction: InventoryDialogAction?,
    dialogError: InventoryDialogError?,
    onDismiss: () -> Unit,
    onMove: (List<String>, String) -> Unit,
    onFill: (List<String>, String) -> Unit,
    onDestroyBeer: (List<String>) -> Unit,
    onReserve: (List<String>, String) -> Unit,
    onUnreserve: (List<String>) -> Unit,
    onSell: (List<String>, String, String) -> Unit, // ids, brewerId, customerName
    onSelfConsume: (List<String>, String) -> Unit,
    onContainerReturn: (List<String>, String, String) -> Unit,
    onDelete: (List<String>) -> Unit,
) {
    val container = group.sampleContainer
    var selectedQuantity by remember { mutableIntStateOf(1) }
    var totalQuantity by remember { mutableIntStateOf(group.count) }
    val ids = group.containerIds.take(selectedQuantity)

    var showMove by remember { mutableStateOf(false) }
    var showFill by remember { mutableStateOf(false) }
    var showReserve by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var showSell by remember { mutableStateOf(false) }
    var showConsume by remember { mutableStateOf(false) }
    var showReturn by remember { mutableStateOf(false) }
    var showDestroyBeerConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val isSubmitting = submittingAction != null
    fun errorMessageFor(action: InventoryDialogAction): String? = dialogError?.takeIf { it.action == action }?.message

    ModalBottomSheet(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // ── Title row with optional quantity spinner ──────────────────
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "${container.containerType?.name} · ${if (container.isEmpty) "Leer" else container.beer?.name ?: "?"}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                if (group.count > 1) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        FilledTonalIconButton(
                            onClick = { if (selectedQuantity > 1) selectedQuantity-- },
                            enabled = selectedQuantity > 1 && !isSubmitting,
                            modifier = Modifier.size(32.dp),
                        ) { Text("−") }
                        Text("$selectedQuantity/$totalQuantity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 28.dp), textAlign = TextAlign.Center)
                        FilledTonalIconButton(
                            onClick = { if (selectedQuantity < group.count) selectedQuantity++ },
                            enabled = selectedQuantity < group.count && !isSubmitting,
                            modifier = Modifier.size(32.dp),
                        ) { Text("+") }
                    }
                }
            }

            // ── Standard actions ──────────────────────────────────────────
            DialogActionButton(
                label = "📦 Verschieben",
                onClick = { showMove = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            )

            if (container.isEmpty) {
                DialogActionButton(
                    label = "🍺 Befüllen",
                    onClick = { showFill = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                )
            }

            if (!container.isEmpty && !container.isReserved) {
                DialogActionButton(
                    label = "📋 Reservieren",
                    onClick = { showReserve = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                )
            }
            if (container.isReserved) {
                DialogActionButton(
                    label = "📋 Reservierung aufheben (${container.reservedFor})",
                    onClick = { onUnreserve(ids) },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    isLoading = submittingAction == InventoryDialogAction.UNRESERVE,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (!container.isEmpty) {
                DialogActionButton(
                    label = "💰 Verkaufen (${container.containerType?.externalPrice ?: 0} + ${container.containerType?.depositFee ?: 0} CHF)",
                    onClick = { showSell = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                )
                DialogActionButton(
                    label = "🍻 Eigenverbrauch (${container.containerType?.internalPrice ?: 0} CHF)",
                    onClick = { showConsume = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                )
            }

            DialogActionButton(
                label = "↩️ Rückgabe (${container.containerType?.depositFee ?: 0} CHF Pfand)",
                onClick = { showReturn = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
            )

            // ── Danger zone ───────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (!container.isEmpty) {
                DialogActionButton(
                    label = "🗑️ Bier vernichten",
                    onClick = { showDestroyBeerConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                )
            }

            DialogActionButton(
                label = "🗑️ Gebinde löschen",
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isSubmitting,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            )
        }
    }

    // ── Sub-dialogs ───────────────────────────────────────────────────────────
    if (showMove) {
        MoveLocationDialog(
            locations = locations,
            isSubmitting = isSubmitting,
            errorMessage = errorMessageFor(InventoryDialogAction.MOVE),
            loadingLabel = "Verschiebe…",
            onSelect = { onMove(ids, it) },
            onDismiss = { if (!isSubmitting) showMove = false },
        )
    }
    if (showFill) {
        PickerDialog(
            title = "Bier auswählen",
            options = beers.map { it.name to it.id },
            isSubmitting = isSubmitting,
            errorMessage = errorMessageFor(InventoryDialogAction.FILL),
            loadingLabel = "Befülle…",
            onSelect = { onFill(ids, it) },
            onDismiss = { if (!isSubmitting) showFill = false },
        )
    }

    if (showReserve) {
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showReserve = false },
            title = { Text("Reservieren für") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        enabled = !isSubmitting,
                        label = { Text("Kundenname") },
                    )
                    errorMessageFor(InventoryDialogAction.RESERVE)?.let { DialogErrorMessage(it) }
                }
            },
            confirmButton = {
                DialogActionButton(
                    label = "OK",
                    onClick = { onReserve(ids, customerName) },
                    enabled = customerName.isNotBlank(),
                    isLoading = submittingAction == InventoryDialogAction.RESERVE,
                )
            },
            dismissButton = { DialogActionButton(label = "Abbrechen", onClick = { showReserve = false }, enabled = !isSubmitting) },
        )
    }

    if (showSell) {
        SellDialog(
            reservedFor = container.reservedFor,
            brewers = brewers,
            isSubmitting = isSubmitting,
            errorMessage = errorMessageFor(InventoryDialogAction.SELL),
            onConfirm = { brewerId, customerName -> onSell(ids, brewerId, customerName) },
            onDismiss = { if (!isSubmitting) showSell = false },
        )
    }
    if (showConsume) {
        PickerDialog(
            title = "Brauer",
            options = brewers.map { it.name to it.id },
            isSubmitting = isSubmitting,
            errorMessage = errorMessageFor(InventoryDialogAction.SELF_CONSUME),
            loadingLabel = "Buche Eigenverbrauch…",
            onSelect = { onSelfConsume(ids, it) },
            onDismiss = { if (!isSubmitting) showConsume = false },
        )
    }
    if (showReturn) {
        TwoPickerDialog(
            title = "Rückgabe",
            label1 = "Brauer",
            options1 = brewers.map { it.name to it.id },
            label2 = "Rückgabeort",
            options2 = locations.filter { it.type in breweryLocationTypes }.map { it.name to it.id },
            isSubmitting = isSubmitting,
            errorMessage = errorMessageFor(InventoryDialogAction.RETURN),
            isLoading = submittingAction == InventoryDialogAction.RETURN,
            onConfirm = { brewerId, locationId -> onContainerReturn(ids, brewerId, locationId) },
            onDismiss = { if (!isSubmitting) showReturn = false },
        )
    }

    if (showDestroyBeerConfirm) {
        val qLabel = if (selectedQuantity == 1) "diesem Gebinde" else "$selectedQuantity Gebinden"
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showDestroyBeerConfirm = false },
            title = { Text("Bier vernichten?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Das Bier in $qLabel wird als vernichtet markiert. Die Gebinde werden leer — der Inhalt geht verloren. Diese Aktion kann nicht rückgängig gemacht werden.")
                    errorMessageFor(InventoryDialogAction.DESTROY_BEER)?.let { DialogErrorMessage(it) }
                }
            },
            confirmButton = {
                DialogActionButton(
                    label = "Vernichten",
                    onClick = { onDestroyBeer(ids) },
                    isLoading = submittingAction == InventoryDialogAction.DESTROY_BEER,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                )
            },
            dismissButton = { DialogActionButton(label = "Abbrechen", onClick = { showDestroyBeerConfirm = false }, enabled = !isSubmitting) },
        )
    }

    if (showDeleteConfirm) {
        val qLabel = if (selectedQuantity == 1) "dieses Gebinde" else "diese $selectedQuantity Gebinde"
        AlertDialog(
            onDismissRequest = { if (!isSubmitting) showDeleteConfirm = false },
            title = { Text("Gebinde löschen?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Du bist dabei, $qLabel endgültig aus dem System zu löschen. Alle zugehörigen Daten (Füllstand, Reservierungen) gehen verloren. Diese Aktion kann nicht rückgängig gemacht werden.")
                    errorMessageFor(InventoryDialogAction.DELETE)?.let { DialogErrorMessage(it) }
                }
            },
            confirmButton = {
                DialogActionButton(
                    label = "Löschen",
                    onClick = { onDelete(ids) },
                    isLoading = submittingAction == InventoryDialogAction.DELETE,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                )
            },
            dismissButton = { DialogActionButton(label = "Abbrechen", onClick = { showDeleteConfirm = false }, enabled = !isSubmitting) },
        )
    }
}

@Composable
private fun SellDialog(
    reservedFor: String?,
    brewers: List<com.haertibraeu.hopledger.data.model.Brewer>,
    isSubmitting: Boolean,
    errorMessage: String?,
    onConfirm: (brewerId: String, customerName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedBrewerId by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf(reservedFor ?: "") }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("💰 Verkaufen") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Brauer", style = MaterialTheme.typography.labelLarge)
                brewers.forEach { brewer ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedBrewerId == brewer.id, onClick = { selectedBrewerId = brewer.id }, enabled = !isSubmitting)
                        Text(brewer.name)
                    }
                }
                HorizontalDivider()
                Text("Kunde", style = MaterialTheme.typography.labelLarge)
                if (reservedFor != null) {
                    Text(
                        "📋 Reserviert für: $reservedFor",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    OutlinedTextField(
                        value = customerName,
                        onValueChange = { customerName = it },
                        enabled = !isSubmitting,
                        label = { Text("Kundenname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                errorMessage?.let { DialogErrorMessage(it) }
            }
        },
        confirmButton = {
            DialogActionButton(
                label = "Verkaufen",
                onClick = { if (selectedBrewerId.isNotBlank() && customerName.isNotBlank()) onConfirm(selectedBrewerId, customerName) },
                enabled = selectedBrewerId.isNotBlank() && customerName.isNotBlank(),
                isLoading = isSubmitting,
            )
        },
        dismissButton = { DialogActionButton(label = "Abbrechen", onClick = onDismiss, enabled = !isSubmitting) },
    )
}

@Composable
private fun MoveLocationDialog(
    locations: List<com.haertibraeu.hopledger.data.model.Location>,
    isSubmitting: Boolean,
    errorMessage: String?,
    loadingLabel: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val primary = locations.filter { it.type == "brewer" || it.type == "brewery" }
    val secondary = locations.filter { it.type != "brewer" && it.type != "brewery" }
    var showOthers by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text("Neuer Standort") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                primary.forEach { loc ->
                    TextButton(onClick = { onSelect(loc.id) }, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) { Text(loc.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start) }
                }
                if (secondary.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TextButton(onClick = { showOthers = !showOthers }, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (showOthers) "▲ Weitere ausblenden" else "▼ Weitere anzeigen (${secondary.size})",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showOthers) {
                        secondary.forEach { loc ->
                            TextButton(onClick = { onSelect(loc.id) }, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) { Text(loc.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start) }
                        }
                    }
                }
                if (isSubmitting) {
                    DialogLoadingMessage(loadingLabel)
                }
                errorMessage?.let { DialogErrorMessage(it) }
            }
        },
        confirmButton = {},
        dismissButton = { DialogActionButton(label = "Abbrechen", onClick = onDismiss, enabled = !isSubmitting) },
    )
}

@Composable
private fun PickerDialog(
    title: String,
    options: List<Pair<String, String>>,
    isSubmitting: Boolean,
    errorMessage: String?,
    loadingLabel: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                options.forEach { (n, id) ->
                    TextButton(onClick = { onSelect(id) }, enabled = !isSubmitting, modifier = Modifier.fillMaxWidth()) { Text(n) }
                }
                if (isSubmitting) {
                    DialogLoadingMessage(loadingLabel)
                }
                errorMessage?.let { DialogErrorMessage(it) }
            }
        },
        confirmButton = {},
        dismissButton = { DialogActionButton(label = "Abbrechen", onClick = onDismiss, enabled = !isSubmitting) },
    )
}

@Composable
private fun TwoPickerDialog(
    title: String,
    label1: String,
    options1: List<Pair<String, String>>,
    label2: String,
    options2: List<Pair<String, String>>,
    isSubmitting: Boolean,
    errorMessage: String?,
    isLoading: Boolean,
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var s1 by remember { mutableStateOf("") }
    var s2 by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label1, style = MaterialTheme.typography.labelLarge)
                options1.forEach { (n, id) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s1 == id, onClick = { s1 = id }, enabled = !isSubmitting)
                        Text(n)
                    }
                }
                Text(label2, style = MaterialTheme.typography.labelLarge)
                options2.forEach { (n, id) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s2 == id, onClick = { s2 = id }, enabled = !isSubmitting)
                        Text(n)
                    }
                }
                errorMessage?.let { DialogErrorMessage(it) }
            }
        },
        confirmButton = {
            DialogActionButton(
                label = "Bestätigen",
                onClick = { if (s1.isNotBlank() && s2.isNotBlank()) onConfirm(s1, s2) },
                enabled = s1.isNotBlank() && s2.isNotBlank(),
                isLoading = isLoading,
            )
        },
        dismissButton = { DialogActionButton(label = "Abbrechen", onClick = onDismiss, enabled = !isSubmitting) },
    )
}

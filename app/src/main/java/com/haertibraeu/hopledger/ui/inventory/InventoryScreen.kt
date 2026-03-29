package com.haertibraeu.hopledger.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(uiState.groups, key = { "${it.containerTypeId}_${it.beerId}_${it.locationId}_${it.reservedFor}" }) { group ->
                        ContainerGroupCard(group) { viewModel.selectGroup(group) }
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
            onDismiss = viewModel::dismissSheet,
            onMove = { ids, loc -> viewModel.batchMove(ids, loc) },
            onFill = { ids, beer -> viewModel.batchFillContainers(ids, beer) },
            onDestroyBeer = { ids -> viewModel.batchDestroyBeer(ids) },
            onReserve = { ids, name -> viewModel.batchReserve(ids, name) },
            onUnreserve = { ids -> viewModel.batchUnreserve(ids) },
            onSell = { ids, b, name -> viewModel.batchSellWithCustomer(ids, b, name) },
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
private fun ContainerGroupCard(group: ContainerGroup, onClick: () -> Unit) {
    val isReserved = group.reservedFor != null
    val isEmpty = group.beer == null

    val cardColor = when {
        isReserved -> MaterialTheme.colorScheme.secondaryContainer

        // reserved: warm tint
        isEmpty -> MaterialTheme.colorScheme.surfaceVariant

        // empty: muted neutral
        else -> MaterialTheme.colorScheme.primaryContainer // full: primary tint
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Text(group.containerType?.name ?: "?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (group.count > 1) Badge { Text("×${group.count}") }
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
    onConfirm: (containerTypeId: String, locationId: String, beerId: String?, count: Int) -> Unit,
    onDismiss: () -> Unit,
) {
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
        onDismissRequest = onDismiss,
        title = { Text("Gebinde hinzufügen") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Gebindetyp", style = MaterialTheme.typography.labelLarge)
                SpinnerField(selectedTypeName, containerTypes.map { it.name to it.id }) { selectedTypeId = it }
                Text("Standort", style = MaterialTheme.typography.labelLarge)
                SpinnerField(selectedLocationName, breweryLocations.map { it.name to it.id }) { selectedLocationId = it }
                Text("Bier (optional)", style = MaterialTheme.typography.labelLarge)
                SpinnerField(selectedBeerName, listOf("Leer" to "") + beers.map { it.name to it.id }) { selectedBeerId = it }
                Text("Anzahl", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = countText,
                    onValueChange = { if (it.all(Char::isDigit) && it.length <= 2) countText = it },
                    label = { Text("Stück (max. 50)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedTypeId, selectedLocationId, selectedBeerId.ifBlank { null }, countText.toIntOrNull() ?: 1) },
                enabled = selectedTypeId.isNotBlank() && selectedLocationId.isNotBlank(),
            ) { Text("Hinzufügen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun SpinnerField(value: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
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
    var quantity by remember { mutableIntStateOf(group.count) }
    val ids = group.containerIds.take(quantity)

    var showMove by remember { mutableStateOf(false) }
    var showFill by remember { mutableStateOf(false) }
    var showReserve by remember { mutableStateOf(false) }
    var customerName by remember { mutableStateOf("") }
    var showSell by remember { mutableStateOf(false) }
    var showConsume by remember { mutableStateOf(false) }
    var showReturn by remember { mutableStateOf(false) }
    var showDestroyBeerConfirm by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
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
                            onClick = { quantity = 1 },
                            enabled = quantity > 1,
                            modifier = Modifier.size(32.dp),
                        ) { Text("1") }
                        FilledTonalIconButton(
                            onClick = { if (quantity > 1) quantity-- },
                            enabled = quantity > 1,
                            modifier = Modifier.size(32.dp),
                        ) { Text("−") }
                        Text("$quantity", style = MaterialTheme.typography.titleMedium, modifier = Modifier.widthIn(min = 28.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        FilledTonalIconButton(
                            onClick = { if (quantity < group.count) quantity++ },
                            enabled = quantity < group.count,
                            modifier = Modifier.size(32.dp),
                        ) { Text("+") }
                    }
                }
            }

            // ── Standard actions ──────────────────────────────────────────
            TextButton(onClick = { showMove = true }, modifier = Modifier.fillMaxWidth()) { Text("📦 Verschieben") }

            if (container.isEmpty) {
                TextButton(onClick = { showFill = true }, modifier = Modifier.fillMaxWidth()) { Text("🍺 Befüllen") }
            }

            if (!container.isEmpty && !container.isReserved) {
                TextButton(onClick = { showReserve = true }, modifier = Modifier.fillMaxWidth()) { Text("📋 Reservieren") }
            }
            if (container.isReserved) {
                TextButton(onClick = { onUnreserve(ids) }, modifier = Modifier.fillMaxWidth()) { Text("📋 Reservierung aufheben (${container.reservedFor})") }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (!container.isEmpty) {
                TextButton(onClick = { showSell = true }, modifier = Modifier.fillMaxWidth()) { Text("💰 Verkaufen (${container.containerType?.externalPrice ?: 0} + ${container.containerType?.depositFee ?: 0} CHF)") }
                TextButton(onClick = { showConsume = true }, modifier = Modifier.fillMaxWidth()) { Text("🍻 Eigenverbrauch (${container.containerType?.internalPrice ?: 0} CHF)") }
            }

            TextButton(onClick = { showReturn = true }, modifier = Modifier.fillMaxWidth()) { Text("↩️ Rückgabe (${container.containerType?.depositFee ?: 0} CHF Pfand)") }

            // ── Danger zone ───────────────────────────────────────────────
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            if (!container.isEmpty) {
                TextButton(
                    onClick = { showDestroyBeerConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("🗑️ Bier vernichten") }
            }

            TextButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) { Text("🗑️ Gebinde löschen") }
        }
    }

    // ── Sub-dialogs ───────────────────────────────────────────────────────────
    if (showMove) {
        MoveLocationDialog(locations, {
            onMove(ids, it)
            showMove = false
        }, { showMove = false })
    }
    if (showFill) {
        PickerDialog("Bier auswählen", beers.map { it.name to it.id }, {
            onFill(ids, it)
            showFill = false
        }, { showFill = false })
    }

    if (showReserve) {
        AlertDialog(
            onDismissRequest = { showReserve = false },
            title = { Text("Reservieren für") },
            text = { OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Kundenname") }) },
            confirmButton = {
                TextButton(onClick = {
                    onReserve(ids, customerName)
                    showReserve = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showReserve = false }) { Text("Abbrechen") } },
        )
    }

    if (showSell) {
        SellDialog(
            reservedFor = container.reservedFor,
            brewers = brewers,
            onConfirm = { brewerId, customerName ->
                onSell(ids, brewerId, customerName)
                showSell = false
            },
            onDismiss = { showSell = false },
        )
    }
    if (showConsume) {
        PickerDialog("Brauer", brewers.map { it.name to it.id }, {
            onSelfConsume(ids, it)
            showConsume = false
        }, { showConsume = false })
    }
    if (showReturn) {
        TwoPickerDialog("Rückgabe", "Brauer", brewers.map { it.name to it.id }, "Rückgabeort", locations.filter { it.type in breweryLocationTypes }.map { it.name to it.id }, { b, l ->
            onContainerReturn(ids, b, l)
            showReturn = false
        }, { showReturn = false })
    }

    if (showDestroyBeerConfirm) {
        val qLabel = if (quantity == 1) "diesem Gebinde" else "$quantity Gebinden"
        AlertDialog(
            onDismissRequest = { showDestroyBeerConfirm = false },
            title = { Text("Bier vernichten?") },
            text = { Text("Das Bier in $qLabel wird als vernichtet markiert. Die Gebinde werden leer — der Inhalt geht verloren. Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = {
                    onDestroyBeer(ids)
                    showDestroyBeerConfirm = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Vernichten") }
            },
            dismissButton = { TextButton(onClick = { showDestroyBeerConfirm = false }) { Text("Abbrechen") } },
        )
    }

    if (showDeleteConfirm) {
        val qLabel = if (quantity == 1) "dieses Gebinde" else "diese $quantity Gebinde"
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Gebinde löschen?") },
            text = { Text("Du bist dabei, $qLabel endgültig aus dem System zu löschen. Alle zugehörigen Daten (Füllstand, Reservierungen) gehen verloren. Diese Aktion kann nicht rückgängig gemacht werden.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(ids)
                    showDeleteConfirm = false
                }, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Abbrechen") } },
        )
    }
}

@Composable
private fun SellDialog(
    reservedFor: String?,
    brewers: List<com.haertibraeu.hopledger.data.model.Brewer>,
    onConfirm: (brewerId: String, customerName: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedBrewerId by remember { mutableStateOf("") }
    var customerName by remember { mutableStateOf(reservedFor ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("💰 Verkaufen") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Brauer", style = MaterialTheme.typography.labelLarge)
                brewers.forEach { brewer ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedBrewerId == brewer.id, onClick = { selectedBrewerId = brewer.id })
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
                        label = { Text("Kundenname") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selectedBrewerId.isNotBlank() && customerName.isNotBlank()) onConfirm(selectedBrewerId, customerName) },
                enabled = selectedBrewerId.isNotBlank() && customerName.isNotBlank(),
            ) { Text("Verkaufen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun MoveLocationDialog(
    locations: List<com.haertibraeu.hopledger.data.model.Location>,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val primary = locations.filter { it.type == "brewer" || it.type == "brewery" }
    val secondary = locations.filter { it.type != "brewer" && it.type != "brewery" }
    var showOthers by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neuer Standort") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                primary.forEach { loc ->
                    TextButton(onClick = { onSelect(loc.id) }, modifier = Modifier.fillMaxWidth()) { Text(loc.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start) }
                }
                if (secondary.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    TextButton(onClick = { showOthers = !showOthers }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            if (showOthers) "▲ Weitere ausblenden" else "▼ Weitere anzeigen (${secondary.size})",
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Start,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (showOthers) {
                        secondary.forEach { loc ->
                            TextButton(onClick = { onSelect(loc.id) }, modifier = Modifier.fillMaxWidth()) { Text(loc.name, modifier = Modifier.weight(1f), textAlign = TextAlign.Start) }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun PickerDialog(title: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(modifier = Modifier.verticalScroll(rememberScrollState())) { options.forEach { (n, id) -> TextButton(onClick = { onSelect(id) }, modifier = Modifier.fillMaxWidth()) { Text(n) } } } },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun TwoPickerDialog(title: String, label1: String, options1: List<Pair<String, String>>, label2: String, options2: List<Pair<String, String>>, onConfirm: (String, String) -> Unit, onDismiss: () -> Unit) {
    var s1 by remember { mutableStateOf("") }
    var s2 by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(label1, style = MaterialTheme.typography.labelLarge)
                options1.forEach { (n, id) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s1 == id, onClick = { s1 = id })
                        Text(n)
                    }
                }
                Text(label2, style = MaterialTheme.typography.labelLarge)
                options2.forEach { (n, id) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = s2 == id, onClick = { s2 = id })
                        Text(n)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { if (s1.isNotBlank() && s2.isNotBlank()) onConfirm(s1, s2) }, enabled = s1.isNotBlank() && s2.isNotBlank()) { Text("Bestätigen") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

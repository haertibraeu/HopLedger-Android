package com.haertibraeu.hopledger.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(viewModel: InventoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Filter chips
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = uiState.filterEmpty == false,
                    onClick = { viewModel.setFilterEmpty(if (uiState.filterEmpty == false) null else false) },
                    label = { Text("Gefüllt") },
                )
                FilterChip(
                    selected = uiState.filterEmpty == true,
                    onClick = { viewModel.setFilterEmpty(if (uiState.filterEmpty == true) null else true) },
                    label = { Text("Leer") },
                )
                FilterChip(
                    selected = uiState.filterReserved == true,
                    onClick = { viewModel.setFilterReserved(if (uiState.filterReserved == true) null else true) },
                    label = { Text("Reserviert") },
                )
            }

            if (uiState.isLoading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            uiState.error?.let { error ->
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(uiState.containers, key = { it.id }) { container ->
                    ContainerCard(container) { viewModel.selectContainer(container) }
                }

                if (uiState.containers.isEmpty() && !uiState.isLoading) {
                    item {
                        Text(
                            "Keine Gebinde gefunden",
                            modifier = Modifier.padding(32.dp).fillMaxWidth(),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = viewModel::showAddDialog,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, "Gebinde hinzufügen")
        }
    }

    // Add container dialog
    if (uiState.showAddDialog) {
        AddContainerDialog(
            containerTypes = uiState.containerTypes,
            locations = uiState.locations,
            beers = uiState.beers,
            onConfirm = { ctId, locId, beerId -> viewModel.addContainer(ctId, locId, beerId) },
            onDismiss = viewModel::dismissAddDialog,
        )
    }

    // Container action bottom sheet
    if (uiState.showActionSheet && uiState.selectedContainer != null) {
        ContainerActionSheet(
            container = uiState.selectedContainer!!,
            brewers = uiState.brewers,
            beers = uiState.beers,
            locations = uiState.locations,
            onDismiss = viewModel::dismissSheet,
            onMove = { locationId -> viewModel.moveContainer(uiState.selectedContainer!!.id, locationId) },
            onFill = { beerId -> viewModel.fillContainer(uiState.selectedContainer!!.id, beerId) },
            onDestroyBeer = { viewModel.destroyBeer(uiState.selectedContainer!!.id) },
            onReserve = { name -> viewModel.reserveContainer(uiState.selectedContainer!!.id, name) },
            onUnreserve = { viewModel.unreserveContainer(uiState.selectedContainer!!.id) },
            onSell = { brewerId, locId -> viewModel.sell(uiState.selectedContainer!!.id, brewerId, locId) },
            onSelfConsume = { brewerId -> viewModel.selfConsume(uiState.selectedContainer!!.id, brewerId) },
            onContainerReturn = { brewerId, locId -> viewModel.containerReturn(uiState.selectedContainer!!.id, brewerId, locId) },
            onDelete = { viewModel.deleteContainer(uiState.selectedContainer!!.id) },
        )
    }
}

@Composable
private fun ContainerCard(container: com.haertibraeu.hopledger.data.model.Container, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    container.containerType?.name ?: "Unknown type",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                if (container.isReserved) {
                    AssistChip(onClick = {}, label = { Text("Reserviert: ${container.reservedFor}") })
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (container.isEmpty) "🫙 Leer" else "🍺 ${container.beer?.name ?: "Unbekannt"}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                "📍 ${container.location?.name ?: "Unbekannt"}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContainerActionSheet(
    container: com.haertibraeu.hopledger.data.model.Container,
    brewers: List<com.haertibraeu.hopledger.data.model.Brewer>,
    beers: List<com.haertibraeu.hopledger.data.model.Beer>,
    locations: List<com.haertibraeu.hopledger.data.model.Location>,
    onDismiss: () -> Unit,
    onMove: (String) -> Unit,
    onFill: (String) -> Unit,
    onDestroyBeer: () -> Unit,
    onReserve: (String) -> Unit,
    onUnreserve: () -> Unit,
    onSell: (String, String) -> Unit,
    onSelfConsume: (String) -> Unit,
    onContainerReturn: (String, String) -> Unit,
    onDelete: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("${container.containerType?.name}", style = MaterialTheme.typography.titleMedium)

            // Move
            var showMoveDialog by remember { mutableStateOf(false) }
            TextButton(onClick = { showMoveDialog = true }) { Text("📦 Verschieben") }
            if (showMoveDialog) {
                PickerDialog("Neuer Standort", locations.map { it.name to it.id }, { onMove(it); showMoveDialog = false }, { showMoveDialog = false })
            }

            // Fill (only if empty)
            if (container.isEmpty) {
                var showFillDialog by remember { mutableStateOf(false) }
                TextButton(onClick = { showFillDialog = true }) { Text("🍺 Befüllen") }
                if (showFillDialog) {
                    PickerDialog("Bier auswählen", beers.map { it.name to it.id }, { onFill(it); showFillDialog = false }, { showFillDialog = false })
                }
            }

            // Destroy beer (only if filled)
            if (!container.isEmpty) {
                TextButton(onClick = onDestroyBeer) { Text("🗑️ Bier vernichten") }
            }

            // Reserve / Unreserve
            if (!container.isEmpty && !container.isReserved) {
                var showReserveDialog by remember { mutableStateOf(false) }
                var customerName by remember { mutableStateOf("") }
                TextButton(onClick = { showReserveDialog = true }) { Text("📋 Reservieren") }
                if (showReserveDialog) {
                    AlertDialog(
                        onDismissRequest = { showReserveDialog = false },
                        title = { Text("Reservieren für") },
                        text = { OutlinedTextField(value = customerName, onValueChange = { customerName = it }, label = { Text("Kundenname") }) },
                        confirmButton = { TextButton(onClick = { onReserve(customerName); showReserveDialog = false }) { Text("OK") } },
                        dismissButton = { TextButton(onClick = { showReserveDialog = false }) { Text("Abbrechen") } },
                    )
                }
            }
            if (container.isReserved) {
                TextButton(onClick = onUnreserve) { Text("📋 Reservierung aufheben") }
            }

            HorizontalDivider()

            // Combined actions (only if filled)
            if (!container.isEmpty) {
                var showSellDialog by remember { mutableStateOf(false) }
                TextButton(onClick = { showSellDialog = true }) {
                    Text("💰 Verkaufen (${container.containerType?.externalPrice ?: 0}€ + ${container.containerType?.depositFee ?: 0}€ Pfand)")
                }
                if (showSellDialog) {
                    TwoPickerDialog(
                        "Verkaufen", "Brauer", brewers.map { it.name to it.id }, "Kundenstandort", locations.map { it.name to it.id },
                        { b, l -> onSell(b, l); showSellDialog = false }, { showSellDialog = false },
                    )
                }

                var showConsumeDialog by remember { mutableStateOf(false) }
                TextButton(onClick = { showConsumeDialog = true }) {
                    Text("🍻 Eigenverbrauch (${container.containerType?.internalPrice ?: 0}€)")
                }
                if (showConsumeDialog) {
                    PickerDialog("Brauer", brewers.map { it.name to it.id }, { onSelfConsume(it); showConsumeDialog = false }, { showConsumeDialog = false })
                }
            }

            // Container return
            var showReturnDialog by remember { mutableStateOf(false) }
            TextButton(onClick = { showReturnDialog = true }) {
                Text("↩️ Rückgabe (${container.containerType?.depositFee ?: 0}€ Pfand)")
            }
            if (showReturnDialog) {
                TwoPickerDialog(
                    "Rückgabe", "Brauer", brewers.map { it.name to it.id }, "Rückgabeort", locations.map { it.name to it.id },
                    { b, l -> onContainerReturn(b, l); showReturnDialog = false }, { showReturnDialog = false },
                )
            }

            HorizontalDivider()
            TextButton(onClick = onDelete, colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text("🗑️ Gebinde löschen")
            }
        }
    }
}

@Composable
private fun PickerDialog(title: String, options: List<Pair<String, String>>, onSelect: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn {
                items(options.size) { idx ->
                    TextButton(onClick = { onSelect(options[idx].second) }, modifier = Modifier.fillMaxWidth()) {
                        Text(options[idx].first)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun TwoPickerDialog(
    title: String,
    label1: String, options1: List<Pair<String, String>>,
    label2: String, options2: List<Pair<String, String>>,
    onConfirm: (String, String) -> Unit, onDismiss: () -> Unit,
) {
    var selected1 by remember { mutableStateOf("") }
    var selected2 by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(label1, style = MaterialTheme.typography.labelLarge)
                options1.forEach { (name, id) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected1 == id, onClick = { selected1 = id })
                        Text(name)
                    }
                }
                Text(label2, style = MaterialTheme.typography.labelLarge)
                options2.forEach { (name, id) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selected2 == id, onClick = { selected2 = id })
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (selected1.isNotBlank() && selected2.isNotBlank()) onConfirm(selected1, selected2) },
                enabled = selected1.isNotBlank() && selected2.isNotBlank(),
            ) { Text("Bestätigen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

@Composable
private fun AddContainerDialog(
    containerTypes: List<com.haertibraeu.hopledger.data.model.ContainerType>,
    locations: List<com.haertibraeu.hopledger.data.model.Location>,
    beers: List<com.haertibraeu.hopledger.data.model.Beer>,
    onConfirm: (containerTypeId: String, locationId: String, beerId: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedTypeId by remember { mutableStateOf("") }
    var selectedLocationId by remember { mutableStateOf("") }
    var selectedBeerId by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Gebinde hinzufügen") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Gebindetyp", style = MaterialTheme.typography.labelLarge)
                if (containerTypes.isEmpty()) {
                    Text("Keine Gebindetypen vorhanden — erst in Einstellungen anlegen.", color = MaterialTheme.colorScheme.error)
                }
                containerTypes.forEach { ct ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedTypeId == ct.id, onClick = { selectedTypeId = ct.id })
                        Text("${ct.name} (${ct.externalPrice}€)")
                    }
                }

                Text("Standort", style = MaterialTheme.typography.labelLarge)
                locations.forEach { loc ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedLocationId == loc.id, onClick = { selectedLocationId = loc.id })
                        Text("${loc.name} [${loc.type}]")
                    }
                }

                Text("Bier (optional)", style = MaterialTheme.typography.labelLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = selectedBeerId == "", onClick = { selectedBeerId = "" })
                    Text("Leer")
                }
                beers.forEach { beer ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedBeerId == beer.id, onClick = { selectedBeerId = beer.id })
                        Text(beer.name)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedTypeId, selectedLocationId, selectedBeerId.ifBlank { null }) },
                enabled = selectedTypeId.isNotBlank() && selectedLocationId.isNotBlank(),
            ) { Text("Hinzufügen") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Abbrechen") } },
    )
}

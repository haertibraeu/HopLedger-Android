package com.haertibraeu.hopledger.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Backend connection
        item {
            Text("Verbindung", style = MaterialTheme.typography.titleMedium)
        }
        item {
            OutlinedTextField(
                value = uiState.backendUrl,
                onValueChange = viewModel::onBackendUrlChanged,
                label = { Text("Backend URL") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            OutlinedTextField(
                value = uiState.apiKey,
                onValueChange = viewModel::onApiKeyChanged,
                label = { Text("API Key (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = viewModel::saveSettings) { Text("Speichern") }
                Button(onClick = viewModel::checkHealth) { Text("Verbindung testen") }
            }
        }
        item {
            if (uiState.healthStatus.isNotEmpty()) {
                Text(
                    uiState.healthStatus,
                    color = if (uiState.healthOk) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
            }
        }

        // Master data sections
        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // Brewers
        item {
            SettingsSection(
                title = "Brauer",
                items = uiState.brewers.map { it.name },
                onAdd = viewModel::showAddBrewer,
                onDelete = { idx -> uiState.brewers.getOrNull(idx)?.let { viewModel.deleteBrewer(it.id) } },
            )
        }

        // Beers
        item {
            SettingsSection(
                title = "Biere",
                items = uiState.beers.map { "${it.name}${it.style?.let { s -> " ($s)" } ?: ""}" },
                onAdd = viewModel::showAddBeer,
                onDelete = { idx -> uiState.beers.getOrNull(idx)?.let { viewModel.deleteBeer(it.id) } },
            )
        }

        // Locations
        item {
            SettingsSection(
                title = "Standorte",
                items = uiState.locations.map { "${it.name} [${it.type}]" },
                onAdd = viewModel::showAddLocation,
                onDelete = { idx -> uiState.locations.getOrNull(idx)?.let { viewModel.deleteLocation(it.id) } },
            )
        }

        // Container Types
        item {
            SettingsSection(
                title = "Gebindetypen",
                items = uiState.containerTypes.map { "${it.name} (${it.externalPrice}€/${it.internalPrice}€, Pfand: ${it.depositFee}€)" },
                onAdd = viewModel::showAddContainerType,
                onDelete = { idx -> uiState.containerTypes.getOrNull(idx)?.let { viewModel.deleteContainerType(it.id) } },
            )
        }
    }

    // Dialogs
    if (uiState.showAddBrewerDialog) AddBrewerDialog(viewModel)
    if (uiState.showAddBeerDialog) AddBeerDialog(viewModel)
    if (uiState.showAddLocationDialog) AddLocationDialog(viewModel)
    if (uiState.showAddContainerTypeDialog) AddContainerTypeDialog(viewModel)
}

@Composable
private fun SettingsSection(title: String, items: List<String>, onAdd: () -> Unit, onDelete: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Hinzufügen") }
        }
        items.forEachIndexed { index, item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(item, modifier = Modifier.weight(1f))
                IconButton(onClick = { onDelete(index) }) {
                    Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (items.isEmpty()) {
            Text("Keine Einträge", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun AddBrewerDialog(viewModel: SettingsViewModel) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = viewModel::dismissDialogs,
        title = { Text("Brauer hinzufügen") },
        text = { OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }) },
        confirmButton = { TextButton(onClick = { viewModel.addBrewer(name); name = "" }) { Text("Hinzufügen") } },
        dismissButton = { TextButton(onClick = viewModel::dismissDialogs) { Text("Abbrechen") } },
    )
}

@Composable
private fun AddBeerDialog(viewModel: SettingsViewModel) {
    var name by remember { mutableStateOf("") }
    var style by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = viewModel::dismissDialogs,
        title = { Text("Bier hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = style, onValueChange = { style = it }, label = { Text("Stil (optional)") })
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.addBeer(name, style.ifBlank { null }) }) { Text("Hinzufügen") } },
        dismissButton = { TextButton(onClick = viewModel::dismissDialogs) { Text("Abbrechen") } },
    )
}

@Composable
private fun AddLocationDialog(viewModel: SettingsViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("general") }
    AlertDialog(
        onDismissRequest = viewModel::dismissDialogs,
        title = { Text("Standort hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Typ (general/brewer/brewery/customer)") })
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.addLocation(name, type) }) { Text("Hinzufügen") } },
        dismissButton = { TextButton(onClick = viewModel::dismissDialogs) { Text("Abbrechen") } },
    )
}

@Composable
private fun AddContainerTypeDialog(viewModel: SettingsViewModel) {
    var name by remember { mutableStateOf("") }
    var externalPrice by remember { mutableStateOf("") }
    var internalPrice by remember { mutableStateOf("") }
    var depositFee by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = viewModel::dismissDialogs,
        title = { Text("Gebindetyp hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name (z.B. 0.5l Flasche)") })
                OutlinedTextField(value = externalPrice, onValueChange = { externalPrice = it }, label = { Text("Verkaufspreis (€)") })
                OutlinedTextField(value = internalPrice, onValueChange = { internalPrice = it }, label = { Text("Eigenverbrauch (€)") })
                OutlinedTextField(value = depositFee, onValueChange = { depositFee = it }, label = { Text("Pfand (€)") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.addContainerType(name, externalPrice.toDoubleOrNull() ?: 0.0, internalPrice.toDoubleOrNull() ?: 0.0, depositFee.toDoubleOrNull() ?: 0.0)
            }) { Text("Hinzufügen") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissDialogs) { Text("Abbrechen") } },
    )
}

package com.haertibraeu.hopledger.ui.accounting

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haertibraeu.hopledger.data.model.Balance

@Composable
fun AccountingScreen(viewModel: AccountingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Balances card
            item {
                Text("Kontostände", style = MaterialTheme.typography.titleLarge)
            }
            items(uiState.balances) { balance ->
                BalanceCard(balance)
            }
            if (uiState.balances.isEmpty() && !uiState.isLoading) {
                item { Text("Keine Kontostände vorhanden", modifier = Modifier.padding(8.dp)) }
            }

            item {
                Button(onClick = viewModel::calculateSettlements, modifier = Modifier.fillMaxWidth()) {
                    Text("🤝 Ausgleichszahlungen berechnen")
                }
            }

            // Filter by brewer
            item { HorizontalDivider() }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = uiState.selectedBrewerId == null,
                        onClick = { viewModel.filterByBrewer(null) },
                        label = { Text("Alle") },
                    )
                    uiState.brewers.forEach { brewer ->
                        FilterChip(
                            selected = uiState.selectedBrewerId == brewer.id,
                            onClick = { viewModel.filterByBrewer(brewer.id) },
                            label = { Text(brewer.name) },
                        )
                    }
                }
            }

            // Entries
            item { Text("Buchungen", style = MaterialTheme.typography.titleMedium) }

            if (uiState.isLoading) {
                item { LinearProgressIndicator(modifier = Modifier.fillMaxWidth()) }
            }

            uiState.error?.let { error ->
                item { Text(error, color = MaterialTheme.colorScheme.error) }
            }

            items(uiState.entries) { entry ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(entry.type, style = MaterialTheme.typography.labelMedium)
                            Text(
                                "${if (entry.amount >= 0) "+" else ""}${"%.2f".format(entry.amount)} CHF",
                                fontWeight = FontWeight.Bold,
                                color = if (entry.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                        Text(entry.description ?: "", style = MaterialTheme.typography.bodySmall)
                        entry.brewer?.let { brewer ->
                            Text("Brauer: ${brewer.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (uiState.entries.isEmpty() && !uiState.isLoading) {
                item { Text("Keine Buchungen vorhanden", modifier = Modifier.padding(8.dp)) }
            }
        }

        // FAB for manual entry
        FloatingActionButton(
            onClick = viewModel::showManualEntryDialog,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, "Manuelle Buchung")
        }
    }

    // Settlements dialog
    if (uiState.showSettlements) {
        AlertDialog(
            onDismissRequest = viewModel::dismissSettlements,
            title = { Text("Ausgleichszahlungen") },
            text = {
                if (uiState.settlements.isEmpty()) {
                    Text("Keine Ausgleichszahlungen nötig — alle Konten sind ausgeglichen! ✅")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.settlements.forEach { s ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text("${s.from.name} → ${s.to.name}")
                                    Text("${"%.2f".format(s.amount)} CHF", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = viewModel::dismissSettlements) { Text("OK") } },
        )
    }

    // Manual entry dialog
    if (uiState.showManualEntryDialog) {
        ManualEntryDialog(viewModel)
    }
}

@Composable
private fun BalanceCard(balance: Balance) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(balance.brewerName, style = MaterialTheme.typography.titleSmall)
            Text(
                "${"%.2f".format(balance.balance)} CHF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    balance.balance > 0 -> MaterialTheme.colorScheme.primary
                    balance.balance < 0 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

@Composable
private fun ManualEntryDialog(viewModel: AccountingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedBrewerId by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("manual") }

    AlertDialog(
        onDismissRequest = viewModel::dismissManualEntryDialog,
        title = { Text("Manuelle Buchung") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Brauer", style = MaterialTheme.typography.labelLarge)
                uiState.brewers.forEach { brewer ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedBrewerId == brewer.id, onClick = { selectedBrewerId = brewer.id })
                        Text(brewer.name)
                    }
                }
                OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Betrag (CHF)") })
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Beschreibung") })
                OutlinedTextField(value = type, onValueChange = { type = it }, label = { Text("Typ") })
            }
        },
        confirmButton = {
            TextButton(
                onClick = { amount.toDoubleOrNull()?.let { viewModel.addManualEntry(selectedBrewerId, it, description, type) } },
                enabled = selectedBrewerId.isNotBlank() && amount.toDoubleOrNull() != null,
            ) { Text("Buchen") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissManualEntryDialog) { Text("Abbrechen") } },
    )
}

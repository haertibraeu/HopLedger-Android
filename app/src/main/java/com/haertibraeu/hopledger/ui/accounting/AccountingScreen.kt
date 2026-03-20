package com.haertibraeu.hopledger.ui.accounting

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Settlements (split-bills core view) ───────────────────────
            item {
                Text("🤝 Ausgleich", style = MaterialTheme.typography.titleLarge)
            }
            item {
                SettlementsCard(settlements = uiState.settlements)
            }

            // ── Per-brewer balances ───────────────────────────────────────
            item {
                Text("Kontostände", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 4.dp))
            }
            items(uiState.balances) { balance ->
                BalanceCard(balance)
            }
            if (uiState.balances.isEmpty() && !uiState.isLoading) {
                item { Text("Keine Brauer erfasst", modifier = Modifier.padding(8.dp)) }
            }

            // ── Transaction history ───────────────────────────────────────
            item { HorizontalDivider(modifier = Modifier.padding(top = 4.dp)) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Buchungen", style = MaterialTheme.typography.titleMedium)
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
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
                            Text(entry.type, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                            Text(
                                "${if (entry.amount >= 0) "+" else ""}${"%.2f".format(entry.amount)} CHF",
                                fontWeight = FontWeight.Bold,
                                color = if (entry.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            )
                        }
                        entry.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        entry.brewer?.let {
                            Text("${it.name}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (uiState.entries.isEmpty() && !uiState.isLoading) {
                item { Text("Keine Buchungen vorhanden", modifier = Modifier.padding(8.dp)) }
            }
        }

        FloatingActionButton(
            onClick = viewModel::showManualEntryDialog,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Icon(Icons.Default.Add, "Zahlung erfassen")
        }
    }

    if (uiState.showManualEntryDialog) {
        ManualEntryDialog(viewModel)
    }
}

// ── Settlements card ──────────────────────────────────────────────────────────

@Composable
private fun SettlementsCard(settlements: List<com.haertibraeu.hopledger.data.model.Settlement>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (settlements.isEmpty()) MaterialTheme.colorScheme.secondaryContainer
                             else MaterialTheme.colorScheme.errorContainer,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (settlements.isEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✅", style = MaterialTheme.typography.titleMedium)
                    Text("Alle Konten sind ausgeglichen", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                settlements.forEach { s ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "${s.from.name}  →  ${s.to.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${"%.2f".format(s.amount)} CHF",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

// ── Balance card ──────────────────────────────────────────────────────────────

@Composable
private fun BalanceCard(balance: Balance) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(balance.brewerName, style = MaterialTheme.typography.titleSmall)
                Text(
                    when {
                        balance.balance > 0.005 -> "bekommt noch Geld"
                        balance.balance < -0.005 -> "schuldet Geld"
                        else -> "ausgeglichen"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${"%.2f".format(balance.balance)} CHF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    balance.balance > 0.005 -> MaterialTheme.colorScheme.primary
                    balance.balance < -0.005 -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// ── Manual entry dialog ───────────────────────────────────────────────────────

@Composable
private fun ManualEntryDialog(viewModel: AccountingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedBrewerId by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isExpense by remember { mutableStateOf(false) }

    // Brewer dropdown
    var brewerExpanded by remember { mutableStateOf(false) }
    val selectedBrewerName = uiState.brewers.find { it.id == selectedBrewerId }?.name ?: "Brauer auswählen…"

    AlertDialog(
        onDismissRequest = viewModel::dismissManualEntryDialog,
        title = { Text("Zahlung erfassen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Expense / income toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterChip(
                        selected = !isExpense,
                        onClick = { isExpense = false },
                        label = { Text("💰 Einnahme") },
                        modifier = Modifier.weight(1f),
                    )
                    FilterChip(
                        selected = isExpense,
                        onClick = { isExpense = true },
                        label = { Text("🧾 Ausgabe") },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Brewer picker
                Box {
                    OutlinedButton(onClick = { brewerExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(selectedBrewerName, modifier = Modifier.weight(1f))
                    }
                    DropdownMenu(expanded = brewerExpanded, onDismissRequest = { brewerExpanded = false }) {
                        uiState.brewers.forEach { brewer ->
                            DropdownMenuItem(text = { Text(brewer.name) }, onClick = { selectedBrewerId = brewer.id; brewerExpanded = false })
                        }
                    }
                }

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("\\d*\\.?\\d*"))) amountText = it },
                    label = { Text("Betrag (CHF)") },
                    prefix = { Text(if (isExpense) "−" else "+") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Beschreibung (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val amt = amountText.toDoubleOrNull() ?: return@TextButton
                    val signed = if (isExpense) -amt else amt
                    viewModel.addManualEntry(selectedBrewerId, signed, description, "manual")
                },
                enabled = selectedBrewerId.isNotBlank() && amountText.toDoubleOrNull() != null,
            ) { Text("Buchen") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissManualEntryDialog) { Text("Abbrechen") } },
    )
}



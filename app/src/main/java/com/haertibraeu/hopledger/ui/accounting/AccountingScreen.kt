package com.haertibraeu.hopledger.ui.accounting

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haertibraeu.hopledger.data.model.AccountEntry
import com.haertibraeu.hopledger.data.model.Balance
import com.haertibraeu.hopledger.data.model.Settlement

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountingScreen(viewModel: AccountingViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    // Refresh every time this screen enters composition (tab switches, navigation back)
    LaunchedEffect(Unit) { viewModel.refresh() }

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
                SettlementsCard(
                    settlements = uiState.settlements,
                    onSettle = viewModel::confirmBookSettlement,
                )
            }

            // ── Per-brewer relative balances ──────────────────────────────
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
                EntryCard(
                    entry = entry,
                    onLongPress = { viewModel.confirmDeleteEntry(entry) },
                )
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

    uiState.entryToDelete?.let { entry ->
        DeleteEntryDialog(
            entry = entry,
            onConfirm = viewModel::deleteEntry,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }

    uiState.settlementToBook?.let { settlement ->
        BookSettlementDialog(
            settlement = settlement,
            onConfirm = viewModel::bookSettlement,
            onDismiss = viewModel::dismissSettlementDialog,
        )
    }
}

// ── Entry type display labels ─────────────────────────────────────────────────

private fun entryTypeLabel(type: String) = when (type) {
    "sale"             -> "Verkauf"
    "container_return" -> "Pfand"
    "self_consume"     -> "Eigenverbrauch"
    "settlement"       -> "Ausgleich"
    "manual"           -> "Manuell"
    "fill"             -> "Abfüllung"
    "batch_fill"       -> "Abfüllung"
    else               -> type.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

// ── Entry card (long-press to delete) ─────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryCard(entry: AccountEntry, onLongPress: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = {},
                onLongClick = onLongPress,
            ),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val categoryEmoji = when (entry.category?.type) {
                        "income" -> "💰"
                        "expense" -> "💸"
                        else -> if (entry.amount >= 0) "💰" else "💸"
                    }
                    Text(
                        "$categoryEmoji ${entry.category?.name ?: entryTypeLabel(entry.type)}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Text(
                    "${if (entry.amount >= 0) "+" else ""}${"%.2f".format(entry.amount)} CHF",
                    fontWeight = FontWeight.Bold,
                    color = if (entry.amount >= 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                )
            }
            entry.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
            entry.brewer?.let {
                Text(it.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// ── Delete confirmation dialog ────────────────────────────────────────────────

@Composable
private fun DeleteEntryDialog(entry: AccountEntry, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Buchung löschen?") },
        text = {
            Text("${entry.category?.name ?: entryTypeLabel(entry.type)}: ${"%.2f".format(entry.amount)} CHF" +
                (entry.description?.let { "\n$it" } ?: ""))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Löschen", color = MaterialTheme.colorScheme.error) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

// ── Settlements card ──────────────────────────────────────────────────────────

@Composable
private fun SettlementsCard(
    settlements: List<Settlement>,
    onSettle: (Settlement) -> Unit,
) {
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
                Text(
                    "Tippe auf eine Zahlung, um sie zu buchen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                settlements.forEach { s ->
                    Surface(
                        onClick = { onSettle(s) },
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 4.dp),
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
                        balance.balance > 0.005 -> "schuldet der Gemeinschaft"
                        balance.balance < -0.005 -> "bekommt zurück"
                        else -> "ausgeglichen"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "${"%.2f".format(kotlin.math.abs(balance.balance))} CHF",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    balance.balance > 0.005 -> MaterialTheme.colorScheme.error
                    balance.balance < -0.005 -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
    }
}

// ── Book settlement dialog ────────────────────────────────────────────────────

@Composable
private fun BookSettlementDialog(
    settlement: Settlement,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ausgleich buchen?") },
        text = {
            Text(
                "${settlement.from.name} zahlt ${"%.2f".format(settlement.amount)} CHF an ${settlement.to.name}.\n\n" +
                "Dies wird als neue Buchung erfasst."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Buchen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

// ── Manual entry dialog ──────────────────────────────────────────────────────

@Composable
private fun ManualEntryDialog(viewModel: AccountingViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedBrewerId by remember { mutableStateOf(uiState.brewers.firstOrNull()?.id ?: "") }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    // Update selection if it was empty but brewers are now available
    if (selectedBrewerId.isEmpty() && uiState.brewers.isNotEmpty()) {
        selectedBrewerId = uiState.brewers.first().id
    }

    // Auto-apply sign when category changes
    val selectedCategory = uiState.categories.find { it.id == selectedCategoryId }

    val selectedBrewerName = uiState.brewers.find { it.id == selectedBrewerId }?.name ?: "Auswählen…"
    val selectedCategoryName = selectedCategory?.let {
        "${if (it.type == "income") "💰" else "💸"} ${it.name}"
    } ?: "Keine Kategorie"

    AlertDialog(
        onDismissRequest = viewModel::dismissManualEntryDialog,
        title = { Text("Zahlung erfassen") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Brauer", style = MaterialTheme.typography.labelLarge)
                SpinnerField(
                    value = selectedBrewerName,
                    options = uiState.brewers.map { it.name to it.id },
                    onSelect = { selectedBrewerId = it ?: "" }
                )

                Text("Kategorie", style = MaterialTheme.typography.labelLarge)
                SpinnerField(
                    value = selectedCategoryName,
                    options = listOf("Keine Kategorie" to null) + uiState.categories.map {
                        "${if (it.type == "income") "💰" else "💸"} ${it.name}" to it.id
                    },
                    onSelect = { selectedCategoryId = it }
                )

                Text("Betrag (CHF)", style = MaterialTheme.typography.labelLarge)
                val isExpense = selectedCategory?.type == "expense"
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.replace(',', '.').trimStart('-') },
                    label = { Text("z.B. 15.50") },
                    prefix = {
                        Text(
                            if (isExpense) "−" else "+",
                            color = if (isExpense) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Beschreibung", style = MaterialTheme.typography.labelLarge)
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Zweck (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val absAmount = amountText.toDoubleOrNull()?.let { kotlin.math.abs(it) } ?: 0.0
                    val finalAmount = if (selectedCategory?.type == "expense") -absAmount else absAmount
                    viewModel.addManualEntry(selectedBrewerId, finalAmount, description, "manual", selectedCategoryId)
                },
                enabled = selectedBrewerId.isNotBlank() && amountText.toDoubleOrNull() != null
            ) {
                Text("Buchen")
            }
        },
        dismissButton = {
            TextButton(onClick = viewModel::dismissManualEntryDialog) {
                Text("Abbrechen")
            }
        }
    )
}

@Composable
private fun SpinnerField(
    value: String,
    options: List<Pair<String, String?>>,
    onSelect: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                value,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ArrowDropDown, null)
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { (label, id) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(id)
                        expanded = false
                    }
                )
            }
        }
    }
}

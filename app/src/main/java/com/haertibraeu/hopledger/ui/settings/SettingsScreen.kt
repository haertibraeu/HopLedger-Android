package com.haertibraeu.hopledger.ui.settings

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.haertibraeu.hopledger.BuildConfig
import com.haertibraeu.hopledger.R
import com.haertibraeu.hopledger.data.model.ContainerType
import com.haertibraeu.hopledger.data.model.Location
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    var gebindetypenExpanded by remember { mutableStateOf(false) }
    var biereExpanded by remember { mutableStateOf(false) }
    var brauerExpanded by remember { mutableStateOf(false) }
    var kategorienExpanded by remember { mutableStateOf(false) }
    var standorteExpanded by remember { mutableStateOf(false) }
    var backupExpanded by remember { mutableStateOf(false) }

    // Delete confirmation state: holds a pair of (message, onConfirm)
    var pendingDelete by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    val scanLauncher = rememberLauncherForActivityResult(ScanContract()) { result ->
        result.contents?.let { viewModel.applyQrPayload(it) }
    }

    val restoreFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let { viewModel.onRestoreFileSelected(it) } }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ── Biere ────────────────────────────────────────────────────────────
        item {
            CollapsibleSectionHeader("🍺 Biere", biereExpanded) { biereExpanded = !biereExpanded }
        }
        item {
            AnimatedVisibility(visible = biereExpanded) {
                SettingsSection(
                    title = "Biere",
                    items = uiState.beers.map { "${it.name}${it.style?.let { s -> " ($s)" } ?: ""}" },
                    onAdd = viewModel::showAddBeer,
                    onDelete = { idx ->
                        uiState.beers.getOrNull(idx)?.let { beer ->
                            pendingDelete = "Bier \"${beer.name}\" löschen?\n\nDas Bier wird aus allen Gebinden entfernt, die es enthalten." to { viewModel.deleteBeer(beer.id) }
                        }
                    },
                )
            }
        }

        // ── Gebindetypen ─────────────────────────────────────────────────────
        item {
            CollapsibleSectionHeader("🫙 Gebindetypen", gebindetypenExpanded) { gebindetypenExpanded = !gebindetypenExpanded }
        }
        item {
            AnimatedVisibility(visible = gebindetypenExpanded) {
                ContainerTypesSection(
                    containerTypes = uiState.containerTypes,
                    onAdd = viewModel::showAddContainerType,
                    onEdit = { viewModel.showEditContainerType(it) },
                    onDelete = { ct ->
                        pendingDelete = "Gebindetyp \"${ct.name}\" löschen?\n\nKann nur gelöscht werden, wenn keine Gebinde dieses Typs mehr existieren." to { viewModel.deleteContainerType(ct.id) }
                    },
                )
            }
        }

        // ── Standorte ────────────────────────────────────────────────────────
        item {
            CollapsibleSectionHeader("📍 Standorte", standorteExpanded) { standorteExpanded = !standorteExpanded }
        }
        item {
            AnimatedVisibility(visible = standorteExpanded) {
                SettingsSection(
                    title = "Standorte",
                    items = uiState.locations.map { "${it.name} · ${locationTypeLabel(it.type)}" },
                    onAdd = viewModel::showAddLocation,
                    onDelete = { idx ->
                        uiState.locations.getOrNull(idx)?.let { loc ->
                            pendingDelete = "Standort \"${loc.name}\" löschen?\n\nAlle Gebinde an diesem Standort müssen zuerst verschoben werden." to { viewModel.deleteLocation(loc.id) }
                        }
                    },
                )
            }
        }

        // ── Finanzkategorien ─────────────────────────────────────────────────
        item {
            CollapsibleSectionHeader("📊 Finanzkategorien", kategorienExpanded) { kategorienExpanded = !kategorienExpanded }
        }
        item {
            AnimatedVisibility(visible = kategorienExpanded) {
                SettingsSection(
                    title = "Finanzkategorien",
                    items = uiState.categories.map { "${if (it.type == "income") "💰" else "💸"} ${it.name}" },
                    onAdd = viewModel::showAddCategory,
                    onDelete = { idx ->
                        uiState.categories.getOrNull(idx)?.let { cat ->
                            pendingDelete = "Kategorie \"${cat.name}\" löschen?\n\nBestehende Buchungen behalten ihre Kategorie — nur neue Buchungen können sie nicht mehr verwenden." to { viewModel.deleteCategory(cat.id) }
                        }
                    },
                )
            }
        }

        // ── Brauer ───────────────────────────────────────────────────────────
        item {
            CollapsibleSectionHeader("🧙 Brauer", brauerExpanded) { brauerExpanded = !brauerExpanded }
        }
        item {
            AnimatedVisibility(visible = brauerExpanded) {
                SettingsSection(
                    title = "Brauer",
                    items = uiState.brewers.map { it.name },
                    onAdd = viewModel::showAddBrewer,
                    onDelete = { idx ->
                        uiState.brewers.getOrNull(idx)?.let { brewer ->
                            pendingDelete = "Brauer \"${brewer.name}\" löschen?\n\nAlle Buchungen und Biere bleiben erhalten, aber der Brauer kann danach nicht mehr verwendet werden." to { viewModel.deleteBrewer(brewer.id) }
                        }
                    },
                )
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // ── Verbindung ────────────────────────────────────────────────────────
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = viewModel::toggleConnectionExpanded),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("🔗 Verbindung", style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        scanLauncher.launch(ScanOptions().apply {
                            setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            setPrompt("HopLedger QR-Code scannen")
                            setBeepEnabled(false)
                        })
                    }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "QR scannen")
                    }
                    IconButton(onClick = viewModel::toggleConnectionExpanded) {
                        Icon(
                            if (uiState.connectionExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (uiState.connectionExpanded) "Einklappen" else "Ausklappen",
                        )
                    }
                }
            }
        }
        item {
            AnimatedVisibility(visible = uiState.connectionExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = uiState.backendUrl,
                        onValueChange = viewModel::onBackendUrlChanged,
                        label = { Text("Backend URL") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = uiState.apiKey,
                        onValueChange = viewModel::onApiKeyChanged,
                        label = { Text("API Key (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(onClick = viewModel::saveSettings) { Text("Speichern") }
                        Button(onClick = viewModel::checkHealth) { Text("Verbindung testen") }
                        IconButton(onClick = viewModel::showQr) {
                            Icon(Icons.Default.QrCode2, contentDescription = "QR-Code anzeigen")
                        }
                    }
                    if (uiState.healthStatus.isNotEmpty()) {
                        Text(
                            uiState.healthStatus,
                            color = if (uiState.healthOk) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }

        // ── Backup & Restore ──────────────────────────────────────────────────
        item {
            CollapsibleSectionHeader("💾 Backup & Wiederherstellung", backupExpanded) { backupExpanded = !backupExpanded }
        }
        item {
            AnimatedVisibility(visible = backupExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
                    Button(onClick = viewModel::createBackup, modifier = Modifier.fillMaxWidth()) {
                        Text("Backup erstellen (in Downloads speichern)")
                    }
                    OutlinedButton(
                        onClick = { restoreFileLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Aus Backup wiederherstellen…")
                    }
                    uiState.backupMessage?.let { msg ->
                        Text(
                            msg,
                            color = if (msg.startsWith("✅")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.clickable { viewModel.clearBackupMessage() },
                        )
                    }
                }
            }
        }

        item { HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp)) }

        // ── About ─────────────────────────────────────────────────────────────
        item { AboutSection() }
    }

    // Delete confirmation dialog
    pendingDelete?.let { (message, onConfirm) ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Löschen bestätigen") },
            text = { Text(message) },
            confirmButton = {
                TextButton(
                    onClick = { onConfirm(); pendingDelete = null },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Löschen") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Abbrechen") } },
        )
    }

    // Dialogs
    if (uiState.showAddBrewerDialog) AddBrewerDialog(viewModel)
    if (uiState.showAddBeerDialog) AddBeerDialog(viewModel)
    if (uiState.showAddLocationDialog) AddLocationDialog(viewModel)
    if (uiState.showAddContainerTypeDialog) AddContainerTypeDialog(viewModel)
    if (uiState.showAddCategoryDialog) AddCategoryDialog(viewModel)
    uiState.editingContainerType?.let { EditContainerTypeDialog(it, viewModel) }
    if (uiState.showQrDialog) QrDialog(url = uiState.backendUrl, apiKey = uiState.apiKey, onDismiss = viewModel::dismissQr)

    if (uiState.showRestoreConfirmDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissRestoreConfirm,
            title = { Text("Daten wiederherstellen?") },
            text = { Text("Alle aktuellen Daten auf dem Server werden unwiderruflich überschrieben. Fortfahren?") },
            confirmButton = {
                TextButton(
                    onClick = viewModel::confirmRestore,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Wiederherstellen") }
            },
            dismissButton = { TextButton(onClick = viewModel::dismissRestoreConfirm) { Text("Abbrechen") } },
        )
    }
}

private fun locationTypeLabel(type: String) = when (type) {
    "brewer" -> "Brauer"
    "brewery" -> "Brauerei"
    "customer" -> "Kunde"
    else -> "Andere"
}

@Composable
private fun CollapsibleSectionHeader(title: String, expanded: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Icon(
            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
            contentDescription = if (expanded) "Einklappen" else "Ausklappen",
            modifier = Modifier.padding(8.dp),
        )
    }
}

@Composable
private fun ContainerTypesSection(
    containerTypes: List<ContainerType>,
    onAdd: () -> Unit,
    onEdit: (ContainerType) -> Unit,
    onDelete: (ContainerType) -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Gebindetypen", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            IconButton(onClick = onAdd) { Icon(Icons.Default.Add, "Hinzufügen") }
        }
        containerTypes.forEach { ct ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(ct.name)
                    Text(
                        "Verkauf: ${ct.externalPrice} CHF · Eigenverbrauch: ${ct.internalPrice} CHF · Pfand: ${ct.depositFee} CHF",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = { onEdit(ct) }) {
                    Icon(Icons.Default.Edit, "Bearbeiten", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = { onDelete(ct) }) {
                    Icon(Icons.Default.Delete, "Löschen", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
        if (containerTypes.isEmpty()) {
            Text("Keine Einträge", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(8.dp))
        }
    }
}

@Composable
private fun EditContainerTypeDialog(ct: ContainerType, viewModel: SettingsViewModel) {
    var name by remember(ct.id) { mutableStateOf(ct.name) }
    var externalPrice by remember(ct.id) { mutableStateOf(ct.externalPrice.toString()) }
    var internalPrice by remember(ct.id) { mutableStateOf(ct.internalPrice.toString()) }
    var depositFee by remember(ct.id) { mutableStateOf(ct.depositFee.toString()) }
    AlertDialog(
        onDismissRequest = viewModel::dismissDialogs,
        title = { Text("Gebindetyp bearbeiten") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = externalPrice, onValueChange = { externalPrice = it }, label = { Text("Verkaufspreis (CHF)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = internalPrice, onValueChange = { internalPrice = it }, label = { Text("Eigenverbrauch (CHF)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = depositFee, onValueChange = { depositFee = it }, label = { Text("Pfand (CHF)") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.updateContainerType(
                        ct.id, name,
                        externalPrice.toDoubleOrNull() ?: ct.externalPrice,
                        internalPrice.toDoubleOrNull() ?: ct.internalPrice,
                        depositFee.toDoubleOrNull() ?: ct.depositFee,
                    )
                },
                enabled = name.isNotBlank(),
            ) { Text("Speichern") }
        },
        dismissButton = { TextButton(onClick = viewModel::dismissDialogs) { Text("Abbrechen") } },
    )
}

@Composable
private fun SettingsSection(title: String, items: List<String>, onAdd: () -> Unit, onDelete: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddLocationDialog(viewModel: SettingsViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("other") }
    val types = listOf("brewer" to "Brauer", "brewery" to "Brauerei", "customer" to "Kunde", "other" to "Andere")
    AlertDialog(
        onDismissRequest = viewModel::dismissDialogs,
        title = { Text("Standort hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Text("Typ", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { (value, label) ->
                        FilterChip(
                            selected = type == value,
                            onClick = { type = value },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.addLocation(name, type) }, enabled = name.isNotBlank()) { Text("Hinzufügen") } },
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
                OutlinedTextField(value = externalPrice, onValueChange = { externalPrice = it }, label = { Text("Verkaufspreis (CHF)") })
                OutlinedTextField(value = internalPrice, onValueChange = { internalPrice = it }, label = { Text("Eigenverbrauch (CHF)") })
                OutlinedTextField(value = depositFee, onValueChange = { depositFee = it }, label = { Text("Pfand (CHF)") })
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddCategoryDialog(viewModel: SettingsViewModel) {
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("income") }
    AlertDialog(
        onDismissRequest = viewModel::dismissDialogs,
        title = { Text("Finanzkategorie hinzufügen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, modifier = Modifier.fillMaxWidth())
                Text("Typ", style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == "income", onClick = { type = "income" }, label = { Text("💰 Einnahme") })
                    FilterChip(selected = type == "expense", onClick = { type = "expense" }, label = { Text("💸 Ausgabe") })
                }
            }
        },
        confirmButton = { TextButton(onClick = { viewModel.addCategory(name, type) }, enabled = name.isNotBlank()) { Text("Hinzufügen") } },
        dismissButton = { TextButton(onClick = viewModel::dismissDialogs) { Text("Abbrechen") } },
    )
}

@Composable
private fun QrDialog(url: String, apiKey: String, onDismiss: () -> Unit) {
    val bitmap: Bitmap? = remember(url, apiKey) { buildQrBitmap(url, apiKey) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Verbindungs-QR-Code") },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "QR-Code",
                        modifier = Modifier.size(260.dp),
                    )
                } else {
                    Text("QR-Code konnte nicht erstellt werden.", color = MaterialTheme.colorScheme.error)
                }
                Text(
                    "Mit einem anderen Gerät scannen, um URL und API-Key zu übertragen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Schliessen") } },
    )
}

@Composable
private fun AboutSection() {
    val uriHandler = LocalUriHandler.current
    var libsExpanded by remember { mutableStateOf(false) }
    val openSourceLibraries = listOf(
        "Hilt / Dagger" to "https://dagger.dev/hilt/",
        "Retrofit" to "https://square.github.io/retrofit/",
        "OkHttp" to "https://square.github.io/okhttp/",
        "Kotlin Serialization" to "https://github.com/Kotlin/kotlinx.serialization",
        "ZXing / zxing-android-embedded" to "https://github.com/journeyapps/zxing-android-embedded",
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(Color(0xFF49494A), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(id = R.drawable.haertibraeu),
                contentDescription = "HÄRTIBRÄU Logo",
                modifier = Modifier.size(64.dp),
            )
        }
        Text("HopLedger", style = MaterialTheme.typography.titleMedium)
        Text(
            "Version ${BuildConfig.VERSION_NAME}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "Made by the Brewers of HÄRTIBRÄU",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.clickable { uriHandler.openUri("https://haertibraeu.ch/") },
        )
        Text(
            "MIT License",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FilterChip(
            selected = libsExpanded,
            onClick = { libsExpanded = !libsExpanded },
            label = { Text("Open-Source Libraries") },
            trailingIcon = {
                Icon(
                    if (libsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                )
            },
        )
        AnimatedVisibility(visible = libsExpanded) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                openSourceLibraries.forEach { (name, url) ->
                    Text(
                        "• $name",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { uriHandler.openUri(url) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

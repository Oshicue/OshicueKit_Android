package com.oshicue.kit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerError
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.LedgerRecord
import com.oshicue.kit.core.LedgerRecordDeletion
import com.oshicue.kit.core.LedgerRecordFilter
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import java.time.LocalDate

/** The three-part management sheet: records, import, backup, plus catalog management. */
@Composable
fun LedgerManagementSheet(store: LedgerStore, onClose: () -> Unit) {
    val kit = LocalKit.current
    var section by remember { mutableStateOf("records") }
    var showCatalogManagement by remember { mutableStateOf(false) }

    ModalScreen(title = L10n.text("ledger.management"), onClose = onClose) {
        Column(Modifier.fillMaxSize().background(kit.background)) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                SegmentedControl(
                    options = listOf(
                        "records" to L10n.text("management.records"),
                        "import" to L10n.text("ledger.import"),
                        "backup" to L10n.text("management.backup")
                    ),
                    selected = section,
                    onSelect = { section = it }
                )
            }
            when (section) {
                "records" -> LedgerRecordManagement(store, onOpenCatalog = { showCatalogManagement = true })
                "import" -> LedgerImportPane(store) { onClose() }
                else -> LedgerBackupPane(store)
            }
        }
    }
    if (showCatalogManagement) {
        LedgerCatalogManagementSheet(store, onClose = { showCatalogManagement = false })
    }
}

@Composable
private fun LedgerRecordManagement(store: LedgerStore, onOpenCatalog: () -> Unit) {
    val kit = LocalKit.current
    val document = store.document
    var search by remember { mutableStateOf("") }
    var useDateRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var memberId by remember { mutableStateOf("") }
    var groupId by remember { mutableStateOf("") }
    var productId by remember { mutableStateOf("") }
    var currency by remember { mutableStateOf("") }
    var eventSelection by remember { mutableStateOf("all") }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingDeletion by remember { mutableStateOf<LedgerRecordDeletion?>(null) }
    var deletingAll by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val filter = LedgerRecordFilter(
        startDate = if (useDateRange) com.oshicue.kit.core.KitDate.formatDay(startDate) else null,
        endDate = if (useDateRange) com.oshicue.kit.core.KitDate.formatDay(endDate) else null,
        memberId = memberId.ifEmpty { null },
        groupId = groupId.ifEmpty { null },
        productId = productId.ifEmpty { null },
        currency = currency.ifEmpty { null },
        eventId = eventSelection.takeIf { it.startsWith("event:") }?.removePrefix("event:")?.ifEmpty { null },
        requiresNoEvent = eventSelection == "unlinked",
        query = search
    )
    val records = filter.matching(document.records).sortedWith(compareByDescending<LedgerRecord> { it.date }.thenBy { it.id })

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                KitTextField(search, { search = it; selected = emptySet() }, L10n.text("management.search"))
            }
            item {
                FilterDisclosure(
                    useDateRange = useDateRange,
                    onUseDateRange = { useDateRange = it },
                    startDate = startDate,
                    endDate = endDate,
                    onStartDate = { startDate = it },
                    onEndDate = { endDate = it },
                    validRange = !useDateRange || !startDate.isAfter(endDate),
                    options = buildList {
                        add(FilterOption("", L10n.text("management.all")))
                        document.activeMembers.forEach { add(FilterOption(it.id, it.name)) }
                    },
                    selectedOption = memberId,
                    onSelect = { memberId = it; selected = emptySet() },
                    extra = {
                        Column {
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SelectableChip(L10n.text("management.all"), groupId.isEmpty()) { groupId = ""; selected = emptySet() }
                                document.activeGroups.forEach { group ->
                                    SelectableChip(group.name, groupId == group.id) { groupId = group.id; selected = emptySet() }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SelectableChip(L10n.text("ledger.allTypes"), productId.isEmpty()) { productId = ""; selected = emptySet() }
                                document.products.forEach { product ->
                                    SelectableChip(productDisplayName(product.id, product.name), productId == product.id) {
                                        productId = product.id; selected = emptySet()
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SelectableChip(L10n.text("management.all"), currency.isEmpty()) { currency = ""; selected = emptySet() }
                                com.oshicue.kit.core.LedgerCurrency.entries.forEach { value ->
                                    SelectableChip(value.code, currency == value.code) { currency = value.code; selected = emptySet() }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                SelectableChip(L10n.text("management.all"), eventSelection == "all") { eventSelection = "all"; selected = emptySet() }
                                SelectableChip(L10n.text("ledger.noEvent"), eventSelection == "unlinked") { eventSelection = "unlinked"; selected = emptySet() }
                                document.events.forEach { event ->
                                    SelectableChip(event.name, eventSelection == "event:" + event.id) {
                                        eventSelection = "event:" + event.id; selected = emptySet()
                                    }
                                }
                            }
                        }
                    }
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        L10n.text("management.selectFiltered"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (records.isEmpty()) kit.muted else kit.accentText,
                        modifier = Modifier.clickable(enabled = records.isNotEmpty()) { selected = records.map { it.id }.toSet() }
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        L10n.text("management.clearSelection"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (selected.isEmpty()) kit.muted else kit.accentText,
                        modifier = Modifier.clickable(enabled = selected.isNotEmpty()) { selected = emptySet() }
                    )
                }
            }
            item {
                Text(
                    L10n.format("management.resultCount", records.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = kit.muted
                )
            }
            if (records.isEmpty()) {
                item { Text(L10n.text("management.empty"), fontSize = 13.sp, color = kit.muted, modifier = Modifier.padding(vertical = 12.dp)) }
            }
            items(records, key = { it.id }) { record ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(kit.surface)
                        .clickable {
                            selected = if (record.id in selected) selected - record.id else selected + record.id
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkmark(record.id in selected)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            record.memberNameSnapshot + " · " + record.groupNameSnapshot,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = kit.ink
                        )
                        Text(
                            record.date + " · " + productDisplayName(record.productId, record.productNameSnapshot) +
                                " × " + record.quantity,
                            fontSize = 11.sp,
                            color = kit.muted
                        )
                        record.eventNameSnapshot?.let { Text(it, fontSize = 11.sp, color = kit.muted) }
                        record.note?.takeIf { it.isNotEmpty() }?.let {
                            Text(it, fontSize = 11.sp, color = kit.muted, maxLines = 2)
                        }
                    }
                    Text(
                        moneyText(mapOf(record.currency to (record.totalMinor ?: 0L))),
                        fontSize = 11.sp,
                        color = kit.ink
                    )
                }
            }
            item {
                Spacer(Modifier.height(10.dp))
                KitSecondaryButton(L10n.text("management.catalog"), icon = Icons.Filled.PersonOff) { onOpenCatalog() }
                Spacer(Modifier.height(8.dp))
                KitSecondaryButton(
                    L10n.text("management.clearAll"),
                    icon = Icons.Filled.Delete,
                    enabled = document.records.isNotEmpty()
                ) {
                    try {
                        deletingAll = true
                        pendingDeletion = LedgerRecordDeletion(document.records, document.records.map { it.id }.toSet())
                    } catch (failure: Exception) {
                        error = ledgerErrorText(failure)
                    }
                }
                Text(L10n.text("management.clearAllHint"), fontSize = 11.sp, color = kit.muted)
                Spacer(Modifier.height(20.dp))
            }
        }
        Row(
            Modifier.fillMaxWidth().background(kit.surface).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(L10n.format("management.selectedCount", selected.size), fontSize = 13.sp, color = kit.ink)
            Spacer(Modifier.weight(1f))
            Text(
                L10n.text("management.deleteSelected"),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected.isEmpty()) kit.muted else kit.accentText,
                modifier = Modifier.clickable(enabled = selected.isNotEmpty()) {
                    try {
                        deletingAll = false
                        val deletion = LedgerRecordDeletion(document.records, selected)
                        if (deletion.recordCount > 0) pendingDeletion = deletion
                    } catch (failure: Exception) {
                        error = ledgerErrorText(failure)
                    }
                }
            )
        }
    }

    pendingDeletion?.let { deletion ->
        ConfirmDialog(
            title = L10n.text(if (deletingAll) "ledger.management.clearTitle" else "ledger.management.deleteTitle"),
            message = L10n.format("ledger.management.deleteSummary", deletion.recordCount, deletion.quantity.toString()) +
                "\n" + moneyText(deletion.amountsByCurrency) +
                "\n\n" + L10n.text("ledger.management.deleteImpact"),
            confirmLabel = L10n.text(if (deletingAll) "management.clearAll" else "management.deleteSelected"),
            onConfirm = {
                if (store.deleteRecords(deletion.recordIds)) {
                    selected = selected - deletion.recordIds
                } else {
                    error = store.storageError
                    store.storageError = null
                }
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }
    error?.let { MessageDialog(L10n.text("settings.saveError"), it) { error = null; store.storageError = null } }
}

fun ledgerErrorText(failure: Throwable): String = when (failure) {
    is LedgerError -> L10n.text(failure.key) + "\n" + failure.path
    else -> failure.localizedMessage ?: ""
}

@Composable
private fun LedgerImportPane(store: LedgerStore, onApplied: () -> Unit) {
    val kit = LocalKit.current
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<LedgerDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<LedgerDocument?>(null) }
    var copied by remember { mutableStateOf(false) }
    var demoError by remember { mutableStateOf<String?>(null) }

    val pickFiles = rememberFilePicker(multiple = true) { contents ->
        try {
            var combined = LedgerDocument()
            contents.forEach { content ->
                combined = combined.merging(LedgerImporter.parse(content))
            }
            checkLedgerPreview(store, combined)
            val input = LedgerImporter.encode(combined)
            text = input
            preview = combined
            error = null
        } catch (failure: Exception) {
            preview = null
            error = ledgerErrorText(failure)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionLabel(L10n.text("ledger.import"))
        Spacer(Modifier.height(6.dp))
        Text(L10n.text("ledger.importHint"), fontSize = 12.sp, color = kit.muted)
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            KitSecondaryButton(L10n.text("ledger.file"), icon = Icons.Filled.FileOpen, modifier = Modifier.weight(1f)) {
                pickFiles()
            }
            KitSecondaryButton(
                L10n.text(if (copied) "import.copied" else "ledger.copyTemplate"),
                icon = Icons.Filled.Share,
                modifier = Modifier.weight(1f)
            ) {
                copyToClipboard(context, loadTemplateText(context, "ledger-template.json"))
                copied = true
            }
        }
        Spacer(Modifier.height(10.dp))
        JsonEditorField(
            text = text,
            onValueChange = {
                text = it
                preview = pending
                pending = null
                error = null
            },
            placeholder = "JSON · UTF-8",
            minHeight = 220.dp
        )
        Spacer(Modifier.height(10.dp))
        KitPrimaryButton(L10n.text("import.validate"), enabled = text.isNotBlank()) {
            try {
                val input = LedgerImporter.parse(text)
                checkLedgerPreview(store, input)
                preview = input
                error = null
            } catch (failure: Exception) {
                preview = null
                error = ledgerErrorText(failure)
            }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            InlineError(it)
        }
        preview?.let { document ->
            Spacer(Modifier.height(12.dp))
            FormCard {
                Text(
                    L10n.text("ledger.preview"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                )
                Text(
                    L10n.format("ledger.summary", document.members.size, document.records.size),
                    fontSize = 13.sp,
                    color = kit.ink
                )
                Text(
                    L10n.text(if (store.canReplaceDemo) "ledger.replacesDemo" else "ledger.restoreHint"),
                    fontSize = 11.sp,
                    color = kit.muted
                )
                Spacer(Modifier.height(10.dp))
                KitPrimaryButton(L10n.text("ledger.apply")) {
                    if (store.importDocument(document)) onApplied() else error = store.storageError
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        DividerLine()
        Spacer(Modifier.height(16.dp))
        FormCard {
            Text(L10n.text("ledger.title"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = kit.ink)
            Spacer(Modifier.height(8.dp))
            KitSecondaryButton(L10n.text("settings.ledgerDemo"), icon = Icons.Filled.PersonOff) {
                if (store.loadDemo()) {
                    onApplied()
                } else {
                    demoError = store.storageError
                    store.storageError = null
                }
            }
            Text(L10n.text("ledger.loadDemoHint"), fontSize = 11.sp, color = kit.muted)
            demoError?.let {
                Spacer(Modifier.height(6.dp))
                InlineError(it)
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

private fun checkLedgerPreview(store: LedgerStore, input: LedgerDocument) {
    if (input.members.isEmpty() && input.groups.isEmpty() && input.products.isEmpty() &&
        input.events.isEmpty() && input.records.isEmpty()
    ) {
        throw LedgerError("ledger.error.emptyImport", "There is no catalog or record to import.", "$")
    }
    if (!store.canReplaceDemo) {
        store.document.merging(input)
    }
}

@Composable
private fun LedgerBackupPane(store: LedgerStore) {
    val kit = LocalKit.current
    var error by remember { mutableStateOf<String?>(null) }
    val exporter = rememberTextExporter { error = it }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionLabel(L10n.text("ledger.backup"))
        Spacer(Modifier.height(6.dp))
        Text(L10n.text("ledger.backupHint"), fontSize = 12.sp, color = kit.muted)
        Spacer(Modifier.height(12.dp))
        KitSecondaryButton(L10n.text("ledger.export"), icon = Icons.Filled.Share) {
            try {
                exporter("OshicueKit-ledger-" + com.oshicue.kit.core.LedgerDates.today() + ".json", store.exportJson())
            } catch (failure: Exception) {
                error = ledgerErrorText(failure)
            }
        }
        error?.let {
            Spacer(Modifier.height(10.dp))
            InlineError(it)
        }
    }
}

/** Catalog-wide deletion with the same confirmation cadence as the record manager. */
@Composable
private fun LedgerCatalogManagementSheet(store: LedgerStore, onClose: () -> Unit) {
    val kit = LocalKit.current
    val document = store.document
    var search by remember { mutableStateOf("") }
    var memberSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var groupSelection by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingMembers by remember { mutableStateOf<Set<String>?>(null) }
    var pendingGroups by remember { mutableStateOf<Set<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalScreen(title = L10n.text("management.catalog"), onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            KitTextField(search, { search = it }, L10n.text("management.catalog.search"))
            Spacer(Modifier.height(6.dp))
            Text(L10n.text("management.catalog.hint"), fontSize = 11.sp, color = kit.muted)
            Spacer(Modifier.height(12.dp))

            SectionLabel(L10n.text("ledger.catalog.members"))
            Spacer(Modifier.height(6.dp))
            document.activeMembers
                .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }
                .forEach { member ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                memberSelection = if (member.id in memberSelection) {
                                    memberSelection - member.id
                                } else {
                                    memberSelection + member.id
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkmark(member.id in memberSelection)
                        Spacer(Modifier.width(10.dp))
                        Text(member.name, fontSize = 13.5.sp, color = kit.ink)
                    }
                }
            Spacer(Modifier.height(8.dp))
            KitSecondaryButton(
                L10n.text("ledger.member.delete"),
                icon = Icons.Filled.Delete,
                enabled = memberSelection.isNotEmpty()
            ) { pendingMembers = memberSelection }
            Spacer(Modifier.height(16.dp))

            SectionLabel(L10n.text("ledger.group"))
            Spacer(Modifier.height(6.dp))
            document.activeGroups
                .filter { search.isBlank() || it.name.contains(search, ignoreCase = true) }
                .forEach { group ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clickable {
                                groupSelection = if (group.id in groupSelection) {
                                    groupSelection - group.id
                                } else {
                                    groupSelection + group.id
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkmark(group.id in groupSelection)
                        Spacer(Modifier.width(10.dp))
                        Text(group.name, fontSize = 13.5.sp, color = kit.ink)
                    }
                }
            Spacer(Modifier.height(8.dp))
            KitSecondaryButton(
                L10n.text("ledger.group.delete"),
                icon = Icons.Filled.Delete,
                enabled = groupSelection.isNotEmpty()
            ) { pendingGroups = groupSelection }
            error?.let {
                Spacer(Modifier.height(10.dp))
                InlineError(it)
            }
        }
    }

    pendingMembers?.let { ids ->
        ConfirmDialog(
            title = L10n.text("management.catalog.deleteTitle"),
            message = L10n.text("management.catalog.memberImpact"),
            confirmLabel = L10n.text("ledger.member.delete"),
            onConfirm = {
                if (store.deleteMembers(ids)) memberSelection = memberSelection - ids else error = store.storageError
                pendingMembers = null
            },
            onDismiss = { pendingMembers = null }
        )
    }
    pendingGroups?.let { ids ->
        ConfirmDialog(
            title = L10n.text("management.catalog.deleteTitle"),
            message = L10n.text("management.catalog.groupImpact"),
            confirmLabel = L10n.text("ledger.group.delete"),
            onConfirm = {
                if (store.deleteGroups(ids)) groupSelection = groupSelection - ids else error = store.storageError
                pendingGroups = null
            },
            onDismiss = { pendingGroups = null }
        )
    }
}

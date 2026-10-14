package com.oshicue.kit.ui.diary

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.DiaryCore
import com.oshicue.kit.core.DiaryDocument
import com.oshicue.kit.core.DiaryEntry
import com.oshicue.kit.core.DiaryEntryFilter
import com.oshicue.kit.core.DiaryImporter
import com.oshicue.kit.core.DiaryStatus
import com.oshicue.kit.data.DiaryStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.ledger.LedgerPresentation
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.LocalDate

@Composable
fun DiaryPublishSheet(entry: DiaryEntry, onClose: () -> Unit) {
    val palette = kitPalette()
    val context = LocalContext.current
    var copied by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<Pair<String, String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { uri ->
        val pending = pendingExport
        if (uri != null && pending != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(pending.first.toByteArray(Charsets.UTF_8)) }
            } catch (value: Exception) {
                error = value.message
            }
        }
        pendingExport = null
    }

    fun copy(key: String, content: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(key, content))
        copied = key
    }

    KitSheet(L10n.text("diary.export"), onClose) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(L10n.text("diary.exportHint"), color = palette.muted, fontSize = 14.sp)
            PublishAction(
                label = L10n.text(if (copied == "diary.copyMarkdown") "import.copied" else "diary.copyMarkdown"),
                icon = Icons.Filled.ContentCopy
            ) { copy("diary.copyMarkdown", DiaryCore.markdown(entry)) }
            PublishAction(
                label = L10n.text(if (copied == "diary.copyText") "import.copied" else "diary.copyText"),
                icon = Icons.Filled.ContentCopy
            ) { copy("diary.copyText", DiaryCore.plainText(entry)) }
            PublishAction(label = L10n.text("diary.exportMarkdown"), icon = Icons.Filled.Description) {
                pendingExport = DiaryCore.markdown(entry) to "md"
                exportLauncher.launch("OshicueKit-recap-" + entry.event.date + ".md")
            }
            PublishAction(label = L10n.text("diary.exportText"), icon = Icons.Filled.Description) {
                pendingExport = DiaryCore.plainText(entry) to "txt"
                exportLauncher.launch("OshicueKit-recap-" + entry.event.date + ".txt")
            }
            PublishAction(label = L10n.text("diary.share"), icon = Icons.Filled.IosShare) {
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, DiaryCore.plainText(entry))
                }
                context.startActivity(Intent.createChooser(intent, L10n.text("diary.share")))
            }
            Spacer(modifier = Modifier.height(1.dp).fillMaxWidth().background(palette.line))
            Text(
                DiaryCore.plainText(entry),
                color = palette.ink,
                fontSize = 15.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            )
            error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PublishAction(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    val palette = kitPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(label, color = palette.ink, fontSize = 15.sp)
    }
}

@Composable
fun DiaryManagementSheet(store: DiaryStore, onClose: () -> Unit) {
    val palette = kitPalette()
    var section by remember { mutableStateOf(0) }
    KitSheet(L10n.text("diary.management"), onClose) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 10.dp)
                    .background(palette.surfaceMuted, RoundedCornerShape(11.dp))
                    .padding(3.dp)
            ) {
                for ((index, label) in listOf(L10n.text("management.records"), L10n.text("management.backup")).withIndex()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .background(if (section == index) palette.surface else Color.Transparent, RoundedCornerShape(9.dp))
                            .clickable { section = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(label, color = if (section == index) palette.accentText else palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (section == 0) DiaryEntryManagement(store) else DiaryBackupContent(store)
        }
    }
}

@Composable
private fun DiaryEntryManagement(store: DiaryStore) {
    val palette = kitPalette()
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var useDateRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now()) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var showFilters by remember { mutableStateOf(false) }
    var selectedIDs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingIDs by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showConfirmation by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    val filter = DiaryEntryFilter(
        startDate = if (useDateRange) LedgerPresentation.calendar.format(startDate) else null,
        endDate = if (useDateRange) LedgerPresentation.calendar.format(endDate) else null,
        status = DiaryStatus.fromRaw(status),
        query = search
    )
    val entries = remember(filter, store.document.entries) {
        filter.matching(store.document.entries)
            .sortedWith(compareByDescending<DiaryEntry> { it.event.date }.thenByDescending { it.updatedAt })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(14.dp))
                    .padding(horizontal = 14.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
                androidx.compose.material3.TextField(
                    value = search,
                    onValueChange = { search = it },
                    singleLine = true,
                    placeholder = { Text(L10n.text("diary.search"), color = palette.muted, fontSize = 13.sp) },
                    colors = androidx.compose.material3.TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    textStyle = TextStyle(color = palette.ink, fontSize = 14.sp),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().clickable { showFilters = !showFilters },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(L10n.text("management.filters"), color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.weight(1f))
            }
            if (showFilters) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    KitDropdownRow(
                        label = L10n.text("diary.status"),
                        value = status,
                        options = listOf(
                            "" to L10n.text("management.all"),
                            "draft" to L10n.text("diary.filter.draft"),
                            "ready" to L10n.text("diary.filter.ready")
                        ),
                        onSelect = { status = it }
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L10n.text("management.dateRange"), color = palette.ink, fontSize = 14.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = useDateRange,
                            onCheckedChange = { useDateRange = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = palette.accentFill)
                        )
                    }
                    if (useDateRange) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showStartPicker = true }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.text("management.startDate"), color = palette.ink, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(KitDate.string(startDate, "yyyy MMM d"), color = palette.accentText, fontSize = 14.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showEndPicker = true }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.text("management.endDate"), color = palette.ink, fontSize = 14.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(KitDate.string(endDate, "yyyy MMM d"), color = palette.accentText, fontSize = 14.sp)
                        }
                        if (!filter.hasValidDateRange) {
                            Text(L10n.text("management.invalidDateRange"), color = palette.warningText, fontSize = 12.sp)
                        }
                    }
                    Text(L10n.text("management.selectionHint"), color = palette.muted, fontSize = 11.sp)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    L10n.text("management.selectFiltered"),
                    color = if (entries.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = entries.isNotEmpty()) { selectedIDs = entries.map { it.id }.toSet() }
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    L10n.text("management.clearSelection"),
                    color = if (selectedIDs.isEmpty()) palette.muted.copy(alpha = 0.5f) else palette.accentText,
                    fontSize = 13.sp,
                    modifier = Modifier.clickable(enabled = selectedIDs.isNotEmpty()) { selectedIDs = emptySet() }
                )
            }
            Text(L10n.format("management.resultCount", entries.size), color = palette.muted, fontSize = 13.sp)
            if (entries.isEmpty()) {
                Text(L10n.text("management.empty"), color = palette.muted, fontSize = 14.sp)
            }
            for (entry in entries) {
                val selected = entry.id in selectedIDs
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedIDs = if (selected) selectedIDs - entry.id else selectedIDs + entry.id }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (selected) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = if (selected) palette.accentText else palette.muted,
                        modifier = Modifier.size(20.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            entry.title.trim().ifEmpty { L10n.text("diary.untitled") },
                            color = palette.ink,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            entry.event.date + " · " + L10n.text(if (entry.status == DiaryStatus.ready) "diary.filter.ready" else "diary.filter.draft"),
                            color = palette.muted,
                            fontSize = 12.sp
                        )
                        if (entry.event.name.isNotEmpty()) Text(entry.event.name, color = palette.muted, fontSize = 12.sp)
                        val excerpt = if (entry.summary.isEmpty()) entry.body else entry.summary
                        if (excerpt.isNotEmpty()) {
                            Text(excerpt, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
            Spacer(modifier = Modifier.height(12.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth().background(palette.surface).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(L10n.format("management.selectedCount", selectedIDs.size), color = palette.ink, fontSize = 14.sp)
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = {
                    pendingIDs = selectedIDs.intersect(store.document.entries.map { it.id }.toSet())
                    if (pendingIDs.isNotEmpty()) showConfirmation = true
                },
                enabled = selectedIDs.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceMuted, contentColor = palette.warningText),
                shape = RoundedCornerShape(12.dp)
            ) { Text(L10n.text("management.deleteSelected"), fontSize = 13.sp) }
        }
    }

    if (showStartPicker) KitDatePickerDialog(startDate, { startDate = it }, { showStartPicker = false })
    if (showEndPicker) KitDatePickerDialog(endDate, { endDate = it }, { showEndPicker = false })
    if (showConfirmation) {
        KitAlert(
            title = L10n.text("diary.management.deleteTitle"),
            message = L10n.format("diary.management.deleteMessage", pendingIDs.size),
            confirmLabel = L10n.text("management.deleteSelected"),
            onConfirm = {
                if (store.deleteEntries(pendingIDs)) selectedIDs = selectedIDs - pendingIDs
                else {
                    error = store.storageError
                    store.storageError = null
                }
                pendingIDs = emptySet()
                showConfirmation = false
            },
            onDismiss = { pendingIDs = emptySet(); showConfirmation = false },
            destructive = true
        )
    }
}

@Composable
private fun DiaryBackupContent(store: DiaryStore) {
    val palette = kitPalette()
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<DiaryDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pendingExport by remember { mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val content = pendingExport
        if (uri != null && content != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray(Charsets.UTF_8)) }
            } catch (value: Exception) {
                error = value.message
            }
        }
        pendingExport = null
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        try {
            var combined = DiaryDocument()
            for (uri in uris) {
                val input = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: throw IllegalStateException("Unreadable file")
                combined = combined.merging(DiaryImporter.parse(input))
            }
            checkDiaryPreview(store, combined)
            text = DiaryImporter.encode(combined)
            preview = combined
            error = null
        } catch (value: Exception) {
            preview = null
            error = diaryErrorText(value)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(56.dp).background(palette.surface, RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Description, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(26.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(L10n.text("diary.backup"), color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text(L10n.text("diary.backupHint"), color = palette.muted, fontSize = 12.sp)
            }
        }
        Button(
            onClick = {
                try {
                    pendingExport = store.exportJSON()
                    exportLauncher.launch("OshicueKit-diary-" + LedgerPresentation.calendar.format(LocalDate.now()))
                } catch (value: Exception) {
                    error = diaryErrorText(value)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = palette.surface, contentColor = palette.ink),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.text("diary.backup.export"))
        }
        Spacer(modifier = Modifier.height(2.dp).fillMaxWidth().background(palette.line))
        Text(L10n.text("diary.backup.import"), color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(L10n.text("diary.backup.importHint"), color = palette.muted, fontSize = 12.sp)
        Row(
            modifier = Modifier.clickable { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = palette.ink, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(L10n.text("ledger.file"), color = palette.ink, fontSize = 14.sp)
        }
        androidx.compose.material3.TextField(
            value = text,
            onValueChange = {
                text = it
                preview = null
                error = null
            },
            placeholder = { Text("JSON · UTF-8", color = palette.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace) },
            textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = palette.ink),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = palette.surface,
                unfocusedContainerColor = palette.surface,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(220.dp)
        )
        Button(
            onClick = {
                try {
                    val input = DiaryImporter.parse(text)
                    checkDiaryPreview(store, input)
                    preview = input
                    error = null
                } catch (value: Exception) {
                    preview = null
                    error = diaryErrorText(value)
                }
            },
            enabled = text.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(L10n.text("import.validate"), fontWeight = FontWeight.SemiBold) }
        error?.let { message ->
            Text(
                message,
                color = palette.warningText,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.warningText.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        }
        preview?.let { document ->
            val existing = store.document.entries.map { it.id }.toSet()
            val newCount = document.entries.count { it.id !in existing }
            Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Verified, contentDescription = null, tint = Color(0xFF2E9E5B), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.text("diary.backup.preview"), color = palette.ink, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    L10n.format("diary.backup.summary", newCount, document.entries.size - newCount),
                    color = palette.ink,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(L10n.text("diary.backup.mergeHint"), color = palette.muted, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (store.importDocument(document)) {
                            preview = null
                            text = ""
                        } else {
                            error = store.storageError
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(46.dp)
                ) { Text(L10n.text("diary.backup.apply"), fontWeight = FontWeight.SemiBold) }
            }
        }
        Spacer(modifier = Modifier.height(30.dp))
    }
}

private fun checkDiaryPreview(store: DiaryStore, input: DiaryDocument) {
    if (input.entries.isEmpty()) {
        throw com.oshicue.kit.core.DiaryError("diary.backup.empty", "This backup contains no entries", "$")
    }
    store.document.merging(input)
}

private fun diaryErrorText(value: Exception): String {
    if (value is com.oshicue.kit.core.DiaryError) return L10n.text(value.key) + "\n" + value.path
    return value.message ?: value.toString()
}

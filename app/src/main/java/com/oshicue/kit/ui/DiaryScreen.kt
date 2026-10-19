package com.oshicue.kit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.DiaryCore
import com.oshicue.kit.core.DiaryEntry
import com.oshicue.kit.core.DiaryStatus
import com.oshicue.kit.core.DiaryStore
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerStore
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DiaryScreen(
    store: DiaryStore,
    ledgerStore: LedgerStore,
    modifier: Modifier = Modifier
) {
    val kit = LocalKit.current
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var editorEntry by remember { mutableStateOf<DiaryEntry?>(null) }
    var editingNew by remember { mutableStateOf(false) }
    var showManagement by remember { mutableStateOf(false) }

    val entries = DiaryCore.search(store.document.entries, search)
        .filter { filter == "all" || it.status.rawValue == filter }
        .sortedWith { left, right ->
            if (left.event.date == right.event.date) {
                right.updatedAt.compareTo(left.updatedAt)
            } else {
                right.event.date.compareTo(left.event.date)
            }
        }
    val days = entries.map { it.event.date }.distinct().sortedDescending()

    Column(modifier.fillMaxSize().background(kit.background)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BrandWordmark()
            Spacer(Modifier.weight(1f))
            CircleIconButton(Icons.Filled.Tune, L10n.text("diary.management")) { showManagement = true }
            Spacer(Modifier.width(10.dp))
            AccentCircleButton(Icons.Filled.Add, L10n.text("diary.add")) {
                editorEntry = null
                editingNew = true
            }
        }
        Text(
            L10n.text("diary.title"),
            modifier = Modifier.padding(horizontal = 20.dp),
            fontSize = 29.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-1).sp,
            color = kit.ink
        )
        Spacer(Modifier.height(16.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(kit.surface)
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, null, tint = kit.muted, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp, color = kit.ink),
                decorationBox = { inner ->
                    Box {
                        if (search.isEmpty()) Text(L10n.text("diary.search"), fontSize = 13.5.sp, color = kit.muted)
                        inner()
                    }
                }
            )
            if (search.isNotEmpty()) {
                Icon(
                    Icons.Filled.Clear,
                    contentDescription = L10n.text("diary.clearSearch"),
                    tint = kit.muted,
                    modifier = Modifier.size(17.dp).clickable { search = "" }
                )
            }
        }
        Spacer(Modifier.height(13.dp))
        Box(Modifier.padding(horizontal = 20.dp)) {
            SegmentedControl(
                options = listOf(
                    "all" to L10n.text("diary.filter.all"),
                    "draft" to L10n.text("diary.filter.draft"),
                    "ready" to L10n.text("diary.filter.ready")
                ),
                selected = filter,
                onSelect = { filter = it }
            )
        }
        store.storageError?.let { error ->
            Row(
                Modifier
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(kit.accent.copy(alpha = 0.06f))
                    .padding(12.dp)
            ) {
                Icon(Icons.Filled.Warning, null, tint = kit.accentText, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(error, fontSize = 11.5.sp, color = kit.accentText)
            }
        }
        Spacer(Modifier.height(8.dp))
        if (entries.isEmpty()) {
            EmptyState(
                title = L10n.text(if (search.isEmpty() && filter == "all") "diary.empty.title" else "diary.empty.search"),
                hint = L10n.text("diary.empty.hint"),
                symbol = Icons.Filled.Edit,
                action = {
                    KitPrimaryButton(L10n.text("diary.add")) {
                        editorEntry = null
                        editingNew = true
                    }
                }
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                days.forEach { day ->
                    item(key = "day-$day") {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(kit.accent)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                dayLabel(day),
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = kit.ink
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    items(entries.filter { it.event.date == day }, key = { it.id }) { entry ->
                        DiaryCard(entry) {
                            editorEntry = entry
                            editingNew = false
                        }
                    }
                }
                item { Spacer(Modifier.height(24.dp)) }
            }
        }
    }

    if (editingNew || editorEntry != null) {
        DiaryEditor(
            store = store,
            ledgerStore = ledgerStore,
            entry = editorEntry,
            onClose = {
                editorEntry = null
                editingNew = false
            }
        )
    }
    if (showManagement) {
        DiaryManagementSheet(store = store, onClose = { showManagement = false })
    }
}

fun dayLabel(day: String): String {
    val date = LedgerDates.parse(day) ?: return day
    return KitDate.string(
        date.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
        "yyyy MMM d E",
        ZoneId.systemDefault()
    )
}

@Composable
private fun DiaryCard(entry: DiaryEntry, onClick: () -> Unit) {
    val kit = LocalKit.current
    SurfaceCard(corner = 19.dp) {
        Column(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(17.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    if (entry.title.isBlank()) L10n.text("diary.untitled") else entry.title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = kit.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .clip(CircleShape)
                        .background(kit.accent.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        L10n.text(if (entry.status == DiaryStatus.Ready) "diary.filter.ready" else "diary.filter.draft"),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kit.accentText
                    )
                }
            }
            if (entry.event.name.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.ConfirmationNumber, null, tint = kit.muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(entry.event.name, fontSize = 11.5.sp, color = kit.muted, maxLines = 2)
                }
            }
            val location = listOf(entry.event.city, entry.event.venue).filter { it.isNotEmpty() }.joinToString(" · ")
            if (location.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, null, tint = kit.muted, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(location, fontSize = 11.5.sp, color = kit.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            val excerpt = if (entry.summary.isEmpty()) entry.body else entry.summary
            if (excerpt.isNotEmpty()) {
                Spacer(Modifier.height(7.dp))
                Text(excerpt, fontSize = 13.sp, color = kit.ink.copy(alpha = 0.8f), maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            if (entry.tags.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    entry.tags.take(3).forEach { tag ->
                        Box(
                            Modifier
                                .padding(end = 5.dp)
                                .clip(CircleShape)
                                .background(kit.background)
                                .padding(horizontal = 7.dp, vertical = 4.dp)
                        ) {
                            Text("#" + tag, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = kit.muted)
                        }
                    }
                    Spacer(Modifier.weight(1f))
                    Text("›", fontSize = 14.sp, color = kit.muted)
                }
            }
        }
    }
}

@Composable
fun DiaryManagementSheet(store: DiaryStore, onClose: () -> Unit) {
    val kit = LocalKit.current
    var section by remember { mutableStateOf("records") }
    ModalScreen(title = L10n.text("diary.management"), onClose = onClose) {
        Column(Modifier.fillMaxSize().background(kit.background)) {
            Box(Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                SegmentedControl(
                    options = listOf(
                        "records" to L10n.text("management.records"),
                        "backup" to L10n.text("management.backup")
                    ),
                    selected = section,
                    onSelect = { section = it }
                )
            }
            if (section == "records") {
                DiaryRecordManagement(store)
            } else {
                DiaryBackupPane(store)
            }
        }
    }
}

@Composable
private fun DiaryRecordManagement(store: DiaryStore) {
    val kit = LocalKit.current
    var search by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var useDateRange by remember { mutableStateOf(false) }
    var startDate by remember { mutableStateOf(LocalDate.now().minusDays(30)) }
    var endDate by remember { mutableStateOf(LocalDate.now()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }
    var pendingIds by remember { mutableStateOf<Set<String>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    val filter = com.oshicue.kit.core.DiaryEntryFilter(
        startDate = if (useDateRange) KitDate.formatDay(startDate) else null,
        endDate = if (useDateRange) KitDate.formatDay(endDate) else null,
        status = DiaryStatus.from(status),
        query = search
    )
    val entries = filter.matching(store.document.entries).sortedWith { left, right ->
        if (left.event.date == right.event.date) right.updatedAt.compareTo(left.updatedAt)
        else right.event.date.compareTo(left.event.date)
    }

    Column(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { KitTextField(search, { search = it; selected = emptySet() }, L10n.text("diary.search")) }
            item {
                FilterDisclosure(
                    useDateRange = useDateRange,
                    onUseDateRange = { useDateRange = it },
                    startDate = startDate,
                    endDate = endDate,
                    onStartDate = { startDate = it },
                    onEndDate = { endDate = it },
                    validRange = !useDateRange || !startDate.isAfter(endDate),
                    options = listOf(
                        FilterOption("", L10n.text("management.all")),
                        FilterOption("draft", L10n.text("diary.filter.draft")),
                        FilterOption("ready", L10n.text("diary.filter.ready"))
                    ),
                    selectedOption = status,
                    onSelect = { status = it; selected = emptySet() }
                )
            }
            item {
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        L10n.text("management.selectFiltered"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (entries.isEmpty()) kit.muted else kit.accentText,
                        modifier = Modifier.clickable(enabled = entries.isNotEmpty()) { selected = entries.map { it.id }.toSet() }
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
                Text(L10n.format("management.resultCount", entries.size), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = kit.muted)
            }
            if (entries.isEmpty()) {
                item { Text(L10n.text("management.empty"), fontSize = 13.sp, color = kit.muted) }
            }
            items(entries, key = { it.id }) { entry ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(kit.surface)
                        .clickable {
                            selected = if (entry.id in selected) selected - entry.id else selected + entry.id
                        }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkmark(entry.id in selected)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (entry.title.isBlank()) L10n.text("diary.untitled") else entry.title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = kit.ink
                        )
                        Text(
                            entry.event.date + " · " +
                                L10n.text(if (entry.status == DiaryStatus.Ready) "diary.filter.ready" else "diary.filter.draft"),
                            fontSize = 11.sp,
                            color = kit.muted
                        )
                        if (entry.event.name.isNotEmpty()) Text(entry.event.name, fontSize = 11.sp, color = kit.muted)
                        val excerpt = if (entry.summary.isEmpty()) entry.body else entry.summary
                        if (excerpt.isNotEmpty()) Text(excerpt, fontSize = 11.sp, color = kit.muted, maxLines = 2)
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
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
                    val ids = selected.intersect(store.document.entries.map { it.id }.toSet())
                    if (ids.isNotEmpty()) pendingIds = ids
                }
            )
        }
    }

    pendingIds?.let { ids ->
        ConfirmDialog(
            title = L10n.text("diary.management.deleteTitle"),
            message = L10n.format("diary.management.deleteMessage", ids.size),
            confirmLabel = L10n.text("management.deleteSelected"),
            onConfirm = {
                if (store.deleteEntries(ids)) selected = selected - ids else error = store.storageError
                pendingIds = null
            },
            onDismiss = { pendingIds = null }
        )
    }
    error?.let {
        MessageDialog(L10n.text("settings.saveError"), it) { error = null; store.storageError = null }
    }
}

@Composable
private fun DiaryBackupPane(store: DiaryStore) {
    val kit = LocalKit.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<com.oshicue.kit.core.DiaryDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val exporter = rememberTextExporter { error = it }
    val picker = rememberFilePicker(multiple = true) { contents ->
        try {
            var combined = com.oshicue.kit.core.DiaryDocument()
            contents.forEach { combined = combined.merging(com.oshicue.kit.core.DiaryImporter.parse(it)) }
            if (combined.entries.isEmpty()) {
                error = L10n.text("diary.backup.empty")
                preview = null
            } else {
                store.document.merging(combined)
                text = com.oshicue.kit.core.DiaryImporter.encode(combined)
                preview = combined
                error = null
            }
        } catch (failure: Exception) {
            preview = null
            error = diaryErrorText(failure)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        SectionLabel(L10n.text("diary.backup"))
        Spacer(Modifier.height(6.dp))
        Text(L10n.text("diary.backupHint"), fontSize = 12.sp, color = kit.muted)
        Spacer(Modifier.height(12.dp))
        KitSecondaryButton(L10n.text("diary.backup.export"), icon = Icons.Filled.Clear) {
            try {
                exporter("OshicueKit-diary-" + LedgerDates.today() + ".json", store.exportJson())
            } catch (failure: Exception) {
                error = diaryErrorText(failure)
            }
        }
        Spacer(Modifier.height(16.dp))
        DividerLine()
        Spacer(Modifier.height(16.dp))
        Text(L10n.text("diary.backup.import"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = kit.ink)
        Text(L10n.text("diary.backup.importHint"), fontSize = 11.sp, color = kit.muted)
        Spacer(Modifier.height(8.dp))
        KitSecondaryButton(L10n.text("ledger.file"), icon = Icons.Filled.Add) { picker() }
        Spacer(Modifier.height(10.dp))
        JsonEditorField(
            text = text,
            onValueChange = { text = it; preview = null; error = null },
            placeholder = "JSON · UTF-8",
            minHeight = 220.dp
        )
        Spacer(Modifier.height(10.dp))
        KitPrimaryButton(L10n.text("import.validate"), enabled = text.isNotBlank()) {
            try {
                val input = com.oshicue.kit.core.DiaryImporter.parse(text)
                if (input.entries.isEmpty()) throw IllegalStateException(L10n.text("diary.backup.empty"))
                store.document.merging(input)
                preview = input
                error = null
            } catch (failure: Exception) {
                preview = null
                error = diaryErrorText(failure)
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
                    L10n.text("diary.backup.preview"),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color(0xFF2E7D32)
                )
                val existing = store.document.entries.map { it.id }.toSet()
                val newCount = document.entries.count { it.id !in existing }
                Text(
                    L10n.format("diary.backup.summary", newCount, document.entries.size - newCount),
                    fontSize = 13.sp,
                    color = kit.ink
                )
                Text(L10n.text("diary.backup.mergeHint"), fontSize = 11.sp, color = kit.muted)
                Spacer(Modifier.height(10.dp))
                KitPrimaryButton(L10n.text("diary.backup.apply")) {
                    if (store.importDocument(document)) {
                        preview = null
                        text = ""
                    } else {
                        error = store.storageError
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
    }
}

fun diaryErrorText(failure: Throwable): String = when (failure) {
    is com.oshicue.kit.core.DiaryError -> L10n.text(failure.key) + "\n" + failure.path
    else -> failure.localizedMessage ?: ""
}

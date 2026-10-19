package com.oshicue.kit.ui

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.DiaryCore
import com.oshicue.kit.core.DiaryEntry
import com.oshicue.kit.core.DiaryStatus
import com.oshicue.kit.core.DiaryStore
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.newIdentifier
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun DiaryEditor(
    store: DiaryStore,
    ledgerStore: LedgerStore,
    entry: DiaryEntry?,
    onClose: () -> Unit
) {
    val kit = LocalKit.current
    val context = LocalContext.current
    val initial = remember(entry?.id) { entry ?: DiaryEntry() }
    var draft by remember(entry?.id) { mutableStateOf(initial) }
    var lastSaved by remember(entry?.id) { mutableStateOf(entry) }
    var selectedDate by remember(entry?.id) {
        mutableStateOf(LedgerDates.parse(initial.event.date) ?: LocalDate.now())
    }
    var eventSelection by remember(entry?.id) { mutableStateOf(initial.event.ledgerEventId ?: "") }
    var tagsText by remember(entry?.id) { mutableStateOf(initial.tags.joinToString(", ")) }
    var saveState by remember { mutableStateOf("diary.saved") }
    var error by remember { mutableStateOf<String?>(null) }
    var showEventEditor by remember { mutableStateOf(false) }
    var showEventSelector by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPublish by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var wasDeleted by remember { mutableStateOf(false) }
    var saveTrigger by remember { mutableStateOf(0) }

    fun flushSave(): Boolean {
        if (wasDeleted) return true
        val hasMeaningful = draft.isMeaningfulContent
        val changed = lastSaved != null || hasMeaningful
        if (!changed || draft == lastSaved) return true
        val now = Instant.now()
        val value = draft.copy(
            updatedAt = if (now.isAfter(draft.createdAt)) now else draft.createdAt
        )
        return if (store.upsertEntry(value)) {
            draft = value
            lastSaved = value
            saveState = "diary.saved"
            error = null
            true
        } else {
            error = store.storageError ?: L10n.text("diary.saveError")
            saveState = "diary.saveError"
            false
        }
    }

    fun finish() {
        if (flushSave()) onClose()
    }

    // Debounced autosave, matching the 500 ms delay used on iOS.
    LaunchedEffect(draft, saveTrigger) {
        val hasMeaningful = draft.isMeaningfulContent
        val changed = lastSaved != null || hasMeaningful
        if (wasDeleted || !changed || draft == lastSaved) {
            if (error == null) saveState = "diary.saved"
            return@LaunchedEffect
        }
        saveState = "diary.saving"
        delay(500)
        flushSave()
    }

    ModalScreen(
        title = L10n.text(if (lastSaved == null) "diary.add" else "diary.edit"),
        onClose = { finish() },
        actions = {
            KitTextAction(L10n.text("common.done")) { finish() }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionLabel(L10n.text("diary.titleField"))
            Spacer(Modifier.height(6.dp))
            KitTextField(
                draft.title,
                { draft = draft.copy(title = it) },
                L10n.text("diary.titleField"),
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            )
            Spacer(Modifier.height(10.dp))
            SectionLabel(L10n.text("diary.body"))
            Spacer(Modifier.height(6.dp))
            KitTextField(
                draft.body,
                { draft = draft.copy(body = it) },
                L10n.text("diary.bodyHint"),
                singleLine = false
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (error == null) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    null,
                    tint = if (error == null) kit.muted else kit.accentText,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(5.dp))
                Text(L10n.text(saveState), fontSize = 11.sp, color = if (error == null) kit.muted else kit.accentText)
                Spacer(Modifier.weight(1f))
                if (error != null) {
                    Text(
                        L10n.text("diary.retry"),
                        fontSize = 11.sp,
                        color = kit.accentText,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { saveTrigger++ }
                    )
                }
            }
            error?.let {
                Spacer(Modifier.height(6.dp))
                InlineError(it)
            }
            Text(L10n.text("diary.autosaveHint"), fontSize = 11.sp, color = kit.muted)

            Spacer(Modifier.height(16.dp))
            SectionLabel(L10n.text("diary.summary"))
            Spacer(Modifier.height(6.dp))
            KitTextField(
                draft.summary,
                { draft = draft.copy(summary = it) },
                L10n.text("diary.summary"),
                singleLine = false
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel(L10n.text("diary.tags"))
            Spacer(Modifier.height(6.dp))
            KitTextField(
                tagsText,
                { value ->
                    tagsText = value
                    draft = draft.copy(tags = DiaryCore.parseTags(value))
                },
                L10n.text("diary.tags")
            )
            Text(L10n.text("diary.tagsHint"), fontSize = 11.sp, color = kit.muted)
            Spacer(Modifier.height(8.dp))
            Text(L10n.text("diary.status"), fontSize = 12.sp, color = kit.muted)
            Spacer(Modifier.height(4.dp))
            SegmentedControl(
                options = listOf(
                    "draft" to L10n.text("diary.filter.draft"),
                    "ready" to L10n.text("diary.filter.ready")
                ),
                selected = draft.status.rawValue,
                onSelect = { value ->
                    draft = draft.copy(status = DiaryStatus.from(value) ?: DiaryStatus.Draft)
                }
            )

            Spacer(Modifier.height(16.dp))
            SectionLabel(L10n.text("diary.eventSection"))
            Spacer(Modifier.height(6.dp))
            SelectorRow(
                label = L10n.text("diary.chooseEvent"),
                value = draft.event.name.ifEmpty { L10n.text("diary.manualEvent") },
                onClick = { showEventSelector = true }
            )
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SelectableChip(L10n.text("diary.manualEvent"), eventSelection.isEmpty()) {
                    eventSelection = ""
                    draft = draft.copy(event = draft.event.copy(ledgerEventId = null))
                }
                ledgerStore.document.events.take(3).forEach { event ->
                    SelectableChip(
                        event.name,
                        eventSelection == event.id
                    ) {
                        eventSelection = event.id
                        val contextValue = DiaryCore.contextFrom(event, draft.event.date)
                        draft = draft.copy(event = contextValue)
                        selectedDate = LedgerDates.parse(contextValue.date) ?: selectedDate
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            KitSecondaryButton(L10n.text("ledger.addEvent"), icon = Icons.Filled.Add) {
                showEventEditor = true
            }
            Spacer(Modifier.height(8.dp))
            SelectorRow(
                label = L10n.text("ledger.date"),
                value = LedgerDates.string(selectedDate),
                onClick = { showDatePicker = true }
            )
            Spacer(Modifier.height(4.dp))
            Row(
                Modifier.fillMaxWidth().padding(bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(-1L, 0L, 1L).forEach { offset ->
                    val value = LocalDate.now().plusDays(offset)
                    SelectableChip(
                        KitDate.string(value.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), "MM.dd", ZoneId.systemDefault()),
                        selectedDate == value
                    ) {
                        selectedDate = value
                        draft = draft.copy(event = draft.event.copy(date = LedgerDates.string(value)))
                    }
                }
            }
            FieldLabel(L10n.text("ledger.event"))
            KitTextField(draft.event.name, { draft = draft.copy(event = draft.event.copy(name = it)) }, L10n.text("ledger.event"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.city"))
            KitTextField(draft.event.city, { draft = draft.copy(event = draft.event.copy(city = it)) }, L10n.text("ledger.city"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.venue"))
            KitTextField(draft.event.venue, { draft = draft.copy(event = draft.event.copy(venue = it)) }, L10n.text("ledger.venue"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.address"))
            KitTextField(draft.event.address, { draft = draft.copy(event = draft.event.copy(address = it)) }, L10n.text("ledger.address"))

            Spacer(Modifier.height(16.dp))
            KitSecondaryButton(
                L10n.text("diary.export"),
                icon = Icons.Filled.Share,
                enabled = draft.isMeaningfulContent
            ) {
                if (flushSave()) showPublish = true
            }
            if (lastSaved != null) {
                Spacer(Modifier.height(10.dp))
                KitSecondaryButton(L10n.text("diary.delete"), icon = Icons.Filled.Delete) { confirmDelete = true }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDatePicker) {
        DatePickerSheet(selectedDate, { showDatePicker = false }) { picked ->
            selectedDate = picked
            draft = draft.copy(event = draft.event.copy(date = LedgerDates.string(picked)))
            showDatePicker = false
        }
    }
    if (showEventSelector) {
        OptionSelector(
            title = L10n.text("diary.chooseEvent"),
            options = listOf("" to L10n.text("diary.manualEvent")) + ledgerStore.document.events.map {
                it.id to listOfNotNull(it.name, it.city, it.venue).filter { value -> value.isNotEmpty() }.joinToString(" · ")
            },
            selected = eventSelection,
            onDismiss = { showEventSelector = false },
            onSelect = { id ->
                eventSelection = id
                if (id.isEmpty()) {
                    draft = draft.copy(event = draft.event.copy(ledgerEventId = null))
                } else {
                    val event = ledgerStore.document.events.firstOrNull { it.id == id }
                    if (event != null) {
                        val contextValue = DiaryCore.contextFrom(event, draft.event.date)
                        draft = draft.copy(event = contextValue)
                        selectedDate = LedgerDates.parse(contextValue.date) ?: selectedDate
                    }
                }
            }
        )
    }
    if (showEventEditor) {
        LedgerEventEditor(store = ledgerStore, event = null, onClose = { showEventEditor = false }) { id ->
            val created = ledgerStore.document.events.firstOrNull { it.id == id }
            if (created != null) {
                eventSelection = id
                val contextValue = DiaryCore.contextFrom(created, draft.event.date)
                draft = draft.copy(event = contextValue)
                selectedDate = LedgerDates.parse(contextValue.date) ?: selectedDate
            }
        }
    }
    if (showPublish) {
        DiaryPublishSheet(entry = draft, onClose = { showPublish = false })
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = L10n.text("diary.deleteTitle"),
            message = L10n.text("diary.deleteMessage"),
            confirmLabel = L10n.text("diary.delete"),
            onConfirm = {
                wasDeleted = true
                if (store.deleteEntry(draft.id)) onClose() else {
                    error = store.storageError
                    saveState = "diary.saveError"
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun DiaryPublishSheet(entry: DiaryEntry, onClose: () -> Unit) {
    val kit = LocalKit.current
    val context = LocalContext.current
    var copied by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val textExporter = rememberTextExporter { error = it }
    val markdown = DiaryCore.markdown(entry)
    val plain = DiaryCore.plainText(entry)

    ModalScreen(title = L10n.text("diary.export"), onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(L10n.text("diary.exportHint"), fontSize = 12.5.sp, color = kit.muted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                KitSecondaryButton(
                    L10n.text(if (copied == "markdown") "import.copied" else "diary.copyMarkdown"),
                    modifier = Modifier.weight(1f)
                ) {
                    copyToClipboard(context, markdown)
                    copied = "markdown"
                }
                KitSecondaryButton(
                    L10n.text(if (copied == "text") "import.copied" else "diary.copyText"),
                    modifier = Modifier.weight(1f)
                ) {
                    copyToClipboard(context, plain)
                    copied = "text"
                }
            }
            Spacer(Modifier.height(10.dp))
            KitSecondaryButton(L10n.text("diary.exportMarkdown")) {
                textExporter("OshicueKit-recap-" + entry.event.date + ".md", markdown)
            }
            Spacer(Modifier.height(8.dp))
            KitSecondaryButton(L10n.text("diary.exportText")) {
                textExporter("OshicueKit-recap-" + entry.event.date + ".txt", plain)
            }
            Spacer(Modifier.height(8.dp))
            KitSecondaryButton(L10n.text("diary.share"), icon = Icons.Filled.Share) {
                shareText(context, plain, entry.title.ifEmpty { entry.event.name })
            }
            error?.let {
                Spacer(Modifier.height(10.dp))
                InlineError(it)
            }
            Spacer(Modifier.height(14.dp))
            DividerLine()
            Spacer(Modifier.height(12.dp))
            FormCard { Text(plain, fontSize = 13.sp, color = kit.ink) }
        }
    }
}

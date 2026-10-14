package com.oshicue.kit.ui.diary

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
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.oshicue.kit.core.DiaryCore
import com.oshicue.kit.core.DiaryEntry
import com.oshicue.kit.core.DiaryEventContext
import com.oshicue.kit.core.DiaryStatus
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.data.DiaryStore
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.common.KitField
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.ledger.LedgerEventEditor
import com.oshicue.kit.ui.ledger.LedgerPresentation
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate

@Composable
fun DiaryEditor(store: DiaryStore, ledgerStore: LedgerStore, entry: DiaryEntry?, onClose: () -> Unit) {
    val palette = kitPalette()
    val initial = entry ?: DiaryEntry()
    var draft by remember { mutableStateOf(initial) }
    var lastSaved by remember { mutableStateOf(entry) }
    var selectedDate by remember { mutableStateOf(LedgerPresentation.date(initial.event.date)) }
    var eventSelection by remember { mutableStateOf(initial.event.ledgerEventID ?: "") }
    var tagsText by remember { mutableStateOf(initial.tags.joinToString(", ")) }
    var saveState by remember { mutableStateOf("diary.saved") }
    var error by remember { mutableStateOf<String?>(null) }
    var wasDeleted by remember { mutableStateOf(false) }
    var showEventEditor by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showPublish by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val hasMeaningfulContent = draft.isMeaningfulContent
    val hasUnsavedChanges = !wasDeleted && (lastSaved != null || hasMeaningfulContent) && draft != lastSaved

    fun flushSave(): Boolean {
        if (wasDeleted || !hasUnsavedChanges) return true
        val now = Instant.now()
        var value = draft.copy(updatedAt = maxOf(now, maxOf(draft.createdAt, draft.updatedAt)))
        if (store.upsertEntry(value)) {
            draft = value
            lastSaved = value
            saveState = "diary.saved"
            error = null
            return true
        }
        error = store.storageError ?: L10n.text("diary.saveError")
        saveState = "diary.saveError"
        return false
    }

    LaunchedEffect(draft) {
        if (!hasUnsavedChanges) {
            if (error == null) saveState = "diary.saved"
            return@LaunchedEffect
        }
        saveState = "diary.saving"
        delay(500)
        flushSave()
    }

    val currentFlush by rememberUpdatedState { flushSave() }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) currentFlush()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    KitSheet(
        title = L10n.text(if (lastSaved == null) "diary.add" else "diary.edit"),
        onClose = {
            if (flushSave()) onClose()
        }
    ) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("diary.titleField"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    androidx.compose.material3.TextField(
                        value = draft.title,
                        onValueChange = { draft = draft.copy(title = it) },
                        placeholder = { Text(L10n.text("diary.titleField"), color = palette.muted) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = palette.line,
                            unfocusedIndicatorColor = palette.line
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = palette.ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(L10n.text("diary.body"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    androidx.compose.material3.TextField(
                        value = draft.body,
                        onValueChange = { draft = draft.copy(body = it) },
                        placeholder = { Text(L10n.text("diary.bodyHint"), color = palette.muted, fontSize = 15.sp) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = palette.line,
                            unfocusedIndicatorColor = palette.line
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = palette.ink, fontSize = 16.sp),
                        modifier = Modifier.fillMaxWidth().height(310.dp)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (error == null) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                            contentDescription = null,
                            tint = if (error == null) palette.muted else palette.warningText,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.text(saveState), color = if (error == null) palette.muted else palette.warningText, fontSize = 12.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        if (error != null) {
                            Text(
                                L10n.text("diary.retry"),
                                color = palette.accentText,
                                fontSize = 12.sp,
                                modifier = Modifier.clickable { flushSave() }
                            )
                        }
                    }
                    error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
                    Text(L10n.text("diary.autosaveHint"), color = palette.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("diary.summary"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    androidx.compose.material3.TextField(
                        value = draft.summary,
                        onValueChange = { draft = draft.copy(summary = it) },
                        colors = androidx.compose.material3.TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = palette.line,
                            unfocusedIndicatorColor = palette.line
                        ),
                        textStyle = androidx.compose.ui.text.TextStyle(color = palette.ink, fontSize = 15.sp),
                        modifier = Modifier.fillMaxWidth().height(110.dp)
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    KitField(L10n.text("diary.tags"), tagsText) { value ->
                        tagsText = value
                        draft = draft.copy(tags = DiaryCore.parseTags(value))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(L10n.text("diary.status"), color = palette.ink, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Row(
                            modifier = Modifier
                                .background(palette.surfaceMuted, RoundedCornerShape(10.dp))
                                .padding(3.dp)
                        ) {
                            for (status in listOf(DiaryStatus.draft, DiaryStatus.ready)) {
                                Box(
                                    modifier = Modifier
                                        .background(
                                            if (draft.status == status) palette.surface else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { draft = draft.copy(status = status) }
                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                ) {
                                    Text(
                                        L10n.text(if (status == DiaryStatus.ready) "diary.filter.ready" else "diary.filter.draft"),
                                        color = if (draft.status == status) palette.accentText else palette.muted,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                    Text(L10n.text("diary.tagsHint"), color = palette.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("diary.eventSection"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    val eventOptions = mutableListOf("" to L10n.text("diary.manualEvent"))
                    eventOptions.addAll(ledgerStore.document.events.map { event ->
                        event.id to listOf(event.name, event.city ?: "", event.venue ?: "").filter { it.isNotEmpty() }.joinToString(" · ")
                    })
                    val savedID = draft.event.ledgerEventID
                    if (savedID != null && ledgerStore.document.events.none { it.id == savedID }) {
                        eventOptions.add(savedID to draft.event.name)
                    }
                    KitDropdownRow(
                        label = L10n.text("diary.chooseEvent"),
                        value = eventSelection,
                        options = eventOptions,
                        onSelect = { id ->
                            eventSelection = id
                            if (id.isEmpty()) {
                                draft = draft.copy(event = draft.event.copy(ledgerEventID = null))
                            } else {
                                val event = ledgerStore.document.events.firstOrNull { it.id == id }
                                if (event != null) {
                                    val context = DiaryCore.context(event, defaultDate = draft.event.date)
                                    draft = draft.copy(event = context)
                                    selectedDate = LedgerPresentation.date(context.date)
                                }
                            }
                        },
                        emptyLabel = L10n.text("diary.manualEvent")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showEventEditor = true }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("ledger.addEvent"), color = palette.accentText, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.text("ledger.date"), color = palette.ink, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(KitDate.string(selectedDate, "yyyy MMM d EEE"), color = palette.accentText, fontSize = 15.sp)
                    }
                    KitField(L10n.text("ledger.event"), draft.event.name) { value ->
                        draft = draft.copy(event = draft.event.copy(name = value))
                    }
                    KitField(L10n.text("ledger.city"), draft.event.city) { value ->
                        draft = draft.copy(event = draft.event.copy(city = value))
                    }
                    KitField(L10n.text("ledger.venue"), draft.event.venue) { value ->
                        draft = draft.copy(event = draft.event.copy(venue = value))
                    }
                    KitField(L10n.text("ledger.address"), draft.event.address) { value ->
                        draft = draft.copy(event = draft.event.copy(address = value))
                    }
                }

                Button(
                    onClick = {
                        if (flushSave()) showPublish = true
                    },
                    enabled = hasMeaningfulContent,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.surface, contentColor = palette.accentText),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Filled.IosShare, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.text("diary.export"), fontWeight = FontWeight.SemiBold)
                }

                if (lastSaved != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("diary.delete"), color = palette.warningText, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    LaunchedEffect(selectedDate) {
        if (draft.event.date != LedgerPresentation.calendar.format(selectedDate)) {
            draft = draft.copy(event = draft.event.copy(date = LedgerPresentation.calendar.format(selectedDate)))
        }
    }

    if (showDatePicker) {
        KitDatePickerDialog(selectedDate, { selectedDate = it }, { showDatePicker = false })
    }
    if (showEventEditor) {
        LedgerEventEditor(
            store = ledgerStore,
            event = null,
            onSaved = { id ->
                val event = ledgerStore.document.events.firstOrNull { it.id == id }
                if (event != null) {
                    eventSelection = id
                    val context = DiaryCore.context(event, defaultDate = draft.event.date)
                    draft = draft.copy(event = context)
                    selectedDate = LedgerPresentation.date(context.date)
                }
            },
            onClose = { showEventEditor = false }
        )
    }
    if (showPublish) {
        DiaryPublishSheet(draft) { showPublish = false }
    }
    if (confirmDelete && lastSaved != null) {
        KitAlert(
            title = L10n.text("diary.deleteTitle"),
            message = L10n.text("diary.deleteMessage"),
            confirmLabel = L10n.text("diary.delete"),
            onConfirm = {
                confirmDelete = false
                if (store.deleteEntry(draft.id)) {
                    wasDeleted = true
                    onClose()
                } else {
                    error = store.storageError
                    saveState = "diary.saveError"
                }
            },
            onDismiss = { confirmDelete = false },
            destructive = true
        )
    }
}

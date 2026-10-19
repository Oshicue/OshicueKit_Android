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
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.ImportError
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.ScheduleDemo
import com.oshicue.kit.core.ScheduleDocument
import com.oshicue.kit.core.ScheduleImporter
import com.oshicue.kit.core.ScheduleStore
import com.oshicue.kit.core.SessionKind
import java.time.ZoneId

@Composable
fun ScheduleImportSheet(store: ScheduleStore, onClose: () -> Unit) {
    val kit = LocalKit.current
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ScheduleDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var pending by remember { mutableStateOf<ScheduleDocument?>(null) }
    var copied by remember { mutableStateOf(false) }

    val pickFiles = rememberFilePicker(multiple = true) { contents ->
        try {
            var combined = ScheduleDocument(events = emptyList())
            contents.forEach { content ->
                val parsed = ScheduleImporter.parse(content)
                combined = ScheduleDocument(1, combined.events + parsed.events)
            }
            // Validate the whole batch, including duplicate event IDs across files.
            val serialized = ScheduleImporter.encode(combined)
            ScheduleImporter.parse(serialized)
            if (combined.events.isEmpty() || combined.events.none { it.sessions.isNotEmpty() }) {
                preview = null
                error = L10n.text("import.empty")
                return@rememberFilePicker
            }
            text = serialized
            preview = combined
            error = null
        } catch (failure: Exception) {
            preview = null
            error = scheduleErrorText(failure)
        }
    }

    ModalScreen(title = L10n.text("import.title"), onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(kit.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Download, null, tint = kit.accentText, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(L10n.text("import.paste"), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                    Text("JSON · UTF-8", fontSize = 11.sp, color = kit.muted)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Row(
                    Modifier.clickable { pickFiles() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Folder, null, tint = kit.accentText, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(L10n.text("import.file"), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = kit.ink)
                }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.clickable {
                        copyToClipboard(context, loadTemplateText(context, "timetable-template.json"))
                        copied = true
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        L10n.text(if (copied) "import.copied" else "import.copyTemplate"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = kit.accentText
                    )
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
                placeholder = L10n.text("import.placeholder"),
                minHeight = 230.dp
            )
            Spacer(Modifier.height(10.dp))
            KitPrimaryButton(
                L10n.text("import.validate"),
                enabled = text.isNotBlank()
            ) {
                try {
                    val parsed = ScheduleImporter.parse(text)
                    if (parsed.events.isEmpty() || parsed.events.none { it.sessions.isNotEmpty() }) {
                        preview = null
                        error = L10n.text("import.empty")
                    } else {
                        preview = parsed
                        error = null
                    }
                } catch (failure: Exception) {
                    preview = null
                    error = scheduleErrorText(failure)
                }
            }
            error?.let {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(kit.accent.copy(alpha = 0.08f))
                        .padding(14.dp)
                ) {
                    Icon(Icons.Filled.Warning, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(it, fontSize = 12.sp, color = androidx.compose.material3.MaterialTheme.colorScheme.error)
                }
            }
            preview?.let { document ->
                Spacer(Modifier.height(12.dp))
                FormCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CheckCircle, null, tint = androidx.compose.ui.graphics.Color(0xFF2E7D32), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(L10n.text("import.preview"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = androidx.compose.ui.graphics.Color(0xFF2E7D32))
                    }
                    Text(
                        L10n.format(
                            "import.summary",
                            document.events.size,
                            document.events.sumOf { it.sessions.size }
                        ),
                        fontSize = 13.sp,
                        color = kit.ink
                    )
                    Spacer(Modifier.height(8.dp))
                    document.events.forEach { event ->
                        Text(event.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                        Text(event.city + " · " + event.timeZone, fontSize = 11.sp, color = kit.muted)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        L10n.text(if (store.isDemo) "import.replacesDemo" else "import.updateHint"),
                        fontSize = 11.sp,
                        color = kit.muted
                    )
                    Spacer(Modifier.height(10.dp))
                    KitPrimaryButton(L10n.text("import.apply")) {
                        store.importDocument(document)
                        onClose()
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(L10n.text("import.timeZonesHint"), fontSize = 11.sp, color = kit.muted)
            Spacer(Modifier.height(18.dp))
            DividerLine()
            Spacer(Modifier.height(16.dp))
            FormCard {
                Text(L10n.text("settings.demoLabel"), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                Spacer(Modifier.height(8.dp))
                KitSecondaryButton(L10n.text("settings.loadDemo"), icon = Icons.Filled.PlayArrow) {
                    store.loadDemo()
                    onClose()
                }
                Text(L10n.text("settings.demoHint"), fontSize = 11.sp, color = kit.muted)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

/** Maps importer failures to the localized category plus the exact input path. */
fun scheduleErrorText(failure: Throwable): String = when (failure) {
    is ImportError -> L10n.text(failure.key) + "\n" + failure.path
    else -> L10n.text("import.readError") + "\n" + (failure.localizedMessage ?: "")
}

@Composable
fun EventSelectionSheet(
    store: ScheduleStore,
    selection: Set<String>,
    onSelectionChange: (Set<String>) -> Unit,
    onClose: () -> Unit
) {
    val kit = LocalKit.current
    var pendingDeletion by remember { mutableStateOf<FestivalEvent?>(null) }
    var deletionError by remember { mutableStateOf<String?>(null) }

    ModalScreen(title = L10n.text("schedule.selectEvents"), onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            val events = store.document.events
            deletionError?.let {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Icon(Icons.Filled.Warning, null, tint = kit.accentText, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(it, fontSize = 11.5.sp, color = kit.accentText)
                }
            }
            if (events.isEmpty()) {
                EmptyState(
                    title = L10n.text("schedule.noEvents"),
                    hint = L10n.text("schedule.noEventsHint")
                )
            } else {
                Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Text(
                        L10n.text("common.selectAll"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kit.accentText,
                        modifier = Modifier.clickable { onSelectionChange(events.map { it.id }.toSet()) }
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        L10n.text("common.deselectAll"),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = kit.accentText,
                        modifier = Modifier.clickable { onSelectionChange(emptySet()) }
                    )
                }
                events.forEach { event ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            Modifier
                                .weight(1f)
                                .clickable {
                                    onSelectionChange(
                                        if (event.id in selection) selection - event.id else selection + event.id
                                    )
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkmark(event.id in selection)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(event.name, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                                Text(event.city + " · " + event.timeZone, fontSize = 11.sp, color = kit.muted)
                                Text(
                                    L10n.format("import.summary", 1, event.sessions.size),
                                    fontSize = 10.5.sp,
                                    color = kit.muted
                                )
                            }
                        }
                        Row(
                            Modifier.clickable { pendingDeletion = event },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.Delete, null, tint = androidx.compose.material3.MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(
                                L10n.text("schedule.deleteEvent"),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = androidx.compose.material3.MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    DividerLine()
                }
            }
            Spacer(Modifier.height(20.dp))
            KitPrimaryButton(L10n.text("common.done")) { onClose() }
        }
    }

    pendingDeletion?.let { event ->
        ConfirmDialog(
            title = L10n.text("schedule.deleteEventTitle"),
            message = L10n.format("schedule.deleteEventMessage", event.name),
            confirmLabel = L10n.text("schedule.deleteEvent"),
            onConfirm = {
                val wasSelected = event.id in selection
                if (store.deleteEvent(event.id)) {
                    val remaining = store.document.events.map { it.id }
                    var updated = selection - event.id
                    if (wasSelected && updated.isEmpty()) updated = remaining.toSet()
                    onSelectionChange(updated)
                    deletionError = null
                } else {
                    deletionError = store.storageError
                }
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null }
        )
    }
}

@Composable
fun SessionDetailSheet(
    entry: SessionEntry,
    store: ScheduleStore,
    zone: ZoneId,
    onClose: () -> Unit
) {
    val kit = LocalKit.current
    val favorite = store.isFavorite(entry.id)
    ModalScreen(title = L10n.text("schedule.sessionDetails"), onClose = onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .width(5.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(hexToColor(entry.venue.color))
                )
                Spacer(Modifier.width(13.dp))
                Column {
                    Text(entry.artist, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            L10n.text(if (entry.session.kind == SessionKind.Live) "schedule.live" else "schedule.goods"),
                            fontSize = 13.sp,
                            color = kit.muted
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                KitDate.string(entry.session.startsAt, "yyyy.MM.dd EEE", zone),
                fontSize = 13.sp,
                color = kit.ink
            )
            Text(
                KitDate.time(entry.session.startsAt, zone) + " — " + KitDate.time(entry.session.endsAt, zone),
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = kit.ink
            )
            Spacer(Modifier.height(12.dp))
            DetailLine(L10n.text("schedule.timeZone"), zone.id)
            DetailLine(L10n.text("schedule.venue"), entry.venue.name)
            entry.session.area?.let { DetailLine(L10n.text("schedule.artist"), it) }
            DetailLine(L10n.text("schedule.event"), entry.event.name)
            val eventZone = entry.event.zone
            if (eventZone != zone) {
                Text(
                    entry.event.timeZone + " · " + KitDate.time(entry.session.startsAt, eventZone) +
                        " — " + KitDate.time(entry.session.endsAt, eventZone),
                    fontSize = 11.sp,
                    color = kit.muted
                )
            }
            val notes = entry.session.notes
            if (!notes.isNullOrEmpty()) {
                Spacer(Modifier.height(16.dp))
                SectionLabel(L10n.text("schedule.notes"))
                Spacer(Modifier.height(6.dp))
                Text(notes, fontSize = 13.5.sp, color = kit.ink)
            }
            Spacer(Modifier.height(20.dp))
            KitSecondaryButton(
                L10n.text(if (favorite) "schedule.unfavorite" else "schedule.favorite"),
                icon = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder
            ) {
                store.toggleFavorite(entry.id)
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    val kit = LocalKit.current
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, fontSize = 12.5.sp, color = kit.muted)
        Spacer(Modifier.weight(1f))
        Text(value, fontSize = 12.5.sp, color = kit.ink, fontWeight = FontWeight.Medium)
    }
}

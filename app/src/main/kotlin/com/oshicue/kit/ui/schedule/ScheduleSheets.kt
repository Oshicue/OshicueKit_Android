package com.oshicue.kit.ui.schedule

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.ImportError
import com.oshicue.kit.core.ScheduleDocument
import com.oshicue.kit.core.ScheduleImporter
import com.oshicue.kit.core.SessionKind
import com.oshicue.kit.data.ScheduleStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitDemoSection
import com.oshicue.kit.ui.common.KitDivider
import com.oshicue.kit.ui.common.KitMessageAlert
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.hexColor
import com.oshicue.kit.ui.theme.kitPalette
import java.time.ZoneId

@Composable
fun ImportSheet(store: ScheduleStore, onClose: () -> Unit) {
    val palette = kitPalette()
    val context = LocalContext.current
    var text by remember { mutableStateOf("") }
    var preview by remember { mutableStateOf<ScheduleDocument?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var copied by remember { mutableStateOf(false) }

    fun showError(value: Exception) {
        preview = null
        error = if (value is ImportError) {
            L10n.text(value.key) + "\n" + value.path
        } else {
            L10n.text("import.readError") + "\n" + (value.message ?: value.toString())
        }
    }

    val chooseFiles = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        if (uris.isEmpty()) return@rememberLauncherForActivityResult
        try {
            var combined = ScheduleDocument(events = emptyList())
            for (uri in uris) {
                val input = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw IllegalStateException("Unreadable file")
                val parsed = ScheduleImporter.parse(input)
                combined = combined.copy(events = combined.events + parsed.events)
            }
            // Validate the whole batch too, including duplicate event IDs across files.
            val serialized = ScheduleImporter.encode(combined)
            ScheduleImporter.parse(serialized)
            if (combined.events.isEmpty() || combined.events.none { it.sessions.isNotEmpty() }) {
                preview = null
                error = L10n.text("import.empty")
                return@rememberLauncherForActivityResult
            }
            text = serialized
            preview = combined
            error = null
        } catch (value: Exception) {
            showError(value)
        }
    }

    LaunchedEffect(text) {
        error = null
    }

    KitSheet(L10n.text("import.title"), onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(56.dp).background(palette.surface, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(26.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(L10n.text("import.paste"), color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text("JSON · UTF-8", color = palette.muted, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    modifier = Modifier.clickable { chooseFiles.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null, tint = palette.ink, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.text("import.file"), color = palette.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier.clickable {
                        val template = runCatching { context.assets.open("timetable-template.json").bufferedReader().use { it.readText() } }.getOrNull()
                            ?: runCatching { ScheduleImporter.encode(com.oshicue.kit.core.ScheduleDemo.document()) }.getOrNull() ?: ""
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("timetable-template.json", template))
                        copied = true
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (copied) Icons.Filled.Check else Icons.Filled.ContentCopy,
                        contentDescription = null,
                        tint = palette.ink,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        L10n.text(if (copied) "import.copied" else "import.copyTemplate"),
                        color = palette.ink,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            TextField(
                value = text,
                onValueChange = {
                    text = it
                    preview = null
                },
                placeholder = { Text(L10n.text("import.placeholder"), color = palette.muted, fontSize = 12.sp) },
                textStyle = TextStyle(fontFamily = FontFamily.Monospace, fontSize = 12.sp, color = palette.ink),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, autoCorrectEnabled = false),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = palette.surface,
                    unfocusedContainerColor = palette.surface,
                    focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth().height(230.dp)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = {
                    try {
                        val parsed = ScheduleImporter.parse(text)
                        if (parsed.events.isEmpty() || parsed.events.none { it.sessions.isNotEmpty() }) {
                            preview = null
                            error = L10n.text("import.empty")
                            return@Button
                        }
                        preview = parsed
                        error = null
                    } catch (value: Exception) {
                        showError(value)
                    }
                },
                enabled = text.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) { Text(L10n.text("import.validate"), fontWeight = FontWeight.SemiBold) }
            error?.let { message ->
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.warningText.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(message, color = palette.warningText, fontSize = 12.sp)
                }
            }
            preview?.let { document ->
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface, RoundedCornerShape(16.dp))
                        .padding(18.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Verified, contentDescription = null, tint = androidx.compose.ui.graphics.Color(0xFF2E9E5B), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(L10n.text("import.preview"), color = palette.ink, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        L10n.format("import.summary", document.events.size, document.events.sumOf { it.sessions.size }),
                        color = palette.ink,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    for (event in document.events) {
                        Column(modifier = Modifier.padding(vertical = 3.dp)) {
                            Text(event.name, color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            Text(event.city + " · " + event.timeZone, color = palette.muted, fontSize = 12.sp)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        L10n.text(if (store.isDemo) "import.replacesDemo" else "import.updateHint"),
                        color = palette.muted,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            store.importDocument(document)
                            onClose()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().height(46.dp)
                    ) { Text(L10n.text("import.apply"), fontWeight = FontWeight.SemiBold) }
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(L10n.text("import.timeZonesHint"), color = palette.muted, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(20.dp))
            KitDivider()
            Spacer(modifier = Modifier.height(20.dp))
            KitDemoSection(
                title = L10n.text("settings.demoLabel"),
                actionLabel = L10n.text("settings.loadDemo"),
                hint = L10n.text("settings.demoHint"),
                icon = Icons.Filled.SmartDisplay
            ) {
                store.loadDemo()
                onClose()
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun EventSelectionSheet(
    store: ScheduleStore,
    selection: Set<String>,
    onSelection: (Set<String>) -> Unit,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    var pendingDeletion by remember { mutableStateOf<FestivalEvent?>(null) }
    var deletionError by remember { mutableStateOf<String?>(null) }
    val events = store.document.events

    KitSheet(L10n.text("schedule.selectEvents"), onClose) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            deletionError?.let { message ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .background(palette.warningText.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Warning, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(message, color = palette.warningText, fontSize = 12.sp)
                }
            }
            if (events.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(L10n.text("schedule.noEvents"), color = palette.ink, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(L10n.text("schedule.noEventsHint"), color = palette.muted, fontSize = 13.sp)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        L10n.text("common.selectAll"),
                        color = palette.accentText,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onSelection(events.map { it.id }.toSet()) }.padding(4.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        L10n.text("common.deselectAll"),
                        color = palette.accentText,
                        fontSize = 14.sp,
                        modifier = Modifier.clickable { onSelection(emptySet()) }.padding(4.dp)
                    )
                }
                LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(events, key = { it.id }) { event ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(palette.surface, RoundedCornerShape(14.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).clickable {
                                    onSelection(if (event.id in selection) selection - event.id else selection + event.id)
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (event.id in selection) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (event.id in selection) palette.accentText else palette.muted,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(event.name, color = palette.ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text(event.city + " · " + event.timeZone, color = palette.muted, fontSize = 12.sp)
                                    Text(L10n.format("import.summary", 1, event.sessions.size), color = palette.muted, fontSize = 11.sp)
                                }
                            }
                            Row(
                                modifier = Modifier.clickable { pendingDeletion = event }.padding(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    L10n.text("schedule.deleteEvent"),
                                    color = palette.warningText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }

    pendingDeletion?.let { event ->
        KitAlert(
            title = L10n.text("schedule.deleteEventTitle"),
            message = L10n.format("schedule.deleteEventMessage", event.name),
            confirmLabel = L10n.text("schedule.deleteEvent"),
            onConfirm = {
                val wasSelected = event.id in selection
                if (store.deleteEvent(event.id)) {
                    val next = selection - event.id
                    onSelection(if (wasSelected && next.isEmpty()) store.document.events.map { it.id }.toSet() else next)
                    deletionError = null
                } else {
                    deletionError = store.storageError
                }
                pendingDeletion = null
            },
            onDismiss = { pendingDeletion = null },
            destructive = true
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
    val palette = kitPalette()
    val favorite = store.isFavorite(entry.id)
    KitSheet(L10n.text("schedule.sessionDetails"), onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .width(5.dp)
                        .height(56.dp)
                        .background(hexColor(entry.venue.color), RoundedCornerShape(5.dp))
                )
                Spacer(modifier = Modifier.width(13.dp))
                Column {
                    Text(entry.artist, color = palette.ink, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            if (entry.session.kind == SessionKind.live) Icons.Filled.GraphicEq else Icons.Filled.ShoppingBag,
                            contentDescription = null,
                            tint = palette.muted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            L10n.text(if (entry.session.kind == SessionKind.live) "schedule.live" else "schedule.goods"),
                            color = palette.muted,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                KitDate.string(entry.session.startsAt, "yyyy.MM.dd EEE", zone),
                color = palette.ink,
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                KitDate.time(entry.session.startsAt, zone) + " — " + KitDate.time(entry.session.endsAt, zone),
                color = palette.ink,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(14.dp))
            DetailRow(L10n.text("schedule.timeZone"), zone.id)
            DetailRow(L10n.text("schedule.venue"), entry.venue.name)
            entry.session.area?.let { area -> Text(area, color = palette.muted, fontSize = 14.sp) }
            DetailRow(L10n.text("schedule.event"), entry.event.name)
            if (zone != entry.event.timeZoneValue) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    entry.event.timeZone + " · " + KitDate.time(entry.session.startsAt, entry.event.timeZoneValue) +
                        " — " + KitDate.time(entry.session.endsAt, entry.event.timeZoneValue),
                    color = palette.muted,
                    fontSize = 12.sp
                )
            }
            entry.session.notes?.takeIf { it.isNotEmpty() }?.let { notes ->
                Spacer(modifier = Modifier.height(16.dp))
                Text(L10n.text("schedule.notes"), color = palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(notes, color = palette.ink)
            }
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = { store.toggleFavorite(entry.id) },
                colors = ButtonDefaults.buttonColors(containerColor = palette.surfaceMuted, contentColor = palette.accentText),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(
                    L10n.text(if (favorite) "schedule.unfavorite" else "schedule.favorite"),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    val palette = kitPalette()
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Text(label, color = palette.muted, fontSize = 14.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(value, color = palette.ink, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

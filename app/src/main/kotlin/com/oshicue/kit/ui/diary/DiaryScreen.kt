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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.DiaryCore
import com.oshicue.kit.core.DiaryEntry
import com.oshicue.kit.core.DiaryStatus
import com.oshicue.kit.data.DiaryStore
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.BrandWordmark
import com.oshicue.kit.ui.common.CircleIconButton
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette

@Composable
fun DiaryScreen(store: DiaryStore, ledgerStore: LedgerStore) {
    val palette = kitPalette()
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("all") }
    var editing by remember { mutableStateOf<DiaryEditorSelection?>(null) }
    var showManagement by remember { mutableStateOf(false) }

    val entries = remember(store.document.entries, search, filter) {
        DiaryCore.search(store.document.entries, search)
            .filter { filter == "all" || it.status.raw == filter }
            .sortedWith(
                compareByDescending<DiaryEntry> { it.event.date }.thenByDescending { it.updatedAt }
            )
    }
    val days = remember(entries) { entries.map { it.event.date }.toSet().sortedDescending() }

    Column(modifier = Modifier.fillMaxSize().background(palette.background)) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(top = 10.dp, bottom = 17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandWordmark()
                Spacer(modifier = Modifier.weight(1f))
                CircleIconButton(Icons.Filled.Tune, L10n.text("diary.management")) { showManagement = true }
                Spacer(modifier = Modifier.width(10.dp))
                IconButton(
                    onClick = { editing = DiaryEditorSelection(null) },
                    modifier = Modifier.size(42.dp).background(palette.accentFill, CircleShape)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = L10n.text("diary.add"), tint = palette.onAccent)
                }
            }
            Spacer(modifier = Modifier.height(17.dp))
            Text(L10n.text("diary.title"), color = palette.ink, fontSize = 29.sp, fontWeight = FontWeight.Bold)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 13.dp)
                .background(palette.surface, RoundedCornerShape(14.dp))
                .padding(13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(10.dp))
            androidx.compose.material3.TextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                placeholder = { Text(L10n.text("diary.search"), color = palette.muted, fontSize = 14.sp) },
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = palette.ink, fontSize = 15.sp),
                modifier = Modifier.weight(1f)
            )
            if (search.isNotEmpty()) {
                Icon(
                    Icons.Filled.Cancel,
                    contentDescription = L10n.text("diary.clearSearch"),
                    tint = palette.muted,
                    modifier = Modifier.size(18.dp).clickable { search = "" }
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 14.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.surfaceMuted)
                .padding(4.dp)
        ) {
            DiaryFilter(L10n.text("diary.filter.all"), filter == "all", Modifier.weight(1f)) { filter = "all" }
            DiaryFilter(L10n.text("diary.filter.draft"), filter == "draft", Modifier.weight(1f)) { filter = "draft" }
            DiaryFilter(L10n.text("diary.filter.ready"), filter == "ready", Modifier.weight(1f)) { filter = "ready" }
        }
        store.storageError?.let { error ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 12.dp)
                    .background(palette.warningText.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(error, color = palette.warningText, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text(
                    L10n.text("common.close"),
                    color = palette.warningText,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { store.storageError = null }
                )
            }
        }
        if (entries.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .background(palette.surface, RoundedCornerShape(25.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(36.dp))
                }
                Text(
                    L10n.text(if (search.isEmpty() && filter == "all") "diary.empty.title" else "diary.empty.search"),
                    color = palette.ink,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(L10n.text("diary.empty.hint"), color = palette.muted, fontSize = 13.sp)
                Button(
                    onClick = { editing = DiaryEditorSelection(null) },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent)
                ) { Text(L10n.text("diary.add")) }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(15.dp)
            ) {
                for (day in days) {
                    item(key = "day-$day") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.width(4.dp).height(20.dp).background(palette.accent, RoundedCornerShape(50)))
                            Text(
                                KitDate.string(com.oshicue.kit.ui.ledger.LedgerPresentation.date(day), "yyyy MMM d E"),
                                color = palette.ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    items(entries.filter { it.event.date == day }, key = { it.id }) { entry ->
                        DiaryEntryCard(entry) { editing = DiaryEditorSelection(entry) }
                    }
                }
            }
        }
    }

    editing?.let { selection ->
        DiaryEditor(store, ledgerStore, selection.entry) { editing = null }
    }
    if (showManagement) {
        DiaryManagementSheet(store) { showManagement = false }
    }
}

data class DiaryEditorSelection(val entry: DiaryEntry?)

@Composable
private fun DiaryFilter(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = kitPalette()
    Box(
        modifier = modifier
            .height(37.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) palette.surface else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) palette.accentText else palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DiaryEntryCard(entry: DiaryEntry, onClick: () -> Unit) {
    val palette = kitPalette()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(19.dp))
            .clickable(onClick = onClick)
            .padding(17.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Text(
                entry.title.trim().ifEmpty { L10n.text("diary.untitled") },
                color = palette.ink,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                L10n.text(if (entry.status == DiaryStatus.ready) "diary.filter.ready" else "diary.filter.draft"),
                color = palette.accentText,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .background(palette.accent.copy(alpha = 0.08f), RoundedCornerShape(50))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        if (entry.event.name.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = palette.muted, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(entry.event.name, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        val location = listOf(entry.event.city, entry.event.venue).filter { it.isNotEmpty() }.joinToString(" · ")
        if (location.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Place, contentDescription = null, tint = palette.muted, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(5.dp))
                Text(location, color = palette.muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        val excerpt = if (entry.summary.isEmpty()) entry.body else entry.summary
        if (excerpt.isNotEmpty()) {
            Text(excerpt, color = palette.ink.copy(alpha = 0.8f), fontSize = 14.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (tag in entry.tags.take(3)) {
                Text(
                    "#" + tag,
                    color = palette.muted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .background(palette.background, RoundedCornerShape(50))
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Filled.KeyboardArrowRight, contentDescription = null, tint = palette.muted, modifier = Modifier.size(16.dp))
        }
    }
}

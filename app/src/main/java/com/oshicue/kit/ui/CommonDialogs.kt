package com.oshicue.kit.ui

import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

/**
 * A full-screen modal page used for editors, importers and management sheets.
 * It opens in its own window, like an iOS sheet: it covers the tab bar and does not
 * depend on the layout of the screen that presents it.
 */
@Composable
fun ModalScreen(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeLabel: String = L10n.text("common.close"),
    leadingBack: Boolean = false,
    actions: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false
        )
    ) {
        // Dialog windows re-provide the device configuration; keep Material text in the app language.
        LocalizedConfiguration {
            ModalWindowStyle()
            ModalScreenContent(title, onClose, modifier, closeLabel, leadingBack, actions, content)
        }
    }
}

/** Matches the status and navigation bar icons to the sheet and lets the keyboard resize it. */
@Composable
private fun ModalWindowStyle() {
    val window = (LocalView.current.parent as? DialogWindowProvider)?.window ?: return
    val view = LocalView.current
    val dark = LocalKit.current.dark
    SideEffect {
        @Suppress("DEPRECATION")
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !dark
            isAppearanceLightNavigationBars = !dark
        }
    }
}

@Composable
private fun ModalScreenContent(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier,
    closeLabel: String,
    leadingBack: Boolean,
    actions: (@Composable androidx.compose.foundation.layout.RowScope.() -> Unit)?,
    content: @Composable () -> Unit
) {
    val kit = LocalKit.current
    Surface(modifier = modifier.fillMaxSize(), color = kit.background) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(kit.background)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(
                        if (leadingBack) Icons.Filled.ArrowBack else Icons.Filled.Close,
                        contentDescription = closeLabel,
                        tint = kit.ink
                    )
                }
                Text(
                    title,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = kit.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    actions?.invoke(this)
                    if (actions == null) Spacer(Modifier.width(48.dp))
                }
            }
            DividerLine()
            Box(Modifier.weight(1f)) { content() }
        }
    }
}

@Composable
fun KitTextAction(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text,
            color = if (enabled) LocalKit.current.accentText else LocalKit.current.muted,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = true
) {
    val kit = LocalKit.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = kit.ink) },
        text = { Text(message, color = kit.ink.copy(alpha = 0.8f), fontSize = 13.5.sp) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirmLabel, color = if (destructive) MaterialTheme.colorScheme.error else kit.accentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.cancel"), color = kit.muted) }
        }
    )
}

@Composable
fun MessageDialog(title: String, message: String, onDismiss: () -> Unit) {
    val kit = LocalKit.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = kit.ink) },
        text = {
            val scroll = androidx.compose.foundation.rememberScrollState()
            Text(
                message,
                modifier = Modifier
                    .heightIn(max = 360.dp)
                    .verticalScroll(scroll),
                color = kit.ink.copy(alpha = 0.85f),
                fontSize = 13.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.done"), color = kit.accentText) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(
    initial: LocalDate,
    onDismiss: () -> Unit,
    onSelect: (LocalDate) -> Unit
) {
    val kit = LocalKit.current
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val millis = state.selectedDateMillis
                if (millis != null) {
                    onSelect(Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate())
                } else {
                    onSelect(initial)
                }
            }) { Text(L10n.text("common.done"), color = kit.accentText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.cancel"), color = kit.muted) }
        }
    ) {
        // The default title reads device-language resources, so the localized headline stands alone.
        LocalizedConfiguration { DatePicker(state = state, title = null) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(
    initial: LocalTime,
    onDismiss: () -> Unit,
    onSelect: (LocalTime) -> Unit
) {
    val kit = LocalKit.current
    val state = rememberTimePickerState(initialHour = initial.hour, initialMinute = initial.minute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state) },
        confirmButton = {
            TextButton(onClick = {
                onSelect(LocalTime.of(state.hour, state.minute))
            }) { Text(L10n.text("common.done"), color = kit.accentText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.cancel"), color = kit.muted) }
        }
    )
}

/** A compact date + time row that opens the platform pickers. */
@Composable
fun DateTimeField(
    label: String,
    value: Instant,
    zoneId: ZoneId,
    modifier: Modifier = Modifier,
    onPickDate: (LocalDate) -> Unit,
    onPickTime: (LocalTime) -> Unit
) {
    val kit = LocalKit.current
    var showDate by remember { mutableStateOf(false) }
    var showTime by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDate = true }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 13.5.sp, color = kit.ink)
        Spacer(Modifier.weight(1f))
        Text(
            KitDate.string(value, "yyyy.MM.dd HH:mm", zoneId),
            fontSize = 13.sp,
            color = kit.accentText,
            fontWeight = FontWeight.Medium
        )
        Spacer(Modifier.width(8.dp))
        Text(
            L10n.text("schedule.date"),
            fontSize = 11.sp,
            color = kit.muted,
            modifier = Modifier.clickable { showTime = true }
        )
    }
    if (showDate) {
        DatePickerSheet(
            initial = value.atZone(zoneId).toLocalDate(),
            onDismiss = { showDate = false },
            onSelect = { picked ->
                onPickDate(picked)
                showDate = false
                showTime = true
            }
        )
    }
    if (showTime) {
        TimePickerSheet(
            initial = value.atZone(zoneId).toLocalTime(),
            onDismiss = { showTime = false },
            onSelect = { picked ->
                onPickTime(picked)
                showTime = false
            }
        )
    }
}

@Composable
fun OptionSelector(
    title: String,
    options: List<Pair<String, String>>,
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val kit = LocalKit.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.SemiBold, color = kit.ink) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(options) { (value, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(value); onDismiss() }
                            .padding(vertical = 12.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, modifier = Modifier.weight(1f), color = kit.ink, fontSize = 14.sp)
                        if (value == selected) {
                            Text("✓", color = kit.accentText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.cancel"), color = kit.muted) }
        }
    )
}

@Composable
fun TimeZoneSelector(
    selected: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val kit = LocalKit.current
    var query by remember { mutableStateOf("") }
    val zones = remember(query) {
        ZoneId.getAvailableZoneIds()
            .filter { query.isBlank() || it.contains(query, ignoreCase = true) }
            .sorted()
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(L10n.text("agenda.timeZone"), fontWeight = FontWeight.SemiBold, color = kit.ink) },
        text = {
            Column {
                KitTextField(query, { query = it }, placeholder = L10n.text("agenda.timeZone"))
                Spacer(Modifier.height(8.dp))
                LazyColumn(Modifier.heightIn(max = 380.dp)) {
                    items(zones) { zone ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(zone); onDismiss() }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                zone.replace('_', ' '),
                                modifier = Modifier.weight(1f),
                                color = kit.ink,
                                fontSize = 13.sp
                            )
                            if (zone == selected) Text("✓", color = kit.accentText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.cancel"), color = kit.muted) }
        }
    )
}

@Composable
fun SelectableChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val kit = LocalKit.current
    Box(
        modifier = Modifier
            .background(
                if (selected) kit.accentFill else kit.surface,
                RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text,
            color = if (selected) kit.onAccent else kit.ink,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

fun Instant.withDate(date: LocalDate, zoneId: ZoneId): Instant {
    val time = this.atZone(zoneId).toLocalTime()
    return LocalDateTime.of(date, time).atZone(zoneId).toInstant()
}

fun Instant.withTime(time: LocalTime, zoneId: ZoneId): Instant {
    val date = this.atZone(zoneId).toLocalDate()
    return LocalDateTime.of(date, time).atZone(zoneId).toInstant()
}

fun Instant.atStartOfDayIn(zoneId: ZoneId): Instant =
    this.atZone(zoneId).toLocalDate().atStartOfDay(zoneId).toInstant()

/** Short weekday names starting from [firstDay], in the app language. */
fun weekdaySymbols(
    firstDay: java.time.DayOfWeek,
    locale: java.util.Locale = L10n.locale
): List<String> = (0 until 7).map { offset ->
    firstDay.plus(offset.toLong()).getDisplayName(java.time.format.TextStyle.SHORT, locale)
}

/** A localized month heading such as "October 2026" or "2026年10月", like UICalendarView. */
fun monthTitle(month: java.time.YearMonth, locale: java.util.Locale = L10n.locale): String {
    val formatter = android.icu.text.DateFormat.getInstanceForSkeleton("yMMMM", locale)
    val instant = month.atDay(1).atTime(12, 0).atZone(java.time.ZoneId.systemDefault()).toInstant()
    return formatter.format(java.util.Date.from(instant))
}

package com.oshicue.kit.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.oshicue.kit.R
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.theme.kitPalette

val UnboundedFamily = FontFamily(Font(R.font.unbounded))

@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    val palette = kitPalette()
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            text = "OSHI",
            color = palette.ink,
            fontFamily = UnboundedFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(1.dp))
        Text(
            text = "CUE",
            color = palette.accent,
            fontFamily = UnboundedFamily,
            fontWeight = FontWeight.Black,
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.width(5.dp))
        Text(
            text = "KIT",
            color = palette.ink,
            fontFamily = UnboundedFamily,
            fontWeight = FontWeight.Black,
            fontSize = 10.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
    }
}

@Composable
fun CircleIconButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    val palette = kitPalette()
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .background(palette.surface, CircleShape)
            .border(1.dp, palette.line, CircleShape)
    ) {
        Icon(icon, contentDescription = label, tint = palette.ink, modifier = Modifier.size(19.dp))
    }
}

/** A full-height page sheet matching the iOS presentation style. */
@Composable
fun KitSheet(
    title: String,
    onClose: () -> Unit,
    closeLabel: String = L10n.text("common.close"),
    content: @Composable ColumnScope.() -> Unit
) {
    val palette = kitPalette()
    Dialog(
        onDismissRequest = onClose,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        color = palette.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onClose) {
                        Icon(Icons.Filled.Close, contentDescription = closeLabel, tint = palette.muted)
                    }
                }
                content()
            }
        }
    }
}

@Composable
fun KitAlert(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    destructive: Boolean = false,
    dismissLabel: String = L10n.text("common.cancel")
) {
    val palette = kitPalette()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = palette.ink) },
        text = { Text(message, color = palette.muted) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel, color = if (destructive) palette.warningText else palette.accentText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(dismissLabel, color = palette.muted) }
        },
        containerColor = palette.surface,
        titleContentColor = palette.ink,
        textContentColor = palette.muted
    )
}

@Composable
fun KitMessageAlert(title: String, message: String, onDismiss: () -> Unit) {
    val palette = kitPalette()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = palette.ink) },
        text = { Text(message, color = palette.muted) },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.done"), color = palette.accentText) }
        },
        containerColor = palette.surface,
        titleContentColor = palette.ink,
        textContentColor = palette.muted
    )
}

@Composable
fun KitSectionCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val palette = kitPalette()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface, RoundedCornerShape(16.dp))
            .padding(16.dp),
        content = content
    )
}

@Composable
fun KitEmptyState(symbol: ImageVector, title: String, hint: String, modifier: Modifier = Modifier) {
    val palette = kitPalette()
    Column(
        modifier = modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(symbol, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(38.dp))
        Text(title, color = palette.ink, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        if (hint.isNotEmpty()) {
            Text(hint, color = palette.muted, fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = kitPalette()
    Box(
        modifier = modifier
            .shadow(if (selected) 2.dp else 0.dp, RoundedCornerShape(10.dp))
            .background(if (selected) palette.surface else androidx.compose.ui.graphics.Color.Transparent, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) palette.accentText else palette.muted,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun KitDivider() {
    val palette = kitPalette()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.line)
    )
}

@Composable
fun KitDatePickerDialog(initial: java.time.LocalDate, onPick: (java.time.LocalDate) -> Unit, onDismiss: () -> Unit) {
    val palette = kitPalette()
    val state = androidx.compose.material3.rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    androidx.compose.material3.DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { millis ->
                    onPick(java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneOffset.UTC).toLocalDate())
                }
                onDismiss()
            }) { Text(L10n.text("common.done"), color = palette.accentText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(L10n.text("common.cancel"), color = palette.muted) }
        }
    ) {
        androidx.compose.material3.DatePicker(state = state, showModeToggle = false)
    }
}

/** A labeled dropdown matching the app's picker rows. */
@Composable
fun KitDropdownRow(
    label: String,
    value: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    emptyLabel: String = ""
) {
    val palette = kitPalette()
    var expanded by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = palette.ink, fontSize = 15.sp)
        Spacer(modifier = Modifier.weight(1f))
        Text(
            value.ifEmpty { emptyLabel },
            color = if (value.isEmpty()) palette.muted else palette.accentText,
            fontSize = 15.sp,
            maxLines = 1
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = palette.muted,
            modifier = Modifier.size(16.dp)
        )
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            for ((id, title) in options) {
                DropdownMenuItem(
                    text = { Text(title, color = if (id == value) palette.accentText else palette.ink) },
                    onClick = {
                        expanded = false
                        onSelect(id)
                    }
                )
            }
        }
    }
}

/** A labeled text field used across the editing forms. */
@Composable
fun KitField(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    singleLine: Boolean = true,
    minLines: Int = 1,
    onValueChange: (String) -> Unit
) {
    val palette = kitPalette()
    androidx.compose.foundation.layout.Column(modifier = modifier.fillMaxWidth()) {
        Text(label, color = palette.muted, fontSize = 12.sp)
        androidx.compose.material3.TextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = keyboardType,
                autoCorrectEnabled = false
            ),
            colors = androidx.compose.material3.TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = palette.line,
                unfocusedIndicatorColor = palette.line
            ),
            textStyle = androidx.compose.ui.text.TextStyle(color = palette.ink, fontSize = 15.sp)
        )
    }
}

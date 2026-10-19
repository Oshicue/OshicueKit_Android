package com.oshicue.kit.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrandWordmark(modifier: Modifier = Modifier, compact: Boolean = false) {
    val kit = LocalKit.current
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        Text(
            "OSHI",
            fontFamily = Unbounded,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 15.sp else 18.sp,
            color = kit.ink,
            letterSpacing = 0.25.sp
        )
        Text(
            "CUE",
            fontFamily = Unbounded,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 15.sp else 18.sp,
            color = kit.accent,
            letterSpacing = 0.25.sp
        )
        Spacer(Modifier.width(5.dp))
        Text(
            "KIT",
            fontFamily = Unbounded,
            fontWeight = FontWeight.Black,
            fontSize = if (compact) 8.sp else 10.sp,
            color = kit.ink
        )
    }
}

@Composable
fun CircleIconButton(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    onClick: () -> Unit
) {
    val kit = LocalKit.current
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(kit.surface)
            .border(1.dp, kit.line, CircleShape)
    ) {
        Icon(icon, contentDescription = label, tint = kit.ink, modifier = Modifier.size(19.dp))
    }
}

@Composable
fun AccentCircleButton(
    icon: ImageVector = Icons.Filled.Add,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val kit = LocalKit.current
    IconButton(
        onClick = onClick,
        modifier = modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(kit.accentFill)
    ) {
        Icon(icon, contentDescription = label, tint = kit.onAccent, modifier = Modifier.size(21.dp))
    }
}

@Composable
fun KitPrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val kit = LocalKit.current
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(48.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = kit.accentFill,
            contentColor = kit.onAccent,
            disabledContainerColor = kit.accentFill.copy(alpha = 0.35f),
            disabledContentColor = kit.onAccent.copy(alpha = 0.8f)
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
fun KitSecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val kit = LocalKit.current
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().height(46.dp),
        enabled = enabled,
        border = BorderStroke(1.dp, kit.line),
        shape = RoundedCornerShape(13.dp)
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = kit.accentText, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = kit.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    corner: Dp = 17.dp,
    content: @Composable () -> Unit
) {
    val kit = LocalKit.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(corner),
        color = kit.surface
    ) {
        content()
    }
}

@Composable
fun SegmentedControl(
    options: List<Pair<String, String>>,
    selected: String,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit
) {
    val kit = LocalKit.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(kit.surfaceMuted)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEach { (value, label) ->
            val isSelected = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) kit.surface else Color.Transparent)
                    .clickable { onSelect(value) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) kit.accentText else kit.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun FilterChip(text: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val kit = LocalKit.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (selected) kit.surface else Color.Transparent)
            .border(1.dp, if (selected) kit.line else Color.Transparent, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) {
        Text(
            text,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (selected) kit.ink else kit.muted
        )
    }
}

@Composable
fun Pill(text: String, modifier: Modifier = Modifier, background: Color? = null, color: Color? = null) {
    val kit = LocalKit.current
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(background ?: kit.background)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, fontSize = 9.5.sp, fontWeight = FontWeight.Medium, color = color ?: kit.ink)
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val kit = LocalKit.current
    Text(
        text.uppercase(),
        modifier = modifier,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.1.sp,
        color = kit.muted
    )
}

@Composable
fun DividerLine(modifier: Modifier = Modifier) {
    val kit = LocalKit.current
    Box(modifier.fillMaxWidth().height(1.dp).background(kit.line))
}

@Composable
fun EmptyState(
    title: String,
    hint: String,
    modifier: Modifier = Modifier,
    symbol: ImageVector? = null,
    action: (@Composable () -> Unit)? = null
) {
    val kit = LocalKit.current
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = 34.dp, horizontal = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (symbol != null) {
            Icon(symbol, contentDescription = null, tint = kit.accentText, modifier = Modifier.size(36.dp))
            Spacer(Modifier.height(14.dp))
        }
        Text(title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = kit.ink, textAlign = TextAlign.Center)
        Spacer(Modifier.height(7.dp))
        Text(hint, fontSize = 12.5.sp, color = kit.muted, textAlign = TextAlign.Center)
        if (action != null) {
            Spacer(Modifier.height(18.dp))
            action()
        }
    }
}

@Composable
fun Checkmark(selected: Boolean, modifier: Modifier = Modifier, size: Dp = 20.dp) {
    val kit = LocalKit.current
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.08f
        if (selected) {
            drawCircle(color = kit.accentText, radius = this.size.minDimension / 2f)
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(this@Canvas.size.width * 0.28f, this@Canvas.size.height * 0.52f)
                lineTo(this@Canvas.size.width * 0.44f, this@Canvas.size.height * 0.68f)
                lineTo(this@Canvas.size.width * 0.74f, this@Canvas.size.height * 0.33f)
            }
            drawPath(path, color = Color.White, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * 1.6f))
        } else {
            drawCircle(color = kit.muted, radius = this.size.minDimension / 2f - stroke, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke * 1.4f))
        }
    }
}

@Composable
fun KitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    keyboardType: androidx.compose.ui.text.input.KeyboardType = androidx.compose.ui.text.input.KeyboardType.Text,
    textStyle: TextStyle = TextStyle(fontSize = 14.sp)
) {
    val kit = LocalKit.current
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(kit.surfaceSubtle)
            .padding(horizontal = 13.dp, vertical = 12.dp),
        textStyle = textStyle.copy(color = kit.ink),
        singleLine = singleLine,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, color = kit.muted, fontSize = textStyle.fontSize)
                inner()
            }
        }
    )
}

@Composable
fun VerticalSpacer(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun HorizontalSpacer(width: Dp) = Spacer(Modifier.width(width))

@Composable
fun ListRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    val kit = LocalKit.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(11.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = kit.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) {
                Text(subtitle, fontSize = 11.5.sp, color = kit.muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        title,
        modifier = modifier.fillMaxWidth().padding(top = 18.dp, bottom = 8.dp),
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = LocalKit.current.ink
    )
}

@Composable
fun FormCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val kit = LocalKit.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(kit.surface)
            .padding(16.dp)
    ) { content() }
}

@Composable
fun InlineError(text: String, modifier: Modifier = Modifier) {
    val kit = LocalKit.current
    Text(
        text,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x14D93025))
            .padding(12.dp),
        fontSize = 11.5.sp,
        color = Color(0xFFD93025)
    )
}

@Composable
fun IconGlyph(icon: ImageVector, tint: Color, size: Dp = 16.dp) {
    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size))
}

@Composable
fun SmallCaps(text: String, color: Color, size: TextUnit = 9.sp) {
    Text(text, fontSize = size, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp, color = color)
}

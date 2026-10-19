package com.oshicue.kit.ui

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerMember
import com.oshicue.kit.core.LedgerProduct
import com.oshicue.kit.core.LedgerProductSummary
import com.oshicue.kit.core.LedgerCurrency

/** Decodes the stored base64 JPEG into a small avatar, or draws the member color. */
@Composable
fun LedgerAvatar(
    member: LedgerMember?,
    color: String,
    size: Dp = 44.dp,
    modifier: Modifier = Modifier
) {
    val avatarColor = hexToColor(color)
    val bitmap = remember(member?.avatarJpegBase64) {
        member?.avatarJpegBase64?.let { encoded ->
            try {
                val bytes = Base64.decode(encoded, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        }
    }
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(avatarColor.copy(alpha = 0.12f))
            .border(2.5.dp, avatarColor, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = avatarColor,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}

/** Product name shown to the user; standard IDs keep their localized labels. */
@Composable
fun productDisplayName(id: String, snapshot: String): String {
    val standard = LedgerDocument.defaultProducts.firstOrNull { it.id == id }
    if (standard == null || standard.name != snapshot) return snapshot
    return when (id) {
        "cheki" -> L10n.text("ledger.product.polaroid")
        "wide-cheki" -> L10n.text("ledger.product.wide")
        "phone-photo" -> L10n.text("ledger.product.mobile")
        else -> snapshot
    }
}

@Composable
fun LedgerProduct.displayName(): String = productDisplayName(id, name)

@Composable
fun FlowProductSummary(rows: List<LedgerProductSummary>, modifier: Modifier = Modifier) {
    val kit = LocalKit.current
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        rows.forEach { row ->
            Box(
                Modifier
                    .clip(CircleShape)
                    .background(kit.background)
                    .padding(horizontal = 7.dp, vertical = 4.dp)
            ) {
                Text(
                    productDisplayName(row.id, row.name) + " × " + row.quantity,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Medium,
                    color = kit.ink
                )
            }
        }
    }
}

/** Money joined by currency, sorted by code. */
@Composable
fun moneyText(amounts: Map<String, Long>): String {
    val locale = L10n.locale
    return amounts.keys.sorted().joinToString(" · ") { key ->
        key + " " + (LedgerCurrency.from(key)?.format(amounts[key] ?: 0L, locale) ?: "")
    }
}

@Composable
fun LedgerTopBar(
    onAdd: () -> Unit,
    onManagement: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrandWordmark()
        Spacer(Modifier.weight(1f))
        CircleIconButton(Icons.Filled.Tune, L10n.text("ledger.management"), onClick = onManagement)
        Spacer(Modifier.width(10.dp))
        AccentCircleButton(Icons.Filled.Add, L10n.text("ledger.add"), onClick = onAdd)
    }
}

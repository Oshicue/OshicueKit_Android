package com.oshicue.kit.ui.ledger

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import android.graphics.BitmapFactory
import android.util.Base64
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerError
import com.oshicue.kit.core.LedgerMember
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.theme.hexColor
import java.time.LocalDate

object LedgerPresentation {
    fun money(amounts: Map<String, Long>): String = amounts.keys.sorted().joinToString(" · ") { key ->
        key + " " + (LedgerCurrency.fromCode(key)?.format(amounts[key] ?: 0L, L10n.currentLocale()) ?: "")
    }

    fun productName(id: String, snapshot: String): String {
        val standard = LedgerDocument.defaultProducts.firstOrNull { it.id == id } ?: return snapshot
        if (standard.name != snapshot) return snapshot
        return when (id) {
            "cheki" -> L10n.text("ledger.product.polaroid")
            "wide-cheki" -> L10n.text("ledger.product.wide")
            "phone-photo" -> L10n.text("ledger.product.mobile")
            else -> snapshot
        }
    }

    fun productDisplayName(product: com.oshicue.kit.core.LedgerProduct): String =
        productName(id = product.id, snapshot = product.name)

    fun error(value: Exception): String {
        if (value is LedgerError) return L10n.text(value.key) + "\n" + value.path
        return value.message ?: value.toString()
    }

    fun date(value: String): LocalDate = LedgerDates.parse(value) ?: LocalDate.now()

    val calendar: java.time.format.DateTimeFormatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
}

@Composable
fun LedgerAvatar(member: LedgerMember?, color: String, size: Dp) {
    val bitmap = remember(member?.avatarJPEGBase64) { decodeAvatar(member?.avatarJPEGBase64) }
    val tint = hexColor(color)
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .border(2.5.dp, tint, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Icon(
                Icons.Filled.Person,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(size * 0.42f)
            )
        }
    }
}

fun decodeAvatar(base64: String?): androidx.compose.ui.graphics.ImageBitmap? {
    if (base64.isNullOrEmpty()) return null
    return try {
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
    } catch (_: Exception) {
        null
    }
}

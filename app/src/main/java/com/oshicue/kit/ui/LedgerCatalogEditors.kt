package com.oshicue.kit.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
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
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerGroup
import com.oshicue.kit.core.LedgerMember
import com.oshicue.kit.core.LedgerMembership
import com.oshicue.kit.core.LedgerPrice
import com.oshicue.kit.core.LedgerProduct
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.membersForSelection
import com.oshicue.kit.core.newIdentifier
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Base64

private data class PriceDraft(
    val productId: String,
    val currency: String,
    val text: String
)

@Composable
fun LedgerMemberEditor(
    store: LedgerStore,
    membership: LedgerMembership?,
    onClose: () -> Unit,
    onSaved: (String) -> Unit = {}
) {
    val kit = LocalKit.current
    val context = LocalContext.current
    val document = store.document
    val existingMember = document.activeMembers.firstOrNull { it.id == membership?.memberId }

    var name by remember { mutableStateOf(existingMember?.name ?: "") }
    var reuseMember by remember { mutableStateOf(false) }
    var existingMemberId by remember { mutableStateOf(existingMember?.id ?: "") }
    var groupId by remember { mutableStateOf(membership?.groupId ?: document.activeGroups.firstOrNull()?.id ?: "") }
    var colorHex by remember { mutableStateOf(membership?.color ?: "#F23C6B") }
    var colorText by remember { mutableStateOf(colorHex.removePrefix("#")) }
    var avatar by remember { mutableStateOf(existingMember?.avatarJpegBase64) }
    var isFavorite by remember { mutableStateOf(existingMember?.isFavorited ?: false) }
    var loadingPhoto by remember { mutableStateOf(false) }
    var prices by remember {
        val rows = (membership?.prices ?: emptyList()).map {
            PriceDraft(it.productId, it.currency, (LedgerCurrency.from(it.currency) ?: LedgerCurrency.JPY).priceText(it.unitPriceMinor))
        }.toMutableList()
        document.products.forEach { product ->
            if (rows.none { it.productId == product.id }) {
                rows.add(PriceDraft(product.id, "JPY", ""))
            }
        }
        mutableStateOf(rows.toList())
    }
    var showGroupEditor by remember { mutableStateOf(false) }
    var showMemberSelector by remember { mutableStateOf(false) }
    var showGroupSelector by remember { mutableStateOf(false) }
    var showProductEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        loadingPhoto = true
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                error = L10n.text("ledger.member.photoError")
            } else if (bytes.size > 10 * 1024 * 1024) {
                error = L10n.text("ledger.member.photoTooLarge")
            } else {
                val encoded = encodeAvatarJpeg(bytes)
                if (encoded == null) {
                    error = L10n.text("ledger.member.photoError")
                } else {
                    avatar = encoded
                    error = null
                }
            }
        } catch (_: Exception) {
            error = L10n.text("ledger.member.photoError")
        }
        loadingPhoto = false
    }

    val canSave = name.isNotBlank() && groupId.isNotEmpty() && (!reuseMember || existingMemberId.isNotEmpty())

    fun save() {
        val savedPrices = mutableListOf<LedgerPrice>()
        for (row in prices) {
            if (row.text.isBlank()) continue
            val amount = LedgerCurrency.from(row.currency)?.parsePrice(row.text.replace(',', '.'))
            if (amount == null) {
                error = L10n.text("ledger.invalidPrice")
                return
            }
            savedPrices.add(LedgerPrice(row.productId, row.currency, amount))
        }
        val memberId = membership?.memberId ?: if (reuseMember) existingMemberId else newIdentifier()
        val member = LedgerMember(
            id = memberId,
            name = name.trim(),
            avatarJpegBase64 = avatar,
            isFavorite = isFavorite
        )
        val affiliation = LedgerMembership(
            id = membership?.id ?: newIdentifier(),
            memberId = memberId,
            groupId = groupId,
            color = "#" + colorText.uppercase(),
            prices = savedPrices
        )
        if (!store.saveMember(member, affiliation)) {
            error = store.storageError
            return
        }
        onSaved(affiliation.id)
        onClose()
    }

    ModalScreen(
        title = L10n.text(if (membership == null) "ledger.member.add" else "ledger.member.edit"),
        onClose = onClose,
        actions = {
            KitTextAction(L10n.text("ledger.save"), enabled = canSave && !loadingPhoto) { save() }
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            SectionLabel(L10n.text("ledger.member.identity"))
            Spacer(Modifier.height(8.dp))
            if (membership == null && document.activeMembers.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(L10n.text("ledger.member.reuse"), fontSize = 13.5.sp, color = kit.ink)
                    Spacer(Modifier.weight(1f))
                    Switch(checked = reuseMember, onCheckedChange = { value ->
                        reuseMember = value
                        if (value) {
                            existingMemberId = document.membersForSelection.firstOrNull()?.id ?: ""
                            document.activeMembers.firstOrNull { it.id == existingMemberId }?.let { member ->
                                name = member.name
                                avatar = member.avatarJpegBase64
                                isFavorite = member.isFavorited
                            }
                        } else {
                            existingMemberId = ""
                            name = ""
                            avatar = null
                            isFavorite = false
                        }
                    })
                }
                if (reuseMember) {
                    SelectorRow(
                        label = L10n.text("ledger.member.existing"),
                        value = document.activeMembers.firstOrNull { it.id == existingMemberId }?.name
                            ?: L10n.text("ledger.chooseMember"),
                        onClick = { showMemberSelector = true }
                    )
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        document.membersForSelection.forEach { member ->
                            SelectableChip(member.name, existingMemberId == member.id) {
                                existingMemberId = member.id
                                name = member.name
                                avatar = member.avatarJpegBase64
                                isFavorite = member.isFavorited
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            FieldLabel(L10n.text("ledger.member.name"))
            KitTextField(name, { name = it }, L10n.text("ledger.member.name"))
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LedgerAvatar(
                    member = LedgerMember(id = "preview", name = name, avatarJpegBase64 = avatar),
                    color = "#" + colorText,
                    size = 70.dp
                )
                Spacer(Modifier.width(16.dp))
                Column {
                    KitSecondaryButton(L10n.text("ledger.member.photo"), icon = Icons.Filled.PhotoLibrary) {
                        photoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    }
                    if (avatar != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            L10n.text("ledger.member.removePhoto"),
                            fontSize = 11.5.sp,
                            color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                            modifier = Modifier.clickable { avatar = null }
                        )
                    }
                    if (loadingPhoto) {
                        Spacer(Modifier.height(4.dp))
                        Text("…", color = kit.muted)
                    }
                }
            }
            Text(L10n.text("ledger.member.identityHint"), fontSize = 11.sp, color = kit.muted)

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                    null,
                    tint = if (isFavorite) kit.accentText else kit.muted,
                    modifier = Modifier.size(17.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(L10n.text("ledger.member.favorite"), fontSize = 13.5.sp, color = kit.ink)
                Spacer(Modifier.weight(1f))
                Switch(checked = isFavorite, onCheckedChange = { isFavorite = it })
            }
            Text(L10n.text("ledger.member.favoriteHint"), fontSize = 11.sp, color = kit.muted)

            Spacer(Modifier.height(16.dp))
            SectionLabel(L10n.text("ledger.member.affiliation"))
            Spacer(Modifier.height(8.dp))
            SelectorRow(
                label = L10n.text("ledger.group"),
                value = document.groups.firstOrNull { it.id == groupId }?.name ?: L10n.text("ledger.chooseGroup"),
                onClick = { showGroupSelector = true }
            )
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                document.activeGroups.forEach { group ->
                    SelectableChip(group.name, groupId == group.id) { groupId = group.id }
                }
                SelectableChip("+ " + L10n.text("ledger.group.add"), false) { showGroupEditor = true }
            }
            Spacer(Modifier.height(10.dp))
            FieldLabel(L10n.text("ledger.groupColor"))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(hexToColor("#" + colorText))
                        .border(1.dp, kit.line, CircleShape)
                )
                Spacer(Modifier.width(10.dp))
                KitTextField(
                    colorText,
                    { value ->
                        val cleaned = value.filter { it.isLetterOrDigit() }.take(6)
                        colorText = cleaned
                    },
                    "F23C6B",
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                Modifier.fillMaxWidth().padding(top = 8.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("F23C6B", "9B8CF5", "66C7A5", "67B9EF", "F4A174", "C58EC4", "EECF67").forEach { hex ->
                    Box(
                        Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(hexToColor("#$hex"))
                            .clickable { colorText = hex }
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionLabel(L10n.text("ledger.member.prices"))
            Spacer(Modifier.height(8.dp))
            prices.forEach { row ->
                val productName = document.products.firstOrNull { it.id == row.productId }
                    ?.let { productDisplayName(it.id, it.name) } ?: row.productId
                // Name above a currency menu and price field, like the iOS price section.
                Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                    Text(productName, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CurrencyMenu(row.currency) { code ->
                            prices = prices.map { if (it.productId == row.productId) it.copy(currency = code) else it }
                        }
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(kit.surface)
                                .padding(horizontal = 12.dp, vertical = 11.dp)
                        ) {
                            androidx.compose.foundation.text.BasicTextField(
                                value = row.text,
                                onValueChange = { value ->
                                    val cleaned = value.filter { character -> character.isDigit() || character == '.' || character == ',' }
                                    prices = prices.map { if (it.productId == row.productId) it.copy(text = cleaned) else it }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 14.sp, color = kit.ink),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                ),
                                decorationBox = { inner ->
                                    if (row.text.isEmpty()) {
                                        Text(L10n.text("ledger.unitPrice"), fontSize = 14.sp, color = kit.muted)
                                    }
                                    inner()
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            KitSecondaryButton(L10n.text("ledger.product.add"), icon = Icons.Filled.Add) { showProductEditor = true }
            Text(L10n.text("ledger.member.pricesHint"), fontSize = 11.sp, color = kit.muted)

            if (membership != null) {
                Spacer(Modifier.height(18.dp))
                KitSecondaryButton(L10n.text("ledger.member.delete"), icon = Icons.Filled.Delete) { confirmDelete = true }
            }
            error?.let {
                Spacer(Modifier.height(12.dp))
                InlineError(it)
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showMemberSelector) {
        OptionSelector(
            title = L10n.text("ledger.member.existing"),
            options = document.membersForSelection.map { it.id to it.name },
            selected = existingMemberId,
            onDismiss = { showMemberSelector = false },
            onSelect = { id ->
                existingMemberId = id
                document.activeMembers.firstOrNull { it.id == id }?.let { member ->
                    name = member.name
                    avatar = member.avatarJpegBase64
                    isFavorite = member.isFavorited
                }
            }
        )
    }
    if (showGroupSelector) {
        OptionSelector(
            title = L10n.text("ledger.group"),
            options = document.activeGroups.map { it.id to it.name },
            selected = groupId,
            onDismiss = { showGroupSelector = false },
            onSelect = { groupId = it }
        )
    }
    if (showGroupEditor) {
        LedgerGroupEditor(store = store, group = null, onClose = { showGroupEditor = false }) { id ->
            groupId = id
        }
    }
    if (showProductEditor) {
        LedgerProductEditor(store = store, product = null, onClose = { showProductEditor = false }) { id ->
            if (prices.none { it.productId == id }) {
                prices = prices + PriceDraft(id, "JPY", "")
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = L10n.text("ledger.member.deleteTitle"),
            message = L10n.text("ledger.member.deleteMessage"),
            confirmLabel = L10n.text("ledger.member.delete"),
            onConfirm = {
                val current = membership
                if (current != null) {
                    if (store.deleteMember(current.memberId)) onClose() else error = store.storageError
                }
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

/** A compact currency picker, the counterpart of the iOS menu-style Picker. */
@Composable
private fun CurrencyMenu(selected: String, onSelect: (String) -> Unit) {
    val kit = LocalKit.current
    var expanded by remember { mutableStateOf(false) }
    val label = L10n.text("ledger.currency")
    Box {
        Row(
            Modifier
                .width(115.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(kit.surface)
                .clickable { expanded = true }
                .semantics(mergeDescendants = true) { contentDescription = "$label · $selected" }
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(selected, modifier = Modifier.weight(1f), fontSize = 14.sp, fontWeight = FontWeight.Medium, color = kit.accentText)
            Icon(Icons.Filled.ArrowDropDown, contentDescription = null, tint = kit.muted)
        }
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LedgerCurrency.entries.forEach { value ->
                androidx.compose.material3.DropdownMenuItem(
                    text = { Text(value.code) },
                    onClick = {
                        expanded = false
                        onSelect(value.code)
                    }
                )
            }
        }
    }
}

/** Downscales the selected photo to at most 512 px and encodes it as JPEG. */
fun encodeAvatarJpeg(bytes: ByteArray): String? {
    return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 512) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
        val scale = minOf(1f, 512f / maxOf(decoded.width, decoded.height).toFloat())
        val target = if (scale < 1f) {
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else decoded
        val output = ByteArrayOutputStream()
        target.compress(Bitmap.CompressFormat.JPEG, 80, output)
        Base64.getEncoder().encodeToString(output.toByteArray())
    } catch (_: Exception) {
        null
    }
}

@Composable
fun LedgerGroupEditor(
    store: LedgerStore,
    group: LedgerGroup?,
    onClose: () -> Unit,
    onSaved: (String) -> Unit = {}
) {
    var name by remember { mutableStateOf(group?.name ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    ModalScreen(
        title = L10n.text(if (group == null) "ledger.group.add" else "ledger.group.edit"),
        onClose = onClose,
        actions = {
            KitTextAction(L10n.text("ledger.save"), enabled = name.isNotBlank()) {
                val value = LedgerGroup(group?.id ?: newIdentifier(), name.trim())
                if (store.upsertGroup(value)) {
                    onSaved(value.id)
                    onClose()
                } else {
                    error = store.storageError
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            FieldLabel(L10n.text("ledger.group.name"))
            KitTextField(name, { name = it }, L10n.text("ledger.group.name"))
            error?.let {
                Spacer(Modifier.height(10.dp))
                InlineError(it)
            }
            if (group != null) {
                Spacer(Modifier.height(18.dp))
                KitSecondaryButton(L10n.text("ledger.group.delete"), icon = Icons.Filled.Delete) { confirmDelete = true }
            }
        }
    }
    if (confirmDelete && group != null) {
        ConfirmDialog(
            title = L10n.text("ledger.group.deleteTitle"),
            message = L10n.text("ledger.group.deleteMessage"),
            confirmLabel = L10n.text("ledger.group.delete"),
            onConfirm = {
                if (store.deleteGroup(group.id)) onClose() else error = store.storageError
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

@Composable
fun LedgerProductEditor(
    store: LedgerStore,
    product: LedgerProduct?,
    onClose: () -> Unit,
    onSaved: (String) -> Unit = {}
) {
    val existingName = product?.let { productDisplayName(it.id, it.name) } ?: ""
    var name by remember { mutableStateOf(existingName) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalScreen(
        title = L10n.text(if (product == null) "ledger.product.add" else "ledger.product.edit"),
        onClose = onClose,
        actions = {
            KitTextAction(L10n.text("ledger.save"), enabled = name.isNotBlank()) {
                val edited = name.trim()
                val savedName = if (product != null && edited == existingName) product.name else edited
                val value = LedgerProduct(product?.id ?: newIdentifier(), savedName)
                if (store.upsertProduct(value)) {
                    onSaved(value.id)
                    onClose()
                } else {
                    error = store.storageError
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            FieldLabel(L10n.text("ledger.product.name"))
            KitTextField(name, { name = it }, L10n.text("ledger.product.name"))
            error?.let {
                Spacer(Modifier.height(10.dp))
                InlineError(it)
            }
        }
    }
}

@Composable
fun LedgerEventEditor(
    store: LedgerStore,
    event: LedgerEvent?,
    onClose: () -> Unit,
    onSaved: (String) -> Unit = {}
) {
    var name by remember { mutableStateOf(event?.name ?: "") }
    var hasDate by remember { mutableStateOf(event?.date != null) }
    var date by remember { mutableStateOf(LedgerDates.parse(event?.date ?: LedgerDates.today()) ?: LocalDate.now()) }
    var city by remember { mutableStateOf(event?.city ?: "") }
    var venue by remember { mutableStateOf(event?.venue ?: "") }
    var address by remember { mutableStateOf(event?.address ?: "") }
    var showDate by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    fun optional(value: String): String? = value.trim().ifEmpty { null }

    ModalScreen(
        title = L10n.text(if (event == null) "ledger.event.add" else "ledger.event.edit"),
        onClose = onClose,
        actions = {
            KitTextAction(L10n.text("ledger.save"), enabled = name.isNotBlank()) {
                val value = LedgerEvent(
                    id = event?.id ?: newIdentifier(),
                    name = name.trim(),
                    date = if (hasDate) LedgerDates.string(date) else null,
                    city = optional(city),
                    venue = optional(venue),
                    address = optional(address)
                )
                if (store.upsertEvent(value)) {
                    onSaved(value.id)
                    onClose()
                } else {
                    error = store.storageError
                }
            }
        }
    ) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
            FieldLabel(L10n.text("ledger.event.name"))
            KitTextField(name, { name = it }, L10n.text("ledger.event.name"))
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.text("ledger.event.hasDate"), fontSize = 13.5.sp, color = LocalKit.current.ink)
                Spacer(Modifier.weight(1f))
                Switch(checked = hasDate, onCheckedChange = { hasDate = it })
            }
            if (hasDate) {
                SelectorRow(
                    label = L10n.text("ledger.date"),
                    value = LedgerDates.string(date),
                    onClick = { showDate = true }
                )
            }
            Spacer(Modifier.height(12.dp))
            SectionLabel(L10n.text("ledger.optional"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.city"))
            KitTextField(city, { city = it }, L10n.text("ledger.city"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.venue"))
            KitTextField(venue, { venue = it }, L10n.text("ledger.venue"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.address"))
            KitTextField(address, { address = it }, L10n.text("ledger.address"))
            error?.let {
                Spacer(Modifier.height(10.dp))
                InlineError(it)
            }
        }
    }
    if (showDate) {
        DatePickerSheet(date, { showDate = false }) { date = it; showDate = false }
    }
}

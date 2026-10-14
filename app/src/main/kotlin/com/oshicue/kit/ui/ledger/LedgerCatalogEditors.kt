package com.oshicue.kit.ui.ledger

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerGroup
import com.oshicue.kit.core.LedgerMember
import com.oshicue.kit.core.LedgerMembership
import com.oshicue.kit.core.LedgerPrice
import com.oshicue.kit.core.LedgerProduct
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.membersForSelection
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.common.KitField
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.theme.hexColor
import com.oshicue.kit.ui.theme.kitPalette
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.UUID

private val memberColorChoices = listOf(
    "#F28AAF", "#9B8CF5", "#66C7A5", "#67B9EF", "#F4A174", "#EECF67",
    "#C58EC4", "#78C9EB", "#E789A5", "#94C988", "#F23C6B", "#20212B"
)

private data class PriceDraft(val productID: String, val currency: String, val text: String)

@Composable
fun LedgerMemberEditor(
    store: LedgerStore,
    membership: LedgerMembership?,
    onSaved: (String) -> Unit,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    val context = LocalContext.current
    val member = store.document.activeMembers.firstOrNull { it.id == membership?.memberID }
    var name by remember { mutableStateOf(member?.name ?: "") }
    var existingMemberID by remember { mutableStateOf(member?.id ?: "") }
    var reuseMember by remember { mutableStateOf(false) }
    var groupID by remember {
        mutableStateOf(membership?.groupID ?: store.document.activeGroups.firstOrNull()?.id ?: "")
    }
    var color by remember { mutableStateOf(membership?.color ?: "#F23C6B") }
    var avatar by remember { mutableStateOf(member?.avatarJPEGBase64) }
    var isFavorite by remember { mutableStateOf(member?.isFavorited ?: false) }
    var prices by remember {
        mutableStateOf(
            buildList<PriceDraft> {
                for (price in membership?.prices ?: emptyList()) {
                    val currency = LedgerCurrency.fromCode(price.currency) ?: LedgerCurrency.JPY
                    add(PriceDraft(price.productID, currency.code, currency.priceText(price.unitPriceMinor)))
                }
                for (product in store.document.products) {
                    if (none { it.productID == product.id }) add(PriceDraft(product.id, "JPY", ""))
                }
            }
        )
    }
    var error by remember { mutableStateOf<String?>(null) }
    var addGroup by remember { mutableStateOf(false) }
    var addProduct by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            if (bytes == null) {
                error = L10n.text("ledger.member.photoError")
                return@rememberLauncherForActivityResult
            }
            if (bytes.size > 10 * 1024 * 1024) {
                error = L10n.text("ledger.member.photoTooLarge")
                return@rememberLauncherForActivityResult
            }
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
            var sample = 1
            while (options.outWidth / sample > 512 || options.outHeight / sample > 512) sample *= 2
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            if (decoded == null) {
                error = L10n.text("ledger.member.photoError")
                return@rememberLauncherForActivityResult
            }
            val scaled = if (decoded.width > 512 || decoded.height > 512) {
                val ratio = minOf(512f / decoded.width, 512f / decoded.height)
                Bitmap.createScaledBitmap(decoded, (decoded.width * ratio).toInt(), (decoded.height * ratio).toInt(), true)
            } else decoded
            val output = ByteArrayOutputStream()
            scaled.compress(Bitmap.CompressFormat.JPEG, 80, output)
            avatar = Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } catch (_: Exception) {
            error = L10n.text("ledger.member.photoError")
        }
    }

    val canSave = name.trim().isNotEmpty() && groupID.isNotEmpty() && (!reuseMember || existingMemberID.isNotEmpty())

    KitSheet(L10n.text(if (membership == null) "ledger.member.add" else "ledger.member.edit"), onClose) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("ledger.member.identity"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (membership == null && store.document.activeMembers.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(L10n.text("ledger.member.reuse"), color = palette.ink, fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Switch(
                                checked = reuseMember,
                                onCheckedChange = { value ->
                                    reuseMember = value
                                    if (value) {
                                        existingMemberID = store.document.membersForSelection.firstOrNull()?.id ?: ""
                                        val selected = store.document.activeMembers.firstOrNull { it.id == existingMemberID }
                                        if (selected != null) {
                                            name = selected.name
                                            avatar = selected.avatarJPEGBase64
                                            isFavorite = selected.isFavorited
                                        }
                                    } else {
                                        existingMemberID = ""
                                        name = ""
                                        avatar = null
                                        isFavorite = false
                                    }
                                },
                                colors = SwitchDefaults.colors(checkedTrackColor = palette.accentFill)
                            )
                        }
                        if (reuseMember) {
                            KitDropdownRow(
                                label = L10n.text("ledger.member.existing"),
                                value = store.document.membersForSelection.firstOrNull { it.id == existingMemberID }?.name ?: "",
                                options = store.document.membersForSelection.map { it.id to (if (it.isFavorited) "★ " else "") + it.name },
                                onSelect = { id ->
                                    existingMemberID = id
                                    val selected = store.document.activeMembers.firstOrNull { it.id == id }
                                    if (selected != null) {
                                        name = selected.name
                                        avatar = selected.avatarJPEGBase64
                                        isFavorite = selected.isFavorited
                                    }
                                }
                            )
                        }
                    }
                    KitField(L10n.text("ledger.member.name"), name) { name = it }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        LedgerAvatar(LedgerMember(id = "preview", name = name, avatarJPEGBase64 = avatar), color, 70.dp)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.clickable {
                                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.AddPhotoAlternate, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.width(7.dp))
                                Text(L10n.text("ledger.member.photo"), color = palette.accentText, fontSize = 14.sp)
                            }
                            if (avatar != null) {
                                Text(
                                    L10n.text("ledger.member.removePhoto"),
                                    color = palette.warningText,
                                    fontSize = 12.sp,
                                    modifier = Modifier.clickable { avatar = null }
                                )
                            }
                        }
                    }
                    Text(L10n.text("ledger.member.identityHint"), color = palette.muted, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface, RoundedCornerShape(14.dp))
                        .clickable { isFavorite = !isFavorite }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Star else Icons.Filled.StarBorder,
                        contentDescription = null,
                        tint = palette.accentText,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(L10n.text("ledger.member.favorite"), color = palette.ink, fontSize = 15.sp)
                        Text(L10n.text("ledger.member.favoriteHint"), color = palette.muted, fontSize = 11.sp)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("ledger.member.affiliation"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    KitDropdownRow(
                        label = L10n.text("ledger.group"),
                        value = store.document.activeGroups.firstOrNull { it.id == groupID }?.name ?: "",
                        options = store.document.activeGroups.map { it.id to it.name },
                        onSelect = { groupID = it },
                        emptyLabel = L10n.text("ledger.chooseGroup")
                    )
                    Text(
                        L10n.text("ledger.group.add"),
                        color = palette.accentText,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 6.dp).clickable { addGroup = true }
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(L10n.text("ledger.groupColor"), color = palette.ink, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (choice in memberColorChoices) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(hexColor(choice), CircleShape)
                                    .border(if (color == choice) 2.5.dp else 0.dp, if (color == choice) palette.ink else Color.Transparent, CircleShape)
                                    .clickable { color = choice }
                            )
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("ledger.member.prices"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    for (row in prices) {
                        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                store.document.products.firstOrNull { it.id == row.productID }?.let { LedgerPresentation.productDisplayName(it) }
                                    ?: row.productID,
                                color = palette.ink,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                KitDropdownRow(
                                    label = "",
                                    value = row.currency,
                                    options = LedgerCurrency.entries.map { it.code to it.code },
                                    onSelect = { code ->
                                        prices = prices.map {
                                            if (it.productID == row.productID) it.copy(currency = code, text = "") else it
                                        }
                                    },
                                    modifier = Modifier.width(120.dp)
                                )
                                TextField(
                                    value = row.text,
                                    onValueChange = { value ->
                                        prices = prices.map { if (it.productID == row.productID) it.copy(text = value) else it }
                                    },
                                    singleLine = true,
                                    placeholder = { Text(L10n.text("ledger.unitPrice"), color = palette.muted, fontSize = 13.sp) },
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                                    ),
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = palette.line,
                                        unfocusedIndicatorColor = palette.line
                                    ),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = palette.ink, fontSize = 15.sp),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                    Text(
                        L10n.text("ledger.product.add"),
                        color = palette.accentText,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(vertical = 6.dp).clickable { addProduct = true }
                    )
                    Text(L10n.text("ledger.member.pricesHint"), color = palette.muted, fontSize = 11.sp)
                }

                error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }

                Button(
                    onClick = {
                        val savedPrices = mutableListOf<LedgerPrice>()
                        for (row in prices) {
                            if (row.text.trim().isEmpty()) continue
                            val currency = LedgerCurrency.fromCode(row.currency) ?: LedgerCurrency.JPY
                            val amount = currency.parsePrice(row.text.replace(',', '.'))
                            if (amount == null) {
                                error = L10n.text("ledger.invalidPrice")
                                return@Button
                            }
                            savedPrices.add(LedgerPrice(row.productID, currency.code, amount))
                        }
                        val memberID = membership?.memberID
                            ?: if (reuseMember) existingMemberID else UUID.randomUUID().toString().lowercase()
                        val value = LedgerMember(id = memberID, name = name.trim(), avatarJPEGBase64 = avatar, isFavorite = isFavorite)
                        val affiliation = LedgerMembership(
                            id = membership?.id ?: UUID.randomUUID().toString().lowercase(),
                            memberID = memberID,
                            groupID = groupID,
                            color = color,
                            prices = savedPrices
                        )
                        if (!store.saveMember(value, affiliation)) {
                            error = store.storageError
                            return@Button
                        }
                        onSaved(affiliation.id)
                        onClose()
                    },
                    enabled = canSave,
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(L10n.text("ledger.save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                if (membership != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("ledger.member.delete"), color = palette.warningText, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (addGroup) {
        LedgerGroupEditor(store, null, { groupID = it }, { addGroup = false })
    }
    if (addProduct) {
        LedgerProductEditor(
            store = store,
            product = null,
            onSaved = { id ->
                if (prices.none { it.productID == id }) prices = prices + PriceDraft(id, "JPY", "")
            },
            onClose = { addProduct = false }
        )
    }
    if (confirmDelete && membership != null) {
        KitAlert(
            title = L10n.text("ledger.member.deleteTitle"),
            message = L10n.text("ledger.member.deleteMessage"),
            confirmLabel = L10n.text("ledger.member.delete"),
            onConfirm = {
                confirmDelete = false
                if (store.deleteMember(membership.memberID)) onClose() else error = store.storageError
            },
            onDismiss = { confirmDelete = false },
            destructive = true
        )
    }
}

@Composable
fun LedgerGroupEditor(store: LedgerStore, group: LedgerGroup?, onSaved: (String) -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    var name by remember { mutableStateOf(group?.name ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    KitSheet(L10n.text(if (group == null) "ledger.group.add" else "ledger.group.edit"), onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    KitField(L10n.text("ledger.group.name"), name) { name = it }
                }
                error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
                Button(
                    onClick = {
                        val value = LedgerGroup(id = group?.id ?: UUID.randomUUID().toString().lowercase(), name = name.trim())
                        if (store.upsertGroup(value)) {
                            onSaved(value.id)
                            onClose()
                        } else {
                            error = store.storageError
                        }
                    },
                    enabled = name.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(L10n.text("ledger.save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                if (group != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { confirmDelete = true }.padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("ledger.group.delete"), color = palette.warningText, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (confirmDelete && group != null) {
        KitAlert(
            title = L10n.text("ledger.group.deleteTitle"),
            message = L10n.text("ledger.group.deleteMessage"),
            confirmLabel = L10n.text("ledger.group.delete"),
            onConfirm = {
                confirmDelete = false
                if (store.deleteGroup(group.id)) onClose() else error = store.storageError
            },
            onDismiss = { confirmDelete = false },
            destructive = true
        )
    }
}

@Composable
fun LedgerProductEditor(store: LedgerStore, product: LedgerProduct?, onSaved: (String) -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    var name by remember { mutableStateOf(product?.let { LedgerPresentation.productDisplayName(it) } ?: "") }
    var error by remember { mutableStateOf<String?>(null) }

    KitSheet(L10n.text(if (product == null) "ledger.product.add" else "ledger.product.edit"), onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    KitField(L10n.text("ledger.product.name"), name) { name = it }
                }
                error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
                Button(
                    onClick = {
                        val trimmed = name.trim()
                        val savedName = if (product != null && trimmed == LedgerPresentation.productDisplayName(product)) product.name else trimmed
                        val value = LedgerProduct(id = product?.id ?: UUID.randomUUID().toString().lowercase(), name = savedName)
                        if (store.upsertProduct(value)) {
                            onSaved(value.id)
                            onClose()
                        } else {
                            error = store.storageError
                        }
                    },
                    enabled = name.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(L10n.text("ledger.save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

@Composable
fun LedgerEventEditor(store: LedgerStore, event: LedgerEvent?, onSaved: (String) -> Unit, onClose: () -> Unit) {
    val palette = kitPalette()
    var name by remember { mutableStateOf(event?.name ?: "") }
    var hasDate by remember { mutableStateOf(event?.date != null) }
    var date by remember { mutableStateOf(LedgerPresentation.date(event?.date ?: LedgerDates.string(LocalDate.now()))) }
    var city by remember { mutableStateOf(event?.city ?: "") }
    var venue by remember { mutableStateOf(event?.venue ?: "") }
    var address by remember { mutableStateOf(event?.address ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    fun optional(text: String): String? {
        val value = text.trim()
        return if (value.isEmpty()) null else value
    }

    KitSheet(L10n.text(if (event == null) "ledger.event.add" else "ledger.event.edit"), onClose) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    KitField(L10n.text("ledger.event.name"), name) { name = it }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(L10n.text("ledger.event.hasDate"), color = palette.ink, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Switch(
                            checked = hasDate,
                            onCheckedChange = { hasDate = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = palette.accentFill)
                        )
                    }
                    if (hasDate) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true }.padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(L10n.text("ledger.date"), color = palette.ink, fontSize = 15.sp)
                            Spacer(modifier = Modifier.weight(1f))
                            Text(com.oshicue.kit.ui.theme.KitDate.string(date, "yyyy MMM d"), color = palette.accentText, fontSize = 15.sp)
                        }
                    }
                }
                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("ledger.optional"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    KitField(L10n.text("ledger.city"), city) { city = it }
                    KitField(L10n.text("ledger.venue"), venue) { venue = it }
                    KitField(L10n.text("ledger.address"), address) { address = it }
                }
                error?.let { Text(it, color = palette.warningText, fontSize = 12.sp) }
                Button(
                    onClick = {
                        val value = LedgerEvent(
                            id = event?.id ?: UUID.randomUUID().toString().lowercase(),
                            name = name.trim(),
                            date = if (hasDate) LedgerPresentation.calendar.format(date) else null,
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
                    },
                    enabled = name.trim().isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(L10n.text("ledger.save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showDatePicker) {
        KitDatePickerDialog(date, { date = it }, { showDatePicker = false })
    }
}

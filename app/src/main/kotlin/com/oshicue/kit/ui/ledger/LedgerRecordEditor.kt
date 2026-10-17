package com.oshicue.kit.ui.ledger

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerPriceSuggestions
import com.oshicue.kit.core.LedgerRecord
import com.oshicue.kit.core.favoriteMemberships
import com.oshicue.kit.core.preferredRecordMembership
import com.oshicue.kit.core.recordMembershipChoices
import com.oshicue.kit.core.recordMembershipGroups
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.data.ScheduleStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.KitAlert
import com.oshicue.kit.ui.common.KitDatePickerDialog
import com.oshicue.kit.ui.common.KitDropdownRow
import com.oshicue.kit.ui.common.KitField
import com.oshicue.kit.ui.common.KitSheet
import com.oshicue.kit.ui.theme.KitDate
import com.oshicue.kit.ui.theme.kitPalette
import java.time.LocalDate
import java.util.UUID

@Composable
fun LedgerRecordEditor(
    store: LedgerStore,
    scheduleStore: ScheduleStore,
    date: String,
    record: LedgerRecord?,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    val document = store.document
    val initialMembership = document.preferredRecordMembership(record)
    val initialProduct = record?.productID ?: document.products.firstOrNull()?.id ?: ""
    val purchaseDate = record?.date ?: date
    val initialCurrency = record?.currency
        ?: LedgerPriceSuggestions.currency(
            document, initialMembership, initialProduct, purchaseDate, store.lastAddedPrices
        )
    val initialPrice = record?.unitPriceMinor
        ?: LedgerPriceSuggestions.price(
            document, initialMembership, initialProduct, initialCurrency, purchaseDate, store.lastAddedPrices
        ).unitPriceMinor

    var selectedDate by remember { mutableStateOf(LedgerPresentation.date(purchaseDate)) }
    var membershipID by remember { mutableStateOf(initialMembership?.id ?: "") }
    var productID by remember { mutableStateOf(initialProduct) }
    var currency by remember { mutableStateOf(initialCurrency) }
    var quantity by remember { mutableStateOf((record?.quantity ?: 1).toString()) }
    var price by remember {
        mutableStateOf(
            (LedgerCurrency.fromCode(initialCurrency) ?: LedgerCurrency.JPY).priceText(initialPrice)
        )
    }
    var eventID by remember { mutableStateOf(record?.eventID ?: "") }
    var eventName by remember { mutableStateOf(record?.eventNameSnapshot ?: "") }
    var city by remember { mutableStateOf(record?.citySnapshot ?: "") }
    var venue by remember { mutableStateOf(record?.venueSnapshot ?: "") }
    var address by remember { mutableStateOf(record?.addressSnapshot ?: "") }
    var note by remember { mutableStateOf(record?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showMembershipSelection by remember { mutableStateOf(false) }
    var showMemberEditor by remember { mutableStateOf(false) }
    var showProductEditor by remember { mutableStateOf(false) }
    var showEventEditor by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val membership = store.document.memberships.firstOrNull { it.id == membershipID }
    val member = store.document.members.firstOrNull { it.id == membership?.memberID }
    val group = store.document.groups.firstOrNull { it.id == membership?.groupID }
    val product = store.document.products.firstOrNull { it.id == productID }
    val selectedCurrency = LedgerCurrency.fromCode(currency) ?: LedgerCurrency.JPY
    val priceValue = selectedCurrency.parsePrice(price.replace(',', '.'))
    val quantityValue = quantity.toIntOrNull()
    val total = if (quantityValue != null && quantityValue > 0 && priceValue != null) {
        runCatching { Math.multiplyExact(priceValue, quantityValue.toLong()) }.getOrNull()
    } else null

    fun isAvailable(item: com.oshicue.kit.core.LedgerMembership): Boolean =
        store.document.activeMemberships.any { it.id == item.id }

    fun prefillPrice(selectCurrency: Boolean = false) {
        val day = LedgerPresentation.calendar.format(selectedDate)
        val currentMembership = store.document.memberships.firstOrNull { it.id == membershipID }
        if (selectCurrency) {
            currency = if (record != null &&
                record.memberID == currentMembership?.memberID &&
                record.groupID == currentMembership?.groupID &&
                record.productID == productID
            ) {
                record.currency
            } else {
                LedgerPriceSuggestions.currency(
                    store.document, currentMembership, productID, day, store.lastAddedPrices
                )
            }
        }
        val currentCurrency = LedgerCurrency.fromCode(currency) ?: LedgerCurrency.JPY
        price = if (record != null &&
            record.memberID == currentMembership?.memberID &&
            record.groupID == currentMembership?.groupID &&
            record.productID == productID &&
            record.currency == currency
        ) {
            currentCurrency.priceText(record.unitPriceMinor)
        } else {
            val suggestion = LedgerPriceSuggestions.price(
                store.document, currentMembership, productID, currency, day, store.lastAddedPrices
            )
            currentCurrency.priceText(suggestion.unitPriceMinor)
        }
    }

    fun optional(text: String): String? {
        val value = text.trim()
        return if (value.isEmpty()) null else value
    }

    fun save() {
        val currentMembership = store.document.memberships.firstOrNull { it.id == membershipID }
        val currentMember = store.document.members.firstOrNull { it.id == currentMembership?.memberID }
        val currentGroup = store.document.groups.firstOrNull { it.id == currentMembership?.groupID }
        val currentProduct = store.document.products.firstOrNull { it.id == productID }
        if (currentMembership == null || currentMember == null || currentGroup == null || currentProduct == null) {
            error = L10n.text("ledger.requiredSelection")
            return
        }
        if (quantityValue == null || quantityValue <= 0) {
            error = L10n.text("ledger.invalidQuantity")
            return
        }
        if (priceValue == null || total == null) {
            error = L10n.text("ledger.invalidPrice")
            return
        }
        val sameMembership = record?.memberID == currentMember.id && record?.groupID == currentGroup.id
        val sameProduct = record?.productID == currentProduct.id
        val value = LedgerRecord(
            id = record?.id ?: UUID.randomUUID().toString(),
            date = LedgerPresentation.calendar.format(selectedDate),
            memberID = currentMember.id,
            groupID = currentGroup.id,
            productID = currentProduct.id,
            quantity = quantityValue,
            unitPriceMinor = priceValue,
            currency = currency,
            eventID = eventID.ifEmpty { null },
            memberNameSnapshot = if (sameMembership) record!!.memberNameSnapshot else currentMember.name,
            groupNameSnapshot = if (sameMembership) record!!.groupNameSnapshot else currentGroup.name,
            productNameSnapshot = if (sameProduct) record!!.productNameSnapshot else currentProduct.name,
            colorSnapshot = if (sameMembership) record!!.colorSnapshot else currentMembership.color,
            eventNameSnapshot = optional(eventName),
            citySnapshot = optional(city),
            venueSnapshot = optional(venue),
            addressSnapshot = optional(address),
            note = optional(note)
        )
        if (store.saveRecord(value)) onClose() else error = store.storageError
    }

    KitSheet(L10n.text(if (record == null) "ledger.newRecord" else "ledger.editRecord"), onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.surface, RoundedCornerShape(14.dp))
                        .clickable { showDatePicker = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(L10n.text("ledger.date"), color = palette.ink, fontSize = 15.sp)
                    Spacer(modifier = Modifier.weight(1f))
                    Text(KitDate.string(selectedDate, "yyyy MMM d EEE"), color = palette.accentText, fontSize = 15.sp)
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("ledger.member"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    if (member != null && group != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            LedgerAvatar(member, membership?.color ?: "#F23C6B", 45.dp)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(member.name, color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                                Text(group.name, color = palette.muted, fontSize = 12.sp)
                                membership?.let {
                                    if (!isAvailable(it)) Text(L10n.text("ledger.deleted"), color = palette.muted, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showMembershipSelection = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.text("ledger.chooseMember"), color = palette.ink, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            membershipLabel(store, membership),
                            color = palette.accentText,
                            fontSize = 14.sp
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showMemberEditor = true }.padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.PersonAdd, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("ledger.addMember"), color = palette.accentText, fontSize = 15.sp)
                    }
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("ledger.type"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    KitDropdownRow(
                        label = L10n.text("ledger.type"),
                        value = product?.let { LedgerPresentation.productDisplayName(it) } ?: "",
                        options = store.document.products.map { it.id to LedgerPresentation.productDisplayName(it) },
                        onSelect = { productID = it; prefillPrice(selectCurrency = true) }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showProductEditor = true }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("ledger.addType"), color = palette.accentText, fontSize = 15.sp)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.text("ledger.quantity"), color = palette.ink, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            Icons.Filled.RemoveCircleOutline,
                            contentDescription = null,
                            tint = palette.accentText,
                            modifier = Modifier.size(24.dp).clickable {
                                val current = quantityValue ?: 1
                                quantity = maxOf(1, current - 1).toString()
                            }
                        )
                        Box(modifier = Modifier.width(70.dp), contentAlignment = Alignment.Center) {
                            androidx.compose.material3.TextField(
                                value = quantity,
                                onValueChange = { quantity = it.filter { character -> character.isDigit() } },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = androidx.compose.material3.TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    color = palette.ink,
                                    fontSize = 16.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            )
                        }
                        Icon(
                            Icons.Filled.AddCircleOutline,
                            contentDescription = null,
                            tint = palette.accentText,
                            modifier = Modifier.size(24.dp).clickable {
                                quantity = ((quantityValue ?: 0) + 1).toString()
                            }
                        )
                    }
                    KitDropdownRow(
                        label = L10n.text("ledger.currency"),
                        value = currency,
                        options = LedgerCurrency.entries.map { it.code to it.code },
                        onSelect = { currency = it; prefillPrice() }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.text("ledger.unitPrice"), color = palette.ink, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        androidx.compose.material3.TextField(
                            value = price,
                            onValueChange = { price = it },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            colors = androidx.compose.material3.TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = palette.line,
                                unfocusedIndicatorColor = palette.line
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = palette.ink,
                                fontSize = 16.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            ),
                            modifier = Modifier.width(140.dp)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                        Text(L10n.text("ledger.amount"), color = palette.ink, fontSize = 15.sp)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            total?.let { selectedCurrency.format(it, L10n.currentLocale()) } ?: "—",
                            color = palette.ink,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(L10n.text("ledger.priceHint"), color = palette.muted, fontSize = 11.sp)
                }

                Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(14.dp)).padding(14.dp)) {
                    Text(L10n.text("ledger.optional"), color = palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    KitDropdownRow(
                        label = L10n.text("ledger.event"),
                        value = eventID,
                        options = listOf("" to L10n.text("ledger.noEvent")) + store.document.events.map { event ->
                            event.id to (event.name + (event.venue?.let { " · " + it } ?: ""))
                        },
                        onSelect = { id ->
                            eventID = id
                            val event = store.document.events.firstOrNull { it.id == id }
                            eventName = event?.name ?: ""
                            city = event?.city ?: ""
                            venue = event?.venue ?: ""
                            address = event?.address ?: ""
                            if (event == null) {
                                eventName = ""
                            }
                        }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { showEventEditor = true }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("ledger.addEvent"), color = palette.accentText, fontSize = 15.sp)
                    }
                    if (scheduleStore.document.events.isNotEmpty()) {
                        var scheduleMenu by remember { mutableStateOf(false) }
                        Box {
                            Row(
                                modifier = Modifier.fillMaxWidth().clickable { scheduleMenu = true }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(L10n.text("ledger.fromTimetable"), color = palette.accentText, fontSize = 15.sp)
                            }
                            androidx.compose.material3.DropdownMenu(expanded = scheduleMenu, onDismissRequest = { scheduleMenu = false }) {
                                for (festival in scheduleStore.document.events) {
                                    for (place in festival.venues) {
                                        androidx.compose.material3.DropdownMenuItem(
                                            text = { Text(festival.name + " · " + place.name, color = palette.ink) },
                                            onClick = {
                                                scheduleMenu = false
                                                val id = "schedule:" + festival.id.toByteArray(Charsets.UTF_8).size + ":" + festival.id + place.id
                                                val event = LedgerEvent(
                                                    id = id, name = festival.name,
                                                    date = LedgerPresentation.calendar.format(selectedDate),
                                                    city = festival.city, venue = place.name
                                                )
                                                if (store.upsertEvent(event)) {
                                                    eventID = id
                                                    eventName = event.name
                                                    city = event.city ?: ""
                                                    venue = event.venue ?: ""
                                                    address = event.address ?: ""
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                    KitField(L10n.text("ledger.event"), eventName) { eventName = it }
                    KitField(L10n.text("ledger.city"), city) { city = it }
                    KitField(L10n.text("ledger.venue"), venue) { venue = it }
                    KitField(L10n.text("ledger.address"), address) { address = it }
                    KitField(L10n.text("ledger.note"), note, singleLine = false, minLines = 2) { note = it }
                }

                error?.let { message ->
                    Text(message, color = palette.warningText, fontSize = 12.sp)
                }

                Button(
                    onClick = { save() },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(L10n.text("ledger.save"), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }

                if (record != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { confirmDelete = true }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, tint = palette.warningText, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(L10n.text("ledger.record.delete"), color = palette.warningText, fontSize = 15.sp)
                    }
                }
                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }

    if (showDatePicker) {
        KitDatePickerDialog(
            selectedDate,
            {
                selectedDate = it
                if (record == null) prefillPrice()
            },
            { showDatePicker = false }
        )
    }
    if (showMembershipSelection) {
        LedgerMembershipSelection(store, record, membershipID, {
            membershipID = it
            prefillPrice(selectCurrency = true)
        }) {
            showMembershipSelection = false
        }
    }
    if (showMemberEditor) {
        LedgerMemberEditor(
            store = store,
            membership = null,
            onSaved = { id ->
                membershipID = id
                prefillPrice(selectCurrency = true)
            },
            onClose = { showMemberEditor = false }
        )
    }
    if (showProductEditor) {
        LedgerProductEditor(
            store = store,
            product = null,
            onSaved = { id ->
                productID = id
                prefillPrice(selectCurrency = true)
            },
            onClose = { showProductEditor = false }
        )
    }
    if (showEventEditor) {
        LedgerEventEditor(
            store = store,
            event = null,
            onSaved = { id ->
                eventID = id
                val event = store.document.events.firstOrNull { it.id == id }
                eventName = event?.name ?: ""
                city = event?.city ?: ""
                venue = event?.venue ?: ""
                address = event?.address ?: ""
            },
            onClose = { showEventEditor = false }
        )
    }
    if (confirmDelete && record != null) {
        KitAlert(
            title = L10n.text("ledger.deleteTitle"),
            message = L10n.text("ledger.deleteMessage"),
            confirmLabel = L10n.text("ledger.record.delete"),
            onConfirm = {
                confirmDelete = false
                if (store.deleteRecord(record.id)) onClose() else error = store.storageError
            },
            onDismiss = { confirmDelete = false },
            destructive = true
        )
    }
}

private fun membershipLabel(store: LedgerStore, membership: com.oshicue.kit.core.LedgerMembership?): String {
    if (membership == null) return ""
    val name = store.document.members.firstOrNull { it.id == membership.memberID }?.name ?: ""
    val group = store.document.groups.firstOrNull { it.id == membership.groupID }?.name ?: ""
    val available = store.document.activeMemberships.any { it.id == membership.id }
    return name + " · " + group + (if (available) "" else " · " + L10n.text("ledger.deleted"))
}

@Composable
private fun LedgerMembershipSelection(
    store: LedgerStore,
    record: LedgerRecord?,
    selected: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    KitSheet(L10n.text("ledger.chooseMember"), onClose) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            val favorites = store.document.favoriteMemberships
            if (favorites.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(L10n.text("ledger.members.favorites"), color = palette.muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                for (item in favorites) {
                    MembershipRow(store, item, selected, onSelect, onClose)
                }
            }
            for (group in store.document.recordMembershipGroups(record)) {
                Text(
                    group.name,
                    color = palette.muted,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
                )
                for (item in store.document.recordMembershipChoices(editing = record).filter { it.groupID == group.id }) {
                    MembershipRow(store, item, selected, onSelect, onClose)
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

@Composable
private fun MembershipRow(
    store: LedgerStore,
    item: com.oshicue.kit.core.LedgerMembership,
    selected: String,
    onSelect: (String) -> Unit,
    onClose: () -> Unit
) {
    val palette = kitPalette()
    val member = store.document.members.firstOrNull { it.id == item.memberID }
    val group = store.document.groups.firstOrNull { it.id == item.groupID }
    val available = store.document.activeMemberships.any { it.id == item.id }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onSelect(item.id)
                onClose()
            }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        LedgerAvatar(member, item.color, 42.dp)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member?.name ?: "", color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                if (member?.isFavorited == true) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Filled.Star, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(13.dp))
                }
            }
            Text(group?.name ?: "", color = palette.muted, fontSize = 12.sp)
            if (!available) Text(L10n.text("ledger.deleted"), color = palette.muted, fontSize = 12.sp)
        }
        if (selected == item.id) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
        }
    }
}

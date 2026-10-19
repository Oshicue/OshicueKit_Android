package com.oshicue.kit.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.FestivalEvent
import com.oshicue.kit.core.KitDate
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerMembership
import com.oshicue.kit.core.LedgerPrice
import com.oshicue.kit.core.LedgerPriceSuggestions
import com.oshicue.kit.core.LedgerRecord
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.ScheduleStore
import com.oshicue.kit.core.Venue
import com.oshicue.kit.core.newIdentifier
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.favoriteMemberships
import com.oshicue.kit.core.preferredRecordMembership
import com.oshicue.kit.core.recordMembershipChoices
import com.oshicue.kit.core.recordMembershipChoicesIn
import com.oshicue.kit.core.recordMembershipGroups
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun LedgerRecordEditor(
    store: LedgerStore,
    scheduleStore: ScheduleStore,
    date: String,
    record: LedgerRecord?,
    onClose: () -> Unit
) {
    val kit = LocalKit.current
    val document = store.document
    val preferred = remember(record?.id) { document.preferredRecordMembership(record) }
    var membershipId by remember(record?.id) { mutableStateOf(preferred?.id ?: "") }
    var productId by remember(record?.id) {
        mutableStateOf(record?.productId ?: document.products.firstOrNull()?.id ?: "")
    }
    var selectedDate by remember(record?.id) {
        mutableStateOf(LedgerDates.parse(record?.date ?: date) ?: LocalDate.now())
    }
    val purchaseDate = KitDate.formatDay(selectedDate)
    var currency by remember(record?.id) {
        mutableStateOf(
            record?.currency ?: LedgerPriceSuggestions.currency(
                document, preferred, productId, purchaseDate, store.lastAddedPrices
            )
        )
    }
    var quantity by remember(record?.id) { mutableStateOf((record?.quantity ?: 1).toString()) }
    var price by remember(record?.id) {
        val suggested = record?.unitPriceMinor ?: LedgerPriceSuggestions.price(
            document, preferred, productId, currency, purchaseDate, store.lastAddedPrices
        ).unitPriceMinor
        mutableStateOf(LedgerCurrency.from(currency)?.priceText(suggested) ?: "0")
    }
    var eventId by remember(record?.id) { mutableStateOf(record?.eventId ?: "") }
    var eventName by remember(record?.id) { mutableStateOf(record?.eventNameSnapshot ?: "") }
    var city by remember(record?.id) { mutableStateOf(record?.citySnapshot ?: "") }
    var venue by remember(record?.id) { mutableStateOf(record?.venueSnapshot ?: "") }
    var address by remember(record?.id) { mutableStateOf(record?.addressSnapshot ?: "") }
    var note by remember(record?.id) { mutableStateOf(record?.note ?: "") }
    var error by remember { mutableStateOf<String?>(null) }
    var showMemberPicker by remember { mutableStateOf(false) }
    var showProductEditor by remember { mutableStateOf(false) }
    var showEventEditor by remember { mutableStateOf(false) }
    var showEventSelector by remember { mutableStateOf(false) }
    var showScheduleEvents by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    val membership = document.memberships.firstOrNull { it.id == membershipId }
    val member = document.members.firstOrNull { it.id == membership?.memberId }
    val group = document.groups.firstOrNull { it.id == membership?.groupId }
    val product = document.products.firstOrNull { it.id == productId }
    val selectedCurrency = LedgerCurrency.from(currency) ?: LedgerCurrency.JPY
    val priceValue = selectedCurrency.parsePrice(price.replace(',', '.'))
    val quantityValue = quantity.toIntOrNull()
    val total = if (priceValue != null && quantityValue != null && quantityValue > 0) {
        try {
            Math.multiplyExact(priceValue, quantityValue.toLong())
        } catch (_: ArithmeticException) {
            null
        }
    } else null

    fun prefillPrice(selectCurrency: Boolean) {
        val suggestionMembership = document.memberships.firstOrNull { it.id == membershipId }
        if (selectCurrency) {
            val useRecordCurrency = record != null &&
                record.memberId == suggestionMembership?.memberId &&
                record.groupId == suggestionMembership?.groupId &&
                record.productId == productId
            currency = if (useRecordCurrency) {
                record?.currency ?: currency
            } else {
                LedgerPriceSuggestions.currency(
                    document, suggestionMembership, productId, purchaseDate, store.lastAddedPrices
                )
            }
        }
        val sameRecordPrice = record != null &&
            record.memberId == suggestionMembership?.memberId &&
            record.groupId == suggestionMembership?.groupId &&
            record.productId == productId &&
            record.currency == currency
        val amount = if (sameRecordPrice) {
            record?.unitPriceMinor ?: 0
        } else {
            LedgerPriceSuggestions.price(
                document, suggestionMembership, productId, currency, purchaseDate, store.lastAddedPrices
            ).unitPriceMinor
        }
        price = (LedgerCurrency.from(currency) ?: LedgerCurrency.JPY).priceText(amount)
    }

    fun selectEvent(id: String) {
        val event = document.events.firstOrNull { it.id == id }
        if (event == null) {
            eventName = ""
            city = ""
            venue = ""
            address = ""
            return
        }
        eventName = event.name
        city = event.city ?: ""
        venue = event.venue ?: ""
        address = event.address ?: ""
    }

    ModalScreen(
        title = L10n.text(if (record == null) "ledger.newRecord" else "ledger.editRecord"),
        onClose = onClose,
        actions = {
            KitTextAction(L10n.text("ledger.save")) {
                val currentMembership = document.memberships.firstOrNull { it.id == membershipId }
                val currentMember = document.members.firstOrNull { it.id == currentMembership?.memberId }
                val currentGroup = document.groups.firstOrNull { it.id == currentMembership?.groupId }
                val currentProduct = document.products.firstOrNull { it.id == productId }
                if (currentMembership == null || currentMember == null || currentGroup == null || currentProduct == null) {
                    error = L10n.text("ledger.requiredSelection")
                    return@KitTextAction
                }
                val quantityNumber = quantity.toIntOrNull()
                if (quantityNumber == null || quantityNumber <= 0) {
                    error = L10n.text("ledger.invalidQuantity")
                    return@KitTextAction
                }
                val amount = selectedCurrency.parsePrice(price.replace(',', '.'))
                if (amount == null) {
                    error = L10n.text("ledger.invalidPrice")
                    return@KitTextAction
                }
                val sameMembership = record?.memberId == currentMember.id && record?.groupId == currentGroup.id
                val sameProduct = record?.productId == currentProduct.id
                fun optional(value: String): String? = value.trim().ifEmpty { null }
                val value = LedgerRecord(
                    id = record?.id ?: newIdentifier(),
                    date = KitDate.formatDay(selectedDate),
                    memberId = currentMember.id,
                    groupId = currentGroup.id,
                    productId = currentProduct.id,
                    quantity = quantityNumber,
                    unitPriceMinor = amount,
                    currency = currency,
                    eventId = eventId.ifEmpty { null },
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
        }
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            FieldLabel(L10n.text("ledger.date"))
            SelectorRow(
                label = L10n.text("ledger.date"),
                value = KitDate.string(
                    selectedDate.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(),
                    "yyyy.MM.dd",
                    ZoneId.systemDefault()
                ),
                onClick = { showDatePicker = true }
            )
            Spacer(Modifier.height(16.dp))

            SectionLabel(L10n.text("ledger.member"))
            Spacer(Modifier.height(6.dp))
            if (member != null && group != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LedgerAvatar(member, membership?.color ?: "#F23C6B", 45.dp)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(member.name, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = kit.ink)
                        Text(group.name, fontSize = 11.sp, color = kit.muted)
                        if (membership != null && document.activeMemberships.none { it.id == membership.id }) {
                            Text(L10n.text("ledger.deleted"), fontSize = 11.sp, color = kit.muted)
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            SelectorRow(
                label = L10n.text("ledger.chooseMember"),
                value = membershipSummary(document, membership, membershipId),
                onClick = { showMemberPicker = true }
            )
            Spacer(Modifier.height(6.dp))
            KitSecondaryButton(L10n.text("ledger.addMember"), icon = Icons.Filled.Add) { showMemberPicker = true }

            Spacer(Modifier.height(16.dp))
            SectionLabel(L10n.text("ledger.type"))
            Spacer(Modifier.height(6.dp))
            SelectorRow(
                label = L10n.text("ledger.type"),
                value = product?.let { productDisplayName(it.id, it.name) } ?: "",
                onClick = { showProductEditor = true }
            )
            Spacer(Modifier.height(6.dp))
            KitSecondaryButton(L10n.text("ledger.addType"), icon = Icons.Filled.Add) { showProductEditor = true }

            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.text("ledger.quantity"), fontSize = 13.5.sp, color = kit.ink)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Filled.Remove,
                    null,
                    tint = kit.ink,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            quantity = maxOf(1, (quantity.toIntOrNull() ?: 1) - 1).toString()
                        }
                )
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier
                        .width(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(kit.surface)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = quantity,
                        onValueChange = { quantity = it.filter { character -> character.isDigit() } },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            color = kit.ink,
                            textAlign = TextAlignCenter
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                        ),
                        singleLine = true
                    )
                }
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Filled.Add,
                    null,
                    tint = kit.ink,
                    modifier = Modifier
                        .size(24.dp)
                        .clickable {
                            val value = (quantity.toIntOrNull() ?: 1) + 1
                            quantity = value.toString()
                        }
                )
            }

            Spacer(Modifier.height(12.dp))
            FieldLabel(L10n.text("ledger.currency"))
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                LedgerCurrency.entries.forEach { value ->
                    SelectableChip(value.code, currency == value.code) {
                        currency = value.code
                        prefillPrice(selectCurrency = false)
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(L10n.text("ledger.unitPrice"), fontSize = 13.5.sp, color = kit.ink)
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .width(120.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(kit.surface)
                        .padding(10.dp)
                ) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = price,
                        onValueChange = { price = it.filter { character -> character.isDigit() || character == '.' || character == ',' } },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            color = kit.ink,
                            textAlign = TextAlignEnd
                        ),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        singleLine = true
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(L10n.text("ledger.amount"), fontSize = 13.5.sp, color = kit.muted)
                Spacer(Modifier.weight(1f))
                Text(
                    total?.let { selectedCurrency.format(it, L10n.locale) } ?: "—",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = kit.ink
                )
            }
            Text(L10n.text("ledger.priceHint"), fontSize = 11.sp, color = kit.muted)

            Spacer(Modifier.height(16.dp))
            SectionLabel(L10n.text("ledger.optional"))
            Spacer(Modifier.height(6.dp))
            SelectorRow(
                label = L10n.text("ledger.event"),
                value = document.events.firstOrNull { it.id == eventId }?.name ?: L10n.text("ledger.noEvent"),
                onClick = { showEventSelector = true }
            )
            Spacer(Modifier.height(6.dp))
            KitSecondaryButton(L10n.text("ledger.addEvent"), icon = Icons.Filled.CalendarToday) {
                showEventEditor = true
            }
            if (scheduleStore.document.events.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                KitSecondaryButton(L10n.text("ledger.fromTimetable"), icon = Icons.Filled.CalendarToday) {
                    showScheduleEvents = true
                }
            }
            Spacer(Modifier.height(8.dp))
            FieldLabel(L10n.text("ledger.event"))
            KitTextField(eventName, { eventName = it }, L10n.text("ledger.event"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.city"))
            KitTextField(city, { city = it }, L10n.text("ledger.city"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.venue"))
            KitTextField(venue, { venue = it }, L10n.text("ledger.venue"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.address"))
            KitTextField(address, { address = it }, L10n.text("ledger.address"))
            Spacer(Modifier.height(6.dp))
            FieldLabel(L10n.text("ledger.note"))
            KitTextField(note, { note = it }, L10n.text("ledger.note"), singleLine = false)

            error?.let {
                Spacer(Modifier.height(12.dp))
                InlineError(it)
            }
            Spacer(Modifier.height(14.dp))
            KitPrimaryButton(L10n.text("ledger.save")) {
                val currentMembership = document.memberships.firstOrNull { it.id == membershipId }
                val currentMember = document.members.firstOrNull { it.id == currentMembership?.memberId }
                val currentGroup = document.groups.firstOrNull { it.id == currentMembership?.groupId }
                val currentProduct = document.products.firstOrNull { it.id == productId }
                if (currentMembership == null || currentMember == null || currentGroup == null || currentProduct == null) {
                    error = L10n.text("ledger.requiredSelection")
                } else {
                    val quantityNumber = quantity.toIntOrNull()
                    val amount = selectedCurrency.parsePrice(price.replace(',', '.'))
                    if (quantityNumber == null || quantityNumber <= 0) {
                        error = L10n.text("ledger.invalidQuantity")
                    } else if (amount == null) {
                        error = L10n.text("ledger.invalidPrice")
                    } else {
                        val sameMembership = record?.memberId == currentMember.id && record?.groupId == currentGroup.id
                        val sameProduct = record?.productId == currentProduct.id
                        fun optional(value: String): String? = value.trim().ifEmpty { null }
                        val value = LedgerRecord(
                            id = record?.id ?: newIdentifier(),
                            date = KitDate.formatDay(selectedDate),
                            memberId = currentMember.id,
                            groupId = currentGroup.id,
                            productId = currentProduct.id,
                            quantity = quantityNumber,
                            unitPriceMinor = amount,
                            currency = currency,
                            eventId = eventId.ifEmpty { null },
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
                }
            }
            if (record != null) {
                Spacer(Modifier.height(10.dp))
                KitSecondaryButton(L10n.text("ledger.record.delete")) { confirmDelete = true }
            }
            Spacer(Modifier.height(40.dp))
        }
    }

    if (showDatePicker) {
        DatePickerSheet(selectedDate, { showDatePicker = false }) { selectedDate = it; showDatePicker = false }
    }
    if (showMemberPicker) {
        LedgerMembershipPicker(
            store = store,
            record = record,
            selectedId = membershipId,
            onDismiss = { showMemberPicker = false },
            onSelect = { id ->
                membershipId = id
                showMemberPicker = false
                prefillPrice(selectCurrency = true)
            }
        )
    }
    if (showProductEditor) {
        LedgerProductEditor(store = store, product = null, onSaved = { id ->
            productId = id
            showProductEditor = false
            prefillPrice(selectCurrency = true)
        }, onClose = { showProductEditor = false })
    }
    if (showEventEditor) {
        LedgerEventEditor(store = store, event = null, onSaved = { id ->
            eventId = id
            selectEvent(id)
            showEventEditor = false
        }, onClose = { showEventEditor = false })
    }
    if (showEventSelector) {
        OptionSelector(
            title = L10n.text("ledger.event"),
            options = listOf("" to L10n.text("ledger.noEvent")) + document.events.map {
                it.id to (it.name + (it.venue?.let { value -> " · " + value } ?: ""))
            },
            selected = eventId,
            onDismiss = { showEventSelector = false },
            onSelect = { id ->
                eventId = id
                selectEvent(id)
            }
        )
    }
    if (showScheduleEvents) {
        OptionSelector(
            title = L10n.text("ledger.fromTimetable"),
            options = scheduleStore.document.events.flatMap { festival ->
                festival.venues.map { place -> "${festival.id}|${place.id}" to (festival.name + " · " + place.name) }
            },
            selected = "",
            onDismiss = { showScheduleEvents = false },
            onSelect = { key ->
                val parts = key.split("|")
                val festival = scheduleStore.document.events.firstOrNull { it.id == parts[0] }
                val place = festival?.venues?.firstOrNull { it.id == parts.getOrNull(1) }
                if (festival != null && place != null) {
                    importScheduleEvent(store, festival, place, KitDate.formatDay(selectedDate)) { id ->
                        eventId = id
                        selectEvent(id)
                    }
                }
            }
        )
    }
    if (confirmDelete && record != null) {
        ConfirmDialog(
            title = L10n.text("ledger.deleteTitle"),
            message = L10n.text("ledger.deleteMessage"),
            confirmLabel = L10n.text("ledger.record.delete"),
            onConfirm = {
                if (store.deleteRecord(record.id)) onClose() else error = store.storageError
            },
            onDismiss = { confirmDelete = false }
        )
    }
}

private fun importScheduleEvent(
    store: LedgerStore,
    festival: FestivalEvent,
    venue: Venue,
    date: String,
    onSaved: (String) -> Unit
) {
    val id = "schedule:" + festival.id.toByteArray(Charsets.UTF_8).size + ":" + festival.id + venue.id
    val event = LedgerEvent(id, festival.name, date, festival.city, venue.name)
    if (store.upsertEvent(event)) onSaved(id)
}

private fun membershipSummary(
    document: com.oshicue.kit.core.LedgerDocument,
    membership: LedgerMembership?,
    membershipId: String
): String {
    val current = membership ?: return ""
    val name = document.members.firstOrNull { it.id == current.memberId }?.name ?: ""
    val group = document.groups.firstOrNull { it.id == current.groupId }?.name ?: ""
    val available = document.activeMemberships.any { it.id == membershipId }
    return name + " · " + group + (if (available) "" else " · " + L10n.text("ledger.deleted"))
}

private val TextAlignCenter = androidx.compose.ui.text.style.TextAlign.Center
private val TextAlignEnd = androidx.compose.ui.text.style.TextAlign.End

@Composable
fun LedgerMembershipPicker(
    store: LedgerStore,
    record: LedgerRecord?,
    selectedId: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val kit = LocalKit.current
    val document = store.document
    ModalScreen(title = L10n.text("ledger.chooseMember"), onClose = onDismiss) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            val favorites = document.favoriteMemberships
            if (favorites.isNotEmpty()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Star, null, tint = kit.accentText, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(5.dp))
                    Text(L10n.text("ledger.members.favorites"), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = kit.accentText)
                }
                Spacer(Modifier.height(6.dp))
                favorites.forEach { item ->
                    MembershipRow(document, item, item.id == selectedId) { id ->
                        onSelect(id)
                        onDismiss()
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
            document.recordMembershipGroups(record).forEach { group ->
                SectionLabel(group.name)
                Spacer(Modifier.height(6.dp))
                document.recordMembershipChoicesIn(group.id, record).forEach { item ->
                    MembershipRow(document, item, item.id == selectedId) { id ->
                        onSelect(id)
                        onDismiss()
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MembershipRow(
    document: com.oshicue.kit.core.LedgerDocument,
    item: LedgerMembership,
    selected: Boolean,
    onSelect: (String) -> Unit
) {
    val kit = LocalKit.current
    val member = document.members.firstOrNull { it.id == item.memberId }
    val group = document.groups.firstOrNull { it.id == item.groupId }
    val available = document.activeMemberships.any { it.id == item.id }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(kit.surface)
            .clickable { onSelect(item.id) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LedgerAvatar(member, item.color, 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(member?.name ?: "", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                if (member?.isFavorited == true) {
                    Spacer(Modifier.width(5.dp))
                    Icon(Icons.Filled.Star, null, tint = kit.accentText, modifier = Modifier.size(12.dp))
                }
            }
            Text(group?.name ?: "", fontSize = 11.sp, color = kit.muted)
            if (!available) Text(L10n.text("ledger.deleted"), fontSize = 11.sp, color = kit.muted)
        }
        if (selected) Text("✓", color = kit.accentText, fontWeight = FontWeight.Bold)
    }
    Spacer(Modifier.height(6.dp))
}

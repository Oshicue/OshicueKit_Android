package com.oshicue.kit.ui

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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ConfirmationNumber
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
import com.oshicue.kit.core.L10n
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerGroup
import com.oshicue.kit.core.LedgerMembership
import com.oshicue.kit.core.LedgerProduct
import com.oshicue.kit.core.LedgerStore
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.foldForSearch
import com.oshicue.kit.core.standardCompare

private sealed interface CatalogSheet {
    data class Member(val membership: LedgerMembership?) : CatalogSheet
    data class Group(val group: LedgerGroup?) : CatalogSheet
    data class Product(val product: LedgerProduct?) : CatalogSheet
    data class Event(val event: LedgerEvent?) : CatalogSheet
}

@Composable
fun LedgerCatalogScreen(store: LedgerStore, modifier: Modifier = Modifier) {
    val kit = LocalKit.current
    var section by remember { mutableStateOf("members") }
    var search by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<CatalogSheet?>(null) }
    var showAddMenu by remember { mutableStateOf(false) }

    val document = store.document
    fun contains(value: String): Boolean =
        search.isEmpty() || foldForSearch(value).contains(foldForSearch(search))

    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                L10n.text("ledger.catalog.title"),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = kit.ink
            )
            Spacer(Modifier.weight(1f))
            Box {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = L10n.text("ledger.catalog.add"),
                    tint = kit.accentText,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(kit.accent.copy(alpha = 0.1f))
                        .clickable { showAddMenu = true }
                        .padding(11.dp)
                )
                androidx.compose.material3.DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false }
                ) {
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.text("ledger.member.add")) },
                        onClick = { sheet = CatalogSheet.Member(null); showAddMenu = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.text("ledger.group.add")) },
                        onClick = { sheet = CatalogSheet.Group(null); showAddMenu = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.text("ledger.product.add")) },
                        onClick = { sheet = CatalogSheet.Product(null); showAddMenu = false }
                    )
                    androidx.compose.material3.DropdownMenuItem(
                        text = { Text(L10n.text("ledger.event.add")) },
                        onClick = { sheet = CatalogSheet.Event(null); showAddMenu = false }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SegmentedControl(
            options = listOf(
                "members" to L10n.text("ledger.catalog.members"),
                "products" to L10n.text("ledger.catalog.products"),
                "events" to L10n.text("ledger.catalog.events")
            ),
            selected = section,
            onSelect = { section = it }
        )
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(kit.surface)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⌕", fontSize = 14.sp, color = kit.muted)
            Spacer(Modifier.width(8.dp))
            androidx.compose.foundation.text.BasicTextField(
                value = search,
                onValueChange = { search = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.5.sp, color = kit.ink),
                decorationBox = { inner ->
                    Box {
                        if (search.isEmpty()) {
                            Text(L10n.text("ledger.catalog.search"), fontSize = 13.5.sp, color = kit.muted)
                        }
                        inner()
                    }
                }
            )
        }
        Spacer(Modifier.height(14.dp))

        when (section) {
            "members" -> {
                val groupList = document.activeGroups
                    .filter { group -> contains(group.name) || document.activeMemberships.any { it.groupId == group.id && contains(document.activeMembers.firstOrNull { member -> member.id == it.memberId }?.name ?: "") } }
                    .sortedWith { left, right -> standardCompare(left.name, right.name) }
                if (groupList.isEmpty()) {
                    CatalogEmpty(L10n.text("ledger.catalog.emptyMembers"))
                }
                groupList.forEach { group ->
                    val affiliations = document.activeMemberships.filter { affiliation ->
                        if (affiliation.groupId != group.id) return@filter false
                        val memberName = document.activeMembers.firstOrNull { it.id == affiliation.memberId }?.name ?: ""
                        contains(group.name) || contains(memberName)
                    }.sortedWith { left, right ->
                        standardCompare(
                            document.activeMembers.firstOrNull { it.id == left.memberId }?.name ?: "",
                            document.activeMembers.firstOrNull { it.id == right.memberId }?.name ?: ""
                        )
                    }
                    var expanded by remember(group.id) { mutableStateOf(true) }
                    SurfaceCard(corner = 16.dp) {
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    group.name,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { expanded = !expanded },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = kit.ink
                                )
                                Box(
                                    Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(kit.accent.copy(alpha = 0.1f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        affiliations.size.toString(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = kit.accentText
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Icon(
                                    Icons.Filled.Edit,
                                    contentDescription = L10n.text("ledger.group.edit"),
                                    tint = kit.accentText,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(kit.accent.copy(alpha = 0.1f))
                                        .clickable { sheet = CatalogSheet.Group(group) }
                                        .padding(10.dp)
                                )
                            }
                            if (expanded) {
                                Spacer(Modifier.height(8.dp))
                                if (affiliations.isEmpty()) {
                                    Text(L10n.text("ledger.catalog.noGroupMembers"), fontSize = 12.5.sp, color = kit.muted)
                                }
                                affiliations.forEach { affiliation ->
                                    val member = document.activeMembers.firstOrNull { it.id == affiliation.memberId }
                                    if (member != null) {
                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .clickable { sheet = CatalogSheet.Member(affiliation) }
                                                .padding(vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            LedgerAvatar(member, affiliation.color, 46.dp)
                                            Spacer(Modifier.width(12.dp))
                                            Column(Modifier.weight(1f)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Text(member.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                                                    if (member.isFavorited) {
                                                        Spacer(Modifier.width(5.dp))
                                                        Icon(Icons.Filled.Star, null, tint = kit.accentText, modifier = Modifier.size(12.dp))
                                                    }
                                                }
                                                if (document.activeMemberships.count { it.memberId == member.id } > 1) {
                                                    Text(L10n.text("ledger.member.shared"), fontSize = 11.sp, color = kit.muted)
                                                }
                                                affiliation.prices.firstOrNull()?.let { price ->
                                                    LedgerCurrency.from(price.currency)?.let { currencyValue ->
                                                        Text(
                                                            currencyValue.format(price.unitPriceMinor, L10n.locale),
                                                            fontSize = 11.sp,
                                                            color = kit.muted
                                                        )
                                                    }
                                                }
                                            }
                                            Icon(Icons.Filled.Edit, null, tint = kit.accentText, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                }
                KitPrimaryButton(L10n.text("ledger.member.add")) { sheet = CatalogSheet.Member(null) }
            }
            "products" -> {
                document.products.filter { contains(productDisplayName(it.id, it.name)) }.forEach { product ->
                    SurfaceCard(corner = 16.dp) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { sheet = CatalogSheet.Product(product) }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, null, tint = kit.accentText, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Text(productDisplayName(product.id, product.name), modifier = Modifier.weight(1f), color = kit.ink)
                            Icon(Icons.Filled.Edit, null, tint = kit.muted, modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                KitPrimaryButton(L10n.text("ledger.product.add")) { sheet = CatalogSheet.Product(null) }
            }
            else -> {
                if (document.events.isEmpty()) CatalogEmpty(L10n.text("ledger.catalog.emptyEvents"))
                document.events.filter {
                    contains(it.name + " " + (it.city ?: "") + " " + (it.venue ?: ""))
                }.forEach { event ->
                    SurfaceCard(corner = 16.dp) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { sheet = CatalogSheet.Event(event) }
                                .padding(18.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Filled.ConfirmationNumber, null, tint = kit.accentText, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text(event.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = kit.ink)
                                val detail = listOfNotNull(event.date, event.city, event.venue).joinToString(" · ")
                                if (detail.isNotEmpty()) Text(detail, fontSize = 11.sp, color = kit.muted)
                            }
                            Icon(Icons.Filled.Edit, null, tint = kit.muted, modifier = Modifier.size(15.dp))
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                KitPrimaryButton(L10n.text("ledger.event.add")) { sheet = CatalogSheet.Event(null) }
            }
        }
        Spacer(Modifier.height(30.dp))
    }

    when (val current = sheet) {
        is CatalogSheet.Member -> LedgerMemberEditor(
            store = store,
            membership = current.membership,
            onClose = { sheet = null }
        )
        is CatalogSheet.Group -> LedgerGroupEditor(
            store = store,
            group = current.group,
            onSaved = { },
            onClose = { sheet = null }
        )
        is CatalogSheet.Product -> LedgerProductEditor(
            store = store,
            product = current.product,
            onSaved = { },
            onClose = { sheet = null }
        )
        is CatalogSheet.Event -> LedgerEventEditor(
            store = store,
            event = current.event,
            onSaved = { },
            onClose = { sheet = null }
        )
        null -> Unit
    }
}

@Composable
private fun CatalogEmpty(message: String) {
    Column(
        Modifier.fillMaxWidth().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Person, null, tint = LocalKit.current.accentText, modifier = Modifier.size(34.dp))
        Spacer(Modifier.height(10.dp))
        Text(message, fontSize = 13.sp, color = LocalKit.current.muted)
    }
}

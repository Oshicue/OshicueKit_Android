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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.ConfirmationNumber
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerGroup
import com.oshicue.kit.core.LedgerProduct
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.LedgerMembership
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.data.LedgerStore
import com.oshicue.kit.i18n.L10n
import com.oshicue.kit.ui.common.CircleIconButton
import com.oshicue.kit.ui.common.kitStandardSort
import com.oshicue.kit.ui.theme.kitPalette

private sealed interface CatalogSheet {
    data class Member(val membership: LedgerMembership?) : CatalogSheet
    data class Group(val group: LedgerGroup?) : CatalogSheet
    data class Product(val product: LedgerProduct?) : CatalogSheet
    data class Event(val event: LedgerEvent?) : CatalogSheet
}

@Composable
fun LedgerCatalogScreen(store: LedgerStore) {
    val palette = kitPalette()
    var section by remember { mutableStateOf(0) }
    var search by remember { mutableStateOf("") }
    var sheet by remember { mutableStateOf<CatalogSheet?>(null) }
    var addMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(L10n.text("ledger.catalog.title"), color = palette.ink, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.weight(1f))
            Box {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = L10n.text("ledger.catalog.add"),
                    tint = palette.accentText,
                    modifier = Modifier
                        .size(42.dp)
                        .background(palette.accent.copy(alpha = 0.1f), CircleShape)
                        .padding(10.dp)
                        .clickable { addMenu = true }
                )
                DropdownMenu(expanded = addMenu, onDismissRequest = { addMenu = false }) {
                    DropdownMenuItem(
                        text = { Text(L10n.text("ledger.member.add"), color = palette.ink) },
                        onClick = { addMenu = false; sheet = CatalogSheet.Member(null) }
                    )
                    DropdownMenuItem(
                        text = { Text(L10n.text("ledger.group.add"), color = palette.ink) },
                        onClick = { addMenu = false; sheet = CatalogSheet.Group(null) }
                    )
                    DropdownMenuItem(
                        text = { Text(L10n.text("ledger.product.add"), color = palette.ink) },
                        onClick = { addMenu = false; sheet = CatalogSheet.Product(null) }
                    )
                    DropdownMenuItem(
                        text = { Text(L10n.text("ledger.event.add"), color = palette.ink) },
                        onClick = { addMenu = false; sheet = CatalogSheet.Event(null) }
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(palette.surfaceMuted)
                .padding(4.dp)
        ) {
            CatalogTab(L10n.text("ledger.catalog.members"), section == 0, Modifier.weight(1f)) { section = 0 }
            CatalogTab(L10n.text("ledger.catalog.products"), section == 1, Modifier.weight(1f)) { section = 1 }
            CatalogTab(L10n.text("ledger.catalog.events"), section == 2, Modifier.weight(1f)) { section = 2 }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(palette.surface, RoundedCornerShape(16.dp))
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Search, contentDescription = null, tint = palette.muted, modifier = Modifier.size(17.dp))
            Spacer(modifier = Modifier.width(10.dp))
            androidx.compose.material3.TextField(
                value = search,
                onValueChange = { search = it },
                singleLine = true,
                placeholder = { Text(L10n.text("ledger.catalog.search"), color = palette.muted, fontSize = 14.sp) },
                colors = androidx.compose.material3.TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                textStyle = androidx.compose.ui.text.TextStyle(color = palette.ink, fontSize = 15.sp)
            )
        }
        when (section) {
            0 -> MemberCatalog(store, search) { sheet = it }
            1 -> ProductCatalog(store, search) { sheet = it }
            else -> EventCatalog(store, search) { sheet = it }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }

    when (val current = sheet) {
        is CatalogSheet.Member -> LedgerMemberEditor(store, current.membership, {}, { sheet = null })
        is CatalogSheet.Group -> LedgerGroupEditor(store, current.group, {}, { sheet = null })
        is CatalogSheet.Product -> LedgerProductEditor(store, current.product, {}, { sheet = null })
        is CatalogSheet.Event -> LedgerEventEditor(store, current.event, {}, { sheet = null })
        null -> {}
    }
}

@Composable
private fun CatalogTab(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = kitPalette()
    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (selected) palette.surface else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = if (selected) palette.accentText else palette.muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun MemberCatalog(store: LedgerStore, search: String, onSheet: (CatalogSheet) -> Unit) {
    val palette = kitPalette()
    val expanded = remember { mutableStateMapOf<String, Boolean>() }

    fun matches(value: String): Boolean = com.oshicue.kit.ui.common.kitStandardContains(value, search)

    fun membershipsFor(group: LedgerGroup): List<LedgerMembership> =
        store.document.activeMemberships.filter { affiliation ->
            if (affiliation.groupID != group.id) return@filter false
            val memberName = store.document.activeMembers.firstOrNull { it.id == affiliation.memberID }?.name ?: ""
            matches(group.name) || matches(memberName)
        }.sortedWith { left, right ->
            val leftName = store.document.activeMembers.firstOrNull { it.id == left.memberID }?.name ?: ""
            val rightName = store.document.activeMembers.firstOrNull { it.id == right.memberID }?.name ?: ""
            kitStandardSort(leftName, rightName)
        }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            L10n.format("ledger.catalog.memberCount", store.document.activeMemberships.map { it.memberID }.toSet().size),
            color = palette.muted,
            fontSize = 14.sp
        )
        if (store.document.activeGroups.isEmpty()) {
            EmptyCatalogState("ledger.catalog.emptyMembers", Icons.Filled.GroupAdd)
        }
        val groups = store.document.activeGroups
            .filter { group -> matches(group.name) || membershipsFor(group).isNotEmpty() }
            .sortedWith { left, right -> kitStandardSort(left.name, right.name) }
        for (group in groups) {
            val affiliations = membershipsFor(group)
            Column(modifier = Modifier.fillMaxWidth().background(palette.surface, RoundedCornerShape(16.dp)).padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        modifier = Modifier.weight(1f).clickable { expanded[group.id] = !(expanded[group.id] ?: false) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.KeyboardArrowDown,
                            contentDescription = null,
                            tint = palette.muted,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(group.name, color = palette.ink, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            affiliations.size.toString(),
                            color = palette.accentText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(palette.accent.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 9.dp, vertical = 5.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(palette.accent.copy(alpha = 0.1f), CircleShape)
                            .clickable { onSheet(CatalogSheet.Group(group)) },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = L10n.text("ledger.group.edit"), tint = palette.accentText, modifier = Modifier.size(17.dp))
                    }
                }
                if (expanded[group.id] == true) {
                    Spacer(modifier = Modifier.height(12.dp))
                    if (affiliations.isEmpty()) {
                        Text(L10n.text("ledger.catalog.noGroupMembers"), color = palette.muted, fontSize = 14.sp)
                    }
                    for (affiliation in affiliations) {
                        val member = store.document.activeMembers.firstOrNull { it.id == affiliation.memberID } ?: continue
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSheet(CatalogSheet.Member(affiliation)) }
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            LedgerAvatar(member, affiliation.color, 46.dp)
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(member.name, color = palette.ink, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                                    if (member.isFavorited) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Filled.Star, contentDescription = L10n.text("ledger.member.favorite"), tint = palette.accentText, modifier = Modifier.size(13.dp))
                                    }
                                }
                                if (store.document.activeMemberships.count { it.memberID == member.id } > 1) {
                                    Text(L10n.text("ledger.member.shared"), color = palette.muted, fontSize = 12.sp)
                                }
                                affiliation.prices.firstOrNull()?.let { price ->
                                    LedgerCurrency.fromCode(price.currency)?.let { currency ->
                                        Text(currency.format(price.unitPriceMinor, L10n.currentLocale()), color = palette.muted, fontSize = 12.sp)
                                    }
                                }
                            }
                            Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(15.dp))
                        }
                    }
                }
            }
        }
        Button(
            onClick = { onSheet(CatalogSheet.Member(null)) },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(L10n.text("ledger.member.add"), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun ProductCatalog(store: LedgerStore, search: String, onSheet: (CatalogSheet) -> Unit) {
    val palette = kitPalette()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        for (product in store.document.products.filter { com.oshicue.kit.ui.common.kitStandardContains(LedgerPresentation.productDisplayName(it), search) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(16.dp))
                    .clickable { onSheet(CatalogSheet.Product(product)) }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Collections, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(10.dp))
                Text(LedgerPresentation.productDisplayName(product), color = palette.ink, fontSize = 15.sp)
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.muted, modifier = Modifier.size(15.dp))
            }
        }
        Button(
            onClick = { onSheet(CatalogSheet.Product(null)) },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(L10n.text("ledger.product.add"), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun EventCatalog(store: LedgerStore, search: String, onSheet: (CatalogSheet) -> Unit) {
    val palette = kitPalette()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (store.document.events.isEmpty()) {
            EmptyCatalogState("ledger.catalog.emptyEvents", Icons.Filled.ConfirmationNumber)
        }
        for (event in store.document.events.filter {
            com.oshicue.kit.ui.common.kitStandardContains(
                listOf(it.name, it.city ?: "", it.venue ?: "").joinToString(" "),
                search
            )
        }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.surface, RoundedCornerShape(16.dp))
                    .clickable { onSheet(CatalogSheet.Event(event)) }
                    .padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.ConfirmationNumber, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(18.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(event.name, color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    val detail = listOfNotNull(event.date, event.city, event.venue).joinToString(" · ")
                    if (detail.isNotEmpty()) Text(detail, color = palette.muted, fontSize = 12.sp)
                }
                Icon(Icons.Filled.Edit, contentDescription = null, tint = palette.muted, modifier = Modifier.size(15.dp))
            }
        }
        Button(
            onClick = { onSheet(CatalogSheet.Event(null)) },
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = palette.accentFill, contentColor = palette.onAccent),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) { Text(L10n.text("ledger.event.add"), fontWeight = FontWeight.SemiBold) }
    }
}

@Composable
private fun EmptyCatalogState(key: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val palette = kitPalette()
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = palette.accentText, modifier = Modifier.size(32.dp))
        Text(L10n.text(key), color = palette.muted, fontSize = 14.sp)
    }
}

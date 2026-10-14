package com.oshicue.kit.core

/** Catalog pickers exclude deleted entries. Archived identities remain available to historical records. */
val LedgerDocument.activeGroups: List<LedgerGroup> get() = groups.filter { it.isActive }
val LedgerDocument.activeMembers: List<LedgerMember> get() = members.filter { it.isActive }

val LedgerDocument.activeMemberships: List<LedgerMembership>
    get() {
        val groupIDs = activeGroups.map { it.id }.toSet()
        val memberIDs = activeMembers.map { it.id }.toSet()
        return memberships.filter { it.isActive && it.groupID in groupIDs && it.memberID in memberIDs }
    }

/** Deleting a person applies to every group, while leaving past purchases and prices intact. */
fun LedgerDocument.deletingMember(id: String): LedgerDocument {
    val index = members.indexOfFirst { it.id == id }
    if (index < 0) return this
    val nextMembers = members.toMutableList().also { it[index] = it[index].copy(isArchived = true) }
    val nextMemberships = memberships.map { if (it.memberID == id) it.copy(isArchived = true) else it }
    return copy(members = nextMembers, memberships = nextMemberships)
}

/** Group deletion affects its affiliations, without deleting a shared member in another group. */
fun LedgerDocument.deletingGroup(id: String): LedgerDocument {
    val index = groups.indexOfFirst { it.id == id }
    if (index < 0) return this
    val nextGroups = groups.toMutableList().also { it[index] = it[index].copy(isArchived = true) }
    val nextMemberships = memberships.map { if (it.groupID == id) it.copy(isArchived = true) else it }
    return copy(groups = nextGroups, memberships = nextMemberships)
}

/** Archive selected people across all groups while retaining purchase snapshots. */
fun LedgerDocument.deletingMembers(ids: Set<String>): LedgerDocument =
    ids.sorted().fold(this) { document, id -> document.deletingMember(id) }

/** Archive only selected groups and their affiliations, retaining shared people. */
fun LedgerDocument.deletingGroups(ids: Set<String>): LedgerDocument =
    ids.sorted().fold(this) { document, id -> document.deletingGroup(id) }

/** Catalog identities, events and immutable snapshots of remaining purchases survive. */
fun LedgerDocument.deletingRecords(ids: Set<String>): LedgerDocument =
    copy(records = records.filter { it.id !in ids })

package com.oshicue.kit.core

/** An archived person never appears among favorites, even if their preference is retained in a backup. */
val LedgerDocument.favoriteMembers: List<LedgerMember>
    get() = activeMembers.filter { it.isFavorited }.sortedWith { left, right -> memberComparison(left, right) }

/** Used when linking an existing person to another group. Each person appears only once. */
val LedgerDocument.membersForSelection: List<LedgerMember>
    get() = activeMembers.sortedWith { left, right ->
        if (left.isFavorited != right.isFavorited) {
            if (left.isFavorited) -1 else 1
        } else {
            memberComparison(left, right)
        }
    }

/** Each active affiliation remains a distinct choice with its own group color and price list. */
val LedgerDocument.favoriteMemberships: List<LedgerMembership>
    get() {
        val favoriteIDs = favoriteMembers.map { it.id }.toSet()
        return activeMemberships.filter { it.memberID in favoriteIDs }
            .sortedWith { left, right -> membershipComparison(left, right) }
    }

/** Existing purchases keep their original identity, including archived members or groups. */
fun LedgerDocument.preferredRecordMembership(editing: LedgerRecord? = null): LedgerMembership? {
    if (editing != null) {
        val original = memberships.firstOrNull { it.memberID == editing.memberID && it.groupID == editing.groupID }
        if (original != null) return original
    }
    return favoriteMemberships.firstOrNull() ?: activeMemberships.firstOrNull()
}

fun LedgerDocument.recordMembershipChoices(editing: LedgerRecord? = null): List<LedgerMembership> {
    val choices = activeMemberships.toMutableList()
    if (editing != null) {
        val original = memberships.firstOrNull { it.memberID == editing.memberID && it.groupID == editing.groupID }
        if (original != null && choices.none { it.id == original.id }) choices.add(original)
    }
    return choices
}

/** Favorite shortcuts do not replace the complete, grouped catalog below them. */
fun LedgerDocument.recordMembershipGroups(editing: LedgerRecord? = null): List<LedgerGroup> {
    val groupIDs = recordMembershipChoices(editing).map { it.groupID }.toSet()
    return groups.filter { it.id in groupIDs }.sortedWith { left, right ->
        val comparison = KitText.standardCompare(left.name, right.name)
        if (comparison == 0) left.id.compareTo(right.id) else comparison
    }
}

fun LedgerDocument.recordMembershipChoices(inGroup: String, editing: LedgerRecord? = null): List<LedgerMembership> =
    recordMembershipChoices(editing).filter { it.groupID == inGroup }
        .sortedWith { left, right -> membershipComparison(left, right) }

private fun LedgerDocument.memberComparison(left: LedgerMember, right: LedgerMember): Int {
    val comparison = KitText.standardCompare(left.name, right.name)
    return if (comparison == 0) left.id.compareTo(right.id) else comparison
}

private fun LedgerDocument.membershipComparison(left: LedgerMembership, right: LedgerMembership): Int {
    val leftName = members.firstOrNull { it.id == left.memberID }?.name ?: ""
    val rightName = members.firstOrNull { it.id == right.memberID }?.name ?: ""
    val memberComparison = KitText.standardCompare(leftName, rightName)
    if (memberComparison != 0) return memberComparison
    val leftGroup = groups.firstOrNull { it.id == left.groupID }?.name ?: ""
    val rightGroup = groups.firstOrNull { it.id == right.groupID }?.name ?: ""
    val groupComparison = KitText.standardCompare(leftGroup, rightGroup)
    return if (groupComparison == 0) left.id.compareTo(right.id) else groupComparison
}

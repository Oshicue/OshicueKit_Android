package com.oshicue.kit

import com.oshicue.kit.core.LedgerDemo
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.activeGroups
import com.oshicue.kit.core.activeMembers
import com.oshicue.kit.core.activeMemberships
import com.oshicue.kit.core.deletingGroups
import com.oshicue.kit.core.deletingMembers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LedgerCatalogManagementTests {
    @Test
    fun testBatchMemberCleanupArchivesEveryAffiliationWithoutChangingHistory() {
        val original = LedgerDemo.document()
        val result = original.deletingMembers(setOf("mio", "rina"))
        assertEquals(setOf("sora"), result.activeMembers.map { it.id }.toSet())
        assertEquals(setOf("sora"), result.activeMemberships.map { it.memberID }.toSet())
        assertEquals(original.records, result.records)
        assertEquals(original.groups, result.groups)
        assertEquals(original.products, result.products)
        assertEquals(original.events, result.events)
        LedgerImporter.validate(result)
        assertEquals(result, LedgerImporter.parse(LedgerImporter.encode(result)))
    }

    @Test
    fun testBatchGroupCleanupKeepsPeopleAndOtherGroupAffiliations() {
        val original = LedgerDemo.document()
        val result = original.deletingGroups(setOf("luna", "unknown"))
        assertEquals(setOf("prism"), result.activeGroups.map { it.id }.toSet())
        assertEquals(original.members, result.members)
        assertEquals(original.records, result.records)
        assertTrue(result.activeMemberships.any { it.id == "mio-prism" })
        assertFalse(result.activeMemberships.any { it.id == "mio-luna" })
        LedgerImporter.validate(result)
    }

    @Test
    fun testEmptyUnknownAndRepeatedCleanupAreIdempotent() {
        val original = LedgerDemo.document()
        assertEquals(original, original.deletingMembers(emptySet()))
        assertEquals(original, original.deletingGroups(setOf("unknown")))
        val cleaned = original.deletingMembers(setOf("mio"))
        assertEquals(cleaned, cleaned.deletingMembers(setOf("mio", "unknown")))
        val allGroups = original.deletingGroups(original.groups.map { it.id }.toSet())
        assertTrue(allGroups.activeGroups.isEmpty())
        assertTrue(allGroups.activeMemberships.isEmpty())
        assertEquals(original.records, allGroups.records)
    }
}

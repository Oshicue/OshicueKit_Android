package com.oshicue.kit

import com.oshicue.kit.core.JObj
import com.oshicue.kit.core.JsonText
import com.oshicue.kit.core.KitJson
import com.oshicue.kit.core.LedgerAnalytics
import com.oshicue.kit.core.LedgerCurrency
import com.oshicue.kit.core.LedgerDates
import com.oshicue.kit.core.LedgerDemo
import com.oshicue.kit.core.LedgerDocument
import com.oshicue.kit.core.LedgerError
import com.oshicue.kit.core.LedgerGrouping
import com.oshicue.kit.core.LedgerImporter
import com.oshicue.kit.core.LedgerMetric
import com.oshicue.kit.core.LedgerPeriod
import com.oshicue.kit.core.ScheduleDates
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import java.io.File
import java.util.Base64
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LedgerTests {
    private val demo: LedgerDocument
        get() = LedgerDemo.document(ScheduleDates.parseTimestamp("2026-10-14T12:00:00+08:00")!!)

    private fun assertError(key: String, path: String? = null, operation: () -> Unit) {
        val error = assertFailsWith<LedgerError> { operation() }
        assertEquals("ledger.error.$key", error.key)
        if (path != null) assertEquals(path, error.path)
        assertTrue(error.detail.isNotEmpty())
    }

    private fun JsonObject.edited(edit: (MutableMap<String, kotlinx.serialization.json.JsonElement>) -> Unit): String {
        val map = this.toMutableMap()
        edit(map)
        return JsonText.encode(JsonObject(map))
    }

    @Test
    fun testEmptyLedgerDemoAndTemplateRoundTrip() {
        LedgerImporter.validate(LedgerDocument())
        LedgerImporter.validate(demo)
        assertEquals(demo, LedgerImporter.parse(LedgerImporter.encode(demo)))
        val template = LedgerImporter.parse(File("src/main/assets/ledger-template.json").readText())
        assertEquals(2, template.memberships.filter { it.memberID == "mio" }.size)
        assertEquals(3, template.records.size)
    }

    @Test
    fun testCurrencyPrecisionAndOverflowWithoutFloatingPoint() {
        assertEquals(2000L, LedgerCurrency.JPY.parsePrice("2000"))
        assertNull(LedgerCurrency.JPY.parsePrice("2000.01"))
        assertEquals(0, LedgerCurrency.KRW.minorDigits)
        assertEquals(2, LedgerCurrency.IDR.minorDigits)
        assertEquals(8050L, LedgerCurrency.CNY.parsePrice(" 80.50 "))
        assertEquals(10L, LedgerCurrency.CNY.parsePrice("0.1"))
        assertNull(LedgerCurrency.CNY.parsePrice("1.001"))
        assertNull(LedgerCurrency.CNY.parsePrice("-1"))
        assertNull(LedgerCurrency.CNY.parsePrice("1,000"))
        assertNull(LedgerCurrency.CNY.parsePrice("1e2"))
        assertEquals(Long.MAX_VALUE, LedgerCurrency.JPY.parsePrice(Long.MAX_VALUE.toString()))
        assertEquals(Long.MAX_VALUE, LedgerCurrency.USD.parsePrice("92233720368547758.07"))
        assertNull(LedgerCurrency.USD.parsePrice("92233720368547758.08"))
        assertNull(LedgerCurrency.JPY.parsePrice("9223372036854775808"))
        assertEquals("80.5", LedgerCurrency.CNY.priceText(8050))
    }

    @Test
    fun testCivilDatesLeapDaysAndTimezoneIndependence() {
        assertTrue(LedgerDates.parse("2024-02-29") != null)
        for (date in listOf("2026-02-29", "2026-02-30", "2026-13-01", "2026-00-01", "2026-10-00", "2026-10-32", "0000-01-01", "2026-1-01", "2026-10-14T00:00:00Z")) {
            assertNull(LedgerDates.parse(date), date)
        }
        val now = ScheduleDates.parseTimestamp("2026-10-14T01:00:00+08:00")!!
        assertEquals("2026-10-14", LedgerDates.string(now, java.time.ZoneId.of("Asia/Shanghai")))
        assertEquals("2026-10-13", LedgerDates.string(now, java.time.ZoneId.of("America/Los_Angeles")))
        assertEquals(3, LedgerAnalytics.filtered(demo.records, LedgerPeriod.day, "2026-10-14").size)
    }

    @Test
    fun testDailyProductBreakdownAndCurrencySeparation() {
        val daily = LedgerAnalytics.filtered(demo.records, LedgerPeriod.day, "2026-10-14")
        val summary = LedgerAnalytics.summarize(daily)
        assertEquals(10, summary.quantity)
        assertEquals(2, summary.memberCount)
        assertEquals(mapOf("JPY" to 23000L), summary.amountsByCurrency)
        assertEquals(8, summary.productRows.first { it.id == "cheki" }.quantity)
        assertEquals(mapOf("JPY" to 7000L), summary.productRows.first { it.id == "wide-cheki" }.amountsByCurrency)
        val lifetime = LedgerAnalytics.summarize(demo.records)
        assertEquals(16, lifetime.quantity)
        assertEquals(mapOf("JPY" to 33000L, "CNY" to 16000L), lifetime.amountsByCurrency)
        assertEquals(1, LedgerAnalytics.filtered(demo.records, LedgerPeriod.month, "2026-10-01", currency = "CNY").size)
    }

    @Test
    fun testSameMemberCombinedAndSplitByGroup() {
        val combined = LedgerAnalytics.rankings(demo.records, LedgerGrouping.memberCombined, LedgerMetric.quantity)
        assertEquals(3, combined.size)
        assertEquals("mio", combined[0].memberID)
        assertNull(combined[0].groupID)
        assertEquals(11, combined[0].quantity)
        assertEquals("LUNA NOTE / PRISM DAYS", combined[0].subtitle)
        assertEquals("#F28AAF", combined[0].color)
        val split = LedgerAnalytics.rankings(demo.records, LedgerGrouping.memberByGroup, LedgerMetric.quantity)
        assertEquals(4, split.size)
        assertEquals(listOf(4, 7), split.filter { it.memberID == "mio" }.map { it.quantity }.sorted())
        val groups = LedgerAnalytics.rankings(demo.records, LedgerGrouping.group, LedgerMetric.quantity)
        assertEquals(listOf(10, 6), groups.map { it.quantity })
        assertTrue(groups.all { it.memberID == null && it.groupID != null })
    }

    @Test
    fun testSpendingRankingRequiresOneCurrency() {
        assertError("currencyRequired") {
            LedgerAnalytics.rankings(demo.records, LedgerGrouping.memberCombined, LedgerMetric.spending)
        }
        val yen = LedgerAnalytics.rankings(demo.records, LedgerGrouping.memberCombined, LedgerMetric.spending, currency = "JPY")
        assertEquals(listOf("mio", "rina"), yen.map { it.memberID })
        assertEquals(mapOf("JPY" to 27000L), yen[0].amountsByCurrency)
        assertTrue(abs(yen[0].share - 27000.0 / 33000) < 0.000001)
        val yuan = LedgerAnalytics.rankings(demo.records, LedgerGrouping.memberCombined, LedgerMetric.spending, currency = "CNY")
        assertEquals(1, yuan.size)
        assertEquals("sora", yuan[0].memberID)
        assertError("currency") {
            LedgerAnalytics.rankings(demo.records, LedgerGrouping.memberCombined, LedgerMetric.quantity, currency = "EUR")
        }
    }

    @Test
    fun testCompetitionRanksDeterministicTiesAndZeroSpending() {
        var records = demo.records.filter { it.id != "demo-shared" && it.id != "demo-wide" }
        records = records.toMutableList().also { it[0] = it[0].copy(quantity = 3) }
        val rows = LedgerAnalytics.rankings(records, LedgerGrouping.memberCombined, LedgerMetric.quantity)
        assertEquals(listOf(1, 1, 3), rows.map { it.rank })
        assertEquals(listOf("mio", "rina", "sora"), rows.map { it.memberID })
        assertEquals(rows, LedgerAnalytics.rankings(records.reversed(), LedgerGrouping.memberCombined, LedgerMetric.quantity))
        records = records.map { it.copy(unitPriceMinor = 0, currency = "JPY") }
        val free = LedgerAnalytics.rankings(records, LedgerGrouping.memberCombined, LedgerMetric.spending, currency = "JPY")
        assertTrue(free.all { it.share == 0.0 && it.rank == 1 })
    }

    @Test
    fun testPeriodBoundariesAndLifetime() {
        var records = MutableList(4) { demo.records[0] }
        for (index in records.indices) records[index] = records[index].copy(id = "date-$index")
        records[0] = records[0].copy(date = "2025-12-31")
        records[1] = records[1].copy(date = "2026-01-01")
        records[2] = records[2].copy(date = "2026-01-31")
        records[3] = records[3].copy(date = "2026-02-01")
        assertEquals(3, LedgerAnalytics.filtered(records, LedgerPeriod.year, "2026-12-31").size)
        assertEquals(2, LedgerAnalytics.filtered(records, LedgerPeriod.month, "2026-01-14").size)
        assertEquals(1, LedgerAnalytics.filtered(records, LedgerPeriod.day, "2026-02-01").size)
        assertEquals(4, LedgerAnalytics.filtered(records, LedgerPeriod.lifetime, "").size)
        assertTrue(LedgerAnalytics.filtered(records, LedgerPeriod.month, "2026-02-30").isEmpty())
    }

    @Test
    fun testCatalogEditsDoNotRewriteSnapshotsOrHistoricalPrice() {
        val edited = demo.copy(
            members = demo.members.toMutableList().also { it[0] = it[0].copy(name = "New Mio") },
            groups = demo.groups.toMutableList().also { it[0] = it[0].copy(name = "New Group") },
            memberships = demo.memberships.toMutableList().also {
                it[0] = it[0].copy(color = "#000000", prices = it[0].prices.toMutableList().also { prices ->
                    prices[0] = prices[0].copy(unitPriceMinor = 9000)
                })
            },
            products = demo.products.toMutableList().also { it[0] = it[0].copy(name = "New product name") }
        )
        LedgerImporter.validate(edited)
        assertEquals(2000L, edited.records[0].unitPriceMinor)
        assertEquals("Mio", edited.records[0].memberNameSnapshot)
        val rows = LedgerAnalytics.rankings(edited.records, LedgerGrouping.memberCombined, LedgerMetric.quantity)
        assertEquals("Mio", rows[0].name)
        assertEquals("LUNA NOTE / PRISM DAYS", rows[0].subtitle)
        assertEquals("#F28AAF", rows[0].color)
        assertEquals(27000L, rows[0].amountsByCurrency["JPY"])
    }

    @Test
    fun testMergeUpsertsCatalogAndIsIdempotentWithNewPurchases() {
        var incoming = demo.copy(
            groups = demo.groups.toMutableList().also { it[0] = it[0].copy(name = "Renamed catalog group") }
        )
        val additional = incoming.records[0].copy(id = "new-purchase")
        incoming = incoming.copy(records = incoming.records + additional)
        val merged = demo.merging(incoming)
        assertEquals(demo.records.size + 1, merged.records.size)
        assertEquals("Renamed catalog group", merged.groups.first { it.id == "luna" }.name)
        assertEquals("LUNA NOTE", merged.records[0].groupNameSnapshot)
        assertEquals(merged, merged.merging(incoming))
    }

    @Test
    fun testMergeConflictingRecordAndInvalidCatalogAreAtomicFailures() {
        var incoming = demo.copy(
            records = demo.records.toMutableList().also { it[0] = it[0].copy(quantity = it[0].quantity + 1) }
        )
        assertError("recordConflict") { demo.merging(incoming) }
        incoming = demo.copy(
            memberships = demo.memberships.toMutableList().also { it[0] = it[0].copy(memberID = "missing") }
        )
        assertError("reference", "$.memberships[0].memberID") { demo.merging(incoming) }
        assertEquals(5, demo.records[0].quantity)
    }

    @Test
    fun testInvalidReferencesMembershipDateCurrencyAndSnapshots() {
        var document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(memberID = "missing") })
        assertError("reference", "$.records[0].memberID") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(groupID = "prism", memberID = "rina") })
        assertError("membership") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(eventID = "missing") })
        assertError("reference", "$.records[0].eventID") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(date = "2026-02-29") })
        assertError("date", "$.records[0].date") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(currency = "jpy") })
        assertError("currency", "$.records[0].currency") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(colorSnapshot = "pink") })
        assertError("color", "$.records[0].colorSnapshot") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(memberNameSnapshot = " \n ") })
        assertError("emptyField", "$.records[0].memberNameSnapshot") { LedgerImporter.validate(document) }
    }

    @Test
    fun testDuplicateCatalogMembershipAndPricesAreRejected() {
        var document = demo.copy(groups = demo.groups + demo.groups[0])
        assertError("duplicateID", "$.groups[2].id") { LedgerImporter.validate(document) }
        document = demo.copy(memberships = demo.memberships + demo.memberships[0].copy(id = "duplicate-pair"))
        assertError("membership") { LedgerImporter.validate(document) }
        document = demo.copy(
            memberships = demo.memberships.toMutableList().also {
                it[0] = it[0].copy(prices = it[0].prices + it[0].prices[0])
            }
        )
        assertError("duplicateID") { LedgerImporter.validate(document) }
    }

    @Test
    fun testQuantityPriceAndRecordOrAggregateOverflowAreRejected() {
        var document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(quantity = 0) })
        assertError("quantity") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(unitPriceMinor = -1) })
        assertError("price") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(quantity = 2, unitPriceMinor = Long.MAX_VALUE) })
        assertNull(document.records[0].totalMinor)
        assertError("overflow") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(quantity = 1, unitPriceMinor = Long.MAX_VALUE) })
        assertError("overflow") { LedgerImporter.validate(document) }
        document = demo.copy(records = demo.records.toMutableList().also { it[0] = it[0].copy(quantity = Int.MAX_VALUE, unitPriceMinor = 0) })
        assertError("overflow") { LedgerImporter.validate(document) }
        assertError("overflow") { LedgerAnalytics.summarize(document.records) }
    }

    @Test
    fun testJSONErrorsPointToTheFieldAndAllowUnknownMetadata() {
        val encoded = LedgerImporter.encode(demo)
        val root = KitJson.parse(encoded)!!.jsonObject
        val withMetadata = root.edited { it["futureMetadata"] = JObj().apply { put("revision", 2) }.build() }
        LedgerImporter.parse(withMetadata)
        val badSchema = root.edited { it["schemaVersion"] = JsonPrimitive(2) }
        assertError("schemaVersion", "$.schemaVersion") { LedgerImporter.parse(badSchema) }
        val missingMembers = root.edited { it.remove("members") }
        assertError("missingField", "$.members") { LedgerImporter.parse(missingMembers) }
        val wrongMembers = root.edited { it["members"] = JsonPrimitive("bad") }
        assertError("fieldType", "$.members") { LedgerImporter.parse(wrongMembers) }
        assertError("invalidJSON", "$") { LedgerImporter.parse("not JSON") }
    }

    @Test
    fun testImageAndImportSizeBudgets() {
        val jpegFixture = "/9j/4AAQSkZJRgABAQAASABIAAD/4QBMRXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAABAAEAAKACAAQAAAABAAAAAaADAAQAAAABAAAAAQAAAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAAA4QklNBCUAAAAAABDUHYzZjwCyBOmACZjs+EJ+/8AAEQgAAQABAwEiAAIRAQMRAf/EAB8AAAEFAQEBAQEBAAAAAAAAAAABAgMEBQYHCAkKC//EALUQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+v/EAB8BAAMBAQEBAQEBAQEAAAAAAAABAgMEBQYHCAkKC//EALURAAIBAgQEAwQHBQQEAAECdwABAgMRBAUhMQYSQVEHYXETIjKBCBRCkaGxwQkjM1LwFWJy0QoWJDThJfEXGBkaJicoKSo1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoKDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uLj5OXm5+jp6vLz9PX29/j5+v/bAEMAAgICAgICAwICAwUDAwMFBgUFBQUGCAYGBgYGCAoICAgICAgKCgoKCgoKCgwMDAwMDA4ODg4ODw8PDw8PDw8PD//bAEMBAgICBAQEBwQEBxALCQsQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEP/dAAQAAf/aAAwDAQACEQMRAD8A/n/ooooA/9k="
        var document = demo.copy(
            members = demo.members.toMutableList().also { it[0] = it[0].copy(avatarJPEGBase64 = jpegFixture) }
        )
        LedgerImporter.validate(document)
        document = demo.copy(
            members = demo.members.toMutableList().also {
                it[0] = it[0].copy(avatarJPEGBase64 = Base64.getEncoder().encodeToString("not an image".toByteArray()))
            }
        )
        assertError("avatar", "$.members[0].avatarJPEGBase64") { LedgerImporter.validate(document) }
        document = demo.copy(
            members = demo.members.toMutableList().also {
                it[0] = it[0].copy(avatarJPEGBase64 = Base64.getEncoder().encodeToString(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())))
            }
        )
        assertError("avatar") { LedgerImporter.validate(document) }
        document = demo.copy(
            members = demo.members.toMutableList().also {
                it[0] = it[0].copy(avatarJPEGBase64 = "A".repeat(3 * 1024 * 1024))
            }
        )
        assertError("avatarSize") { LedgerImporter.validate(document) }
        assertError("documentSize") { LedgerImporter.parse(" ".repeat(LedgerImporter.maximumDocumentBytes + 1)) }
    }
}

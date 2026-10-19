package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.time.ZoneId
import java.util.Base64

/** JVM port of the iOS Tests/LedgerTests.swift suite. */
class LedgerTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    private val demo: LedgerDocument
        get() = LedgerDemo.document(KitDate.parseTimestamp("2026-10-14T12:00:00+08:00")!!)

    private fun assertError(key: String, path: String? = null, operation: () -> Unit) {
        val error = assertThrows(Throwable::class.java) { operation() }
        val failure = error as? LedgerError ?: throw AssertionError("Expected LedgerError: $error", error)
        assertEquals("ledger.error.$key", failure.key)
        if (path != null) assertEquals(path, failure.path)
        assertFalse(failure.detail.isEmpty())
    }

    @Test
    fun testEmptyLedgerDemoAndTemplateRoundTrip() {
        LedgerImporter.validate(LedgerDocument())
        LedgerImporter.validate(demo)
        assertEquals(demo, LedgerImporter.parse(LedgerImporter.encode(demo)))
        // iOS reads the repository's Templates/ledger-template.json; Android ships the same file as an asset.
        val template = LedgerImporter.parse(templateFile("ledger-template.json").readText(Charsets.UTF_8))
        assertEquals(2, template.memberships.filter { it.memberId == "mio" }.size)
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
        assertNotNull(LedgerDates.parse("2024-02-29"))
        for (date in listOf(
            "2026-02-29", "2026-02-30", "2026-13-01", "2026-00-01", "2026-10-00", "2026-10-32",
            "0000-01-01", "2026-1-01", "2026-10-14T00:00:00Z"
        )) {
            assertNull(date, LedgerDates.parse(date))
        }
        val now = KitDate.parseTimestamp("2026-10-14T01:00:00+08:00")
        assertNotNull(now)
        assertEquals("2026-10-14", LedgerDates.string(now!!, ZoneId.of("Asia/Shanghai")))
        assertEquals("2026-10-13", LedgerDates.string(now, ZoneId.of("America/Los_Angeles")))
        val purchasedDay = "2026-10-14"
        assertEquals(3, LedgerAnalytics.filtered(demo.records, LedgerPeriod.Day, purchasedDay).size)
    }

    @Test
    fun testDailyProductBreakdownAndCurrencySeparation() {
        val daily = LedgerAnalytics.filtered(demo.records, LedgerPeriod.Day, "2026-10-14")
        val summary = LedgerAnalytics.summarize(daily)
        assertEquals(10, summary.quantity)
        assertEquals(2, summary.memberCount)
        assertEquals(mapOf("JPY" to 23000L), summary.amountsByCurrency)
        assertEquals(8, summary.productRows.firstOrNull { it.id == "cheki" }?.quantity)
        assertEquals(mapOf("JPY" to 7000L), summary.productRows.firstOrNull { it.id == "wide-cheki" }?.amountsByCurrency)
        val lifetime = LedgerAnalytics.summarize(demo.records)
        assertEquals(16, lifetime.quantity)
        assertEquals(mapOf("JPY" to 33000L, "CNY" to 16000L), lifetime.amountsByCurrency)
        assertEquals(1, LedgerAnalytics.filtered(demo.records, LedgerPeriod.Month, "2026-10-01", currency = "CNY").size)
    }

    @Test
    fun testSameMemberCombinedAndSplitByGroup() {
        val combined = LedgerAnalytics.rankings(demo.records, LedgerGrouping.MemberCombined, LedgerMetric.Quantity)
        assertEquals(3, combined.size)
        assertEquals("mio", combined[0].memberId)
        assertNull(combined[0].groupId)
        assertEquals(11, combined[0].quantity)
        assertEquals("LUNA NOTE / PRISM DAYS", combined[0].subtitle)
        assertEquals("#F28AAF", combined[0].color)
        val split = LedgerAnalytics.rankings(demo.records, LedgerGrouping.MemberByGroup, LedgerMetric.Quantity)
        assertEquals(4, split.size)
        assertEquals(listOf(4, 7), split.filter { it.memberId == "mio" }.map { it.quantity }.sorted())
        val groups = LedgerAnalytics.rankings(demo.records, LedgerGrouping.Group, LedgerMetric.Quantity)
        assertEquals(listOf(10, 6), groups.map { it.quantity })
        assertTrue(groups.all { it.memberId == null && it.groupId != null })
    }

    @Test
    fun testSpendingRankingRequiresOneCurrency() {
        assertError("currencyRequired") {
            LedgerAnalytics.rankings(demo.records, LedgerGrouping.MemberCombined, LedgerMetric.Spending)
        }
        val yen = LedgerAnalytics.rankings(demo.records, LedgerGrouping.MemberCombined, LedgerMetric.Spending, currency = "JPY")
        assertEquals(listOf("mio", "rina"), yen.map { it.memberId })
        assertEquals(mapOf("JPY" to 27000L), yen[0].amountsByCurrency)
        assertEquals(27000.0 / 33000, yen[0].share, 0.000001)
        val yuan = LedgerAnalytics.rankings(demo.records, LedgerGrouping.MemberCombined, LedgerMetric.Spending, currency = "CNY")
        assertEquals(1, yuan.size)
        assertEquals("sora", yuan[0].memberId)
        assertError("currency") {
            LedgerAnalytics.rankings(demo.records, LedgerGrouping.MemberCombined, LedgerMetric.Quantity, currency = "EUR")
        }
    }

    @Test
    fun testCompetitionRanksDeterministicTiesAndZeroSpending() {
        var records = demo.records.filter { it.id != "demo-shared" && it.id != "demo-wide" }
        records = records.replacing(0) { it.copy(quantity = 3) }
        val rows = LedgerAnalytics.rankings(records, LedgerGrouping.MemberCombined, LedgerMetric.Quantity)
        assertEquals(listOf(1, 1, 3), rows.map { it.rank })
        assertEquals(listOf("mio", "rina", "sora"), rows.map { it.memberId })
        assertEquals(rows, LedgerAnalytics.rankings(records.reversed(), LedgerGrouping.MemberCombined, LedgerMetric.Quantity))
        records = records.map { it.copy(unitPriceMinor = 0, currency = "JPY") }
        val free = LedgerAnalytics.rankings(records, LedgerGrouping.MemberCombined, LedgerMetric.Spending, currency = "JPY")
        assertTrue(free.all { it.share == 0.0 && it.rank == 1 })
    }

    @Test
    fun testPeriodBoundariesAndLifetime() {
        var records = List(4) { demo.records[0] }
        records = records.mapIndexed { index, record -> record.copy(id = "date-$index") }
        records = records.replacing(0) { it.copy(date = "2025-12-31") }.replacing(1) { it.copy(date = "2026-01-01") }
        records = records.replacing(2) { it.copy(date = "2026-01-31") }.replacing(3) { it.copy(date = "2026-02-01") }
        assertEquals(3, LedgerAnalytics.filtered(records, LedgerPeriod.Year, "2026-12-31").size)
        assertEquals(2, LedgerAnalytics.filtered(records, LedgerPeriod.Month, "2026-01-14").size)
        assertEquals(1, LedgerAnalytics.filtered(records, LedgerPeriod.Day, "2026-02-01").size)
        assertEquals(4, LedgerAnalytics.filtered(records, LedgerPeriod.Lifetime, "").size)
        assertTrue(LedgerAnalytics.filtered(records, LedgerPeriod.Month, "2026-02-30").isEmpty())
    }

    @Test
    fun testCatalogEditsDoNotRewriteSnapshotsOrHistoricalPrice() {
        var edited = demo
        edited = edited.copy(members = edited.members.replacing(0) { it.copy(name = "New Mio") })
        edited = edited.copy(groups = edited.groups.replacing(0) { it.copy(name = "New Group") })
        edited = edited.copy(memberships = edited.memberships.replacing(0) { membership ->
            membership.copy(
                color = "#000000",
                prices = membership.prices.replacing(0) { it.copy(unitPriceMinor = 9000) }
            )
        })
        edited = edited.copy(products = edited.products.replacing(0) { it.copy(name = "New product name") })
        LedgerImporter.validate(edited)
        assertEquals(2000L, edited.records[0].unitPriceMinor)
        assertEquals("Mio", edited.records[0].memberNameSnapshot)
        val rows = LedgerAnalytics.rankings(edited.records, LedgerGrouping.MemberCombined, LedgerMetric.Quantity)
        assertEquals("Mio", rows[0].name)
        assertEquals("LUNA NOTE / PRISM DAYS", rows[0].subtitle)
        assertEquals("#F28AAF", rows[0].color)
        assertEquals(27000L, rows[0].amountsByCurrency["JPY"])
    }

    @Test
    fun testMergeUpsertsCatalogAndIsIdempotentWithNewPurchases() {
        var incoming = demo
        incoming = incoming.copy(groups = incoming.groups.replacing(0) { it.copy(name = "Renamed catalog group") })
        val additional = incoming.records[0].copy(id = "new-purchase")
        incoming = incoming.copy(records = incoming.records + additional)
        val merged = demo.merging(incoming)
        assertEquals(demo.records.size + 1, merged.records.size)
        assertEquals("Renamed catalog group", merged.groups.firstOrNull { it.id == "luna" }?.name)
        assertEquals("LUNA NOTE", merged.records[0].groupNameSnapshot)
        assertEquals(merged, merged.merging(incoming))
    }

    @Test
    fun testMergeConflictingRecordAndInvalidCatalogAreAtomicFailures() {
        var incoming = demo
        incoming = incoming.withRecord(0) { it.copy(quantity = it.quantity + 1) }
        val conflicting = incoming
        assertError("recordConflict") { demo.merging(conflicting) }
        incoming = demo
        incoming = incoming.copy(memberships = incoming.memberships.replacing(0) { it.copy(memberId = "missing") })
        val invalid = incoming
        assertError("reference", path = "$.memberships[0].memberID") { demo.merging(invalid) }
        assertEquals(5, demo.records[0].quantity)
    }

    @Test
    fun testInvalidReferencesMembershipDateCurrencyAndSnapshots() {
        var document = demo.withRecord(0) { it.copy(memberId = "missing") }
        assertError("reference", path = "$.records[0].memberID") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(groupId = "prism", memberId = "rina") }
        assertError("membership") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(eventId = "missing") }
        assertError("reference", path = "$.records[0].eventID") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(date = "2026-02-29") }
        assertError("date", path = "$.records[0].date") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(currency = "jpy") }
        assertError("currency", path = "$.records[0].currency") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(colorSnapshot = "pink") }
        assertError("color", path = "$.records[0].colorSnapshot") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(memberNameSnapshot = " \n ") }
        assertError("emptyField", path = "$.records[0].memberNameSnapshot") { LedgerImporter.validate(document) }
    }

    @Test
    fun testDuplicateCatalogMembershipAndPricesAreRejected() {
        var document = demo
        document = document.copy(groups = document.groups + document.groups[0])
        assertError("duplicateID", path = "$.groups[2].id") { LedgerImporter.validate(document) }
        document = demo
        val membership = document.memberships[0].copy(id = "duplicate-pair")
        document = document.copy(memberships = document.memberships + membership)
        assertError("membership") { LedgerImporter.validate(document) }
        document = demo
        document = document.copy(memberships = document.memberships.replacing(0) { it.copy(prices = it.prices + it.prices[0]) })
        assertError("duplicateID") { LedgerImporter.validate(document) }
    }

    @Test
    fun testQuantityPriceAndRecordOrAggregateOverflowAreRejected() {
        var document = demo.withRecord(0) { it.copy(quantity = 0) }
        assertError("quantity") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(unitPriceMinor = -1) }
        assertError("price") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(quantity = 2, unitPriceMinor = Long.MAX_VALUE) }
        assertNull(document.records[0].totalMinor)
        assertError("overflow") { LedgerImporter.validate(document) }
        document = demo.withRecord(0) { it.copy(quantity = 1, unitPriceMinor = Long.MAX_VALUE) }
        assertError("overflow") { LedgerImporter.validate(document) }
        // Swift's Int.max is 2^63-1; LedgerRecord.quantity is a 32-bit Int on Android, so this is its platform maximum.
        document = demo.withRecord(0) { it.copy(quantity = Int.MAX_VALUE, unitPriceMinor = 0) }
        assertError("overflow") { LedgerImporter.validate(document) }
        assertError("overflow") { LedgerAnalytics.summarize(document.records) }
    }

    @Test
    fun testJSONErrorsPointToTheFieldAndAllowUnknownMetadata() {
        val root = JsonParser.parse(LedgerImporter.encode(demo)) as JsonObject
        root["futureMetadata"] = JsonObject().put("revision", 2)
        fun text(): String = root.serialize()
        LedgerImporter.parse(text())
        root.put("schemaVersion", 2)
        assertError("schemaVersion", path = "$.schemaVersion") { LedgerImporter.parse(text()) }
        root.put("schemaVersion", 1)
        root.fields.remove("members")
        assertError("missingField", path = "$.members") { LedgerImporter.parse(text()) }
        root.put("members", "bad")
        assertError("fieldType", path = "$.members") { LedgerImporter.parse(text()) }
        assertError("invalidJSON", path = "$") { LedgerImporter.parse("not JSON") }
    }

    @Test
    fun testImageAndImportSizeBudgets() {
        var document = demo
        // A real one-pixel JPEG fixture, independent of the user's photo library or local files.
        document = document.withMember(0) { it.copy(avatarJpegBase64 = "/9j/4AAQSkZJRgABAQAASABIAAD/4QBMRXhpZgAATU0AKgAAAAgAAYdpAAQAAAABAAAAGgAAAAAAA6ABAAMAAAABAAEAAKACAAQAAAABAAAAAaADAAQAAAABAAAAAQAAAAD/7QA4UGhvdG9zaG9wIDMuMAA4QklNBAQAAAAAAAA4QklNBCUAAAAAABDUHYzZjwCyBOmACZjs+EJ+/8AAEQgAAQABAwEiAAIRAQMRAf/EAB8AAAEFAQEBAQEBAAAAAAAAAAABAgMEBQYHCAkKC//EALUQAAIBAwMCBAMFBQQEAAABfQECAwAEEQUSITFBBhNRYQcicRQygZGhCCNCscEVUtHwJDNicoIJChYXGBkaJSYnKCkqNDU2Nzg5OkNERUZHSElKU1RVVldYWVpjZGVmZ2hpanN0dXZ3eHl6g4SFhoeIiYqSk5SVlpeYmZqio6Slpqeoqaqys7S1tre4ubrCw8TFxsfIycrS09TV1tfY2drh4uPk5ebn6Onq8fLz9PX29/j5+v/EAB8BAAMBAQEBAQEBAQEAAAAAAAABAgMEBQYHCAkKC//EALURAAIBAgQEAwQHBQQEAAECdwABAgMRBAUhMQYSQVEHYXETIjKBCBRCkaGxwQkjM1LwFWJy0QoWJDThJfEXGBkaJicoKSo1Njc4OTpDREVGR0hJSlNUVVZXWFlaY2RlZmdoaWpzdHV2d3h5eoKDhIWGh4iJipKTlJWWl5iZmqKjpKWmp6ipqrKztLW2t7i5usLDxMXGx8jJytLT1NXW19jZ2uLj5OXm5+jp6vLz9PX29/j5+v/bAEMAAgICAgICAwICAwUDAwMFBgUFBQUGCAYGBgYGCAoICAgICAgKCgoKCgoKCgwMDAwMDA4ODg4ODw8PDw8PDw8PD//bAEMBAgICBAQEBwQEBxALCQsQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEBAQEP/dAAQAAf/aAAwDAQACEQMRAD8A/n/ooooA/9k=") }
        LedgerImporter.validate(document)
        document = document.withMember(0) {
            it.copy(avatarJpegBase64 = Base64.getEncoder().encodeToString("not an image".toByteArray(Charsets.UTF_8)))
        }
        assertError("avatar", path = "$.members[0].avatarJPEGBase64") { LedgerImporter.validate(document) }
        document = document.withMember(0) {
            it.copy(avatarJpegBase64 = Base64.getEncoder().encodeToString(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xD9.toByte())))
        }
        assertError("avatar") { LedgerImporter.validate(document) }
        document = document.withMember(0) { it.copy(avatarJpegBase64 = "A".repeat(3 * 1024 * 1024)) }
        assertError("avatarSize") { LedgerImporter.validate(document) }
        assertError("documentSize") { LedgerImporter.parse(" ".repeat(LedgerImporter.maximumDocumentBytes + 1)) }
    }

    /** Gradle runs unit tests from the module directory; fall back to the project root. */
    private fun templateFile(name: String): File =
        listOf(File("src/main/assets/templates/$name"), File("app/src/main/assets/templates/$name"))
            .firstOrNull { it.isFile }
            ?: throw AssertionError("Template $name not found from ${File("").absolutePath}")

    private fun <T> List<T>.replacing(index: Int, transform: (T) -> T): List<T> =
        mapIndexed { position, item -> if (position == index) transform(item) else item }

    private fun LedgerDocument.withRecord(index: Int, transform: (LedgerRecord) -> LedgerRecord): LedgerDocument =
        copy(records = records.replacing(index, transform))

    private fun LedgerDocument.withMember(index: Int, transform: (LedgerMember) -> LedgerMember): LedgerDocument =
        copy(members = members.replacing(index, transform))
}

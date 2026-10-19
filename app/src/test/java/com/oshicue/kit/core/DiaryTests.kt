package com.oshicue.kit.core

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Port of Tests/DiaryTests.swift. */
class DiaryTests {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    /** Date(timeIntervalSince1970: 1_792_009_200.123). */
    private val timestamp: Instant = Instant.ofEpochSecond(1_792_009_200, 123_000_000)

    private fun entry(id: String = "report-1", body: String = "今日的演出很开心。\n推しの笑顔を覚えている✨"): DiaryEntry =
        DiaryEntry(id = id, title = "Tokyo Live repo", body = body, summary = "Encore特别好", tags = listOf("远征", "LIVE"),
            event = DiaryEventContext(ledgerEventId = "event-1", name = "秋季演出", date = "2026-10-14", city = "東京都", venue = "渋谷ホール", address = "東京都渋谷区1-2-3"),
            createdAt = timestamp, updatedAt = timestamp.plusSeconds(60))

    private fun expectIssue(key: String, path: String? = null, operation: () -> Unit) {
        val failure = runCatching { operation() }.exceptionOrNull()
            ?: return fail("Expected DiaryError $key, but no error was thrown")
        if (failure !is DiaryError) return fail("Expected DiaryError, got $failure")
        assertEquals(key, failure.key)
        if (path != null) assertEquals(path, failure.path)
    }

    /** Re-encodes an exported diary after editing its JSON object, like JSONSerialization in the Swift test. */
    private fun jsonChanging(document: DiaryDocument, edit: (JsonObject) -> Unit): String {
        val obj = JsonParser.parse(DiaryImporter.encode(document)) as? JsonObject
            ?: throw AssertionError("An encoded diary must be a JSON object")
        edit(obj)
        return obj.serialize()
    }

    private fun JsonObject.firstEntry(): JsonObject = (this["entries"] as JsonArray).items[0] as JsonObject

    /** Date.timeIntervalSince1970. */
    private fun epochSeconds(instant: Instant): Double = instant.epochSecond + instant.nano / 1_000_000_000.0

    @Test
    fun testEmptyDiaryAndIncompleteDraftAreValidWithoutLedgerCatalog() {
        assertEquals(DiaryDocument(), DiaryImporter.parse(DiaryImporter.encode(DiaryDocument())))
        val draft = DiaryEntry(id = "empty-draft", event = DiaryEventContext(date = "2026-10-14"), createdAt = timestamp, updatedAt = timestamp)
        assertFalse(draft.isMeaningfulContent)
        assertEquals(listOf(draft), DiaryImporter.parse(DiaryImporter.encode(DiaryDocument(entries = listOf(draft)))).entries)
        val meaningful = draft.copy(body = "先记一句，活动资料稍后补。")
        assertTrue(meaningful.isMeaningfulContent)
        DiaryImporter.validate(DiaryDocument(entries = listOf(meaningful)))
    }

    @Test
    fun testLinkedEventMetadataIsIndependentSnapshot() {
        val event = LedgerEvent(id = "live-1", name = "原始活动", date = "2026-10-14", city = "大阪市", venue = "BIGCAT", address = "大阪市中央区西心斎橋")
        val snapshot = DiaryCore.contextFrom(event, defaultDate = "2026-10-15")
        // Swift mutates the ledger event after taking the snapshot; Kotlin models are immutable, so edit a copy.
        @Suppress("UNUSED_VARIABLE")
        val edited = event.copy(name = "改名后的活动", city = "東京都", date = "2026-12-01")
        assertEquals("live-1", snapshot.ledgerEventId)
        assertEquals("原始活动", snapshot.name)
        assertEquals("2026-10-14", snapshot.date)
        assertEquals("大阪市", snapshot.city)
        val document = DiaryDocument(entries = listOf(DiaryEntry(event = snapshot, createdAt = timestamp, updatedAt = timestamp)))
        DiaryImporter.validate(document) // No ledger event reference is required to preserve the snapshot.
    }

    @Test
    fun testEventWithoutDateUsesSelectedCivilDay() {
        val event = LedgerEvent(id = "unfinished", name = "活动待定", city = "横浜市")
        assertEquals("2027-01-03", DiaryCore.contextFrom(event, defaultDate = "2027-01-03").date)
        val malformed = event.copy(date = "2026-02-30")
        assertEquals("2027-01-03", DiaryCore.contextFrom(malformed, defaultDate = "2027-01-03").date)
    }

    @Test
    fun testLongUnicodeBodyAndWhitespaceRoundTripWithoutTruncation() {
        val body = "第一行\n推しの笑顔✨🇯🇵 한국어 ไทย Bahasa e\u0301 \t \"quote\" \\path\r\n\n".repeat(5_000) + "最后一行  \n"
        val original = DiaryDocument(entries = listOf(entry(body = body)))
        val encoded = DiaryImporter.encode(original)
        val decoded = DiaryImporter.parse(encoded)
        assertEquals(original, decoded)
        assertArrayEquals(body.toByteArray(Charsets.UTF_8), decoded.entries[0].body.toByteArray(Charsets.UTF_8))
        assertTrue(encoded.contains("2026-"))
        assertTrue(encoded.contains("T"))
        assertTrue(encoded.contains("Z"))
    }

    @Test
    fun testExportedNativeDateReimportsWithoutFalseConflict() {
        val report = entry().copy(
            createdAt = Instant.ofEpochSecond(1_792_009_200, 123_456_000), // 1_792_009_200.123456
            updatedAt = Instant.ofEpochSecond(1_792_009_299, 999_900_000) // 1_792_009_299.9999
        )
        val original = DiaryDocument(entries = listOf(report))
        val decoded = DiaryImporter.parse(DiaryImporter.encode(original))
        assertEquals(original, decoded)
        assertEquals(original, original.merging(decoded))
        assertEquals(1_792_009_300.0, epochSeconds(decoded.entries[0].updatedAt), 0.0001)
    }

    @Test
    fun testTimestampOffsetRepresentsTheSameAbsoluteInstant() {
        val original = DiaryDocument(entries = listOf(entry()))
        val changed = jsonChanging(original) { obj ->
            // ISO8601DateFormatter at +09:00 with [.withInternetDateTime, .withFractionalSeconds].
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ROOT)
                .withZone(ZoneOffset.ofHours(9))
            val first = obj.firstEntry()
            first["createdAt"] = JsonString(formatter.format(timestamp))
            first["updatedAt"] = JsonString(formatter.format(timestamp.plusSeconds(60)))
        }
        assertEquals(original, DiaryImporter.parse(changed))
    }

    @Test
    fun testMergeDeduplicatesSameEntryAndAddsNewEntries() {
        val first = entry()
        val original = DiaryDocument(entries = listOf(first))
        val second = entry(id = "report-2", body = "另一场活动")
        val merged = original.merging(DiaryDocument(entries = listOf(first, second)))
        assertEquals(listOf("report-1", "report-2"), merged.entries.map { it.id })
        assertEquals(merged, merged.merging(DiaryDocument(entries = listOf(first, second))))
    }

    @Test
    fun testConflictRejectsEntireImportAndPreservesOriginal() {
        val original = DiaryDocument(entries = listOf(entry()))
        val changed = entry().let { it.copy(body = it.body + "修改内容") }
        val incoming = DiaryDocument(entries = listOf(entry(id = "new-report"), changed))
        var current = original
        expectIssue("diary.error.entryConflict", path = "$.entries[id=report-1]") {
            current = current.merging(incoming)
        }
        assertEquals(original, current)
    }

    @Test
    fun testMetadataSummaryTagsAndStatusConflictsAreDetected() {
        val original = DiaryDocument(entries = listOf(entry()))
        val base = entry()
        val changes = listOf(
            base.copy(event = base.event.copy(address = base.event.address + "新地址")),
            base.copy(summary = base.summary + "更改"),
            base.copy(tags = base.tags + "新增"),
            base.copy(status = DiaryStatus.Ready),
            base.copy(updatedAt = base.updatedAt.plusSeconds(1))
        )
        for (changed in changes) {
            expectIssue("diary.error.entryConflict") { original.merging(DiaryDocument(entries = listOf(changed))) }
        }
    }

    @Test
    fun testDuplicateAndBlankIDsRejected() {
        expectIssue("diary.error.duplicateID", path = "$.entries[1].id") { DiaryImporter.validate(DiaryDocument(entries = listOf(entry(), entry()))) }
        expectIssue("diary.error.emptyID", path = "$.entries[0].id") { DiaryImporter.validate(DiaryDocument(entries = listOf(entry(id = " \n")))) }
        val changed = entry().let { it.copy(event = it.event.copy(ledgerEventId = "")) }
        expectIssue("diary.error.emptyID", path = "$.entries[0].event.ledgerEventID") { DiaryImporter.validate(DiaryDocument(entries = listOf(changed))) }
    }

    @Test
    fun testInvalidGregorianDaysAndChronologyRejected() {
        for (invalid in listOf("2026-02-29", "2026-02-30", "2026-13-01", "2026-10-0", "2026/10/14", "0000-01-01")) {
            val changed = entry().let { it.copy(event = it.event.copy(date = invalid)) }
            expectIssue("diary.error.date", path = "$.entries[0].event.date") { DiaryImporter.validate(DiaryDocument(entries = listOf(changed))) }
        }
        val valid = entry().let { it.copy(event = it.event.copy(date = "2028-02-29")) }
        DiaryImporter.validate(DiaryDocument(entries = listOf(valid)))
        val earlier = entry().let { it.copy(updatedAt = it.createdAt.minusSeconds(1)) }
        expectIssue("diary.error.timestamp", path = "$.entries[0].updatedAt") { DiaryImporter.validate(DiaryDocument(entries = listOf(earlier))) }
        // Swift uses Date(timeIntervalSince1970: .infinity). java.time has no infinite instant; Instant.MAX is
        // the closest unrepresentable-in-a-backup value (far outside years 0001...9999).
        val unbounded = entry().copy(updatedAt = Instant.MAX)
        expectIssue("diary.error.timestamp") { DiaryImporter.validate(DiaryDocument(entries = listOf(unbounded))) }
    }

    @Test
    fun testSchemaAndJSONFieldTypesAreCheckedWithPaths() {
        expectIssue("diary.error.schemaVersion", path = "$.schemaVersion") { DiaryImporter.validate(DiaryDocument(schemaVersion = 2)) }
        val original = DiaryDocument(entries = listOf(entry()))
        val missing = jsonChanging(original) { it.firstEntry().fields.remove("body") }
        expectIssue("diary.error.missingField", path = "$.entries[0].body") { DiaryImporter.parse(missing) }
        val wrongType = jsonChanging(original) { it.firstEntry()["tags"] = JsonString("not-an-array") }
        expectIssue("diary.error.fieldType", path = "$.entries[0].tags") { DiaryImporter.parse(wrongType) }
        val badTimestamp = jsonChanging(original) { it.firstEntry()["createdAt"] = JsonString("2026-10-14T12:00:00") }
        expectIssue("diary.error.timestamp", path = "$.entries[0].createdAt") { DiaryImporter.parse(badTimestamp) }
        expectIssue("diary.error.invalidJSON") { DiaryImporter.parse("{ broken JSON") }
    }

    @Test
    fun testOversizedImportAndManualEditRejected() {
        val oversized = "x".repeat(DiaryImporter.maximumDocumentBytes + 1)
        expectIssue("diary.error.documentSize") { DiaryImporter.parse(oversized) }
        expectIssue("diary.error.documentSize") { DiaryImporter.encode(DiaryDocument(entries = listOf(entry(body = oversized)))) }
    }

    @Test
    fun testTagNormalizationAndMultilingualSeparators() {
        assertEquals(listOf("Live", "远征", "生日", "东京"), DiaryCore.parseTags(" #Live，远征、生日;live；\n  ＃东京 , "))
        assertEquals(listOf("演出", "Cafe", "caf\u00E9"), DiaryCore.normalizeTags(listOf(" ", "#", "演出", " 演出 ", "Cafe", "caf\u00E9")))
    }

    @Test
    fun testTagDeduplicationPreservesJapaneseVoicingAndAccents() {
        // Code points match the Swift source: precomposed kana (U+30D5/30D6, U+30CF/30D0/30D1),
        // "caf\u00E9" precomposed and "cafe\u0301" decomposed.
        assertEquals(
            listOf("\u30E9\u30A4\u30D5", "\u30E9\u30A4\u30D6", "\u30CF", "\u30D0", "\u30D1", "caf\u00E9", "CAFE"),
            DiaryCore.normalizeTags(listOf(
                "\u30E9\u30A4\u30D5", "\u30E9\u30A4\u30D6", "\u30CF", "\u30D0", "\u30D1",
                "caf\u00E9", "cafe\u0301", "CAFE", "cafe"
            ))
        )
    }

    @Test
    fun testMalformedUTF8IsRejectedInsteadOfReplacingBodyCharacters() {
        val original = DiaryDocument(entries = listOf(entry(body = "BODY_MARKER")))
        val text = DiaryImporter.encode(original)
        val split = text.indexOf("BODY_MARKER")
        assertTrue("BODY_MARKER must be present", split >= 0)
        val corrupted = text.substring(0, split).toByteArray(Charsets.UTF_8) +
            byteArrayOf(0xC3.toByte(), 0x28) + // An incomplete UTF-8 sequence inside a valid JSON string.
            text.substring(split + "BODY_MARKER".length).toByteArray(Charsets.UTF_8)
        assertNotNull(runCatching { DiaryImporter.parse(String(corrupted, Charsets.UTF_8)) }.getOrNull())
        expectIssue("diary.error.invalidJSON", path = "$") { DiaryImporter.parse(corrupted) }
        assertEquals(original, DiaryImporter.parse(text.toByteArray(Charsets.UTF_8)))
    }

    @Test
    fun testSearchMatchesAllWordsAcrossReportAndMetadata() {
        val reports = listOf(entry(), entry(id = "other", body = "没有关键字"))
        assertEquals(listOf("report-1"), DiaryCore.search(reports, "笑顔 渋谷").map { it.id })
        assertEquals(2, DiaryCore.search(reports, "tokyo ENCORE").size)
        assertEquals(2, DiaryCore.search(reports, "远征 秋季 2026-10").size)
        assertTrue(DiaryCore.search(reports, "不存在").isEmpty())
        assertEquals(reports, DiaryCore.search(reports, " \n "))
    }

    @Test
    fun testPortableExportsContainContextAndUnmodifiedBody() {
        val report = entry(body = "## 原稿\n一行  \n\n第二段 **粗体**\n").copy(title = "[Link](x) *Title*\n下一行")
        val text = DiaryCore.plainText(report)
        val markdown = DiaryCore.markdown(report)
        for (exported in listOf(text, markdown)) {
            assertTrue(exported.contains(report.body))
            assertTrue(exported.contains(report.event.date))
            assertTrue(exported.contains(report.event.city))
            assertTrue(exported.contains(report.event.venue))
            assertTrue(exported.contains(report.event.address))
            assertTrue(exported.contains(report.summary))
            assertTrue(exported.contains("#远征"))
        }
        assertTrue(markdown.startsWith("# \\[Link\\](x) \\*Title\\* 下一行"))
        assertFalse(markdown.contains("# [Link](x)"))
    }
}

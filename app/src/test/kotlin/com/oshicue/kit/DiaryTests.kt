package com.oshicue.kit

import com.oshicue.kit.core.DiaryCore
import com.oshicue.kit.core.DiaryDocument
import com.oshicue.kit.core.DiaryEntry
import com.oshicue.kit.core.DiaryError
import com.oshicue.kit.core.DiaryEventContext
import com.oshicue.kit.core.DiaryImporter
import com.oshicue.kit.core.DiaryStatus
import com.oshicue.kit.core.KitJson
import com.oshicue.kit.core.JsonText
import com.oshicue.kit.core.LedgerEvent
import com.oshicue.kit.core.ScheduleDates
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DiaryTests {
    private val timestamp: Instant = Instant.ofEpochSecond(1_792_009_200L, 123_000_000L)

    private fun entry(id: String = "report-1", body: String = "今日的演出很开心。\n推しの笑顔を覚えている✨"): DiaryEntry =
        DiaryEntry(
            id = id,
            title = "Tokyo Live repo",
            body = body,
            summary = "Encore特别好",
            tags = listOf("远征", "LIVE"),
            event = DiaryEventContext(
                ledgerEventID = "event-1", name = "秋季演出", date = "2026-10-14",
                city = "東京都", venue = "渋谷ホール", address = "東京都渋谷区1-2-3"
            ),
            createdAt = timestamp,
            updatedAt = timestamp.plusSeconds(60)
        )

    private fun expectIssue(key: String, path: String? = null, operation: () -> Unit) {
        val error = assertFailsWith<DiaryError> { operation() }
        assertEquals(key, error.key)
        if (path != null) assertEquals(path, error.path)
    }

    private fun jsonChanging(document: DiaryDocument, edit: (MutableMap<String, kotlinx.serialization.json.JsonElement>) -> Unit): String {
        val root = KitJson.parse(DiaryImporter.encode(document))!!.jsonObject
        val map = root.toMutableMap()
        edit(map)
        return JsonText.encode(JsonObject(map))
    }

    private fun entriesOf(map: MutableMap<String, kotlinx.serialization.json.JsonElement>): List<MutableMap<String, kotlinx.serialization.json.JsonElement>> =
        (map["entries"] as JsonArray).map { it.jsonObject.toMutableMap() }

    private fun setEntries(map: MutableMap<String, kotlinx.serialization.json.JsonElement>, entries: List<MutableMap<String, kotlinx.serialization.json.JsonElement>>) {
        map["entries"] = JsonArray(entries.map { JsonObject(it) })
    }

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
        val snapshot = DiaryCore.context(event, defaultDate = "2026-10-15")
        event.copy(name = "改名后的活动", city = "東京都", date = "2026-12-01")
        assertEquals("live-1", snapshot.ledgerEventID)
        assertEquals("原始活动", snapshot.name)
        assertEquals("2026-10-14", snapshot.date)
        assertEquals("大阪市", snapshot.city)
        val document = DiaryDocument(entries = listOf(DiaryEntry(event = snapshot, createdAt = timestamp, updatedAt = timestamp)))
        DiaryImporter.validate(document) // No ledger event reference is required to preserve the snapshot.
    }

    @Test
    fun testEventWithoutDateUsesSelectedCivilDay() {
        val event = LedgerEvent(id = "unfinished", name = "活动待定", city = "横浜市")
        assertEquals("2027-01-03", DiaryCore.context(event, defaultDate = "2027-01-03").date)
        val malformed = event.copy(date = "2026-02-30")
        assertEquals("2027-01-03", DiaryCore.context(malformed, defaultDate = "2027-01-03").date)
    }

    @Test
    fun testLongUnicodeBodyAndWhitespaceRoundTripWithoutTruncation() {
        val body = "第一行\n推しの笑顔✨🇯🇵 한국어 ไทย Bahasa e\u0301 \t \"quote\" \\path\r\n\n".repeat(5_000) + "最后一行  \n"
        val original = DiaryDocument(entries = listOf(entry(body = body)))
        val encoded = DiaryImporter.encode(original)
        val decoded = DiaryImporter.parse(encoded)
        assertEquals(original, decoded)
        assertTrue(encoded.contains("2026-"))
        assertTrue(encoded.contains("T"))
        assertTrue(encoded.contains("Z"))
    }

    @Test
    fun testExportedNativeDateReimportsWithoutFalseConflict() {
        val report = entry().copy(
            createdAt = Instant.ofEpochSecond(1_792_009_200L, 123_456_000L),
            updatedAt = Instant.ofEpochSecond(1_792_009_299L, 999_900_000L)
        )
        val original = DiaryDocument(entries = listOf(report))
        val decoded = DiaryImporter.parse(DiaryImporter.encode(original))
        assertEquals(original, decoded)
        assertEquals(original, original.merging(decoded))
        val updated = decoded.entries[0].updatedAt
        assertTrue(kotlin.math.abs(updated.epochSecond + updated.nano / 1e9 - 1_792_009_300.0) < 0.0001)
    }

    @Test
    fun testTimestampOffsetRepresentsTheSameAbsoluteInstant() {
        val original = DiaryDocument(entries = listOf(entry()))
        val changed = jsonChanging(original) { objectMap ->
            val entries = entriesOf(objectMap)
            entries[0]["createdAt"] = kotlinx.serialization.json.JsonPrimitive(ScheduleDates.timestamp(timestamp, java.time.ZoneOffset.ofHours(9)))
            entries[0]["updatedAt"] = kotlinx.serialization.json.JsonPrimitive(ScheduleDates.timestamp(timestamp.plusSeconds(60), java.time.ZoneOffset.ofHours(9)))
            setEntries(objectMap, entries)
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
        val changed = entry().copy(body = entry().body + "修改内容")
        val incoming = DiaryDocument(entries = listOf(entry(id = "new-report"), changed))
        expectIssue("diary.error.entryConflict", "$.entries[id=report-1]") {
            original.merging(incoming)
        }
        assertEquals(original, DiaryDocument(entries = listOf(entry())))
    }

    @Test
    fun testMetadataSummaryTagsAndStatusConflictsAreDetected() {
        val original = DiaryDocument(entries = listOf(entry()))
        val changes = mutableListOf<DiaryEntry>()
        changes.add(entry().copy(event = entry().event.copy(address = entry().event.address + "新地址")))
        changes.add(entry().copy(summary = entry().summary + "更改"))
        changes.add(entry().copy(tags = entry().tags + "新增"))
        changes.add(entry().copy(status = DiaryStatus.ready))
        changes.add(entry().copy(updatedAt = entry().updatedAt.plusSeconds(1)))
        for (changed in changes) {
            expectIssue("diary.error.entryConflict") { original.merging(DiaryDocument(entries = listOf(changed))) }
        }
    }

    @Test
    fun testDuplicateAndBlankIDsRejected() {
        expectIssue("diary.error.duplicateID", "$.entries[1].id") {
            DiaryImporter.validate(DiaryDocument(entries = listOf(entry(), entry())))
        }
        expectIssue("diary.error.emptyID", "$.entries[0].id") {
            DiaryImporter.validate(DiaryDocument(entries = listOf(entry(id = " \n"))))
        }
        val changed = entry().copy(event = entry().event.copy(ledgerEventID = ""))
        expectIssue("diary.error.emptyID", "$.entries[0].event.ledgerEventID") {
            DiaryImporter.validate(DiaryDocument(entries = listOf(changed)))
        }
    }

    @Test
    fun testInvalidGregorianDaysAndChronologyRejected() {
        for (invalid in listOf("2026-02-29", "2026-02-30", "2026-13-01", "2026-10-0", "2026/10/14", "0000-01-01")) {
            val changed = entry().copy(event = entry().event.copy(date = invalid))
            expectIssue("diary.error.date", "$.entries[0].event.date") {
                DiaryImporter.validate(DiaryDocument(entries = listOf(changed)))
            }
        }
        val valid = entry().copy(event = entry().event.copy(date = "2028-02-29"))
        DiaryImporter.validate(DiaryDocument(entries = listOf(valid)))
        val badTime = entry().copy(updatedAt = entry().createdAt.minusSeconds(1))
        expectIssue("diary.error.timestamp", "$.entries[0].updatedAt") {
            DiaryImporter.validate(DiaryDocument(entries = listOf(badTime)))
        }
    }

    @Test
    fun testSchemaAndJSONFieldTypesAreCheckedWithPaths() {
        expectIssue("diary.error.schemaVersion", "$.schemaVersion") {
            DiaryImporter.validate(DiaryDocument(schemaVersion = 2))
        }
        val original = DiaryDocument(entries = listOf(entry()))
        val missing = jsonChanging(original) { objectMap ->
            val entries = entriesOf(objectMap)
            entries[0].remove("body")
            setEntries(objectMap, entries)
        }
        expectIssue("diary.error.missingField", "$.entries[0].body") { DiaryImporter.parse(missing) }
        val wrongType = jsonChanging(original) { objectMap ->
            val entries = entriesOf(objectMap)
            entries[0]["tags"] = kotlinx.serialization.json.JsonPrimitive("not-an-array")
            setEntries(objectMap, entries)
        }
        expectIssue("diary.error.fieldType", "$.entries[0].tags") { DiaryImporter.parse(wrongType) }
        val badTimestamp = jsonChanging(original) { objectMap ->
            val entries = entriesOf(objectMap)
            entries[0]["createdAt"] = kotlinx.serialization.json.JsonPrimitive("2026-10-14T12:00:00")
            setEntries(objectMap, entries)
        }
        expectIssue("diary.error.timestamp", "$.entries[0].createdAt") { DiaryImporter.parse(badTimestamp) }
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
        assertEquals(listOf("演出", "Cafe", "café"), DiaryCore.normalizeTags(listOf(" ", "#", "演出", " 演出 ", "Cafe", "café")))
    }

    @Test
    fun testTagDeduplicationPreservesJapaneseVoicingAndAccents() {
        assertEquals(
            listOf("ライフ", "ライブ", "ハ", "バ", "パ", "café", "CAFE"),
            DiaryCore.normalizeTags(listOf("ライフ", "ライブ", "ハ", "バ", "パ", "café", "cafe\u0301", "CAFE", "cafe"))
        )
    }

    @Test
    fun testMalformedUTF8IsRejectedInsteadOfReplacingBodyCharacters() {
        val original = DiaryDocument(entries = listOf(entry(body = "BODY_MARKER")))
        val text = DiaryImporter.encode(original)
        val marker = text.indexOf("BODY_MARKER")
        val corrupted = text.substring(0, marker).toByteArray() + byteArrayOf(0xC3.toByte(), 0x28) + text.substring(marker).toByteArray()
        assertNotNull(DiaryImporter.parse(String(corrupted, Charsets.UTF_8)))
        expectIssue("diary.error.invalidJSON", "$") { DiaryImporter.parse(corrupted) }
        assertEquals(original, DiaryImporter.parse(text.toByteArray()))
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

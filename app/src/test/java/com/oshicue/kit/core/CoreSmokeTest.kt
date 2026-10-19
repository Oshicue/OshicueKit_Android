package com.oshicue.kit.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/** Baseline checks that the JVM test setup can exercise the core layer. */
class CoreSmokeTest {
    @Before
    fun pinLanguage() {
        // Resolving "System" reads android.os.LocaleList, which is unavailable on the JVM.
        L10n.setLanguage(AppLanguage.En)
    }

    @Test
    fun jsonRoundTripKeepsSortedKeys() {
        val parsed = JsonParser.parse("""{"b": 1, "a": "x"}""")
        val text = parsed.serialize(pretty = true)
        assertTrue(text.indexOf("\"a\"") < text.indexOf("\"b\""))
        // JsonObject compares by reference, so check that serialization is stable instead.
        assertEquals(text, JsonParser.parse(text).serialize(pretty = true))
    }

    @Test
    fun civilDaysRejectImpossibleDates() {
        assertEquals(LocalDate.of(2026, 10, 19), KitDate.parseDay("2026-10-19"))
        assertNull(KitDate.parseDay("2026-02-30"))
    }

    @Test
    fun timestampsUseMillisecondUtc() {
        val instant = KitDate.parseTimestamp("2026-10-19T10:09:53.470Z")!!
        assertEquals("2026-10-19T10:09:53.470Z", KitDate.timestamp(instant))
    }
}

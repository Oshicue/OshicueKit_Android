package com.oshicue.kit.core

import java.time.Instant
import java.time.ZoneId

object LedgerDemo {
    /** Default fictional entries for first launch and isolated UI tests. */
    fun document(now: Instant = Instant.now()): LedgerDocument {
        val zone = ZoneId.of("Asia/Shanghai")
        val today = LedgerDates.string(now, zone)
        val earlier = LedgerDates.string(now.minusSeconds(86_400), zone)
        val thirdDay = LedgerDates.string(now.minusSeconds(2 * 86_400), zone)
        val groups = listOf(LedgerGroup("luna", "LUNA NOTE"), LedgerGroup("prism", "PRISM DAYS"))
        val members = listOf(LedgerMember("mio", "Mio"), LedgerMember("rina", "Rina"), LedgerMember("sora", "Sora"))
        val memberships = listOf(
            LedgerMembership(
                "mio-luna", "mio", "luna", "#F28AAF", listOf(
                    LedgerPrice("cheki", "JPY", 2000),
                    LedgerPrice("wide-cheki", "JPY", 3500),
                    LedgerPrice("phone-photo", "JPY", 1500)
                )
            ),
            LedgerMembership("mio-prism", "mio", "prism", "#9B8CF5", listOf(LedgerPrice("cheki", "JPY", 2500))),
            LedgerMembership("rina-luna", "rina", "luna", "#66C7A5", listOf(LedgerPrice("cheki", "JPY", 2000))),
            LedgerMembership("sora-prism", "sora", "prism", "#67B9EF", listOf(LedgerPrice("phone-photo", "CNY", 8000)))
        )
        val events = listOf(LedgerEvent("demo-fes", "Oshicue Mini Fes", today, "Tokyo", "Luna Hall", "Shibuya, Tokyo"))
        val records = listOf(
            LedgerRecord(
                id = "demo-cheki", date = today, memberID = "mio", groupID = "luna", productID = "cheki",
                quantity = 5, unitPriceMinor = 2000, currency = "JPY", eventID = "demo-fes",
                memberNameSnapshot = "Mio", groupNameSnapshot = "LUNA NOTE", productNameSnapshot = "Cheki",
                colorSnapshot = "#F28AAF", eventNameSnapshot = "Oshicue Mini Fes", citySnapshot = "Tokyo",
                venueSnapshot = "Luna Hall", addressSnapshot = "Shibuya, Tokyo"
            ),
            LedgerRecord(
                id = "demo-wide", date = today, memberID = "mio", groupID = "luna", productID = "wide-cheki",
                quantity = 2, unitPriceMinor = 3500, currency = "JPY", eventID = "demo-fes",
                memberNameSnapshot = "Mio", groupNameSnapshot = "LUNA NOTE", productNameSnapshot = "WIDE Cheki",
                colorSnapshot = "#F28AAF", eventNameSnapshot = "Oshicue Mini Fes", citySnapshot = "Tokyo",
                venueSnapshot = "Luna Hall"
            ),
            LedgerRecord(
                id = "demo-rina", date = today, memberID = "rina", groupID = "luna", productID = "cheki",
                quantity = 3, unitPriceMinor = 2000, currency = "JPY",
                memberNameSnapshot = "Rina", groupNameSnapshot = "LUNA NOTE", productNameSnapshot = "Cheki",
                colorSnapshot = "#66C7A5"
            ),
            LedgerRecord(
                id = "demo-shared", date = earlier, memberID = "mio", groupID = "prism", productID = "cheki",
                quantity = 4, unitPriceMinor = 2500, currency = "JPY",
                memberNameSnapshot = "Mio", groupNameSnapshot = "PRISM DAYS", productNameSnapshot = "Cheki",
                colorSnapshot = "#9B8CF5"
            ),
            LedgerRecord(
                id = "demo-phone", date = thirdDay, memberID = "sora", groupID = "prism", productID = "phone-photo",
                quantity = 2, unitPriceMinor = 8000, currency = "CNY",
                memberNameSnapshot = "Sora", groupNameSnapshot = "PRISM DAYS", productNameSnapshot = "Phone photo",
                colorSnapshot = "#67B9EF"
            )
        )
        return LedgerDocument(
            groups = groups, members = members, memberships = memberships,
            products = LedgerDocument.defaultProducts, events = events, records = records
        )
    }
}

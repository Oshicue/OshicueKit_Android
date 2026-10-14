package com.oshicue.kit.core

import java.math.BigDecimal
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

/** Catalog entries are normalized; each transaction also carries immutable display and price snapshots. */
data class LedgerDocument(
    val schemaVersion: Int = 1,
    val groups: List<LedgerGroup> = emptyList(),
    val members: List<LedgerMember> = emptyList(),
    val memberships: List<LedgerMembership> = emptyList(),
    val products: List<LedgerProduct> = emptyList(),
    val events: List<LedgerEvent> = emptyList(),
    val records: List<LedgerRecord> = emptyList()
) {
    /** Catalog IDs are upserted. Already recorded transaction IDs retain their original snapshots. */
    fun merging(incoming: LedgerDocument): LedgerDocument {
        LedgerImporter.validate(this)
        LedgerImporter.validate(incoming)
        val existingRecords = records.associateBy { it.id }
        for (record in incoming.records) {
            val existing = existingRecords[record.id]
            if (existing != null && existing != record) {
                throw LedgerError(
                    "ledger.error.recordConflict",
                    "Record '${record.id}' already exists with different contents. Edit it in the app, or use a new ID for a new purchase.",
                    "$.records[id=${record.id}]"
                )
            }
        }
        fun <T> upsert(existing: List<T>, incomingValues: List<T>, idOf: (T) -> String): List<T> {
            val ids = incomingValues.map(idOf).toSet()
            return existing.filter { idOf(it) !in ids } + incomingValues
        }
        val existingRecordIDs = records.map { it.id }.toSet()
        val merged = LedgerDocument(
            groups = upsert(groups, incoming.groups) { it.id },
            members = upsert(members, incoming.members) { it.id },
            memberships = upsert(memberships, incoming.memberships) { it.id },
            products = upsert(products, incoming.products) { it.id },
            events = upsert(events, incoming.events) { it.id },
            records = records + incoming.records.filter { it.id !in existingRecordIDs }
        )
        LedgerImporter.validate(merged)
        return merged
    }

    companion object {
        @JvmField
        val defaultProducts = listOf(
            LedgerProduct("cheki", "Cheki"),
            LedgerProduct("wide-cheki", "WIDE Cheki"),
            LedgerProduct("phone-photo", "Phone photo")
        )
    }
}

data class LedgerGroup(
    val id: String,
    val name: String,
    /** Optional for compatibility with schemaVersion 1 backups created before catalog deletion. */
    val isArchived: Boolean? = null
) {
    val isActive: Boolean get() = isArchived != true
}

data class LedgerMember(
    val id: String,
    val name: String,
    val avatarJPEGBase64: String? = null,
    val isArchived: Boolean? = null,
    /** Only true marks a favorite; missing or null means not favorite. */
    val isFavorite: Boolean? = null
) {
    val isActive: Boolean get() = isArchived != true
    val isFavorited: Boolean get() = isFavorite == true
}

/** A member may have several memberships, with an independent color and price list in each group. */
data class LedgerMembership(
    val id: String,
    val memberID: String,
    val groupID: String,
    val color: String,
    val prices: List<LedgerPrice> = emptyList(),
    val isArchived: Boolean? = null
) {
    val isActive: Boolean get() = isArchived != true
}

data class LedgerPrice(val productID: String, val currency: String, val unitPriceMinor: Long)

data class LedgerProduct(val id: String, val name: String)

data class LedgerEvent(
    val id: String,
    val name: String,
    val date: String? = null,
    val city: String? = null,
    val venue: String? = null,
    val address: String? = null
)

data class LedgerRecord(
    val id: String,
    /** A local civil day, unaffected by later travel or device time-zone changes. */
    val date: String,
    val memberID: String,
    val groupID: String,
    val productID: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val currency: String,
    val eventID: String? = null,
    val memberNameSnapshot: String,
    val groupNameSnapshot: String,
    val productNameSnapshot: String,
    val colorSnapshot: String,
    val eventNameSnapshot: String? = null,
    val citySnapshot: String? = null,
    val venueSnapshot: String? = null,
    val addressSnapshot: String? = null,
    val note: String? = null
) {
    val totalMinor: Long?
        get() {
            if (quantity <= 0 || unitPriceMinor < 0) return null
            return try {
                Math.multiplyExact(unitPriceMinor, quantity.toLong())
            } catch (_: ArithmeticException) {
                null
            }
        }
}

enum class LedgerCurrency(val code: String) {
    JPY("JPY"), CNY("CNY"), TWD("TWD"), HKD("HKD"), USD("USD"), KRW("KRW"), THB("THB"), IDR("IDR"), MYR("MYR");

    /** ISO 4217 minor units. IDR and TWD retain two digits even when cash prices are whole numbers. */
    val minorDigits: Int get() = if (this == JPY || this == KRW) 0 else 2

    /** Parses a nonnegative price without binary floating point or silently rounding excess precision. */
    fun parsePrice(text: String): Long? {
        val input = text.trim()
        if (!pricePattern.matches(input)) return null
        val parts = input.split(".", limit = 2)
        val whole = parts[0].toLongOrNull() ?: return null
        val fraction = if (parts.size == 2) parts[1] else ""
        if (fraction.length > minorDigits) return null
        val padded = fraction.padEnd(minorDigits, '0')
        val fractionalMinor = if (padded.isEmpty()) 0L else padded.toLongOrNull() ?: return null
        return try {
            Math.addExact(Math.multiplyExact(whole, factor), fractionalMinor)
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun format(minor: Long, locale: Locale): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = Currency.getInstance(code)
        formatter.minimumFractionDigits = minorDigits
        formatter.maximumFractionDigits = minorDigits
        val decimal = BigDecimal.valueOf(minor, minorDigits)
        return formatter.format(decimal)
    }

    fun priceText(minor: Long): String = BigDecimal.valueOf(minor, minorDigits).stripTrailingZeros().toPlainString()

    val factor: Long get() = if (minorDigits == 0) 1 else 100

    companion object {
        private val pricePattern = Regex("^[0-9]+(?:\\.[0-9]+)?$")

        fun fromCode(code: String): LedgerCurrency? = entries.firstOrNull { it.code == code }

        fun parsePrice(text: String, currency: String): Long? = fromCode(currency)?.parsePrice(text)
    }
}

object LedgerDates {
    private val datePattern = Regex("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")
    private val writer = DateTimeFormatter.ofPattern("uuuu-MM-dd")

    fun parse(text: String): LocalDate? {
        if (!datePattern.matches(text)) return null
        val parts = text.split("-").mapNotNull { it.toIntOrNull() }
        if (parts.size != 3 || parts[0] <= 0) return null
        return try {
            LocalDate.of(parts[0], parts[1], parts[2])
        } catch (_: Exception) {
            null
        }
    }

    fun string(date: LocalDate): String = writer.format(date)

    fun string(instant: Instant, zone: ZoneId): String = writer.format(instant.atZone(zone).toLocalDate())
}

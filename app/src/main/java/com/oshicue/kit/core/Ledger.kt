package com.oshicue.kit.core

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Currency
import java.util.Locale
import kotlin.math.pow

class LedgerError(val key: String, val detail: String, val path: String) : Exception("$path: $detail")

data class LedgerGroup(val id: String, val name: String, val isArchived: Boolean? = null) {
    val isActive: Boolean get() = isArchived != true
}

data class LedgerMember(
    val id: String,
    val name: String,
    val avatarJpegBase64: String? = null,
    val isArchived: Boolean? = null,
    val isFavorite: Boolean? = null
) {
    val isActive: Boolean get() = isArchived != true
    val isFavorited: Boolean get() = isFavorite == true
}

data class LedgerPrice(val productId: String, val currency: String, val unitPriceMinor: Long)

/** A member may have several memberships, each with an independent color and price list. */
data class LedgerMembership(
    val id: String,
    val memberId: String,
    val groupId: String,
    val color: String,
    val prices: List<LedgerPrice> = emptyList(),
    val isArchived: Boolean? = null
) {
    val isActive: Boolean get() = isArchived != true
}

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
    val date: String,
    val memberId: String,
    val groupId: String,
    val productId: String,
    val quantity: Int,
    val unitPriceMinor: Long,
    val currency: String,
    val eventId: String? = null,
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

data class LedgerDocument(
    val schemaVersion: Int = 1,
    val groups: List<LedgerGroup> = emptyList(),
    val members: List<LedgerMember> = emptyList(),
    val memberships: List<LedgerMembership> = emptyList(),
    val products: List<LedgerProduct> = emptyList(),
    val events: List<LedgerEvent> = emptyList(),
    val records: List<LedgerRecord> = emptyList()
) {
    companion object {
        val defaultProducts = listOf(
            LedgerProduct("cheki", "Cheki"),
            LedgerProduct("wide-cheki", "WIDE Cheki"),
            LedgerProduct("phone-photo", "Phone photo")
        )
    }

    /**
     * Catalog IDs are upserted. Recorded transaction IDs retain their snapshots, so a reimport
     * of the same backup cannot double count or rewrite historical transactions.
     */
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
        fun <T> upsert(existing: List<T>, incomingItems: List<T>, idOf: (T) -> String): List<T> {
            val ids = incomingItems.map(idOf).toSet()
            return existing.filter { idOf(it) !in ids } + incomingItems
        }
        val existingIds = records.map { it.id }.toSet()
        val merged = LedgerDocument(
            schemaVersion = schemaVersion,
            groups = upsert(groups, incoming.groups) { it.id },
            members = upsert(members, incoming.members) { it.id },
            memberships = upsert(memberships, incoming.memberships) { it.id },
            products = upsert(products, incoming.products) { it.id },
            events = upsert(events, incoming.events) { it.id },
            records = records + incoming.records.filter { it.id !in existingIds }
        )
        LedgerImporter.validate(merged)
        return merged
    }
}

enum class LedgerCurrency(val code: String) {
    JPY("JPY"), CNY("CNY"), TWD("TWD"), HKD("HKD"), USD("USD"),
    KRW("KRW"), THB("THB"), IDR("IDR"), MYR("MYR");

    /** ISO 4217 minor units. IDR and TWD keep two digits even for whole-number cash prices. */
    val minorDigits: Int get() = if (this == JPY || this == KRW) 0 else 2
    val factor: Long get() = if (minorDigits == 0) 1L else 100L

    /** Parses a nonnegative price without binary floating point or silent rounding. */
    fun parsePrice(text: String): Long? {
        val input = text.trim()
        if (!Regex("^[0-9]+(?:\\.[0-9]+)?$").matches(input)) return null
        val parts = input.split(".", limit = 2)
        val whole = parts[0].toLongOrNull() ?: return null
        val fraction = if (parts.size == 2) parts[1] else ""
        if (fraction.length > minorDigits) return null
        val padded = fraction.padEnd(minorDigits, '0')
        val fractionalMinor = if (padded.isEmpty()) 0L else (padded.toLongOrNull() ?: 0L)
        return try {
            Math.addExact(Math.multiplyExact(whole, factor), fractionalMinor)
        } catch (_: ArithmeticException) {
            null
        }
    }

    fun format(minor: Long, locale: Locale = localeForFormatting()): String {
        val formatter = NumberFormat.getCurrencyInstance(locale)
        formatter.currency = try {
            Currency.getInstance(code)
        } catch (_: Exception) {
            Currency.getInstance("USD")
        }
        formatter.minimumFractionDigits = minorDigits
        formatter.maximumFractionDigits = minorDigits
        val decimal = BigDecimal.valueOf(minor).movePointLeft(minorDigits)
        return formatter.format(decimal) ?: "$code ${decimal.toPlainString()}"
    }

    /** A plain editable amount, for example 2000 or 80.5. */
    fun priceText(minor: Long): String =
        BigDecimal.valueOf(minor).movePointLeft(minorDigits).stripTrailingZeros().toPlainString()

    companion object {
        private fun localeForFormatting(): Locale = L10n.locale

        fun from(code: String): LedgerCurrency? = entries.firstOrNull { it.code == code }

        fun parsePrice(text: String, currency: String): Long? = from(currency)?.parsePrice(text)
    }
}

object LedgerDates {
    /** Parses yyyy-MM-dd as a Gregorian civil day, rejecting impossible days. */
    fun parse(text: String): LocalDate? = KitDate.parseDay(text)

    fun string(date: LocalDate, zoneId: ZoneId = ZoneId.systemDefault()): String =
        KitDate.formatDay(date)

    fun string(instant: Instant, zoneId: ZoneId = ZoneId.systemDefault()): String =
        KitDate.day(instant, zoneId)

    fun today(): String = KitDate.formatDay(LocalDate.now())
}

object LedgerImporter {
    const val maximumDocumentBytes = 20 * 1024 * 1024
    const val maximumAvatarBytes = 2 * 1024 * 1024

    fun parse(text: String): LedgerDocument {
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw issue(LedgerError("ledger.error.documentSize", "The ledger must not exceed 20 MB.", "$"))
        }
        val root = try {
            JsonParser.parse(text).objectOrThrow("$")
        } catch (failure: JsonParseException) {
            throw error("invalidJSON", failure.message ?: "The ledger could not be decoded.", "$")
        } catch (failure: JsonIssue) {
            throw errorForIssue(failure)
        }
        val document = try {
            LedgerCodec.fromJson(root)
        } catch (failure: JsonIssue) {
            throw errorForIssue(failure)
        } catch (failure: LedgerError) {
            throw failure
        }
        validate(document)
        return document
    }

    fun encode(document: LedgerDocument): String {
        validate(document)
        val text = LedgerCodec.toJson(document).serialize(pretty = true)
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The ledger must not exceed 20 MB.", "$")
        }
        return text
    }

    private fun errorForIssue(issue: JsonIssue): LedgerError = when (issue.category) {
        "missingField" -> error("missingField", issue.detail, issue.path)
        "fieldType", "valueNotFound" -> error("fieldType", issue.detail, issue.path)
        else -> error("invalidJSON", issue.detail, issue.path)
    }

    fun validate(document: LedgerDocument) {
        if (document.schemaVersion != 1) throw error("schemaVersion", "Supported schemaVersion is 1.", "$.schemaVersion")
        unique(document.groups.map { it.id }, "$.groups")
        unique(document.members.map { it.id }, "$.members")
        unique(document.memberships.map { it.id }, "$.memberships")
        unique(document.products.map { it.id }, "$.products")
        unique(document.events.map { it.id }, "$.events")
        unique(document.records.map { it.id }, "$.records")
        val groupIds = document.groups.map { it.id }.toSet()
        val memberIds = document.members.map { it.id }.toSet()
        val productIds = document.products.map { it.id }.toSet()
        val eventIds = document.events.map { it.id }.toSet()

        document.groups.forEachIndexed { index, group ->
            nonempty(group.id, "$.groups[$index].id")
            nonempty(group.name, "$.groups[$index].name")
        }
        document.members.forEachIndexed { index, member ->
            val location = "$.members[$index]"
            nonempty(member.id, "$location.id")
            nonempty(member.name, "$location.name")
            val avatar = member.avatarJpegBase64
            if (avatar != null) {
                val avatarPath = "$location.avatarJPEGBase64"
                // Check the encoded length before allocating the decoded image.
                if (avatar.toByteArray(Charsets.UTF_8).size > ((maximumAvatarBytes + 2) / 3) * 4) {
                    throw error("avatarSize", "Each avatar must not exceed 2 MB.", avatarPath)
                }
                val bytes = try { java.util.Base64.getDecoder().decode(avatar) } catch (_: Exception) { null }
                if (bytes == null || !plausibleJpeg(bytes)) {
                    throw error("avatar", "Use base64-encoded JPEG image data.", avatarPath)
                }
                if (bytes.size > maximumAvatarBytes) {
                    throw error("avatarSize", "Each avatar must not exceed 2 MB.", avatarPath)
                }
            }
        }
        document.products.forEachIndexed { index, product ->
            nonempty(product.id, "$.products[$index].id")
            nonempty(product.name, "$.products[$index].name")
        }
        val membershipPairs = mutableSetOf<String>()
        document.memberships.forEachIndexed { index, membership ->
            val location = "$.memberships[$index]"
            nonempty(membership.id, "$location.id")
            reference(membership.memberId, memberIds, "$location.memberID")
            reference(membership.groupId, groupIds, "$location.groupID")
            if (!membershipPairs.add(pair(membership.memberId, membership.groupId))) {
                throw error("membership", "A member may have only one membership in the same group.", location)
            }
            color(membership.color, "$location.color")
            val priceKeys = mutableSetOf<String>()
            membership.prices.forEachIndexed { priceIndex, price ->
                val pricePath = "$location.prices[$priceIndex]"
                reference(price.productId, productIds, "$pricePath.productID")
                currency(price.currency, "$pricePath.currency")
                if (price.unitPriceMinor < 0) {
                    throw error("price", "unitPriceMinor must not be negative.", "$pricePath.unitPriceMinor")
                }
                if (!priceKeys.add(pair(price.productId, price.currency))) {
                    throw error("duplicateID", "A product/currency price may appear only once per membership.", pricePath)
                }
            }
        }
        document.events.forEachIndexed { index, event ->
            val location = "$.events[$index]"
            nonempty(event.id, "$location.id")
            nonempty(event.name, "$location.name")
            event.date?.let { day(it, "$location.date") }
        }
        var quantityTotal = 0
        val currencyTotals = mutableMapOf<String, Long>()
        document.records.forEachIndexed { index, record ->
            val location = "$.records[$index]"
            nonempty(record.id, "$location.id")
            day(record.date, "$location.date")
            reference(record.memberId, memberIds, "$location.memberID")
            reference(record.groupId, groupIds, "$location.groupID")
            reference(record.productId, productIds, "$location.productID")
            if (!membershipPairs.contains(pair(record.memberId, record.groupId))) {
                throw error("membership", "This member must have a membership in the recorded group.", location)
            }
            record.eventId?.let { reference(it, eventIds, "$location.eventID") }
            nonempty(record.memberNameSnapshot, "$location.memberNameSnapshot")
            nonempty(record.groupNameSnapshot, "$location.groupNameSnapshot")
            nonempty(record.productNameSnapshot, "$location.productNameSnapshot")
            color(record.colorSnapshot, "$location.colorSnapshot")
            currency(record.currency, "$location.currency")
            if (record.quantity <= 0) throw error("quantity", "quantity must be positive.", "$location.quantity")
            if (record.unitPriceMinor < 0) throw error("price", "unitPriceMinor must not be negative.", "$location.unitPriceMinor")
            val total = record.totalMinor ?: throw error("overflow", "quantity × unitPriceMinor is too large.", location)
            try {
                quantityTotal = Math.addExact(quantityTotal, record.quantity)
                currencyTotals[record.currency] = Math.addExact(currencyTotals[record.currency] ?: 0L, total)
            } catch (_: ArithmeticException) {
                throw error("overflow", "The ledger total is too large.", location)
            }
        }
        val encoded = LedgerCodec.toJson(document).serialize(pretty = true)
        if (encoded.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The ledger must not exceed 20 MB.", "$")
        }
    }

    fun pair(first: String, second: String): String =
        "${first.toByteArray(Charsets.UTF_8).size}:$first$second"

    private fun issue(error: LedgerError): Nothing = throw error

    private fun error(category: String, detail: String, path: String): LedgerError =
        LedgerError("ledger.error.$category", detail, path)

    private fun nonempty(value: String, path: String) {
        if (value.isBlank()) throw error("emptyField", "This field must not be empty.", path)
    }

    private fun unique(ids: List<String>, path: String) {
        val seen = mutableSetOf<String>()
        ids.forEachIndexed { index, id ->
            if (!seen.add(id)) throw error("duplicateID", "Duplicate ID '$id'.", "$path[$index].id")
        }
    }

    private fun reference(id: String, ids: Set<String>, path: String) {
        if (id !in ids) throw error("reference", "Referenced ID '$id' is not declared.", path)
    }

    private fun color(value: String, path: String) {
        if (!Regex("^#[0-9a-fA-F]{6}$").matches(value)) {
            throw error("color", "Use a six-digit RGB color such as #F28AAF.", path)
        }
    }

    private fun day(value: String, path: String) {
        if (LedgerDates.parse(value) == null) {
            throw error("date", "Use a valid Gregorian date in yyyy-MM-dd format.", path)
        }
    }

    private fun currency(value: String, path: String) {
        if (LedgerCurrency.from(value) == null) {
            throw error("currency", "Use JPY, CNY, TWD, HKD, USD, KRW, THB, IDR or MYR.", path)
        }
    }

    /** Rejects truncated headers and marker-only payloads without an image framework. */
    private fun plausibleJpeg(data: ByteArray): Boolean {
        val bytes = data.map { it.toInt() and 0xFF }
        if (bytes.size < 4) return false
        if (bytes[0] != 0xFF || bytes[1] != 0xD8) return false
        if (bytes[bytes.size - 2] != 0xFF || bytes[bytes.size - 1] != 0xD9) return false
        val frameMarkers = setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)
        var hasFrame = false
        var offset = 2
        while (offset < bytes.size - 2) {
            if (bytes[offset] != 0xFF) return false
            while (offset < bytes.size && bytes[offset] == 0xFF) offset++
            if (offset >= bytes.size) return false
            val marker = bytes[offset]
            offset++
            if (marker == 0x01) continue
            if (marker == 0x00 || marker == 0xD8 || marker == 0xD9 || marker in 0xD0..0xD7) return false
            if (offset + 2 > bytes.size - 2) return false
            val length = bytes[offset] * 256 + bytes[offset + 1]
            if (length < 2 || offset + length > bytes.size - 2) return false
            if (marker in frameMarkers) {
                if (length < 8) return false
                val height = bytes[offset + 3] * 256 + bytes[offset + 4]
                val width = bytes[offset + 5] * 256 + bytes[offset + 6]
                val components = bytes[offset + 7]
                if (height <= 0 || width <= 0 || components <= 0 || length < 8 + components * 3) return false
                hasFrame = true
            }
            if (marker == 0xDA) {
                if (!hasFrame || length < 6) return false
                val components = bytes[offset + 2]
                return components > 0 && length >= 6 + components * 2 && offset + length < bytes.size - 2
            }
            offset += length
        }
        return false
    }
}

object LedgerCodec {
    fun toJson(document: LedgerDocument): JsonObject = JsonObject().apply {
        put("schemaVersion", document.schemaVersion)
        put("groups", JsonArray(document.groups.map { group ->
            JsonObject().apply {
                put("id", group.id)
                put("name", group.name)
                group.isArchived?.let { put("isArchived", it) }
            }
        }.toMutableList()))
        put("members", JsonArray(document.members.map { member ->
            JsonObject().apply {
                put("id", member.id)
                put("name", member.name)
                member.avatarJpegBase64?.let { put("avatarJPEGBase64", it) }
                member.isArchived?.let { put("isArchived", it) }
                member.isFavorite?.let { put("isFavorite", it) }
            }
        }.toMutableList()))
        put("memberships", JsonArray(document.memberships.map { membership ->
            JsonObject().apply {
                put("id", membership.id)
                put("memberID", membership.memberId)
                put("groupID", membership.groupId)
                put("color", membership.color)
                put("prices", JsonArray(membership.prices.map { price ->
                    JsonObject().apply {
                        put("productID", price.productId)
                        put("currency", price.currency)
                        put("unitPriceMinor", price.unitPriceMinor)
                    }
                }.toMutableList()))
                membership.isArchived?.let { put("isArchived", it) }
            }
        }.toMutableList()))
        put("products", JsonArray(document.products.map {
            JsonObject().apply { put("id", it.id); put("name", it.name) }
        }.toMutableList()))
        put("events", JsonArray(document.events.map { event ->
            JsonObject().apply {
                put("id", event.id)
                put("name", event.name)
                event.date?.let { put("date", it) }
                event.city?.let { put("city", it) }
                event.venue?.let { put("venue", it) }
                event.address?.let { put("address", it) }
            }
        }.toMutableList()))
        put("records", JsonArray(document.records.map { record ->
            JsonObject().apply {
                put("id", record.id)
                put("date", record.date)
                put("memberID", record.memberId)
                put("groupID", record.groupId)
                put("productID", record.productId)
                put("quantity", record.quantity)
                put("unitPriceMinor", record.unitPriceMinor)
                put("currency", record.currency)
                record.eventId?.let { put("eventID", it) }
                put("memberNameSnapshot", record.memberNameSnapshot)
                put("groupNameSnapshot", record.groupNameSnapshot)
                put("productNameSnapshot", record.productNameSnapshot)
                put("colorSnapshot", record.colorSnapshot)
                record.eventNameSnapshot?.let { put("eventNameSnapshot", it) }
                record.citySnapshot?.let { put("citySnapshot", it) }
                record.venueSnapshot?.let { put("venueSnapshot", it) }
                record.addressSnapshot?.let { put("addressSnapshot", it) }
                record.note?.let { put("note", it) }
            }
        }.toMutableList()))
    }

    fun fromJson(root: JsonObject): LedgerDocument = LedgerDocument(
        schemaVersion = root.requireInt("schemaVersion", "$"),
        groups = root.requireArray("groups", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.groups[$index]")
            val path = "$.groups[$index]"
            LedgerGroup(
                json.requireString("id", path),
                json.requireString("name", path),
                json.optionalBool("isArchived", path)
            )
        },
        members = root.requireArray("members", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.members[$index]")
            val path = "$.members[$index]"
            LedgerMember(
                id = json.requireString("id", path),
                name = json.requireString("name", path),
                avatarJpegBase64 = json.optionalString("avatarJPEGBase64", path),
                isArchived = json.optionalBool("isArchived", path),
                isFavorite = json.optionalBool("isFavorite", path)
            )
        },
        memberships = root.requireArray("memberships", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.memberships[$index]")
            val path = "$.memberships[$index]"
            LedgerMembership(
                id = json.requireString("id", path),
                memberId = json.requireString("memberID", path),
                groupId = json.requireString("groupID", path),
                color = json.requireString("color", path),
                prices = json.requireArray("prices", path).items.mapIndexed { priceIndex, priceValue ->
                    val price = priceValue.objectOrThrow("$path.prices[$priceIndex]")
                    val pricePath = "$path.prices[$priceIndex]"
                    LedgerPrice(
                        price.requireString("productID", pricePath),
                        price.requireString("currency", pricePath),
                        price.requireLong("unitPriceMinor", pricePath)
                    )
                },
                isArchived = json.optionalBool("isArchived", path)
            )
        },
        products = root.requireArray("products", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.products[$index]")
            val path = "$.products[$index]"
            LedgerProduct(json.requireString("id", path), json.requireString("name", path))
        },
        events = root.requireArray("events", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.events[$index]")
            val path = "$.events[$index]"
            LedgerEvent(
                id = json.requireString("id", path),
                name = json.requireString("name", path),
                date = json.optionalString("date", path),
                city = json.optionalString("city", path),
                venue = json.optionalString("venue", path),
                address = json.optionalString("address", path)
            )
        },
        records = root.requireArray("records", "$").items.mapIndexed { index, value ->
            val json = value.objectOrThrow("$.records[$index]")
            val path = "$.records[$index]"
            LedgerRecord(
                id = json.requireString("id", path),
                date = json.requireString("date", path),
                memberId = json.requireString("memberID", path),
                groupId = json.requireString("groupID", path),
                productId = json.requireString("productID", path),
                quantity = json.requireInt("quantity", path),
                unitPriceMinor = json.requireLong("unitPriceMinor", path),
                currency = json.requireString("currency", path),
                eventId = json.optionalString("eventID", path),
                memberNameSnapshot = json.requireString("memberNameSnapshot", path),
                groupNameSnapshot = json.requireString("groupNameSnapshot", path),
                productNameSnapshot = json.requireString("productNameSnapshot", path),
                colorSnapshot = json.requireString("colorSnapshot", path),
                eventNameSnapshot = json.optionalString("eventNameSnapshot", path),
                citySnapshot = json.optionalString("citySnapshot", path),
                venueSnapshot = json.optionalString("venueSnapshot", path),
                addressSnapshot = json.optionalString("addressSnapshot", path),
                note = json.optionalString("note", path)
            )
        }
    )
}

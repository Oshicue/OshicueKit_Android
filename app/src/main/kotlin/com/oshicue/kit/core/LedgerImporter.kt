package com.oshicue.kit.core

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.Base64

data class LedgerError(
    override val key: String,
    override val detail: String,
    override val path: String
) : Exception("$path: $detail"), KitIssue

object LedgerImporter {
    const val maximumDocumentBytes = 20 * 1024 * 1024
    const val maximumAvatarBytes = 2 * 1024 * 1024

    fun parse(text: String): LedgerDocument {
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The ledger must not exceed 20 MB.", "$")
        }
        val root = KitJson.parse(text)
            ?: throw error("invalidJSON", "The text is not valid JSON.", "$")
        val document = decode(root)
        validate(document)
        return document
    }

    fun encode(document: LedgerDocument): String {
        validate(document)
        val text = JsonText.encode(encodeElement(document))
        if (text.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The ledger must not exceed 20 MB.", "$")
        }
        return text
    }

    fun validate(document: LedgerDocument) {
        if (document.schemaVersion != 1) throw error("schemaVersion", "Supported schemaVersion is 1.", "$.schemaVersion")
        unique(document.groups.map { it.id }, "$.groups")
        unique(document.members.map { it.id }, "$.members")
        unique(document.memberships.map { it.id }, "$.memberships")
        unique(document.products.map { it.id }, "$.products")
        unique(document.events.map { it.id }, "$.events")
        unique(document.records.map { it.id }, "$.records")
        val groupIDs = document.groups.map { it.id }.toSet()
        val memberIDs = document.members.map { it.id }.toSet()
        val productIDs = document.products.map { it.id }.toSet()
        val eventIDs = document.events.map { it.id }.toSet()

        document.groups.forEachIndexed { index, group ->
            nonempty(group.id, "$.groups[$index].id")
            nonempty(group.name, "$.groups[$index].name")
        }
        document.members.forEachIndexed { index, member ->
            val location = "$.members[$index]"
            nonempty(member.id, "$location.id")
            nonempty(member.name, "$location.name")
            val avatar = member.avatarJPEGBase64
            if (avatar != null) {
                val avatarPath = "$location.avatarJPEGBase64"
                if (avatar.toByteArray(Charsets.UTF_8).size > ((maximumAvatarBytes + 2) / 3) * 4) {
                    throw error("avatarSize", "Each avatar must not exceed 2 MB.", avatarPath)
                }
                val data = try {
                    Base64.getDecoder().decode(avatar)
                } catch (_: IllegalArgumentException) {
                    null
                }
                if (data == null || !plausibleJPEG(data)) {
                    throw error("avatar", "Use base64-encoded JPEG image data.", avatarPath)
                }
                if (data.size > maximumAvatarBytes) throw error("avatarSize", "Each avatar must not exceed 2 MB.", avatarPath)
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
            reference(membership.memberID, memberIDs, "$location.memberID")
            reference(membership.groupID, groupIDs, "$location.groupID")
            if (!membershipPairs.add(pair(membership.memberID, membership.groupID))) {
                throw error("membership", "A member may have only one membership in the same group.", location)
            }
            color(membership.color, "$location.color")
            val priceKeys = mutableSetOf<String>()
            membership.prices.forEachIndexed { priceIndex, price ->
                val pricePath = "$location.prices[$priceIndex]"
                reference(price.productID, productIDs, "$pricePath.productID")
                currency(price.currency, "$pricePath.currency")
                if (price.unitPriceMinor < 0) throw error("price", "unitPriceMinor must not be negative.", "$pricePath.unitPriceMinor")
                if (!priceKeys.add(pair(price.productID, price.currency))) {
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
        var currencyTotals = mutableMapOf<String, Long>()
        document.records.forEachIndexed { index, record ->
            val location = "$.records[$index]"
            nonempty(record.id, "$location.id")
            day(record.date, "$location.date")
            reference(record.memberID, memberIDs, "$location.memberID")
            reference(record.groupID, groupIDs, "$location.groupID")
            reference(record.productID, productIDs, "$location.productID")
            if (!membershipPairs.contains(pair(record.memberID, record.groupID))) {
                throw error("membership", "This member must have a membership in the recorded group.", location)
            }
            record.eventID?.let { reference(it, eventIDs, "$location.eventID") }
            nonempty(record.memberNameSnapshot, "$location.memberNameSnapshot")
            nonempty(record.groupNameSnapshot, "$location.groupNameSnapshot")
            nonempty(record.productNameSnapshot, "$location.productNameSnapshot")
            color(record.colorSnapshot, "$location.colorSnapshot")
            currency(record.currency, "$location.currency")
            if (record.quantity <= 0) throw error("quantity", "quantity must be positive.", "$location.quantity")
            if (record.unitPriceMinor < 0) throw error("price", "unitPriceMinor must not be negative.", "$location.unitPriceMinor")
            val total = record.totalMinor ?: throw error("overflow", "quantity × unitPriceMinor is too large.", location)
            val nextQuantity: Int
            val nextCurrencyTotal: Long
            try {
                nextQuantity = Math.addExact(quantityTotal, record.quantity)
                nextCurrencyTotal = Math.addExact(currencyTotals[record.currency] ?: 0L, total)
            } catch (_: ArithmeticException) {
                throw error("overflow", "The ledger total is too large.", location)
            }
            quantityTotal = nextQuantity
            currencyTotals[record.currency] = nextCurrencyTotal
        }
        // Validation is also used for manual additions, which do not pass through parse/encode.
        val encoded = JsonText.encode(encodeElement(document))
        if (encoded.toByteArray(Charsets.UTF_8).size > maximumDocumentBytes) {
            throw error("documentSize", "The ledger must not exceed 20 MB.", "$")
        }
    }

    fun pair(first: String, second: String): String = "${first.toByteArray(Charsets.UTF_8).size}:$first$second"

    private fun error(category: String, detail: String, path: String) = LedgerError("ledger.error.$category", detail, path)

    private fun nonempty(value: String, path: String) {
        if (value.trim().isEmpty()) throw error("emptyField", "This field must not be empty.", path)
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

    private val colorPattern = Regex("^#[0-9a-fA-F]{6}$")

    private fun color(value: String, path: String) {
        if (!colorPattern.matches(value)) throw error("color", "Use a six-digit RGB color such as #F28AAF.", path)
    }

    private fun day(value: String, path: String) {
        if (LedgerDates.parse(value) == null) throw error("date", "Use a valid Gregorian date in yyyy-MM-dd format.", path)
    }

    private fun currency(value: String, path: String) {
        if (LedgerCurrency.fromCode(value) == null) {
            throw error("currency", "Use JPY, CNY, TWD, HKD, USD, KRW, THB, IDR or MYR.", path)
        }
    }

    /** Rejects truncated headers and marker-only payloads without importing a UI image framework. */
    private fun plausibleJPEG(data: ByteArray): Boolean {
        fun byteAt(index: Int): Int = data[index].toInt() and 0xFF
        if (data.size < 4 || byteAt(0) != 0xFF || byteAt(1) != 0xD8) return false
        if (byteAt(data.size - 2) != 0xFF || byteAt(data.size - 1) != 0xD9) return false
        val frameMarkers = setOf(0xC0, 0xC1, 0xC2, 0xC3, 0xC5, 0xC6, 0xC7, 0xC9, 0xCA, 0xCB, 0xCD, 0xCE, 0xCF)
        var hasFrame = false
        var offset = 2
        while (offset < data.size - 2) {
            if (byteAt(offset) != 0xFF) return false
            while (offset < data.size && byteAt(offset) == 0xFF) offset += 1
            if (offset >= data.size) return false
            val marker = byteAt(offset)
            offset += 1
            if (marker == 0x01) continue
            if (marker == 0x00 || marker == 0xD8 || marker == 0xD9 || marker in 0xD0..0xD7 || offset + 2 > data.size - 2) return false
            val length = byteAt(offset) * 256 + byteAt(offset + 1)
            if (length < 2 || offset + length > data.size - 2) return false
            if (marker in frameMarkers) {
                if (length < 8) return false
                val height = byteAt(offset + 3) * 256 + byteAt(offset + 4)
                val width = byteAt(offset + 5) * 256 + byteAt(offset + 6)
                val components = byteAt(offset + 7)
                if (height <= 0 || width <= 0 || components <= 0 || length < 8 + components * 3) return false
                hasFrame = true
            }
            if (marker == 0xDA) {
                if (!hasFrame || length < 6) return false
                val components = byteAt(offset + 2)
                return components > 0 && length >= 6 + components * 2 && offset + length < data.size - 2
            }
            offset += length
        }
        return false
    }

    internal fun encodeElement(document: LedgerDocument): JsonObject {
        val root = JObj()
        root.putArray("events", document.events.map { event ->
            JObj().apply {
                event.address?.let { put("address", it) }
                event.city?.let { put("city", it) }
                event.date?.let { put("date", it) }
                put("id", event.id)
                put("name", event.name)
                event.venue?.let { put("venue", it) }
            }.build()
        })
        root.putArray("groups", document.groups.map { group ->
            JObj().apply {
                put("id", group.id)
                group.isArchived?.let { put("isArchived", it) }
                put("name", group.name)
            }.build()
        })
        root.putArray("members", document.members.map { member ->
            JObj().apply {
                member.avatarJPEGBase64?.let { put("avatarJPEGBase64", it) }
                put("id", member.id)
                member.isArchived?.let { put("isArchived", it) }
                member.isFavorite?.let { put("isFavorite", it) }
                put("name", member.name)
            }.build()
        })
        root.putArray("memberships", document.memberships.map { membership ->
            JObj().apply {
                put("color", membership.color)
                put("groupID", membership.groupID)
                put("id", membership.id)
                membership.isArchived?.let { put("isArchived", it) }
                put("memberID", membership.memberID)
                putArray("prices", membership.prices.map { price ->
                    JObj().apply {
                        put("currency", price.currency)
                        put("productID", price.productID)
                        put("unitPriceMinor", price.unitPriceMinor)
                    }.build()
                })
            }.build()
        })
        root.putArray("products", document.products.map { product ->
            JObj().apply { put("id", product.id); put("name", product.name) }.build()
        })
        root.putArray("records", document.records.map { record ->
            JObj().apply {
                record.addressSnapshot?.let { put("addressSnapshot", it) }
                record.citySnapshot?.let { put("citySnapshot", it) }
                put("colorSnapshot", record.colorSnapshot)
                put("currency", record.currency)
                put("date", record.date)
                record.eventID?.let { put("eventID", it) }
                record.eventNameSnapshot?.let { put("eventNameSnapshot", it) }
                put("groupID", record.groupID)
                put("groupNameSnapshot", record.groupNameSnapshot)
                put("id", record.id)
                put("memberID", record.memberID)
                put("memberNameSnapshot", record.memberNameSnapshot)
                record.note?.let { put("note", it) }
                put("productID", record.productID)
                put("productNameSnapshot", record.productNameSnapshot)
                put("quantity", record.quantity)
                put("unitPriceMinor", record.unitPriceMinor)
                record.venueSnapshot?.let { put("venueSnapshot", it) }
            }.build()
        })
        root.put("schemaVersion", document.schemaVersion)
        return root.build()
    }

    private fun decode(root: JsonElement): LedgerDocument {
        val reader = JsonReader("ledger.error") { key, detail, path -> LedgerError(key, detail, path) }
        val top = reader.obj(root, "$")
        val schemaVersion = reader.int(top, "schemaVersion", "$")
        val groups = reader.array(top, "groups", "$").mapIndexed { index, element ->
            val path = "$.groups[$index]"
            val obj = reader.obj(element, path)
            LedgerGroup(
                id = reader.string(obj, "id", path),
                name = reader.string(obj, "name", path),
                isArchived = reader.optionalBool(obj, "isArchived", path)
            )
        }
        val members = reader.array(top, "members", "$").mapIndexed { index, element ->
            val path = "$.members[$index]"
            val obj = reader.obj(element, path)
            LedgerMember(
                id = reader.string(obj, "id", path),
                name = reader.string(obj, "name", path),
                avatarJPEGBase64 = reader.optionalString(obj, "avatarJPEGBase64", path),
                isArchived = reader.optionalBool(obj, "isArchived", path),
                isFavorite = reader.optionalBool(obj, "isFavorite", path)
            )
        }
        val memberships = reader.array(top, "memberships", "$").mapIndexed { index, element ->
            val path = "$.memberships[$index]"
            val obj = reader.obj(element, path)
            val prices = reader.array(obj, "prices", path).mapIndexed { priceIndex, priceElement ->
                val pricePath = "$path.prices[$priceIndex]"
                val price = reader.obj(priceElement, pricePath)
                LedgerPrice(
                    productID = reader.string(price, "productID", pricePath),
                    currency = reader.string(price, "currency", pricePath),
                    unitPriceMinor = reader.long(price, "unitPriceMinor", pricePath)
                )
            }
            LedgerMembership(
                id = reader.string(obj, "id", path),
                memberID = reader.string(obj, "memberID", path),
                groupID = reader.string(obj, "groupID", path),
                color = reader.string(obj, "color", path),
                prices = prices,
                isArchived = reader.optionalBool(obj, "isArchived", path)
            )
        }
        val products = reader.array(top, "products", "$").mapIndexed { index, element ->
            val path = "$.products[$index]"
            val obj = reader.obj(element, path)
            LedgerProduct(id = reader.string(obj, "id", path), name = reader.string(obj, "name", path))
        }
        val events = reader.array(top, "events", "$").mapIndexed { index, element ->
            val path = "$.events[$index]"
            val obj = reader.obj(element, path)
            LedgerEvent(
                id = reader.string(obj, "id", path),
                name = reader.string(obj, "name", path),
                date = reader.optionalString(obj, "date", path),
                city = reader.optionalString(obj, "city", path),
                venue = reader.optionalString(obj, "venue", path),
                address = reader.optionalString(obj, "address", path)
            )
        }
        val records = reader.array(top, "records", "$").mapIndexed { index, element ->
            val path = "$.records[$index]"
            val obj = reader.obj(element, path)
            LedgerRecord(
                id = reader.string(obj, "id", path),
                date = reader.string(obj, "date", path),
                memberID = reader.string(obj, "memberID", path),
                groupID = reader.string(obj, "groupID", path),
                productID = reader.string(obj, "productID", path),
                quantity = reader.int(obj, "quantity", path),
                unitPriceMinor = reader.long(obj, "unitPriceMinor", path),
                currency = reader.string(obj, "currency", path),
                eventID = reader.optionalString(obj, "eventID", path),
                memberNameSnapshot = reader.string(obj, "memberNameSnapshot", path),
                groupNameSnapshot = reader.string(obj, "groupNameSnapshot", path),
                productNameSnapshot = reader.string(obj, "productNameSnapshot", path),
                colorSnapshot = reader.string(obj, "colorSnapshot", path),
                eventNameSnapshot = reader.optionalString(obj, "eventNameSnapshot", path),
                citySnapshot = reader.optionalString(obj, "citySnapshot", path),
                venueSnapshot = reader.optionalString(obj, "venueSnapshot", path),
                addressSnapshot = reader.optionalString(obj, "addressSnapshot", path),
                note = reader.optionalString(obj, "note", path)
            )
        }
        return LedgerDocument(schemaVersion, groups, members, memberships, products, events, records)
    }
}

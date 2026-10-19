package com.oshicue.kit.core

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.LocaleList
import java.util.Locale

/**
 * The interface languages shipped by the iOS app. Preference storage and tab selection
 * keep the same keys and values so a future migration can reuse them.
 */
enum class AppLanguage(val id: String) {
    System("system"),
    ZhHans("zh-Hans"),
    ZhHant("zh-Hant"),
    ZhHK("zh-HK"),
    En("en"),
    Ja("ja"),
    Ko("ko"),
    Th("th"),
    Id("id"),
    Ms("ms");

    val nativeName: String
        get() = when (this) {
            System -> L10n.text("settings.followSystem")
            ZhHans -> "简体中文（中国）"
            ZhHant -> "繁體中文（台灣）"
            ZhHK -> "繁體粵語（香港）"
            En -> "English (United States)"
            Ja -> "日本語（日本）"
            Ko -> "한국어（대한민국）"
            Th -> "ไทย (ประเทศไทย)"
            Id -> "Bahasa Indonesia (Indonesia)"
            Ms -> "Bahasa Melayu (Malaysia)"
        }

    val locale: Locale
        get() = when (this) {
            System -> Locale.getDefault()
            ZhHans -> Locale.forLanguageTag("zh-Hans-CN")
            ZhHant -> Locale.forLanguageTag("zh-Hant-TW")
            ZhHK -> Locale.forLanguageTag("zh-Hant-HK")
            En -> Locale.forLanguageTag("en-US")
            Ja -> Locale.forLanguageTag("ja-JP")
            Ko -> Locale.forLanguageTag("ko-KR")
            Th -> Locale.forLanguageTag("th-TH")
            Id -> Locale.forLanguageTag("id-ID")
            Ms -> Locale.forLanguageTag("ms-MY")
        }

    companion object {
        val storageKey = "appLanguage"

        fun fromId(value: String?): AppLanguage =
            entries.firstOrNull { it.id == value } ?: System

        /** Resolves the device languages without collapsing Hong Kong into Taiwan. */
        fun resolveSystemLanguage(
            preferred: List<String> = currentLocales(),
            regionCode: String? = Locale.getDefault().country
        ): AppLanguage {
            for (identifier in preferred) {
                val normalized = identifier.replace('_', '-').lowercase(Locale.ROOT)
                val components = normalized.split('-').filter { it.isNotEmpty() }
                val code = components.firstOrNull() ?: continue
                when (code) {
                    "yue" -> return ZhHK
                    "zh" -> {
                        if (components.any { it == "hk" || it == "mo" }) return ZhHK
                        if (components.any { it == "hans" || it == "cn" || it == "sg" }) return ZhHans
                        if (components.any { it == "tw" }) return ZhHant
                        if (components.any { it == "hant" }) {
                            return if (regionCode?.uppercase(Locale.ROOT) in setOf("HK", "MO")) ZhHK else ZhHant
                        }
                        val region = regionCode?.uppercase(Locale.ROOT)
                        if (region == "HK" || region == "MO") return ZhHK
                        if (region == "TW") return ZhHant
                        return ZhHans
                    }
                    "en" -> return En
                    "ja" -> return Ja
                    "ko" -> return Ko
                    "th" -> return Th
                    "id", "in" -> return Id
                    "ms" -> return Ms
                    else -> continue
                }
            }
            return En
        }

        private fun currentLocales(): List<String> {
            val list = LocaleList.getDefault()
            return (0 until list.size()).map { list[it].toLanguageTag() }
        }
    }
}

/**
 * Runtime localization backed by the original .strings files in assets, so all nine
 * languages behave exactly as they do on iOS, including the English fallback.
 */
object L10n {
    private val tables = mutableMapOf<AppLanguage, Map<String, String>>()
    private var preferences: SharedPreferences? = null
    private var cachedLanguage: AppLanguage? = null
    private var cachedTable: Map<String, String> = emptyMap()
    private var current: AppLanguage = AppLanguage.System

    fun initialize(context: Context) {
        if (preferences != null) return
        preferences = context.applicationContext.getSharedPreferences("OshicueKit", Context.MODE_PRIVATE)
        load(context.applicationContext, AppLanguage.En)
        load(context.applicationContext, AppLanguage.ZhHans)
        load(context.applicationContext, AppLanguage.ZhHant)
        load(context.applicationContext, AppLanguage.ZhHK)
        load(context.applicationContext, AppLanguage.Ja)
        load(context.applicationContext, AppLanguage.Ko)
        load(context.applicationContext, AppLanguage.Th)
        load(context.applicationContext, AppLanguage.Id)
        load(context.applicationContext, AppLanguage.Ms)
    }

    private fun load(context: Context, language: AppLanguage) {
        if (tables.containsKey(language)) return
        val table = try {
            context.assets.open("localization/${language.id}.strings").bufferedReader(Charsets.UTF_8).use { reader ->
                parseStrings(reader.readText())
            }
        } catch (_: Exception) {
            emptyMap()
        }
        tables[language] = table
    }

    val selectedLanguage: AppLanguage
        get() = current

    /** Keeps the resolver in step with the language stored in settings.json. */
    fun setLanguage(language: AppLanguage) {
        current = language
        invalidate()
    }

    val resolvedLanguage: AppLanguage
        get() = selectedLanguage.takeIf { it != AppLanguage.System }
            ?: AppLanguage.resolveSystemLanguage()

    /** Drops the memoized table so every caller picks up a language change immediately. */
    fun invalidate() {
        cachedLanguage = null
        cachedTable = emptyMap()
    }

    val locale: Locale get() = resolvedLanguage.locale

    private fun tableFor(language: AppLanguage): Map<String, String> {
        cachedLanguage?.let { if (it == language) return cachedTable }
        val table = tables[language] ?: emptyMap()
        cachedLanguage = language
        cachedTable = table
        return table
    }

    fun text(key: String): String {
        val language = resolvedLanguage
        tableFor(language)[key]?.let { return it }
        if (language != AppLanguage.En) tableFor(AppLanguage.En)[key]?.let { return it }
        return key
    }

    fun format(key: String, vararg arguments: Any): String = substitute(text(key), arguments)

    /** Replaces %@ and %d placeholders in order, like String(format:locale:arguments:). */
    private fun substitute(template: String, arguments: Array<out Any>): String {
        val builder = StringBuilder()
        var argumentIndex = 0
        var index = 0
        while (index < template.length) {
            val c = template[index]
            if (c == '%' && index + 1 < template.length) {
                when (template[index + 1]) {
                    '@', 'd' -> {
                        val value = arguments.getOrNull(argumentIndex)
                        argumentIndex++
                        builder.append(value?.toString() ?: "")
                        index += 2
                        continue
                    }
                    '%' -> {
                        builder.append('%')
                        index += 2
                        continue
                    }
                }
            }
            builder.append(c)
            index++
        }
        return builder.toString()
    }

    private fun parseStrings(content: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        var index = 0
        val length = content.length
        while (index < length) {
            // Skip whitespace and comments.
            while (index < length && content[index].isWhitespace()) index++
            if (index + 1 < length && content[index] == '/' && content[index + 1] == '/') {
                while (index < length && content[index] != '\n') index++
                continue
            }
            if (index >= length || content[index] != '"') { index++; continue }
            val keyResult = readQuoted(content, index) ?: break
            index = keyResult.second
            while (index < length && content[index].isWhitespace()) index++
            if (index >= length || content[index] != '=') continue
            index++
            while (index < length && content[index].isWhitespace()) index++
            if (index >= length || content[index] != '"') continue
            val valueResult = readQuoted(content, index) ?: break
            index = valueResult.second
            result[keyResult.first] = valueResult.first
            while (index < length && content[index] != '\n') index++
        }
        return result
    }

    private fun readQuoted(content: String, start: Int): Pair<String, Int>? {
        if (start >= content.length || content[start] != '"') return null
        val builder = StringBuilder()
        var index = start + 1
        while (index < content.length) {
            val c = content[index]
            when (c) {
                '"' -> return builder.toString() to (index + 1)
                '\\' -> {
                    if (index + 1 >= content.length) return null
                    when (val escape = content[index + 1]) {
                        'n' -> builder.append('\n')
                        't' -> builder.append('\t')
                        'r' -> builder.append('\r')
                        '"' -> builder.append('"')
                        '\\' -> builder.append('\\')
                        'u' -> {
                            if (index + 5 >= content.length) return null
                            val code = content.substring(index + 2, index + 6).toIntOrNull(16)
                            if (code != null) builder.append(code.toChar())
                            index += 4
                        }
                        else -> builder.append(escape)
                    }
                    index += 2
                }
                else -> { builder.append(c); index++ }
            }
        }
        return null
    }
}

/** Resolves the language for the current configuration without touching storage. */
fun AppLanguage.Companion.fromConfiguration(configuration: Configuration): AppLanguage {
    val list = configuration.locales
    val identifiers = (0 until list.size()).map { list[it].toLanguageTag() }
    return AppLanguage.resolveSystemLanguage(identifiers, configuration.locales.get(0)?.country)
}

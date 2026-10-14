package com.oshicue.kit.i18n

import android.content.Context
import java.util.Locale

enum class AppLanguage(val rawValue: String) {
    system("system"),
    zhHans("zh-Hans"),
    zhHant("zh-Hant"),
    zhHK("zh-HK"),
    en("en"),
    ja("ja"),
    ko("ko"),
    th("th"),
    indonesian("id"),
    ms("ms");

    val localeID: String
        get() = when (this) {
            system -> Locale.getDefault().toLanguageTag()
            zhHans -> "zh-Hans-CN"
            zhHant -> "zh-Hant-TW"
            zhHK -> "zh-Hant-HK"
            en -> "en-US"
            ja -> "ja-JP"
            ko -> "ko-KR"
            th -> "th-TH"
            indonesian -> "id-ID"
            ms -> "ms-MY"
        }

    val locale: Locale get() = Locale.forLanguageTag(localeID)

    val nativeName: String
        get() = when (this) {
            system -> L10n.text("settings.followSystem")
            zhHans -> "简体中文（中国）"
            zhHant -> "繁體中文（台灣）"
            zhHK -> "繁體粵語（香港）"
            en -> "English (United States)"
            ja -> "日本語（日本）"
            ko -> "한국어（대한민국）"
            th -> "ไทย (ประเทศไทย)"
            indonesian -> "Bahasa Indonesia (Indonesia)"
            ms -> "Bahasa Melayu (Malaysia)"
        }

    companion object {
        fun fromRaw(value: String?): AppLanguage = entries.firstOrNull { it.rawValue == value } ?: system

        /** Resolves the user's preferred language without collapsing Hong Kong Cantonese into Taiwan Traditional Chinese. */
        fun resolveSystemLanguage(): AppLanguage {
            val locale = Locale.getDefault()
            val language = locale.language.lowercase(Locale.ROOT)
            return when (language) {
                "yue" -> zhHK
                "zh" -> {
                    val script = locale.script.lowercase(Locale.ROOT)
                    val country = locale.country.uppercase(Locale.ROOT)
                    when {
                        country == "HK" || country == "MO" -> zhHK
                        script == "hans" || country == "CN" || country == "SG" -> zhHans
                        country == "TW" -> zhHant
                        script == "hant" -> if (country == "HK" || country == "MO") zhHK else zhHant
                        else -> zhHans
                    }
                }
                "en" -> en
                "ja" -> ja
                "ko" -> ko
                "th" -> th
                "id", "in" -> indonesian
                "ms" -> ms
                else -> en
            }
        }
    }
}

object L10n {
    const val preferenceKey = "appLanguage"
    const val preferencesName = "oshicue-kit"

    private var context: Context? = null

    /** Keeps the activity context so the selected language's configuration applies to lookups. */
    fun attach(context: Context) {
        this.context = context
    }

    fun selected(context: Context): AppLanguage =
        AppLanguage.fromRaw(preferences(context).getString(preferenceKey, AppLanguage.system.rawValue))

    fun setLanguage(context: Context, language: AppLanguage) {
        preferences(context).edit().putString(preferenceKey, language.rawValue).apply()
    }

    /** The effective language used for the current configuration and number formats. */
    fun resolvedLanguage(context: Context): AppLanguage {
        val chosen = selected(context)
        return if (chosen == AppLanguage.system) AppLanguage.resolveSystemLanguage() else chosen
    }

    fun currentLocale(): Locale {
        val holding = context ?: return Locale.getDefault()
        return resolvedLanguage(holding).locale
    }

    fun text(key: String): String {
        val holding = context ?: return key
        val id = KEY_RESOURCES[key] ?: return key
        return holding.getString(id)
    }

    fun format(key: String, vararg arguments: Any): String {
        val holding = context ?: return key
        val id = KEY_RESOURCES[key] ?: return key
        return holding.getString(id, *arguments)
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)
}

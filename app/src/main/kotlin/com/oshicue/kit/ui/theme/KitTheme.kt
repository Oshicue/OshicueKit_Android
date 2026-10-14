package com.oshicue.kit.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.oshicue.kit.i18n.L10n
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Shared dynamic colors mirroring the iOS AppearancePalette. */
data class KitPalette(
    val accent: Color,
    val accentText: Color,
    val accentFill: Color,
    val onAccent: Color,
    val ink: Color,
    val onInk: Color,
    val muted: Color,
    val warningText: Color,
    val background: Color,
    val surface: Color,
    val surfaceMuted: Color,
    val surfaceSubtle: Color,
    val line: Color,
    val dark: Boolean
) {
    companion object {
        val light = KitPalette(
            accent = hexColor("F23C6B"),
            accentText = hexColor("CE2853"),
            accentFill = hexColor("D92C59"),
            onAccent = Color.White,
            ink = hexColor("20212B"),
            onInk = Color.White,
            muted = hexColor("696873"),
            warningText = hexColor("945300"),
            background = hexColor("F8F7F5"),
            surface = hexColor("FFFFFF"),
            surfaceMuted = hexColor("EDEBE8"),
            surfaceSubtle = hexColor("F1F0ED"),
            line = hexColor("EAE8E5"),
            dark = false
        )
        val dark = KitPalette(
            accent = hexColor("FF7294"),
            accentText = hexColor("FF7294"),
            accentFill = hexColor("D33562"),
            onAccent = Color.White,
            ink = hexColor("EDECF1"),
            onInk = hexColor("20212B"),
            muted = hexColor("AAA9B3"),
            warningText = hexColor("FFB45B"),
            background = hexColor("141519"),
            surface = hexColor("202127"),
            surfaceMuted = hexColor("2A2B33"),
            surfaceSubtle = hexColor("282930"),
            line = hexColor("44434B"),
            dark = true
        )
    }

    /** Large count labels retain their member color whenever it is readable. */
    fun readableDataColor(hex: String): Color {
        val source = hexColor(hex)
        val backgroundLuminance = surface.luminance().toDouble()
        val destination = if (dark) 1f else 0f
        var result = source
        for (step in 0..20) {
            val fraction = step / 20f
            result = Color(
                red = source.red * (1 - fraction) + destination * fraction,
                green = source.green * (1 - fraction) + destination * fraction,
                blue = source.blue * (1 - fraction) + destination * fraction,
                alpha = 1f
            )
            val foregroundLuminance = result.luminance().toDouble()
            val ratio = (maxOf(foregroundLuminance, backgroundLuminance) + 0.05) /
                (minOf(foregroundLuminance, backgroundLuminance) + 0.05)
            if (ratio >= 3) break
        }
        return result
    }
}

fun hexColor(hex: String): Color {
    val value = hex.trim().trimStart('#').toLongOrNull(16) ?: 0x8F8F9B
    return Color(
        red = ((value shr 16) and 0xFF) / 255f,
        green = ((value shr 8) and 0xFF) / 255f,
        blue = (value and 0xFF) / 255f,
        alpha = 1f
    )
}

val LocalKitPalette = staticCompositionLocalOf { KitPalette.light }

object KitDate {
    fun string(date: Instant, pattern: String, zone: ZoneId, locale: Locale = L10n.currentLocale()): String =
        DateTimeFormatter.ofPattern(pattern, locale).withZone(zone).format(date)

    fun string(date: LocalDate, pattern: String, locale: Locale = L10n.currentLocale()): String =
        DateTimeFormatter.ofPattern(pattern, locale).format(date)

    fun string(date: LocalDate, pattern: String, zone: ZoneId): String = string(date, pattern, L10n.currentLocale())

    fun time(date: Instant, zone: ZoneId): String = string(date, "HH:mm", zone)

    fun time(date: Instant): String = time(date, ZoneId.systemDefault())

    fun string(date: Instant, pattern: String): String = string(date, pattern, ZoneId.systemDefault())
}

@Composable
fun kitPalette(): KitPalette = LocalKitPalette.current

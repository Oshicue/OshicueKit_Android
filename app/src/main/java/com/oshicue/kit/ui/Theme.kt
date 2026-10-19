package com.oshicue.kit.ui

import android.app.UiModeManager
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.oshicue.kit.R
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

@Immutable
data class KitColors(
    val dark: Boolean,
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
    val line: Color
) {
    /** Large count labels retain their member color whenever it stays readable. */
    fun readableDataColor(hex: String): Color {
        val source = hexToColor(hex)
        val backgroundLuminance = luminance(listOf(surface.red.toDouble(), surface.green.toDouble(), surface.blue.toDouble()))
        val destination = if (dark) 1.0 else 0.0
        val components = listOf(source.red.toDouble(), source.green.toDouble(), source.blue.toDouble())
        var result = components
        for (step in 0..20) {
            val fraction = step / 20.0
            // Blend from the original color each step, as iOS does, so the tint is the smallest readable shift.
            result = components.map { it * (1 - fraction) + destination * fraction }
            val foreground = luminance(result)
            val ratio = (max(foreground, backgroundLuminance) + 0.05) / (min(foreground, backgroundLuminance) + 0.05)
            if (ratio >= 3.0) break
        }
        return Color(result[0].toFloat(), result[1].toFloat(), result[2].toFloat())
    }

    companion object {
        val light = KitColors(
            dark = false,
            accent = Color(0xFFF23C6B),
            accentText = Color(0xFFCE2853),
            accentFill = Color(0xFFD92C59),
            onAccent = Color.White,
            ink = Color(0xFF20212B),
            onInk = Color.White,
            muted = Color(0xFF696873),
            warningText = Color(0xFF945300),
            background = Color(0xFFF8F7F5),
            surface = Color(0xFFFFFFFF),
            surfaceMuted = Color(0xFFEDEBE8),
            surfaceSubtle = Color(0xFFF1F0ED),
            line = Color(0xFFEAE8E5)
        )

        val darkKit = KitColors(
            dark = true,
            accent = Color(0xFFFF7294),
            accentText = Color(0xFFFF7294),
            accentFill = Color(0xFFD33562),
            onAccent = Color.White,
            ink = Color(0xFFEDECF1),
            onInk = Color(0xFF20212B),
            muted = Color(0xFFAAA9B3),
            warningText = Color(0xFFFFB45B),
            background = Color(0xFF141519),
            surface = Color(0xFF202127),
            surfaceMuted = Color(0xFF2A2B33),
            surfaceSubtle = Color(0xFF282930),
            line = Color(0xFF44434B)
        )

        // Increase Contrast variants, matching the contrastLight/contrastDark values on iOS.
        val lightContrast = light.copy(
            accent = Color(0xFFC42150),
            accentText = Color(0xFFA91B40),
            accentFill = Color(0xFFB61C44),
            ink = Color(0xFF101117),
            onInk = Color.White,
            muted = Color(0xFF53525C),
            warningText = Color(0xFF713F00),
            line = Color(0xFFB8B5B1)
        )

        val darkContrast = darkKit.copy(
            accent = Color(0xFFFF96AE),
            accentText = Color(0xFFFF96AE),
            accentFill = Color(0xFFBA244D),
            ink = Color.White,
            onInk = Color(0xFF101117),
            muted = Color(0xFFC7C6D0),
            warningText = Color(0xFFFFD299),
            line = Color(0xFF777580)
        )

        fun resolve(dark: Boolean, increasedContrast: Boolean): KitColors = when {
            dark && increasedContrast -> darkContrast
            dark -> darkKit
            increasedContrast -> lightContrast
            else -> light
        }
    }
}

val LocalKit = staticCompositionLocalOf { KitColors.light }

val Unbounded: FontFamily = FontFamily(
    Font(R.font.unbounded, FontWeight.Normal),
    Font(R.font.unbounded, FontWeight.Bold),
    Font(R.font.unbounded, FontWeight.Black)
)

fun hexToColor(hex: String, fallback: Color = Color(0xFF8F8F9B)): Color {
    val cleaned = hex.trim().removePrefix("#")
    if (cleaned.length != 6) return fallback
    val value = cleaned.toLongOrNull(16) ?: return fallback
    return Color(
        red = ((value shr 16) and 0xFF) / 255f,
        green = ((value shr 8) and 0xFF) / 255f,
        blue = (value and 0xFF) / 255f
    )
}

fun luminance(rgb: List<Double>): Double {
    val values = rgb.map { component ->
        if (component <= 0.04045) component / 12.92 else ((component + 0.055) / 1.055).pow(2.4)
    }
    return values[0] * 0.2126 + values[1] * 0.7152 + values[2] * 0.0722
}

@Composable
fun kitColor(hex: String): Color = hexToColor(hex)

fun Color.toHexString(): String {
    val red = (red * 255).toInt().coerceIn(0, 255)
    val green = (green * 255).toInt().coerceIn(0, 255)
    val blue = (blue * 255).toInt().coerceIn(0, 255)
    return String.format(java.util.Locale.ROOT, "#%02X%02X%02X", red, green, blue)
}

/**
 * Follows the system contrast level (Android 14+), the counterpart of iOS Increase Contrast.
 * Earlier versions expose no public setting, so they keep the standard palette.
 */
@Composable
private fun rememberIncreasedContrast(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
    val context = LocalContext.current
    val manager = remember(context) { context.getSystemService(UiModeManager::class.java) }
    val contrast = remember(manager) { mutableFloatStateOf(manager?.contrast ?: 0f) }
    DisposableEffect(manager) {
        val listener = UiModeManager.ContrastChangeListener { contrast.floatValue = it }
        manager?.addContrastChangeListener(context.mainExecutor, listener)
        onDispose { manager?.removeContrastChangeListener(listener) }
    }
    // Medium (0.5) and high (1.0) both count as an increased-contrast request.
    return contrast.floatValue >= 0.5f
}

@Composable
fun KitTheme(content: @Composable () -> Unit) {
    val kit = KitColors.resolve(isSystemInDarkTheme(), rememberIncreasedContrast())
    val scheme = if (kit.dark) {
        darkColorScheme(
            primary = kit.accent,
            onPrimary = kit.onAccent,
            secondary = kit.accentText,
            onSecondary = kit.onAccent,
            background = kit.background,
            onBackground = kit.ink,
            surface = kit.surface,
            onSurface = kit.ink,
            surfaceVariant = kit.surfaceMuted,
            onSurfaceVariant = kit.muted,
            outline = kit.line,
            error = Color(0xFFD93025)
        )
    } else {
        lightColorScheme(
            primary = kit.accent,
            onPrimary = kit.onAccent,
            secondary = kit.accentText,
            onSecondary = kit.onAccent,
            background = kit.background,
            onBackground = kit.ink,
            surface = kit.surface,
            onSurface = kit.ink,
            surfaceVariant = kit.surfaceMuted,
            onSurfaceVariant = kit.muted,
            outline = kit.line,
            error = Color(0xFFC4212B)
        )
    }
    CompositionLocalProvider(LocalKit provides kit) {
        MaterialTheme(
            colorScheme = scheme,
            typography = Typography(
                bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
                bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
                bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 16.sp),
                titleLarge = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold),
                labelSmall = TextStyle(fontSize = 11.sp)
            ),
            content = content
        )
    }
}

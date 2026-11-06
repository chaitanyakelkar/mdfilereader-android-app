package io.github.chaitanyakelkar.mdreader.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.chaitanyakelkar.mdreader.data.ThemeMode

val LocalReaderPalette = compositionLocalOf { ReaderPalette.Light }

fun Long.toColor(): Color = Color(this)

@Composable
fun resolvePalette(mode: ThemeMode): ReaderPalette = when (mode) {
    ThemeMode.SYSTEM -> if (isSystemInDarkTheme()) ReaderPalette.Dark else ReaderPalette.Light
    ThemeMode.LIGHT -> ReaderPalette.Light
    ThemeMode.DARK -> ReaderPalette.Dark
    ThemeMode.SEPIA -> ReaderPalette.Sepia
}

private fun ReaderPalette.toColorScheme(): ColorScheme {
    val accentColor = accent.toColor()
    val onAccent = background.toColor()
    val backgroundColor = background.toColor()
    val textColor = text.toColor()
    val mutedColor = muted.toColor()
    val code = codeBackground.toColor()
    val line = divider.toColor()
    val sheetColor = sheet.toColor()
    return if (isDark) {
        darkColorScheme(
            primary = accentColor, onPrimary = onAccent,
            primaryContainer = code, onPrimaryContainer = textColor,
            secondary = accentColor, onSecondary = onAccent,
            secondaryContainer = line, onSecondaryContainer = textColor,
            background = backgroundColor, onBackground = textColor,
            surface = backgroundColor, onSurface = textColor,
            surfaceVariant = code, onSurfaceVariant = mutedColor,
            surfaceContainerLowest = backgroundColor, surfaceContainerLow = sheetColor,
            surfaceContainer = sheetColor, surfaceContainerHigh = sheetColor,
            surfaceContainerHighest = code,
            outline = mutedColor, outlineVariant = line,
        )
    } else {
        lightColorScheme(
            primary = accentColor, onPrimary = onAccent,
            primaryContainer = code, onPrimaryContainer = textColor,
            secondary = accentColor, onSecondary = onAccent,
            secondaryContainer = line, onSecondaryContainer = textColor,
            background = backgroundColor, onBackground = textColor,
            surface = backgroundColor, onSurface = textColor,
            surfaceVariant = code, onSurfaceVariant = mutedColor,
            surfaceContainerLowest = backgroundColor, surfaceContainerLow = sheetColor,
            surfaceContainer = sheetColor, surfaceContainerHigh = sheetColor,
            surfaceContainerHighest = code,
            outline = mutedColor, outlineVariant = line,
        )
    }
}

@Composable
fun MdReaderTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val palette = resolvePalette(mode)
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.isDark
                isAppearanceLightNavigationBars = !palette.isDark
            }
        }
    }
    CompositionLocalProvider(LocalReaderPalette provides palette) {
        MaterialTheme(colorScheme = palette.toColorScheme(), content = content)
    }
}

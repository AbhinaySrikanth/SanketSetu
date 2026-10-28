package com.abhinay.ISLEnglishGujaratiApp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

// Palette: ink (text), teal (the single accent), paper (background). Everything else is a tint of these.
private val Ink = Color(0xFF1B2A41)
private val Teal = Color(0xFF0E6F6E)
private val Paper = Color(0xFFF6F4EF)

private val Light = lightColorScheme(
    primary = Teal, onPrimary = Color.White,
    primaryContainer = Color(0xFFD6ECEB), onPrimaryContainer = Color(0xFF053B3A),
    secondary = Ink, onSecondary = Color.White,
    background = Paper, onBackground = Ink,
    surface = Color.White, onSurface = Ink,
    surfaceVariant = Color(0xFFEAE7DF), onSurfaceVariant = Color(0xFF4B5565),
    outline = Color(0xFF7B8493), outlineVariant = Color(0xFFD9D5CA),
    error = Color(0xFFB3261E), onError = Color.White,
    secondaryContainer = Color(0xFFD6ECEB), onSecondaryContainer = Color(0xFF053B3A),
    surfaceTint = Teal, surfaceContainer = Color(0xFFEFECE4), surfaceContainerLow = Color.White,
    surfaceContainerHigh = Color(0xFFEAE7DF), surfaceContainerHighest = Color(0xFFEAE7DF),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF63C5C3), onPrimary = Color(0xFF00201F),
    primaryContainer = Color(0xFF0B4F4E), onPrimaryContainer = Color(0xFFD6ECEB),
    secondary = Color(0xFFC9D3E3), onSecondary = Ink,
    background = Color(0xFF0F1826), onBackground = Color(0xFFE8ECF1),
    surface = Color(0xFF172233), onSurface = Color(0xFFE8ECF1),
    surfaceVariant = Color(0xFF223046), onSurfaceVariant = Color(0xFFB4BFCF),
    outline = Color(0xFF8795AA), outlineVariant = Color(0xFF2E3D55),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    secondaryContainer = Color(0xFF0B4F4E), onSecondaryContainer = Color(0xFFD6ECEB),
    surfaceTint = Color(0xFF63C5C3), surfaceContainer = Color(0xFF172233), surfaceContainerLow = Color(0xFF172233),
    surfaceContainerHigh = Color(0xFF223046), surfaceContainerHighest = Color(0xFF223046),
)

private val Shape = Shapes(
    small = RoundedCornerShape(8.dp), medium = RoundedCornerShape(12.dp), large = RoundedCornerShape(16.dp),
)

@Composable
fun SanketSetuTheme(textScale: Int, highContrast: Boolean, content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    var scheme = if (dark) Dark else Light
    if (highContrast) scheme = if (dark) scheme.copy(onSurface = Color.White, onBackground = Color.White, outline = Color.White)
    else scheme.copy(onSurface = Color.Black, onBackground = Color.Black, onSurfaceVariant = Color.Black, outline = Color.Black, primary = Color(0xFF00514F))
    val density = LocalDensity.current
    val extra = floatArrayOf(1f, 1.15f, 1.3f)[textScale.coerceIn(0, 2)]
    CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale * extra)) {
        MaterialTheme(colorScheme = scheme, shapes = Shape, content = content)
    }
}

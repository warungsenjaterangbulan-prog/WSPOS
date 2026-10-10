package com.warungsenja.kasirterangbulan.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Obsidian & Warm Amber/Yellow Color Palette (Matching Web UI 100%)
val Zinc950 = Color(0xFF09090B)
val Zinc900 = Color(0xFF18181B)
val Zinc800 = Color(0xFF27272A)
val Zinc700 = Color(0xFF3F3F46)
val Zinc500 = Color(0xFF71717A)
val Zinc400 = Color(0xFFA1A1AA)
val Zinc300 = Color(0xFFD4D4D8)
val Zinc200 = Color(0xFFE4E4E7)
val Zinc100 = Color(0xFFF4F4F5)

val Yellow400 = Color(0xFFFACC15)
val Yellow300 = Color(0xFFFDE047)
val Amber500 = Color(0xFFF59E0B)

val Emerald500 = Color(0xFF10B981)
val Emerald400 = Color(0xFF34D399)
val Emerald300 = Color(0xFF6EE7B7)

val Red600 = Color(0xFFDC2626)
val Red500 = Color(0xFFEF4444)
val Red400 = Color(0xFFF87171)

val YellowAmberGradient = Brush.horizontalGradient(
    colors = listOf(Yellow400, Yellow400, Amber500)
)

private val DarkColorScheme = darkColorScheme(
    primary = Yellow400,
    onPrimary = Zinc950,
    secondary = Amber500,
    onSecondary = Zinc950,
    background = Zinc950,
    onBackground = Zinc100,
    surface = Zinc900,
    onSurface = Zinc100,
    surfaceVariant = Zinc800,
    onSurfaceVariant = Zinc300,
    error = Red400,
    onError = Zinc950
)

@Composable
fun KasirTerangBulanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}

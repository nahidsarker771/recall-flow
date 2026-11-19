package com.studyretention.tracker.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.studyretention.tracker.data.FocusPalette

/**
 * RecallFlow brand palette: a single indigo accent (#4F46E5) on soft,
 * low-chroma neutrals. All other roles are derived from that accent so the
 * app keeps one clear visual voice in both light and dark mode.
 */
val BrandIndigo = Color(0xFF4F46E5)

val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),
    inversePrimary = Color(0xFFA5B4FC),

    secondary = Color(0xFF5B6480),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E9F2),
    onSecondaryContainer = Color(0xFF171B2B),

    tertiary = Color(0xFF0F766E),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCCFBF1),
    onTertiaryContainer = Color(0xFF042F2E),

    background = Color(0xFFFAFAFC),
    onBackground = Color(0xFF111322),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111322),
    surfaceVariant = Color(0xFFF1F2F7),
    onSurfaceVariant = Color(0xFF646A7D),
    surfaceTint = BrandIndigo,
    inverseSurface = Color(0xFF2B2D3A),
    inverseOnSurface = Color(0xFFF4F4F8),

    surfaceDim = Color(0xFFE9E9F0),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F7FA),
    surfaceContainer = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFECECF3),
    surfaceContainerHighest = Color(0xFFE6E6EE),

    outline = Color(0xFFC6C9D6),
    outlineVariant = Color(0xFFE6E7EF),

    error = Color(0xFFD92D20),
    onError = Color.White,
    errorContainer = Color(0xFFFEE4E2),
    onErrorContainer = Color(0xFF4E0D08),
)

val DarkColors = darkColorScheme(
    primary = Color(0xFFA5B4FC),
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),
    inversePrimary = BrandIndigo,

    secondary = Color(0xFFA3ACC7),
    onSecondary = Color(0xFF1A1F30),
    secondaryContainer = Color(0xFF2A3044),
    onSecondaryContainer = Color(0xFFE6E9F2),

    tertiary = Color(0xFF5EEAD4),
    onTertiary = Color(0xFF042F2E),
    tertiaryContainer = Color(0xFF115E59),
    onTertiaryContainer = Color(0xFFCCFBF1),

    background = Color(0xFF0B0B10),
    onBackground = Color(0xFFE8E8EF),
    surface = Color(0xFF121218),
    onSurface = Color(0xFFE8E8EF),
    surfaceVariant = Color(0xFF1B1B24),
    onSurfaceVariant = Color(0xFF9EA3B6),
    surfaceTint = Color(0xFFA5B4FC),
    inverseSurface = Color(0xFFE8E8EF),
    inverseOnSurface = Color(0xFF1B1B24),

    surfaceDim = Color(0xFF0B0B10),
    surfaceBright = Color(0xFF2A2A35),
    surfaceContainerLowest = Color(0xFF08080C),
    surfaceContainerLow = Color(0xFF101017),
    surfaceContainer = Color(0xFF15151D),
    surfaceContainerHigh = Color(0xFF1C1C25),
    surfaceContainerHighest = Color(0xFF23232E),

    outline = Color(0xFF3F4050),
    outlineVariant = Color(0xFF2A2B37),

    error = Color(0xFFF97066),
    onError = Color(0xFF3B0A06),
    errorContainer = Color(0xFF5B1410),
    onErrorContainer = Color(0xFFFEE4E2),
)


/**
 * Calm focus accents. Each palette keeps the same low-chroma neutral surfaces
 * and only swaps the accent roles, so the app stays quiet and readable.
 */
private data class FocusAccent(
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val lightOnContainer: Color,
    val lightInverse: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
    val darkOnContainer: Color,
    val darkInverse: Color,
)

private fun accentFor(palette: FocusPalette): FocusAccent = when (palette) {
    FocusPalette.SAGE -> FocusAccent(
        lightPrimary = Color(0xFF5B7F68),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFDDEBE1),
        lightOnContainer = Color(0xFF16281D),
        lightInverse = Color(0xFFA9C9B4),
        darkPrimary = Color(0xFFA9C9B4),
        darkOnPrimary = Color(0xFF14301F),
        darkContainer = Color(0xFF3B5747),
        darkOnContainer = Color(0xFFDDEBE1),
        darkInverse = Color(0xFF5B7F68),
    )

    FocusPalette.OCEAN -> FocusAccent(
        lightPrimary = Color(0xFF44717D),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFD8E8EC),
        lightOnContainer = Color(0xFF122A30),
        lightInverse = Color(0xFF9FC5CE),
        darkPrimary = Color(0xFF9FC5CE),
        darkOnPrimary = Color(0xFF0F2A30),
        darkContainer = Color(0xFF2D4C55),
        darkOnContainer = Color(0xFFD8E8EC),
        darkInverse = Color(0xFF44717D),
    )

    FocusPalette.DUSK -> FocusAccent(
        lightPrimary = Color(0xFF6D6588),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFE6E2F0),
        lightOnContainer = Color(0xFF221D31),
        lightInverse = Color(0xFFC0B8D8),
        darkPrimary = Color(0xFFC0B8D8),
        darkOnPrimary = Color(0xFF272038),
        darkContainer = Color(0xFF4A4460),
        darkOnContainer = Color(0xFFE6E2F0),
        darkInverse = Color(0xFF6D6588),
    )

    FocusPalette.SAND -> FocusAccent(
        lightPrimary = Color(0xFF8A7355),
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFEEE6D8),
        lightOnContainer = Color(0xFF2B2216),
        lightInverse = Color(0xFFD0BB9C),
        darkPrimary = Color(0xFFD0BB9C),
        darkOnPrimary = Color(0xFF302517),
        darkContainer = Color(0xFF5C4C37),
        darkOnContainer = Color(0xFFEEE6D8),
        darkInverse = Color(0xFF8A7355),
    )

    FocusPalette.INDIGO -> FocusAccent(
        lightPrimary = BrandIndigo,
        lightOnPrimary = Color.White,
        lightContainer = Color(0xFFE0E7FF),
        lightOnContainer = Color(0xFF1E1B4B),
        lightInverse = Color(0xFFA5B4FC),
        darkPrimary = Color(0xFFA5B4FC),
        darkOnPrimary = Color(0xFF1E1B4B),
        darkContainer = Color(0xFF3730A3),
        darkOnContainer = Color(0xFFE0E7FF),
        darkInverse = BrandIndigo,
    )
}

fun focusColorScheme(palette: FocusPalette, dark: Boolean): ColorScheme {
    val accent = accentFor(palette)
    return if (dark) {
        DarkColors.copy(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = accent.darkOnContainer,
            inversePrimary = accent.darkInverse,
            surfaceTint = accent.darkPrimary,
        )
    } else {
        LightColors.copy(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = accent.lightOnContainer,
            inversePrimary = accent.lightInverse,
            surfaceTint = accent.lightPrimary,
        )
    }
}

fun focusPrimaryColor(palette: FocusPalette, dark: Boolean): Color {
    val accent = accentFor(palette)
    return if (dark) accent.darkPrimary else accent.lightPrimary
}

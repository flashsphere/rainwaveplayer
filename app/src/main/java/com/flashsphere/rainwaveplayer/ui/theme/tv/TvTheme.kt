package com.flashsphere.rainwaveplayer.ui.theme.tv

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme
import com.flashsphere.rainwaveplayer.ui.theme.AppTheme
import com.flashsphere.rainwaveplayer.ui.theme.backgroundDark
import com.flashsphere.rainwaveplayer.ui.theme.errorContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.errorDark
import com.flashsphere.rainwaveplayer.ui.theme.inverseOnSurfaceDark
import com.flashsphere.rainwaveplayer.ui.theme.inversePrimaryDark
import com.flashsphere.rainwaveplayer.ui.theme.inverseSurfaceDark
import com.flashsphere.rainwaveplayer.ui.theme.onBackgroundDark
import com.flashsphere.rainwaveplayer.ui.theme.onErrorContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.onErrorDark
import com.flashsphere.rainwaveplayer.ui.theme.onPrimaryContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.onPrimaryDark
import com.flashsphere.rainwaveplayer.ui.theme.onSecondaryContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.onSecondaryDark
import com.flashsphere.rainwaveplayer.ui.theme.onSurfaceDark
import com.flashsphere.rainwaveplayer.ui.theme.onSurfaceVariantDark
import com.flashsphere.rainwaveplayer.ui.theme.onTertiaryContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.onTertiaryDark
import com.flashsphere.rainwaveplayer.ui.theme.primaryContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.primaryDark
import com.flashsphere.rainwaveplayer.ui.theme.scrimDark
import com.flashsphere.rainwaveplayer.ui.theme.secondaryContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.secondaryDark
import com.flashsphere.rainwaveplayer.ui.theme.surfaceDark
import com.flashsphere.rainwaveplayer.ui.theme.surfaceVariantDark
import com.flashsphere.rainwaveplayer.ui.theme.tertiaryContainerDark
import com.flashsphere.rainwaveplayer.ui.theme.tertiaryDark

@Composable
fun TvAppTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = darkScheme,
        typography = TvAppTypography,
        content = {
            AppTheme {
                content()
            }
        }
    )
}

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
)

package com.mgradio.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mgradio.app.domain.model.ThemeMode

private val AmoledDarkColorScheme = darkColorScheme(
    primary = PrimaryCyan,
    onPrimary = Color.Black,
    secondary = SecondaryTeal,
    onSecondary = Color.Black,
    background = AmoledBackground,
    onBackground = AmoledOnBackground,
    surface = AmoledSurface,
    onSurface = AmoledOnBackground,
    surfaceVariant = AmoledSurfaceVariant,
    onSurfaceVariant = AmoledOnSurfaceVariant,
    error = ErrorCoral,
    onError = Color.Black
)

private val LightRadioColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = Color.White,
    secondary = LightSecondary,
    onSecondary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnBackground,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Color(0xFFCF222E),
    onError = Color.White
)

@Composable
fun MgRadioTheme(
    themeMode: ThemeMode,
    useDynamicColor: Boolean,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.AMOLED_DARK -> true
    }

    val context = LocalContext.current
    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> AmoledDarkColorScheme
        else -> LightRadioColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MgRadioTypography,
        content = content
    )
}

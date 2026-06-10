package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = SleekPrimaryDark,
    onPrimary = Color(0xFF003916),
    primaryContainer = SleekPrimaryContainerDark,
    onPrimaryContainer = SleekOnPrimaryContainerDark,
    secondary = SleekPrimaryDark,
    onSecondary = Color(0xFF003916),
    secondaryContainer = SleekSecondaryContainerDark,
    onSecondaryContainer = SleekTextDark,
    tertiary = SleekAccentPeach,
    background = SleekBackgroundDark,
    onBackground = SleekTextDark,
    surface = SleekBackgroundDark,
    onSurface = SleekTextDark,
    surfaceVariant = SleekSecondaryContainerDark,
    onSurfaceVariant = SleekTextDark,
    outline = SleekBorderDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = SleekPrimaryLight,
    onPrimary = Color.White,
    primaryContainer = SleekPrimaryContainerLight,
    onPrimaryContainer = SleekOnPrimaryContainerLight,
    secondary = SleekPrimaryLight,
    onSecondary = Color.White,
    secondaryContainer = SleekSecondaryContainerLight,
    onSecondaryContainer = SleekSecondaryText,
    tertiary = SleekAccentPeach,
    onTertiary = Color(0xFF551D0D),
    tertiaryContainer = SleekAccentPink,
    onTertiaryContainer = Color(0xFF2B121D),
    background = SleekBackgroundLight,
    onBackground = SleekTextLight,
    surface = SleekBackgroundLight,
    onSurface = SleekTextLight,
    surfaceVariant = SleekSecondaryContainerLight,
    onSurfaceVariant = SleekSecondaryText,
    outline = SleekBorderLight
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

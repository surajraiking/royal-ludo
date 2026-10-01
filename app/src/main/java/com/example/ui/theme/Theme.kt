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

private val DarkColorScheme =
  darkColorScheme(
    primary = GoldPrimary,
    secondary = NeonCyan,
    tertiary = LudoRed,
    background = DeepDarkBg,
    surface = GlassCard,
    onPrimary = DeepDarkBg,
    onSecondary = DeepDarkBg,
    onBackground = TextWhite,
    onSurface = TextWhite
  )

private val LightColorScheme = DarkColorScheme // We enforce the beautiful dark premium theme because Ludo King is bright and we want "Royal Ludo Empire" to have a high-contrast neon premium night vibe.

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme for the premium aesthetic
  dynamicColor: Boolean = false, // Disable dynamic colors to keep royal gold and neon branding uniform
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}

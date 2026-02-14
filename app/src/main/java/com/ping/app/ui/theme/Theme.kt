package com.ping.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val HackerGreen = Color(0xFF00FF66)
private val HackerGreenDim = Color(0xFF1EBE62)
private val HackerBlack = Color(0xFF020402)
private val HackerSurface = Color(0xFF081208)
private val HackerSurfaceVariant = Color(0xFF0D1D0D)

private val PingHackerColorScheme = darkColorScheme(
    primary = HackerGreen,
    onPrimary = Color.Black,
    secondary = HackerGreenDim,
    onSecondary = Color.Black,
    background = HackerBlack,
    onBackground = HackerGreen,
    surface = HackerSurface,
    onSurface = HackerGreen,
    surfaceVariant = HackerSurfaceVariant,
    onSurfaceVariant = HackerGreenDim,
)

@Composable
fun PingTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PingHackerColorScheme,
        content = content,
    )
}

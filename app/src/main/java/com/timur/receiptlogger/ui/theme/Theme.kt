package com.timur.receiptlogger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GreenPrimary = Color(0xFF1B5E20)
private val GreenPrimaryDark = Color(0xFF66BB6A)

private val LightColors = lightColorScheme(
    primary = GreenPrimary,
    secondary = Color(0xFF4CAF50)
)

private val DarkColors = darkColorScheme(
    primary = GreenPrimaryDark,
    secondary = Color(0xFF81C784)
)

@Composable
fun ReceiptLoggerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

package com.erfansvsh.isocompressor.ui.theme

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

@Composable
fun ISOCompressorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    customSeedColor: Color = Color(0xFF2196F3),
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> {
            darkColorScheme(
                primary = customSeedColor,
                onPrimary = if (isLightColor(customSeedColor)) Color.Black else Color.White,
                primaryContainer = customSeedColor.copy(alpha = 0.3f),
                onPrimaryContainer = Color.White,
                secondary = customSeedColor.copy(alpha = 0.8f),
                onSecondary = Color.Black,
                surface = Color(0xFF111318),
                onSurface = Color(0xFFE2E2E6)
            )
        }
        else -> {
            lightColorScheme(
                primary = customSeedColor,
                onPrimary = if (isLightColor(customSeedColor)) Color.Black else Color.White,
                primaryContainer = customSeedColor.copy(alpha = 0.2f),
                onPrimaryContainer = customSeedColor,
                secondary = customSeedColor.copy(alpha = 0.7f),
                onSecondary = Color.White,
                surface = Color(0xFFFBFBFC),
                onSurface = Color(0xFF1A1C1E)
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299 * color.red + 0.587 * color.green + 0.114 * color.blue
    return luminance > 0.6
}

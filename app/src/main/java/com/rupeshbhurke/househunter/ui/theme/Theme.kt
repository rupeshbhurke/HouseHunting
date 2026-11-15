package com.rupeshbhurke.househunter.ui.theme

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

private val Teal40 = Color(0xFF006A60)
private val Teal80 = Color(0xFF53DBC9)
private val Amber40 = Color(0xFF7A5900)
private val Amber80 = Color(0xFFF8BD2A)

private val LightColors = lightColorScheme(
    primary = Teal40,
    secondary = Amber40,
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    secondary = Amber80,
)

@Composable
fun HouseHunterTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

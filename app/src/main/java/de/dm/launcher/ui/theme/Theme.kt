package de.dm.launcher.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PureBlackScheme = darkColorScheme(
    background = Color.Black,
    surface = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    primary = Color.White,
    onPrimary = Color.Black,
    secondary = Color(0xFFB0B0B0),
    onSecondary = Color.Black
)

@Composable
fun LauncherTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = PureBlackScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}

package com.nikidayn.taskbox.ui.theme

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

// Темна тема: Синій акцент, темний фон, темно-сірі картки
private val DarkColorScheme = darkColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandBlueLight,
    tertiary = Pink80, // Можна залишити для дрібних акцентів або замінити на синій

    background = BackgroundDark,    // Чорний фон (0xFF121212)
    surface = SurfaceDark,          // Темно-сірі картки (0xFF252525)

    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark
)

// Світла тема: Синій акцент, світлий фон, білі картки
private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    secondary = BrandBlueDark,
    tertiary = Pink40,

    background = BackgroundLight,
    surface = SurfaceWhite,

    onBackground = TextPrimaryLight,
    onSurface = TextPrimaryLight
)

@Composable
fun TaskBoxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Вимкнено, щоб зберегти наш синій стиль
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
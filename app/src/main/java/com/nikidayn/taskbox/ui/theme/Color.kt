package com.nikidayn.taskbox.ui.theme

import androidx.compose.ui.graphics.Color

// --- ОСНОВНА ПАЛІТРА (Blue / Синя) ---
val BrandBlue = Color(0xFF4A90E2)       // Основний синій
val BrandBlueLight = Color(0xFFD0E4FF)  // Світло-синій (для фонів у світлій темі)
val BrandBlueDark = Color(0xFF003366)   // Темно-синій (для тексту/іконок)

// --- КОЛЬОРИ ФОНУ ---
val BackgroundLight = Color(0xFFF5F7FA) // Світло-сірий (Світла тема)
val BackgroundDark = Color(0xFF121212)  // Майже чорний (Темна тема)

// --- КОЛЬОРИ КАРТОК (Surface) ---
val SurfaceWhite = Color(0xFFFFFFFF)    // Біла картка (Світла тема)
val SurfaceDark = Color(0xFF252525)     // Темно-сіра картка (Темна тема) - Світліша за фон!

// --- ТЕКСТ ---
val TextPrimaryLight = Color(0xFF1A1C1E)
val TextPrimaryDark = Color(0xFFE2E2E6) // Майже білий

// --- СТАРІ КОЛЬОРИ (Можна залишити або видалити, якщо не використовуються) ---
val Pink80 = Color(0xFFEFB8C8)
val Pink40 = Color(0xFF7D5260)

// Функція контрасту (залишається без змін)
fun getContrastColor(hexColor: String): Color {
    val color = try {
        android.graphics.Color.parseColor(hexColor)
    } catch (e: Exception) {
        android.graphics.Color.WHITE
    }
    val darkness = 1 - (0.299 * android.graphics.Color.red(color) +
            0.587 * android.graphics.Color.green(color) +
            0.114 * android.graphics.Color.blue(color)) / 255
    return if (darkness < 0.5) Color.Black else Color.White
}

// Список кольорів для вибору (HEX)
val TaskPalette = listOf(
    "#4A90E2", // Синій (Brand)
    "#50E3C2", // Бірюзовий
    "#B8E986", // Салатовий
    "#F5A623", // Оранжевий
    "#FF5E57", // Червоний
    "#D0021B", // Темно-червоний
    "#BD10E0", // Фіолетовий
    "#9013FE", // Темно-фіолетовий
    "#9B9B9B", // Сірий
    "#4A4A4A"  // Темно-сірий
)
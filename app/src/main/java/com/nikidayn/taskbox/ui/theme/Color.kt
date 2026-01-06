package com.nikidayn.taskbox.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// --- ДОДАЙТЕ ЦЕЙ КОД НИЖЧЕ ---

// Функція для визначення контрастного кольору тексту (чорний або білий)
fun getContrastColor(hexColor: String): Color {
    val color = try {
        // Парсимо HEX-код кольору (наприклад, "#FFEB3B")
        android.graphics.Color.parseColor(hexColor)
    } catch (e: Exception) {
        android.graphics.Color.WHITE
    }

    // Формула яскравості: визначаємо, наскільки світлий цей колір
    val darkness = 1 - (0.299 * android.graphics.Color.red(color) +
            0.587 * android.graphics.Color.green(color) +
            0.114 * android.graphics.Color.blue(color)) / 255

    // Якщо фон світлий (darkness < 0.5), повертаємо чорний текст. Інакше - білий.
    return if (darkness < 0.5) Color.Black else Color.White
}
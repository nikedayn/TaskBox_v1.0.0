package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun EmojiSelectorDialog(
    onDismiss: () -> Unit,
    onEmojiSelected: (String) -> Unit
) {
    // Структура даних: Категорія -> Список смайликів
    val categories = remember {
        mapOf(
            "Популярні" to listOf("⚡", "📅", "✅", "🔥", "❤️", "⚠️", "⏰", "📌"),
            "Робота та Навчання" to listOf("📝", "💻", "💼", "📚", "📖", "🎓", "📞", "🗂️", "📊", "🖊️", "🧠"),
            "Дім та Побут" to listOf("🏠", "🧹", "🛒", "🚿", "🛏️", "🪥", "🍽️", "🛠️", "👕", "🪴", "💊", "🧸"),
            "Активність та Спорт" to listOf("🏃", "💪", "🧘", "⚽", "🏀", "🏊", "🚲", "🎯", "🏆", "🚶"),
            "Їжа та Напої" to listOf("☕", "🍎", "🍕", "🥗", "🥪", "🥤", "🍰", "🍌", "🥕", "🍳"),
            "Транспорт та Подорожі" to listOf("🚲", "🚗", "🚌", "✈️", "🚆", "🚇", "🚋", "🚉", "🗺️", "⛽", "🎫", "🚦", "🌍"),
            "Розваги" to listOf("🏓", "🏸", "🥍", "🎾", "🎮", "🎵", "🎨", "🎬", "🎉", "🎁", "🎲", "🎧", "📸")
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier
                .padding(16.dp)
                .heightIn(max = 500.dp) // Обмеження висоти, щоб діалог не вилазив за екран
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Оберіть іконку",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f) // Займає доступний простір
                ) {
                    categories.forEach { (categoryName, emojis) ->
                        // ЗАГОЛОВОК КАТЕГОРІЇ
                        item(span = { GridItemSpan(maxLineSpan) }) {
                            Text(
                                text = categoryName,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .padding(top = 16.dp, bottom = 8.dp)
                                    .fillMaxWidth()
                            )
                        }

                        // СМАЙЛИКИ ЦІЄЇ КАТЕГОРІЇ
                        items(emojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .clickable { onEmojiSelected(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss) {
                    Text("Скасувати")
                }
            }
        }
    }
}
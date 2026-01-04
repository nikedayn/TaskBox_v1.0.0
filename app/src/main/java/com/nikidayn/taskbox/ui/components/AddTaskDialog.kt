package com.nikidayn.taskbox.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import android.app.TimePickerDialog
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import com.nikidayn.taskbox.utils.minutesToTime // Використаємо вашу функцію

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, duration: Int, startTime: Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var durationText by remember { mutableStateOf("30") }

    // Зберігаємо час як Int (хвилини) або null
    var selectedStartTime by remember { mutableStateOf<Int?>(null) }

    // Отримуємо контекст для запуску системного годинника
    val context = LocalContext.current

    // Функція для показу годинника
    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                // Конвертуємо вибір у хвилини (наприклад 1:30 -> 90)
                selectedStartTime = (hourOfDay * 60) + minute
            },
            currentHour,
            currentMinute,
            true // 24-годинний формат
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Нове завдання") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва справи") },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = durationText,
                    onValueChange = {
                        // Захист від введення не цифр прямо під час друку
                        if (it.all { char -> char.isDigit() }) {
                            durationText = it
                        }
                    },
                    label = { Text("Тривалість (хв)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                // Кнопка вибору часу замість введення тексту
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedStartTime != null)
                            "Початок: ${minutesToTime(selectedStartTime!!)}"
                        else "Без часу (у Вхідні)",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    TextButton(onClick = showTimePicker) {
                        Text(if (selectedStartTime != null) "Змінити" else "Вибрати час")
                    }
                }

                // Кнопка очищення часу (якщо передумали і хочете у Вхідні)
                if (selectedStartTime != null) {
                    TextButton(
                        onClick = { selectedStartTime = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Прибрати час (у Вхідні)", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val duration = durationText.toIntOrNull() ?: 30
                    if (title.isNotBlank()) {
                        onConfirm(title, duration, selectedStartTime)
                    }
                }
            ) {
                Text("Зберегти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}
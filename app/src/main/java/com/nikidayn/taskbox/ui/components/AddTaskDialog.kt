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
import com.nikidayn.taskbox.utils.minutesToTime

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, duration: Int, startTime: Int?) -> Unit
) {
    var title by remember { mutableStateOf("") }

    // ЗМІНА: Замість durationText створюємо дві змінні
    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("30") } // За замовчуванням 30 хв

    var selectedStartTime by remember { mutableStateOf<Int?>(null) }
    val context = LocalContext.current

    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedStartTime = (hourOfDay * 60) + minute
            },
            currentHour,
            currentMinute,
            true
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

                // ЗМІНА: Рядок з двома полями (Години і Хвилини)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) hoursText = it },
                        label = { Text("Год") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { if (it.all { char -> char.isDigit() }) minutesText = it },
                        label = { Text("Хв") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

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
                    // ЗМІНА: Рахуємо загальну тривалість
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minutesText.toIntOrNull() ?: 0
                    val totalDuration = (h * 60) + m

                    // Перевіряємо, щоб тривалість була хоча б 5 хвилин (або ваша логіка)
                    val finalDuration = if (totalDuration > 0) totalDuration else 30

                    if (title.isNotBlank()) {
                        onConfirm(title, finalDuration, selectedStartTime)
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
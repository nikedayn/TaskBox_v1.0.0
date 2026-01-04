package com.nikidayn.taskbox.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.utils.minutesToTime

@Composable
fun EditTaskDialog(
    task: Task, // Приймаємо існуюче завдання
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newDuration: Int, newStartTime: Int?) -> Unit
) {
    // Ініціалізуємо змінні даними з завдання
    var title by remember { mutableStateOf(task.title) }
    var durationText by remember { mutableStateOf(task.durationMinutes.toString()) }
    var selectedStartTime by remember { mutableStateOf(task.startTimeMinutes) }

    val context = LocalContext.current

    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
        // Якщо час вже є, відкриваємо годинник на ньому, якщо ні - на поточному часі
        val initialHour = selectedStartTime?.div(60) ?: calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val initialMinute = selectedStartTime?.rem(60) ?: calendar.get(java.util.Calendar.MINUTE)

        TimePickerDialog(
            context,
            { _, hourOfDay, minute ->
                selectedStartTime = (hourOfDay * 60) + minute
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Редагувати справу") },
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
                        if (it.all { char -> char.isDigit() }) durationText = it
                    },
                    label = { Text("Тривалість (хв)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

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
                        Text("Змінити час")
                    }
                }

                if (selectedStartTime != null) {
                    TextButton(
                        onClick = { selectedStartTime = null },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Прибрати час", color = MaterialTheme.colorScheme.error)
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
                Text("Зберегти зміни")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}
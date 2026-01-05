package com.nikidayn.taskbox.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newDuration: Int, newStartTime: Int?) -> Unit,
    onDelete: () -> Unit // Параметр для видалення
) {
    var title by remember { mutableStateOf(task.title) }

    // --- ЛОГІКА РОЗДІЛЕННЯ ЧАСУ ---
    // Ініціалізуємо поля на основі існуючої тривалості
    var hoursText by remember {
        val h = task.durationMinutes / 60
        mutableStateOf(if (h > 0) h.toString() else "")
    }
    var minutesText by remember {
        val m = task.durationMinutes % 60
        mutableStateOf(m.toString())
    }
    // -----------------------------

    var selectedStartTime by remember { mutableStateOf(task.startTimeMinutes) }
    val context = LocalContext.current

    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
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
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Редагувати справу")
                // Кнопка видалення
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Видалити",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва справи") },
                    modifier = Modifier.fillMaxWidth()
                )

                // --- ДВА ПОЛЯ ДЛЯ ВВЕДЕННЯ ---
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
                // -----------------------------

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
                    // Збираємо години і хвилини назад у durationMinutes
                    val h = hoursText.toIntOrNull() ?: 0
                    val m = minutesText.toIntOrNull() ?: 0
                    val totalDuration = (h * 60) + m

                    // Мінімум 5 хвилин (або 30, якщо 0)
                    val finalDuration = if (totalDuration > 0) totalDuration else 30

                    if (title.isNotBlank()) {
                        onConfirm(title, finalDuration, selectedStartTime)
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
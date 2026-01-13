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
import com.nikidayn.taskbox.utils.minutesToTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import androidx.compose.ui.viewinterop.AndroidView // Якщо не імпортувалося автоматично
import android.widget.NumberPicker

@Composable
fun AddTaskDialog(
    selectedDate: String,
    onDismiss: () -> Unit,
    onConfirm: (title: String, duration: Int, startTime: Int?, date: String) -> Unit
) {
    var title by remember { mutableStateOf("") }

    // 👇 ЗМІНЕНО: Замість hoursText/minutesText використовуємо одне число
    var durationMinutes by remember { mutableStateOf(30) }

    var selectedStartTime by remember { mutableStateOf<Int?>(null) }

    val displayDate = remember(selectedDate) {
        try {
            LocalDate.parse(selectedDate).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        } catch (e: Exception) {
            selectedDate
        }
    }

    val context = LocalContext.current
    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        TimePickerDialog(context, { _, h, m -> selectedStartTime = (h * 60) + m }, currentHour, currentMinute, true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Справа на $displayDate") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Назва справи") }, modifier = Modifier.fillMaxWidth()
                )

                // 👇 ВСТАВЛЕНО: Новий компонент вибору "барабаном"
                Text("Тривалість:", style = MaterialTheme.typography.labelMedium)
                DurationWheelPicker(
                    durationMinutes = durationMinutes,
                    onDurationChange = { durationMinutes = it }
                )
                // 👆 КІНЕЦЬ ЗМІН

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (selectedStartTime != null) "Початок: ${minutesToTime(selectedStartTime!!)}" else "Без часу (у Вхідні)")
                    TextButton(onClick = showTimePicker) { Text(if (selectedStartTime != null) "Змінити" else "Вибрати час") }
                }
                if (selectedStartTime != null) {
                    TextButton(onClick = { selectedStartTime = null }, modifier = Modifier.align(Alignment.End)) {
                        Text("Прибрати час", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                // Якщо 0 хвилин, ставимо 30 за замовчуванням
                val finalDuration = if (durationMinutes > 0) durationMinutes else 30
                if (title.isNotBlank()) onConfirm(title, finalDuration, selectedStartTime, selectedDate)
            }) { Text("Створити") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
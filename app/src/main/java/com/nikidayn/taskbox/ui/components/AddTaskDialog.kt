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

@Composable
fun AddTaskDialog(
    selectedDate: String, // НОВИЙ ПАРАМЕТР: Дата, на яку створюємо
    onDismiss: () -> Unit,
    // ОНОВЛЕНО: Повертаємо також дату (хоча вона зазвичай та сама, але для гнучкості)
    onConfirm: (title: String, duration: Int, startTime: Int?, date: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var hoursText by remember { mutableStateOf("") }
    var minutesText by remember { mutableStateOf("30") }
    var selectedStartTime by remember { mutableStateOf<Int?>(null) }

    // Ми створюємо саме на цю дату
    val taskDate by remember { mutableStateOf(selectedDate) }

    val context = LocalContext.current
    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMinute = calendar.get(java.util.Calendar.MINUTE)
        TimePickerDialog(context, { _, h, m -> selectedStartTime = (h * 60) + m }, currentHour, currentMinute, true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Справа на $taskDate") }, // Показуємо дату в заголовку
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Назва справи") }, modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hoursText, onValueChange = { if (it.all { c -> c.isDigit() }) hoursText = it },
                        label = { Text("Год") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutesText, onValueChange = { if (it.all { c -> c.isDigit() }) minutesText = it },
                        label = { Text("Хв") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }
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
                val h = hoursText.toIntOrNull() ?: 0
                val m = minutesText.toIntOrNull() ?: 0
                val totalDuration = (h * 60) + m
                val finalDuration = if (totalDuration > 0) totalDuration else 30
                if (title.isNotBlank()) onConfirm(title, finalDuration, selectedStartTime, taskDate)
            }) { Text("Створити") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
package com.nikidayn.taskbox.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.utils.minutesToTime

@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newDuration: Int, newStartTime: Int?, newParentId: Int?, newIsLocked: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var isLocked by remember { mutableStateOf(task.isLocked) }

    // Час (години та хвилини)
    var hoursText by remember {
        val h = task.durationMinutes / 60
        mutableStateOf(if (h > 0) h.toString() else "")
    }
    var minutesText by remember {
        val m = task.durationMinutes % 60
        mutableStateOf(m.toString())
    }

    var selectedStartTime by remember { mutableStateOf(task.startTimeMinutes) }
    var parentId by remember { mutableStateOf(task.linkedParentId) }

    val context = LocalContext.current
    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
        val initialHour = selectedStartTime?.div(60) ?: calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val initialMinute = selectedStartTime?.rem(60) ?: calendar.get(java.util.Calendar.MINUTE)
        TimePickerDialog(context, { _, h, m -> selectedStartTime = (h * 60) + m }, initialHour, initialMinute, true).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Редагувати справу")
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Видалити", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // 1. Назва
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва") },
                    modifier = Modifier.fillMaxWidth()
                )

                // 2. Тривалість (Год : Хв)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) hoursText = it },
                        label = { Text("Год") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) minutesText = it },
                        label = { Text("Хв") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. РЯДОК ДІЙ: Прив'язка + Замок
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка Ланцюжка (Прив'язати)
                    OutlinedButton(
                        onClick = {
                            if (parentId == null) {
                                // Тестова логіка: прив'язати до попередньої (ID - 1)
                                parentId = (task.id - 1).coerceAtLeast(1)
                            } else {
                                parentId = null
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (parentId != null) Icons.Default.Link else Icons.Default.LinkOff,
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (parentId != null) "ID: $parentId" else "Прив'язати",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Кнопка Замка (Стіна)
                    FilledTonalIconToggleButton(
                        checked = isLocked,
                        onCheckedChange = { isLocked = it }
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock task",
                            tint = if (isLocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // 4. Час початку
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (selectedStartTime != null)
                            "Початок: ${minutesToTime(selectedStartTime!!)}"
                        else "Без часу"
                    )
                    TextButton(onClick = showTimePicker) { Text("Змінити час") }
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
            Button(onClick = {
                val h = hoursText.toIntOrNull() ?: 0
                val m = minutesText.toIntOrNull() ?: 0
                val finalDuration = if ((h * 60 + m) > 0) (h * 60 + m) else 30

                if (title.isNotBlank()) {
                    // Передаємо всі 5 параметрів
                    onConfirm(title, finalDuration, selectedStartTime, parentId, isLocked)
                }
            }) { Text("Зберегти зміни") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Скасувати") }
        }
    )
}
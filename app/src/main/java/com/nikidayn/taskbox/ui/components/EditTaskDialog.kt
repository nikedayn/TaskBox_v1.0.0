package com.nikidayn.taskbox.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikidayn.taskbox.model.Note
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.utils.minutesToTime
import com.nikidayn.taskbox.ui.theme.getContrastColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    potentialParents: List<Task>,
    // НОВІ ПАРАМЕТРИ ДЛЯ НОТАТОК
    linkedNote: Note?,              // Вже прикріплена нотатка (якщо є)
    availableNotes: List<Note>,     // Список вільних нотаток для вибору
    onAttachNote: (Note) -> Unit,   // Колбек прив'язки існуючої
    onCreateNote: (String) -> Unit, // Колбек створення нової (передаємо назву)
    onDetachNote: (Note) -> Unit,   // Колбек відв'язки
    // ---
    onDismiss: () -> Unit,
    onConfirm: (newTitle: String, newDuration: Int, newStartTime: Int?, newParentId: Int?, newIsLocked: Boolean) -> Unit,
    onDelete: () -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var isLocked by remember { mutableStateOf(task.isLocked) }

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
    var isParentMenuExpanded by remember { mutableStateOf(false) }

    // Для меню вибору нотатки
    var isNoteMenuExpanded by remember { mutableStateOf(false) }

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
                    value = title, onValueChange = { title = it },
                    label = { Text("Назва") }, modifier = Modifier.fillMaxWidth()
                )

                // 2. Тривалість
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

                // 3. БЛОК НОТАТОК (НОВЕ)
                HorizontalDivider()
                Text("Нотатка", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)

                if (linkedNote != null) {
                    // 1. Рахуємо колір тексту для нотатки
                    val noteTextColor = getContrastColor(linkedNote.colorHex)

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(android.graphics.Color.parseColor(linkedNote.colorHex)),
                            contentColor = noteTextColor // <--- ЗАСТОСОВУЄМО КОЛІР
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    linkedNote.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                    maxLines = 1,
                                    color = noteTextColor // <--- Явно
                                )
                                Text(
                                    linkedNote.content.take(30),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = noteTextColor // <--- Явно
                                )
                            }
                            IconButton(onClick = { onDetachNote(linkedNote) }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Detach",
                                    tint = noteTextColor.copy(alpha = 0.6f) // <--- Іконка теж адаптується
                                )
                            }
                        }
                    }
                } else {
                    // Якщо нотатки немає - кнопки створення або вибору
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Кнопка створення стандартної нотатки
                        Button(
                            onClick = {
                                val autoTitle = "Нотатка для ${task.title} (${task.date})"
                                onCreateNote(autoTitle)
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Створити")
                        }

                        // Кнопка вибору зі списку
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { isNoteMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Обрати")
                            }
                            DropdownMenu(
                                expanded = isNoteMenuExpanded,
                                onDismissRequest = { isNoteMenuExpanded = false }
                            ) {
                                if (availableNotes.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Немає вільних нотаток") }, onClick = {})
                                } else {
                                    availableNotes.forEach { note ->
                                        DropdownMenuItem(
                                            text = { Text(note.title.ifBlank { "Без назви" }, maxLines = 1) },
                                            onClick = {
                                                onAttachNote(note)
                                                isNoteMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider()

                // 4. Прив'язка до батьківського (Task Parent)
                val currentParentTitle = potentialParents.find { it.id == parentId }?.title ?: "Немає прив'язки"
                ExposedDropdownMenuBox(
                    expanded = isParentMenuExpanded,
                    onExpandedChange = { isParentMenuExpanded = !isParentMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = currentParentTitle, onValueChange = {}, readOnly = true,
                        label = { Text("Прив'язати до...") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentMenuExpanded) },
                        leadingIcon = { Icon(if (parentId != null) Icons.Default.Link else Icons.Default.LinkOff, null) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(expanded = isParentMenuExpanded, onDismissRequest = { isParentMenuExpanded = false }) {
                        DropdownMenuItem(text = { Text("Немає прив'язки") }, onClick = { parentId = null; isParentMenuExpanded = false })
                        potentialParents.filter { it.id != task.id }.forEach { p ->
                            DropdownMenuItem(
                                text = { Text(p.title) },
                                onClick = { parentId = p.id; isParentMenuExpanded = false }
                            )
                        }
                    }
                }

                // 5. Замок та Час
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Зафіксувати час?", modifier = Modifier.weight(1f))
                    Switch(checked = isLocked, onCheckedChange = { isLocked = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(text = if (selectedStartTime != null) "Початок: ${minutesToTime(selectedStartTime!!)}" else "Без часу")
                    TextButton(onClick = showTimePicker) { Text("Змінити час") }
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
                val finalDuration = if ((h * 60 + m) > 0) (h * 60 + m) else 30
                if (title.isNotBlank()) onConfirm(title, finalDuration, selectedStartTime, parentId, isLocked)
            }) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
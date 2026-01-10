package com.nikidayn.taskbox.ui.components

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.nikidayn.taskbox.model.Note
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.ui.theme.getContrastColor
import com.nikidayn.taskbox.utils.minutesToTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    potentialParents: List<Task>,
    linkedNote: Note?,
    availableNotes: List<Note>,
    onAttachNote: (Note) -> Unit,
    onDetachNote: (Note) -> Unit,
    onCreateNote: (String) -> Unit,
    onDismiss: () -> Unit,
    // ОНОВЛЕНО: додано параметр newEmoji
    onConfirm: (newTitle: String, newDuration: Int, newStartTime: Int?, newParentId: Int?, newIsLocked: Boolean, newDate: String, newColor: String, newEmoji: String) -> Unit,
    onDelete: () -> Unit
) {
    // 1. Більше не "парсимо" назву, беремо як є
    var title by remember { mutableStateOf(task.title) }

    // 2. Беремо смайлик з поля iconEmoji (переконайтесь, що воно є в Task.kt)
    var emoji by remember { mutableStateOf(task.iconEmoji) }
    var showEmojiPicker by remember { mutableStateOf(false) }

    var isLocked by remember { mutableStateOf(task.isLocked) }
    var selectedColor by remember { mutableStateOf(task.colorHex) }

    var selectedDate by remember {
        mutableStateOf(try { LocalDate.parse(task.date) } catch (e: Exception) { LocalDate.now() })
    }
    var showDatePicker by remember { mutableStateOf(false) }

    // Duration State
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
    var isNoteMenuExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val showTimePicker = {
        val calendar = java.util.Calendar.getInstance()
        val initialHour = selectedStartTime?.div(60) ?: calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val initialMinute = selectedStartTime?.rem(60) ?: calendar.get(java.util.Calendar.MINUTE)
        TimePickerDialog(context, { _, h, m -> selectedStartTime = (h * 60) + m }, initialHour, initialMinute, true).show()
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = LocalDate.ofEpochDay(millis / (24 * 60 * 60 * 1000))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") } }
        ) { DatePicker(state = datePickerState) }
    }

    // --- ВІКНО ПІКЕРА СМАЙЛИКІВ ---
    if (showEmojiPicker) {
        EmojiSelectorDialog(
            onDismiss = { showEmojiPicker = false },
            onEmojiSelected = { selected ->
                emoji = selected
                showEmojiPicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showDatePicker = true }
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedDate.format(DateTimeFormatter.ofPattern("EEE, d MMM")),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Видалити", tint = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // 1. НАЗВА ТА СМАЙЛИК
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Кнопка-смайлик
                    Surface(
                        onClick = { showEmojiPicker = true },
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(56.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }

                    // Поле назви
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Назва справи") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // 2. КОЛІР
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Колір картки", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    ColorSelector(
                        selectedColorHex = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }

                // 3. ТРИВАЛІСТЬ
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) hoursText = it },
                        label = { Text("Години") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { if (it.all { c -> c.isDigit() }) minutesText = it },
                        label = { Text("Хвилини") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                // 4. ЧАС ПОЧАТКУ
                OutlinedTextField(
                    value = if (selectedStartTime != null) minutesToTime(selectedStartTime!!) else "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Час початку") },
                    placeholder = { Text("Натисніть для вибору") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    trailingIcon = {
                        if (selectedStartTime != null) {
                            IconButton(onClick = { selectedStartTime = null }) {
                                Icon(Icons.Default.Clear, null)
                            }
                        } else {
                            Icon(Icons.Default.AccessTime, null)
                        }
                    },
                    interactionSource = remember { MutableInteractionSource() }
                        .also { interactionSource ->
                            LaunchedEffect(interactionSource) {
                                interactionSource.interactions.collect {
                                    if (it is PressInteraction.Release) showTimePicker()
                                }
                            }
                        }
                )

                // 5. ПРИВ'ЯЗКА + ЗАМОК
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentParent = potentialParents.find { it.id == parentId }
                    val parentText = if (currentParent != null) {
                        "${currentParent.title} (${minutesToTime(currentParent.startTimeMinutes ?: 0)})"
                    } else "Немає прив'язки"

                    ExposedDropdownMenuBox(
                        expanded = isParentMenuExpanded,
                        onExpandedChange = { isParentMenuExpanded = !isParentMenuExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            value = parentText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Прив'язати до...") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isParentMenuExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true,
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                        )
                        ExposedDropdownMenu(
                            expanded = isParentMenuExpanded,
                            onDismissRequest = { isParentMenuExpanded = false }
                        ) {
                            DropdownMenuItem(text = { Text("Немає прив'язки") }, onClick = { parentId = null; isParentMenuExpanded = false })

                            potentialParents.filter { it.id != task.id }.forEach { p ->
                                val timeStr = minutesToTime(p.startTimeMinutes ?: 0)
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(p.title, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                            Text(timeStr, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                        }
                                    },
                                    onClick = { parentId = p.id; isParentMenuExpanded = false }
                                )
                            }
                        }
                    }

                    FilledIconToggleButton(
                        checked = isLocked,
                        onCheckedChange = { isLocked = it },
                        modifier = Modifier.size(52.dp),
                        colors = IconButtonDefaults.filledIconToggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            checkedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock"
                        )
                    }
                }

                // 6. НОТАТКИ
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                if (linkedNote != null) {
                    val noteTextColor = getContrastColor(linkedNote.colorHex)
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(android.graphics.Color.parseColor(linkedNote.colorHex)),
                            contentColor = noteTextColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(linkedNote.title, fontWeight = FontWeight.Bold, maxLines = 1)
                                if (linkedNote.content.isNotBlank()) {
                                    Text(linkedNote.content, maxLines = 1, style = MaterialTheme.typography.bodySmall, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            IconButton(onClick = { onDetachNote(linkedNote) }) {
                                Icon(Icons.Default.Close, null, tint = noteTextColor)
                            }
                        }
                    }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(
                            onClick = { onCreateNote("Нотатка для ${task.title}") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                        ) { Icon(Icons.Default.Add, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Створити нотатку") }

                        Box(modifier = Modifier.weight(1f)) {
                            TextButton(
                                onClick = { isNoteMenuExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.textButtonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                            ) { Text("Прикріпити") }

                            DropdownMenu(expanded = isNoteMenuExpanded, onDismissRequest = { isNoteMenuExpanded = false }) {
                                if (availableNotes.isEmpty()) {
                                    DropdownMenuItem(text = { Text("Немає вільних нотаток") }, onClick = {})
                                } else {
                                    availableNotes.forEach { note ->
                                        DropdownMenuItem(
                                            text = { Text(note.title.ifBlank { "Без назви" }, maxLines = 1) },
                                            onClick = { onAttachNote(note); isNoteMenuExpanded = false }
                                        )
                                    }
                                }
                            }
                        }
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
                    // ЗБЕРЕЖЕННЯ: Передаємо emoji окремим параметром
                    onConfirm(
                        title,
                        finalDuration,
                        selectedStartTime,
                        parentId,
                        isLocked,
                        selectedDate.toString(),
                        selectedColor,
                        emoji // <--- Новий аргумент
                    )
                }
            }) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}


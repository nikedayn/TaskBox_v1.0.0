package com.nikidayn.taskbox.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.nikidayn.taskbox.model.TaskTemplate
import com.nikidayn.taskbox.ui.components.ColorSelector
import com.nikidayn.taskbox.ui.theme.getContrastColor
import com.nikidayn.taskbox.viewmodel.TaskViewModel
import com.nikidayn.taskbox.ui.components.EmojiSelectorDialog

@Composable
fun TemplatesScreen(viewModel: TaskViewModel) {
    val templates by viewModel.templates.collectAsState()

    // Стан для діалогів
    var showAddDialog by remember { mutableStateOf(false) }
    var templateToEdit by remember { mutableStateOf<TaskTemplate?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Template")
            }
        },
        snackbarHost = { SnackbarHost(hostState = remember { SnackbarHostState() }) }
    ) { innerPadding ->

        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(
                text = "Мої шаблони",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (templates.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Немає шаблонів. Створіть перший!", color = Color.Gray)
                }
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 160.dp), // Трохи ширші картки
                horizontalArrangement = Arrangement.spacedBy(12.dp), // Відступ по горизонталі
                verticalArrangement = Arrangement.spacedBy(12.dp),   // Такий самий відступ по вертикалі
                contentPadding = PaddingValues(bottom = 80.dp),      // Відступ знизу, щоб FAB не перекривав
                modifier = Modifier.fillMaxSize()
            ) {
                items(templates) { template ->
                    TemplateItem(
                        template = template,
                        onClick = {
                            viewModel.applyTemplateToInbox(template)
                        },
                        onLongClick = {
                            templateToEdit = template // Відкриваємо редагування
                        },
                        onDelete = { viewModel.deleteTemplate(template) }
                    )
                }
            }
        }

        // Діалог СТВОРЕННЯ
        if (showAddDialog) {
            TemplateDialog(
                template = null, // null означає створення
                onDismiss = { showAddDialog = false },
                onConfirm = { title, dur, emoji, color ->
                    // Вам потрібно буде оновити addTemplate у ViewModel, щоб він приймав color!
                    // Поки що можна передавати, або, якщо ViewModel ще не оновлена, ігнорувати
                    viewModel.addTemplate(title, dur, emoji, color)
                    showAddDialog = false
                }
            )
        }

        // Діалог РЕДАГУВАННЯ
        if (templateToEdit != null) {
            TemplateDialog(
                template = templateToEdit,
                onDismiss = { templateToEdit = null },
                onConfirm = { title, dur, emoji, color ->
                    val updated = templateToEdit!!.copy(
                        title = title,
                        durationMinutes = dur,
                        iconEmoji = emoji,
                        colorHex = color
                    )
                    viewModel.updateTemplate(updated) // Переконайтеся, що цей метод є у ViewModel
                    templateToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TemplateItem(
    template: TaskTemplate,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    val textColor = getContrastColor(template.colorHex)
    val haptics = LocalHapticFeedback.current

    Card(
        modifier = Modifier
            .height(140.dp) // Трохи збільшимо висоту для "повітря"
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(template.colorHex)),
            contentColor = textColor
        ),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp) // Більш округлі кути
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 1. ЕМОДЗІ (Лівий верхній кут)
            Text(
                text = template.iconEmoji,
                fontSize = 28.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // 2. КНОПКА ВИДАЛИТИ (Правий верхній кут)
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(24.dp)
                    .offset(x = 4.dp, y = (-4).dp) // Трохи підсунути в кут
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = textColor.copy(alpha = 0.5f)
                )
            }

            // 3. ТЕКСТ І ЧАС (Знизу зліва)
            Column(
                modifier = Modifier.align(Alignment.BottomStart)
            ) {
                Text(
                    text = template.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp // Фіксований розмір шрифту
                    ),
                    maxLines = 2, // Обмеження рядків, щоб не вилазило
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Час у "таблетці" (краще виглядає)
                Surface(
                    color = textColor.copy(alpha = 0.1f), // Напівпрозорий фон
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(4.dp),
                    contentColor = textColor
                ) {
                    Text(
                        text = "${template.durationMinutes} хв",
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// Універсальний діалог для створення та редагування
@Composable
fun TemplateDialog(
    template: TaskTemplate?,
    onDismiss: () -> Unit,
    onConfirm: (String, Int, String, String) -> Unit
) {
    var title by remember { mutableStateOf(template?.title ?: "") }
    var duration by remember { mutableStateOf(template?.durationMinutes?.toString() ?: "30") }

    // Стан для смайлика та видимості діалогу
    var emoji by remember { mutableStateOf(template?.iconEmoji ?: "⚡") }
    var showEmojiPicker by remember { mutableStateOf(false) } // <--- Додано

    var selectedColor by remember { mutableStateOf(template?.colorHex ?: "#FFEB3B") }

    // --- ВІКНО ПІКЕРА (Додано) ---
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
        title = { Text(if (template == null) "Новий шаблон" else "Редагувати шаблон") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Назва
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Назва") },
                    modifier = Modifier.fillMaxWidth()
                )

                // Колір
                Column {
                    Text("Колір картки", style = MaterialTheme.typography.bodySmall)
                    ColorSelector(
                        selectedColorHex = selectedColor,
                        onColorSelected = { selectedColor = it }
                    )
                }

                // Тривалість і Смайлик в один рядок
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically // Вирівнювання по центру
                ) {
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { if (it.all { c -> c.isDigit() }) duration = it },
                        label = { Text("Хв") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )

                    // --- КНОПКА ВИБОРУ СМАЙЛИКА (Замість текстового поля) ---
                    Surface(
                        onClick = { showEmojiPicker = true },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .size(56.dp) // Висота як у TextField
                            .weight(0.4f) // Трохи менша ширина
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = emoji, fontSize = 24.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onConfirm(
                    title,
                    duration.toIntOrNull() ?: 15,
                    emoji,
                    selectedColor
                )
            }) {
                Text("Зберегти")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
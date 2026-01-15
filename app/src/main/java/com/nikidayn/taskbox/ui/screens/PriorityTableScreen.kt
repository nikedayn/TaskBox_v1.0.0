package com.nikidayn.taskbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PriorityTableScreen(
    viewModel: TaskViewModel,
    onMenuClick: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()

    // Фільтруємо лише невиконані завдання, щоб не засмічувати таблицю
    val activeTasks = tasks.filter { !it.isCompleted }
        .sortedByDescending { it.importance + it.urgency } // Сортуємо за сумою балів (найважливіші зверху)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Таблиця пріоритетів") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            // --- ЗАГОЛОВОК ТАБЛИЦІ ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Завдання",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Термін.\n(1-10)",
                    modifier = Modifier.width(80.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "Важл.\n(1-10)",
                    modifier = Modifier.width(80.dp),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            // --- СПИСОК (РЯДКИ ТАБЛИЦІ) ---
            if (activeTasks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Всі завдання виконані!", color = Color.Gray)
                }
            } else {
                LazyColumn {
                    items(activeTasks, key = { it.id }) { task ->
                        PriorityRow(
                            task = task,
                            onUpdate = { t, u, i -> viewModel.updateTaskPriorities(t, u, i) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityRow(
    task: Task,
    onUpdate: (Task, Int, Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Назва завдання
        Text(
            text = task.title,
            modifier = Modifier.weight(1f).padding(end = 8.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyMedium
        )

        // Контролер Терміновості
        ScoreSelector(
            value = task.urgency,
            onValueChange = { newValue -> onUpdate(task, newValue, task.importance) },
            color = Color(0xFFFFCC80) // Світло-помаранчевий
        )

        Spacer(modifier = Modifier.width(4.dp))

        // Контролер Важливості
        ScoreSelector(
            value = task.importance,
            onValueChange = { newValue -> onUpdate(task, task.urgency, newValue) },
            color = Color(0xFF81C784) // Світло-зелений
        )
    }
}

@Composable
fun ScoreSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    color: Color
) {
    Row(
        modifier = Modifier
            .width(80.dp)
            .background(color.copy(alpha = 0.2f), shape = MaterialTheme.shapes.small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Кнопка Мінус
        IconButton(
            onClick = { if (value > 1) onValueChange(value - 1) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Remove, null, modifier = Modifier.size(12.dp))
        }

        // Значення
        Text(
            text = value.toString(),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )

        // Кнопка Плюс
        IconButton(
            onClick = { if (value < 10) onValueChange(value + 1) },
            modifier = Modifier.size(24.dp)
        ) {
            Icon(Icons.Default.Add, null, modifier = Modifier.size(12.dp))
        }
    }
}
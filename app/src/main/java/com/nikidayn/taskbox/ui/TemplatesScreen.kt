package com.nikidayn.taskbox.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikidayn.taskbox.model.TaskTemplate
import com.nikidayn.taskbox.viewmodel.TaskViewModel

// УВАГА: Тепер функція приймає viewModel
@Composable
fun TemplatesScreen(viewModel: TaskViewModel) {
    // Якщо тут світиться помилка на .templates - значить ви не оновили TaskViewModel (див. нижче)
    val templates by viewModel.templates.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

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
                columns = GridCells.Adaptive(minSize = 150.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(templates) { template ->
                    TemplateItem(
                        template = template,
                        onClick = {
                            viewModel.applyTemplateToInbox(template)
                        },
                        onDelete = { viewModel.deleteTemplate(template) }
                    )
                }
            }
        }

        if (showAddDialog) {
            AddTemplateDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, dur, emoji ->
                    viewModel.addTemplate(title, dur, emoji)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun TemplateItem(template: TaskTemplate, onClick: () -> Unit, onDelete: () -> Unit) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = Color(android.graphics.Color.parseColor(template.colorHex))),
        modifier = Modifier.height(120.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            Column(modifier = Modifier.align(Alignment.Center)) {
                Text(text = template.iconEmoji, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = template.title, style = MaterialTheme.typography.titleMedium)
                Text(text = "${template.durationMinutes} хв", style = MaterialTheme.typography.bodySmall)
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, tint = Color.DarkGray)
            }
        }
    }
}

@Composable
fun AddTemplateDialog(onDismiss: () -> Unit, onConfirm: (String, Int, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("30") }
    var emoji by remember { mutableStateOf("☕") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новий шаблон") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Назва") })
                OutlinedTextField(
                    value = duration,
                    onValueChange = { if (it.all { c -> c.isDigit() }) duration = it },
                    label = { Text("Хвилини") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Смайлик (Icon)") })
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(title, duration.toIntOrNull() ?: 15, emoji) }) {
                Text("Створити")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
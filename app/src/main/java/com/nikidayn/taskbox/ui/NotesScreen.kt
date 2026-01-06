package com.nikidayn.taskbox.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikidayn.taskbox.model.Note
import com.nikidayn.taskbox.ui.theme.getContrastColor
import com.nikidayn.taskbox.viewmodel.TaskViewModel

@Composable
fun NotesScreen(viewModel: TaskViewModel) {
    val notes by viewModel.notes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "New Note")
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(
                text = "Нотатки",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Тут поки порожньо. Запишіть думку!", color = Color.Gray)
                }
            }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2), // У дві колонки
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(notes) { note ->
                    NoteItem(
                        note = note,
                        onClick = { noteToEdit = note }
                    )
                }
            }
        }

        // Діалог створення
        if (showAddDialog) {
            NoteDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, content ->
                    viewModel.addNote(title, content)
                    showAddDialog = false
                }
            )
        }

        // Діалог редагування
        if (noteToEdit != null) {
            NoteDialog(
                initialTitle = noteToEdit!!.title,
                initialContent = noteToEdit!!.content,
                onDismiss = { noteToEdit = null },
                onConfirm = { title, content ->
                    viewModel.updateNote(noteToEdit!!, title, content)
                    noteToEdit = null
                },
                onDelete = {
                    viewModel.deleteNote(noteToEdit!!)
                    noteToEdit = null
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NoteItem(note: Note, onClick: () -> Unit) {
    // Визначаємо контрастний колір
    val textColor = getContrastColor(note.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(note.colorHex)),
            contentColor = textColor // <--- ВИПРАВЛЕННЯ КОЛЬОРУ ТЕКСТУ
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (note.title.isNotBlank()) {
                Text(
                    text = note.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            Text(
                text = note.content,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 6,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NoteDialog(
    initialTitle: String = "",
    initialContent: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(if (onDelete == null) "Нова нотатка" else "Редагувати")
                if (onDelete != null) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Заголовок") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("Зміст") },
                    modifier = Modifier.fillMaxWidth().height(150.dp),
                    maxLines = 10
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                if (title.isNotBlank() || content.isNotBlank()) {
                    onConfirm(title, content)
                }
            }) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
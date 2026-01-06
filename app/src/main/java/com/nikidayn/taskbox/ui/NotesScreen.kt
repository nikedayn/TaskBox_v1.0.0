package com.nikidayn.taskbox.ui

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nikidayn.taskbox.model.Note
import com.nikidayn.taskbox.ui.theme.getContrastColor
import com.nikidayn.taskbox.viewmodel.TaskViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotesScreen(viewModel: TaskViewModel) {
    val notes by viewModel.notes.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<Note?>(null) }

    // Локальний стан: чи активовано режим пошуку
    var isSearchActive by remember { mutableStateOf(false) }

    // Менеджери для фокусу (клавіатури)
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() } // 1. Створюємо запитувач фокусу

    // 2. Ефект: коли isSearchActive стає true -> викликаємо клавіатуру
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    Scaffold(
        floatingActionButton = {
            Surface(
                modifier = Modifier
                    .size(56.dp)
                    .clip(FloatingActionButtonDefaults.shape)
                    .combinedClickable(
                        onClick = { showAddDialog = true },
                        onLongClick = {
                            isSearchActive = !isSearchActive
                            if (!isSearchActive) {
                                viewModel.onSearchQueryChange("")
                                focusManager.clearFocus()
                            }
                        }
                    ),
                shape = FloatingActionButtonDefaults.shape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shadowElevation = 6.dp,
                tonalElevation = 6.dp
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (isSearchActive) Icons.Default.Search else Icons.Default.Add,
                        contentDescription = "Add or Search"
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {

            // Анімація перемикання між Заголовком і Полем пошуку
            Crossfade(targetState = isSearchActive, label = "SearchHeader") { active ->
                if (active) {
                    // --- ПОЛЕ ПОШУКУ ---
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        placeholder = { Text("Пошук...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                            .focusRequester(focusRequester), // 3. Прив'язуємо фокус сюди
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = {
                                isSearchActive = false
                                viewModel.onSearchQueryChange("")
                                focusManager.clearFocus()
                            }) {
                                Icon(Icons.Default.Close, contentDescription = "Close search")
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() })
                    )
                } else {
                    // --- ЗАГОЛОВОК З КНОПКОЮ ПОШУКУ ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Нотатки",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // 4. Додаткова кнопка пошуку зверху
                        IconButton(onClick = { isSearchActive = true }) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Повідомлення, якщо список порожній
            if (notes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isSearchActive) "Нічого не знайдено" else "Тут поки порожньо. Запишіть думку!",
                        color = Color.Gray
                    )
                }
            }

            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
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

        // Діалоги (створення та редагування)
        if (showAddDialog) {
            NoteDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, content ->
                    viewModel.addNote(title, content)
                    showAddDialog = false
                }
            )
        }

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
    val textColor = getContrastColor(note.colorHex)

    Card(
        modifier = Modifier.fillMaxWidth().combinedClickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(android.graphics.Color.parseColor(note.colorHex)),
            contentColor = textColor
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
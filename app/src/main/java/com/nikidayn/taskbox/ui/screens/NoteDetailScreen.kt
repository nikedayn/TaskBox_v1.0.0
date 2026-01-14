package com.nikidayn.taskbox.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.nikidayn.taskbox.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Int,
    viewModel: TaskViewModel,
    navController: NavController
) {
    val notes by viewModel.notes.collectAsState()
    val existingNote = remember(notes, noteId) { notes.find { it.id == noteId } }

    // Якщо це нова нотатка, створюємо пусті поля. Якщо існуюча - заповнюємо.
    var title by remember { mutableStateOf(existingNote?.title ?: "") }
    var content by remember { mutableStateOf(existingNote?.content ?: "") }

    // Функція збереження
    fun saveAndExit() {
        if (title.isBlank() && content.isBlank()) {
            // Якщо нотатка пуста і ми її редагували - можна видалити (опціонально)
            // if (existingNote != null) viewModel.deleteNote(existingNote)
        } else {
            if (existingNote != null) {
                // Оновлюємо тільки якщо щось змінилося
                if (existingNote.title != title || existingNote.content != content) {
                    viewModel.updateNote(existingNote, title, content)
                }
            } else {
                viewModel.addNote(title, content)
            }
        }
        navController.popBackStack()
    }

    // Системна кнопка "Назад" теж зберігає
    BackHandler { saveAndExit() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // КНОПКА ВИДАЛИТИ
                    if (existingNote != null) {
                        IconButton(onClick = {
                            viewModel.deleteNote(existingNote)
                            navController.popBackStack()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Видалити", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    // КНОПКА ЗБЕРЕГТИ (Галочка)
                    IconButton(onClick = { saveAndExit() }) {
                        Icon(Icons.Default.Check, contentDescription = "Зберегти", tint = MaterialTheme.colorScheme.primary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Поле ЗАГОЛОВКУ
            BasicTextField(
                value = title,
                onValueChange = { title = it },
                textStyle = TextStyle(
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text("Заголовок", style = TextStyle(fontSize = 24.sp, color = Color.Gray))
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )

            // Поле ТЕКСТУ
            BasicTextField(
                value = content,
                onValueChange = { content = it },
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
                ),
                decorationBox = { innerTextField ->
                    if (content.isEmpty()) {
                        Text("Нотатка...", style = TextStyle(fontSize = 18.sp, color = Color.Gray))
                    }
                    innerTextField()
                },
                modifier = Modifier.fillMaxSize(),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
            )
        }
    }
}
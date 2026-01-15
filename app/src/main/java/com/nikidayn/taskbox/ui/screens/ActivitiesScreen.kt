package com.nikidayn.taskbox.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nikidayn.taskbox.model.Category
import com.nikidayn.taskbox.ui.components.ColorSelector
import com.nikidayn.taskbox.ui.components.EmojiSelectorDialog
import com.nikidayn.taskbox.viewmodel.TaskViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivitiesScreen(
    viewModel: TaskViewModel,
    onMenuClick: () -> Unit
) {
    val categories by viewModel.categories.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Діяльності") },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Додати")
            }
        }
    ) { padding ->
        if (categories.isEmpty()) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Список пустий. Додайте категорії!", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories) { category ->
                    CategoryItem(category = category, onDelete = { viewModel.deleteCategory(category) })
                }
            }
        }

        if (showAddDialog) {
            AddCategoryDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, color, emoji ->
                    viewModel.addCategory(name, color, emoji)
                    showAddDialog = false
                }
            )
        }
    }
}

@Composable
fun CategoryItem(category: Category, onDelete: () -> Unit) {
    Card(elevation = CardDefaults.cardElevation(2.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Кружечок з кольором та емодзі
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(category.colorHex))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(category.iconEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(category.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Видалити", tint = Color.Gray)
            }
        }
    }
}

@Composable
fun AddCategoryDialog(onDismiss: () -> Unit, onConfirm: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("#2196F3") } // Синій за замовчуванням
    var emoji by remember { mutableStateOf("🏷️") }
    var showEmojiPicker by remember { mutableStateOf(false) }

    if (showEmojiPicker) {
        EmojiSelectorDialog(onDismiss = { showEmojiPicker = false }, onEmojiSelected = { emoji = it; showEmojiPicker = false })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нова діяльність") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { showEmojiPicker = true }, modifier = Modifier.size(50.dp), contentPadding = PaddingValues(0.dp)) {
                        Text(emoji, fontSize = 24.sp)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Назва") }, modifier = Modifier.weight(1f))
                }
                Text("Колір:", style = MaterialTheme.typography.labelMedium)
                ColorSelector(selectedColorHex = color, onColorSelected = { color = it })
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotBlank()) onConfirm(name, color, emoji) }) { Text("Створити") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Скасувати") } }
    )
}
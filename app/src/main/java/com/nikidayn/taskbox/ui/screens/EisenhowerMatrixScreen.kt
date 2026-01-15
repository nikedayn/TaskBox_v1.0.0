//package com.nikidayn.taskbox.ui.screens
//
//import androidx.compose.foundation.BorderStroke
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.*
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.RoundedCornerShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Menu
//import androidx.compose.material3.*
//import androidx.compose.runtime.*
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//import com.nikidayn.taskbox.model.Task
//import com.nikidayn.taskbox.viewmodel.TaskViewModel
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun EisenhowerMatrixScreen(
//    viewModel: TaskViewModel,
//    onMenuClick: () -> Unit
//) {
//    val tasks by viewModel.tasks.collectAsState()
//
//    // Фільтрація завдань по квадрантах
//    val q1 = tasks.filter { it.isImportant && it.isUrgent }      // Зробити зараз
//    val q2 = tasks.filter { it.isImportant && !it.isUrgent }     // Запланувати
//    val q3 = tasks.filter { !it.isImportant && it.isUrgent }     // Делегувати
//    val q4 = tasks.filter { !it.isImportant && !it.isUrgent }    // Видалити
//
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("Матриця пріоритетів") },
//                navigationIcon = {
//                    IconButton(onClick = onMenuClick) {
//                        Icon(Icons.Default.Menu, contentDescription = "Menu")
//                    }
//                }
//            )
//        }
//    ) { padding ->
//        Column(
//            modifier = Modifier
//                .padding(padding)
//                .fillMaxSize()
//                .padding(8.dp),
//            verticalArrangement = Arrangement.spacedBy(8.dp)
//        ) {
//            // Верхній ряд
//            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                QuadrantCard(
//                    title = "🔥 Зробити зараз",
//                    subtitle = "Важливо і Терміново",
//                    tasks = q1,
//                    color = Color(0xFFFFCDD2), // Червоний відтінок
//                    modifier = Modifier.weight(1f)
//                )
//                QuadrantCard(
//                    title = "📅 Запланувати",
//                    subtitle = "Важливо, не Терміново",
//                    tasks = q2,
//                    color = Color(0xFFBBDEFB), // Синій відтінок
//                    modifier = Modifier.weight(1f)
//                )
//            }
//            // Нижній ряд
//            Row(modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                QuadrantCard(
//                    title = "⚠️ Делегувати",
//                    subtitle = "Не важливо, Терміново",
//                    tasks = q3,
//                    color = Color(0xFFFFF9C4), // Жовтий відтінок
//                    modifier = Modifier.weight(1f)
//                )
//                QuadrantCard(
//                    title = "🗑️ Видалити",
//                    subtitle = "Не важливо, не Терміново",
//                    tasks = q4,
//                    color = Color(0xFFF5F5F5), // Сірий
//                    modifier = Modifier.weight(1f)
//                )
//            }
//        }
//    }
//}
//
//@Composable
//fun QuadrantCard(
//    title: String,
//    subtitle: String,
//    tasks: List<Task>,
//    color: Color,
//    modifier: Modifier = Modifier
//) {
//    Card(
//        modifier = modifier.fillMaxSize(),
//        colors = CardDefaults.cardColors(containerColor = color),
//        shape = RoundedCornerShape(12.dp),
//        border = BorderStroke(1.dp, Color.Black.copy(alpha = 0.1f))
//    ) {
//        Column(modifier = Modifier.padding(8.dp)) {
//            Text(
//                text = title,
//                style = MaterialTheme.typography.titleMedium,
//                fontWeight = FontWeight.Bold,
//                color = Color.Black.copy(alpha = 0.8f)
//            )
//            Text(
//                text = subtitle,
//                style = MaterialTheme.typography.labelSmall,
//                color = Color.Black.copy(alpha = 0.5f)
//            )
//            Divider(modifier = Modifier.padding(vertical = 8.dp), color = Color.Black.copy(alpha = 0.1f))
//
//            LazyColumn(
//                verticalArrangement = Arrangement.spacedBy(4.dp),
//                modifier = Modifier.fillMaxSize()
//            ) {
//                items(tasks) { task ->
//                    Surface(
//                        color = Color.White.copy(alpha = 0.7f),
//                        shape = RoundedCornerShape(4.dp),
//                        modifier = Modifier.fillMaxWidth()
//                    ) {
//                        Row(
//                            verticalAlignment = Alignment.CenterVertically,
//                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
//                        ) {
//                            Text(task.iconEmoji, fontSize = 12.sp)
//                            Spacer(modifier = Modifier.width(4.dp))
//                            Text(
//                                text = task.title,
//                                style = MaterialTheme.typography.bodySmall,
//                                maxLines = 1,
//                                color = Color.Black
//                            )
//                        }
//                    }
//                }
//            }
//        }
//    }
//}
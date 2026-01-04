package com.nikidayn.taskbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

// ВАШІ КОМПОНЕНТИ (Переконайтеся, що ці файли існують у папках)
import com.nikidayn.taskbox.ui.TemplatesScreen
import com.nikidayn.taskbox.ui.components.AddTaskDialog
import com.nikidayn.taskbox.ui.components.EditTaskDialog
import com.nikidayn.taskbox.ui.components.TimelineItem
import com.nikidayn.taskbox.ui.components.DayView // <--- НОВИЙ КОМПОНЕНТ
import com.nikidayn.taskbox.viewmodel.TaskViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: TaskViewModel by viewModels()

        setContent {
            MaterialTheme {
                MainAppStructure(viewModel)
            }
        }
    }
}

@Composable
fun MainAppStructure(viewModel: TaskViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    label = { Text("Таймлайн") },
                    selected = currentRoute == "timeline",
                    onClick = {
                        navController.navigate("timeline") {
                            popUpTo("timeline") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = null) },
                    label = { Text("Шаблони") },
                    selected = currentRoute == "templates",
                    onClick = {
                        navController.navigate("templates") {
                            popUpTo("timeline") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "timeline",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("timeline") {
                // Викликаємо функцію, яка написана нижче в цьому ж файлі
                TaskScreen(viewModel)
            }
            composable("templates") {
                TemplatesScreen(viewModel)
            }
        }
    }
}

// --- ОСЬ ВОНА: ФУНКЦІЯ TASK SCREEN ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val taskList by viewModel.tasks.collectAsState()

    // СТАНИ
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<com.nikidayn.taskbox.model.Task?>(null) }
    var selectedIds by remember { mutableStateOf(setOf<Int>()) }
    val isSelectionMode = selectedIds.isNotEmpty()

    // Сортування списків
    val timelineTasks = taskList.filter { it.startTimeMinutes != null }.sortedBy { it.startTimeMinutes }
    val inboxTasks = taskList.filter { it.startTimeMinutes == null }

    fun toggleSelection(taskId: Int) {
        selectedIds = if (selectedIds.contains(taskId)) {
            selectedIds - taskId
        } else {
            selectedIds + taskId
        }
    }

    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedIds.size} вибрано") },
                    navigationIcon = {
                        IconButton(onClick = { selectedIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val tasksToDelete = taskList.filter { it.id in selectedIds }
                            viewModel.deleteTasks(tasksToDelete)
                            selectedIds = emptySet()
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )
            }
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(onClick = { showAddDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { innerPadding ->

        // ДІАЛОГИ
        if (showAddDialog) {
            AddTaskDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { title, duration, start ->
                    viewModel.addTask(title, duration, start)
                    showAddDialog = false
                }
            )
        }

        if (taskToEdit != null) {
            EditTaskDialog(
                task = taskToEdit!!,
                onDismiss = { taskToEdit = null },
                onConfirm = { newTitle, newDuration, newStart ->
                    viewModel.updateTaskDetails(taskToEdit!!, newTitle, newDuration, newStart)
                    taskToEdit = null
                }
            )
        }

        // ГОЛОВНИЙ ВМІСТ
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Секція Вхідних (зверху, фіксована)
            if (inboxTasks.isNotEmpty()) {
                Text(
                    text = "Вхідні (${inboxTasks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.primary
                )

                // Вхідні показуємо звичайним списком (висота обмежена, щоб не займати весь екран)
                LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                    itemsIndexed(inboxTasks) { _, task ->
                        val isSelected = selectedIds.contains(task.id)
                        TimelineItem(
                            task = task,
                            isLast = true,
                            isSelected = isSelected,
                            onCheck = {
                                if (!isSelectionMode) viewModel.toggleComplete(task)
                                else toggleSelection(task.id)
                            },
                            onClick = {
                                if (isSelectionMode) toggleSelection(task.id)
                                else taskToEdit = task
                            },
                            onLongClick = { if (!isSelectionMode) toggleSelection(task.id) },
                            onTimeChange = { newTime -> viewModel.changeTaskStartTime(task, newTime) }
                        )
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            }

            // 2. Секція Таймлайну (Новий DayView)
            // Займає весь вільний простір, що залишився
            DayView(
                tasks = timelineTasks,
                onTaskCheck = { viewModel.toggleComplete(it) },
                onTaskClick = { taskToEdit = it },
                onTaskTimeChange = { task, newTime ->
                    viewModel.changeTaskStartTime(task, newTime)
                }
            )
        }
    }
}
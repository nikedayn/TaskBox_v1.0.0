package com.nikidayn.taskbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import com.nikidayn.taskbox.ui.TemplatesScreen
import com.nikidayn.taskbox.ui.components.*
import com.nikidayn.taskbox.viewmodel.TaskViewModel
import com.nikidayn.taskbox.ui.theme.TaskBoxTheme
import com.nikidayn.taskbox.ui.SettingsScreen
import androidx.compose.material.icons.filled.Description
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: TaskViewModel by viewModels()

        setContent {
            // ОТРИМУЄМО ТЕМУ З VIEWMODEL
            val themeMode by viewModel.themeMode.collectAsState()

            // Визначаємо, чи темна тема:
            // 0 - системна (залежить від налаштувань телефону)
            // 1 - світла (false)
            // 2 - темна (true)
            val useDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            // Передаємо параметр darkTheme
            TaskBoxTheme(darkTheme = useDarkTheme) {
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

                // 1. Календар
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, null) },
                    label = { Text("Календар") },
                    selected = currentRoute == "timeline",
                    onClick = { navController.navigate("timeline") { launchSingleTop = true; restoreState = true } }
                )

                // 2. Нотатки (НОВЕ)
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Description, null) }, // Виберіть іконку
                    label = { Text("Нотатки") },
                    selected = currentRoute == "notes",
                    onClick = {
                        navController.navigate("notes") {
                            popUpTo("timeline") { saveState = true } // Зберігаємо стан календаря
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // 3. Шаблони
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, null) },
                    label = { Text("Шаблони") },
                    selected = currentRoute == "templates",
                    onClick = { navController.navigate("templates") { launchSingleTop = true; restoreState = true } }
                )

                // 4. НАЛАШТУВАННЯ
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, null) },
                    label = { Text("Опції") },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
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
            composable("timeline") { TaskScreen(viewModel) }
            // Додаємо новий екран сюди
            composable("notes") {
                // Не забудьте імпортувати NotesScreen
                com.nikidayn.taskbox.ui.NotesScreen(viewModel)
            }
            composable("templates") { TemplatesScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val taskList by viewModel.tasks.collectAsState()

    // ОТРИМУЄМО НАЛАШТУВАННЯ
    val workHours by viewModel.workHours.collectAsState()
    val startH = workHours.first.toInt()
    val endH = workHours.second.toInt()

    // --- ЛОГІКА КАЛЕНДАРЯ ---
    // Початкова дата для пейджера (середина величезного списку)
    val initialDate = remember { LocalDate.now() }
    val initialPage = Int.MAX_VALUE / 2

    // Пейджер
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()

    // Обчислюємо поточну дату на основі сторінки
    val currentDate = remember(pagerState.currentPage) {
        val daysDiff = pagerState.currentPage - initialPage
        initialDate.plusDays(daysDiff.toLong())
    }

    // Стан для DatePicker
    var showDatePicker by remember { mutableStateOf(false) }
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = currentDate.toEpochDay() * 24 * 60 * 60 * 1000
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selectedMillis = datePickerState.selectedDateMillis
                    if (selectedMillis != null) {
                        val selectedDate = LocalDate.ofEpochDay(selectedMillis / (24 * 60 * 60 * 1000))
                        val daysDiff = java.time.temporal.ChronoUnit.DAYS.between(initialDate, selectedDate).toInt()
                        scope.launch { pagerState.scrollToPage(initialPage + daysDiff) }
                    }
                    showDatePicker = false
                }) { Text("ОК") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Скасувати") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // --- СТАНИ ---
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<com.nikidayn.taskbox.model.Task?>(null) }

    // Форматування дати для заголовка
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        modifier = Modifier.clickable { showDatePicker = true },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(currentDate.format(dateFormatter), fontWeight = FontWeight.Bold)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Date")
                    }
                },
                actions = {
                    // Кнопка "Сьогодні"
                    if (currentDate != LocalDate.now()) {
                        IconButton(onClick = {
                            scope.launch { pagerState.animateScrollToPage(initialPage) }
                        }) {
                            Icon(Icons.Default.Today, contentDescription = "Today")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        }
    ) { innerPadding ->

        if (showAddDialog) {
            AddTaskDialog(
                selectedDate = currentDate.toString(),
                onDismiss = { showAddDialog = false },
                onConfirm = { title, duration, start, date ->
                    viewModel.addTask(title, duration, start, date)
                    showAddDialog = false
                }
            )
        }

        if (taskToEdit != null) {
            EditTaskDialog(
                task = taskToEdit!!,
                onDismiss = { taskToEdit = null },
                onConfirm = { newTitle, newDur, newStart ->
                    viewModel.updateTaskDetails(taskToEdit!!, newTitle, newDur, newStart)
                    taskToEdit = null
                },
                onDelete = {
                    viewModel.deleteTask(taskToEdit!!)
                    taskToEdit = null
                }
            )
        }

        // --- PAGER З ДНЯМИ ---
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(innerPadding).fillMaxSize()
        ) { page ->
            // Обчислюємо дату для ЦІЄЇ сторінки (щоб рендерити правильні дані навіть під час свайпу)
            val pageDate = initialDate.plusDays((page - initialPage).toLong())
            val dateString = pageDate.toString()

            // Фільтруємо завдання для цієї дати
            val tasksForDay = taskList.filter { it.date == dateString }
            val timelineTasks = tasksForDay.filter { it.startTimeMinutes != null }.sortedBy { it.startTimeMinutes }
            val inboxTasks = tasksForDay.filter { it.startTimeMinutes == null }

            // ВМІСТ СТОРІНКИ (ДНЯ)
            Column(modifier = Modifier.fillMaxSize()) {

                // Вхідні
                var isInboxExpanded by remember { mutableStateOf(true) }
                if (inboxTasks.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isInboxExpanded = !isInboxExpanded }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Вхідні (${inboxTasks.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            if (isInboxExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (isInboxExpanded) {
                        LazyColumn(
                            modifier = Modifier.heightIn(max = 200.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(inboxTasks) { _, task ->
                                TimelineItem(
                                    task = task,
                                    minTime = startH * 60,
                                    isLast = true,
                                    isSelected = false,
                                    onCheck = { viewModel.toggleComplete(task) },
                                    onClick = { taskToEdit = task },
                                    onLongClick = { },
                                    onTimeChange = { newTime -> viewModel.changeTaskStartTime(task, newTime) }
                                )
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                }

                // --- ТАЙМЛАЙН ---
                DayView(
                    tasks = timelineTasks,
                    startHour = startH,
                    endHour = endH,
                    // ВАЖЛИВО: weight(1f) змушує календар вписатися в екран і активувати скрол
                    modifier = Modifier.weight(1f),

                    onTaskCheck = { viewModel.toggleComplete(it) },
                    onTaskClick = { taskToEdit = it },
                    onTaskTimeChange = { task, newTime -> viewModel.changeTaskStartTime(task, newTime) }
                )
            }
        }
    }
}
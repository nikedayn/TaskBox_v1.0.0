package com.nikidayn.taskbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List // Якщо червоне - див. пункт 2 нижче
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.layout.boundsInWindow
import com.nikidayn.taskbox.model.Task

import com.nikidayn.taskbox.ui.screens.NotesScreen
import com.nikidayn.taskbox.ui.screens.SettingsScreen

import com.nikidayn.taskbox.ui.components.* // Тут DayViewHourHeight та діалоги
import com.nikidayn.taskbox.ui.screens.NoteDetailScreen
import com.nikidayn.taskbox.ui.theme.TaskBoxTheme
import com.nikidayn.taskbox.viewmodel.TaskViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: TaskViewModel by viewModels()

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val useDarkTheme = when (themeMode) {
                1 -> false
                2 -> true
                else -> androidx.compose.foundation.isSystemInDarkTheme()
            }

            TaskBoxTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainAppStructure(viewModel)
                }
            }
        }
    }
}

@Composable
fun MainAppStructure(viewModel: TaskViewModel) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "calendar"

    fun navigateTo(route: String) {
        scope.launch { drawerState.close() }
        navController.navigate(route) {
            popUpTo("calendar") { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Меню",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant) // Виправлено HorizontalDivider
                Spacer(Modifier.height(12.dp))

                // === 1. КАЛЕНДАР ===
                NavigationDrawerItem(
                    label = { Text("Календар") },
                    icon = { Icon(Icons.Default.DateRange, null) },
                    selected = currentRoute == "calendar",
                    onClick = { navigateTo("calendar") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // === 2. НОТАТКИ (НОВИЙ ПУНКТ) ===
                NavigationDrawerItem(
                    label = { Text("Нотатки") },
                    icon = { Icon(Icons.Default.Edit, null) }, // Іконка олівця
                    selected = currentRoute == "notes",
                    onClick = { navigateTo("notes") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // === 3. ДІЯЛЬНОСТІ ===
                NavigationDrawerItem(
                    label = { Text("Діяльності") },
                    icon = { Icon(Icons.Default.List, null) }, // Виправлено іконку
                    selected = currentRoute == "activities",
                    onClick = { navigateTo("activities") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // === 4. МАТРИЦЯ ===
                NavigationDrawerItem(
                    label = { Text("Матриця") },
                    icon = { Icon(Icons.Default.GridView, null) },
                    selected = currentRoute == "matrix",
                    onClick = { navigateTo("matrix") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // === 5. СТАТИСТИКА ===
                NavigationDrawerItem(
                    label = { Text("Статистика") },
                    icon = { Icon(Icons.Default.PieChart, null) },
                    selected = currentRoute == "stats",
                    onClick = { navigateTo("stats") },
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                Spacer(Modifier.weight(1f))
                Divider(color = MaterialTheme.colorScheme.outlineVariant)

                // === 6. НАЛАШТУВАННЯ ===
                NavigationDrawerItem(
                    label = { Text("Налаштування") },
                    icon = { Icon(Icons.Default.Settings, null) },
                    selected = currentRoute == "settings",
                    onClick = { navigateTo("settings") },
                    modifier = Modifier.padding(12.dp)
                )
            }
        }
    ) {
        NavHost(
            navController = navController,
            startDestination = "calendar"
        ) {
            // КАЛЕНДАР
            composable("calendar") {
                TaskScreen(
                    viewModel = viewModel,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            // НОТАТКИ
            composable("notes") {
                NotesScreen(
                    viewModel = viewModel,
                    navController = navController,
                    onMenuClick = { scope.launch { drawerState.open() } }
                )
            }

            // ДЕТАЛІ НОТАТКИ (для переходу зі списку)
            composable(
                route = "note_detail/{noteId}",
                arguments = listOf(androidx.navigation.navArgument("noteId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                NoteDetailScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    navController = navController
                )
            }

            // ІНШІ ЕКРАНИ (Заглушки)
            composable("activities") {
                ScreenContainer(title = "Діяльності", onMenuClick = { scope.launch { drawerState.open() } }) {
                    PlaceholderContent("Тут буде список діяльностей")
                }
            }
            composable("matrix") {
                ScreenContainer(title = "Матриця Ейзенхауера", onMenuClick = { scope.launch { drawerState.open() } }) {
                    PlaceholderContent("Тут буде матриця пріоритетів")
                }
            }
            composable("stats") {
                ScreenContainer(title = "Статистика", onMenuClick = { scope.launch { drawerState.open() } }) {
                    PlaceholderContent("Тут буде статистика перерозподілу часу")
                }
            }
            composable("templates") {
                ScreenContainer(title = "Шаблони", onMenuClick = { scope.launch { drawerState.open() } }) {
                    PlaceholderContent("Тут будуть картки шаблонів для нотаток і справ")
                }
            }

            // Налаштування
            composable("settings") {
                ScreenContainer(title = "Налаштування", onMenuClick = { scope.launch { drawerState.open() } }) {
                    SettingsScreen(viewModel)
                }
            }
        }
    }
}

// === ОНОВЛЕНИЙ TaskScreen (З підтримкою кнопки Меню) ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(
    viewModel: TaskViewModel,
    onMenuClick: () -> Unit // <--- Додали параметр
) {
    val taskList by viewModel.tasks.collectAsState()
    val notesList by viewModel.notes.collectAsState()
    val workHours by viewModel.workHours.collectAsState()
    val startH = workHours.first.toInt()
    val endH = workHours.second.toInt()

    var isListView by remember { mutableStateOf(false) }
    val timelineScrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerHour = with(density) { DayViewHourHeight.toPx() }

    var draggingTask by remember { mutableStateOf<Task?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var dayViewBoundsInWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    val initialDate = remember { LocalDate.now() }
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()

    val currentDate = remember(pagerState.currentPage) {
        val daysDiff = pagerState.currentPage - initialPage
        initialDate.plusDays(daysDiff.toLong())
    }

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
        ) { DatePicker(state = datePickerState) }
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<Task?>(null) }
    val dateFormatter = DateTimeFormatter.ofPattern("EEE, d MMM", Locale.getDefault())

    Box(modifier = Modifier.fillMaxSize()) {
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
                    navigationIcon = { // <--- КНОПКА БУРГЕР МЕНЮ
                        IconButton(onClick = onMenuClick) {
                            Icon(Icons.Default.Menu, contentDescription = "Меню")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isListView = !isListView }) {
                            val icon = if (isListView) Icons.Default.ViewTimeline else Icons.AutoMirrored.Filled.List
                            Icon(imageVector = icon, contentDescription = "Switch View")
                        }
                        if (currentDate != LocalDate.now()) {
                            IconButton(onClick = { scope.launch { pagerState.animateScrollToPage(initialPage) } }) {
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
            // ... ВМІСТ ТАКИЙ САМИЙ ЯК БУВ (ДІАЛОГИ) ...
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
                val tasksOnSameDay = taskList.filter { it.date == taskToEdit!!.date }
                val linkedNote = notesList.find { it.taskId == taskToEdit!!.id }
                val availableNotes = notesList.filter { it.taskId == null }

                EditTaskDialog(
                    task = taskToEdit!!,
                    potentialParents = tasksOnSameDay,
                    linkedNote = linkedNote,
                    availableNotes = availableNotes,
                    onAttachNote = { note -> viewModel.linkNote(note, taskToEdit!!.id) },
                    onDetachNote = { note -> viewModel.unlinkNote(note) },
                    onCreateNote = { autoTitle -> viewModel.addNote(title = autoTitle, content = "", taskId = taskToEdit!!.id) },
                    onDismiss = { taskToEdit = null },
                    onConfirm = { newTitle, newDuration, newStart, newParentId, newIsLocked, newDate, newColor, newEmoji ->
                        val updatedTask = taskToEdit!!.copy(
                            title = newTitle, durationMinutes = newDuration, startTimeMinutes = newStart,
                            linkedParentId = newParentId, isLocked = newIsLocked, date = newDate, colorHex = newColor, iconEmoji = newEmoji
                        )
                        viewModel.updateTask(updatedTask)
                        taskToEdit = null
                    },
                    onDelete = { viewModel.deleteTask(taskToEdit!!); taskToEdit = null }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) { page ->
                val pageDate = initialDate.plusDays((page - initialPage).toLong())
                val dateString = pageDate.toString()
                val tasksForDay = taskList.filter { it.date == dateString }
                val timelineTasks = tasksForDay.filter { it.startTimeMinutes != null }.sortedBy { it.startTimeMinutes }
                val inboxTasks = tasksForDay.filter { it.startTimeMinutes == null }

                Column(modifier = Modifier.fillMaxSize()) {
                    if (inboxTasks.isNotEmpty()) {
                        // ... ЛОГІКА INBOX (СКОРОЧЕНО ДЛЯ ЕКОНОМІЇ МІСЦЯ - ЗАЛИШТЕ ЯК БУЛО) ...
                        var isInboxExpanded by remember { mutableStateOf(true) }
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), modifier = Modifier.fillMaxWidth().clickable { isInboxExpanded = !isInboxExpanded }) {
                            Row(modifier = Modifier.padding(16.dp, 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("Вхідні (${inboxTasks.size})", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                        AnimatedVisibility(visible = isInboxExpanded) {
                            LazyColumn(modifier = Modifier.heightIn(max = 200.dp)) {
                                itemsIndexed(inboxTasks) { _, task ->
                                    val isDraggingThis = draggingTask?.id == task.id
                                    Box(modifier = Modifier.alpha(if (isDraggingThis) 0f else 1f)) {
                                        InboxItem(task = task, onCheck = { viewModel.toggleComplete(task) }, onClick = { taskToEdit = task },
                                            onDragStart = { draggingTask = task; dragOffset = Offset.Zero },
                                            onDrag = { change -> dragOffset += change },
                                            onDragEnd = {
                                                if (!isListView) {
                                                    val dropY = dragOffset.y; val scrollY = timelineScrollState.value.toFloat()
                                                    if (dropY > 100) {
                                                        val minutesFromStart = ((dropY + scrollY - 200) / pxPerHour * 60).toInt()
                                                        val newStart = (startH * 60 + minutesFromStart).coerceIn(0, 1439)
                                                        draggingTask?.let { viewModel.changeTaskStartTime(it, newStart) }
                                                    }
                                                }
                                                draggingTask = null; dragOffset = Offset.Zero
                                            })
                                    }
                                }
                            }
                        }
                    }

                    if (isListView) {
                        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            itemsIndexed(timelineTasks) { _, task ->
                                ScheduledTaskListItem(task = task, onCheck = { viewModel.toggleComplete(task) }, onClick = { taskToEdit = task })
                            }
                            item { Spacer(modifier = Modifier.height(80.dp)) }
                        }
                    } else {
                        DayView(
                            tasks = timelineTasks, startHour = startH, endHour = endH,
                            modifier = Modifier.weight(1f).onGloballyPositioned {
                                dayViewBoundsInWindow = it.boundsInWindow()
                            },
                            scrollState = timelineScrollState,
                            onTaskCheck = { viewModel.toggleComplete(it) },
                            onTaskClick = { taskToEdit = it },
                            onTaskTimeChange = { task, newTime -> viewModel.changeTaskStartTime(task, newTime) }
                        )
                    }
                }
            }
        }
        // Ghost Card logic remains...
        if (draggingTask != null && !isListView) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .padding(start = 64.dp, top = 120.dp)
                    .width(200.dp)
            ) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer), elevation = CardDefaults.cardElevation(8.dp)) {
                    Text(draggingTask?.title ?: "", modifier = Modifier.padding(16.dp))
                }
            }
        }
    }
}

// === ДОПОМІЖНІ КОМПОНЕНТИ ===

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenContainer(
    title: String,
    onMenuClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onMenuClick) {
                        Icon(Icons.Default.Menu, contentDescription = "Меню")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            content()
        }
    }
}

@Composable
fun PlaceholderContent(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color.Gray)
            Spacer(Modifier.height(16.dp))
            Text(text, color = Color.Gray, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun ScheduledTaskListItem(task: Task, onCheck: () -> Unit, onClick: () -> Unit) {
    // Копія функції з попереднього коду (список завдань)
    val taskColor = try { Color(android.graphics.Color.parseColor(task.colorHex)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    Card(modifier = Modifier.fillMaxWidth().height(72.dp).clickable(onClick = onClick), shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp), elevation = CardDefaults.cardElevation(2.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.width(80.dp).fillMaxHeight().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(com.nikidayn.taskbox.utils.minutesToTime(task.startTimeMinutes ?: 0), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Box(modifier = Modifier.width(4.dp).fillMaxHeight().background(if (task.isCompleted) Color.LightGray else taskColor))
            Row(modifier = Modifier.weight(1f).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, modifier = Modifier.weight(1f))
                Checkbox(checked = task.isCompleted, onCheckedChange = { onCheck() })
            }
        }
    }
}
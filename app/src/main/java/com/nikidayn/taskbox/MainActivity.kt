package com.nikidayn.taskbox

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.ui.SettingsScreen
import com.nikidayn.taskbox.ui.TemplatesScreen
import com.nikidayn.taskbox.ui.components.*
import com.nikidayn.taskbox.ui.theme.TaskBoxTheme
import com.nikidayn.taskbox.viewmodel.TaskViewModel
// Графіка та UI
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width

// Іконки
import androidx.compose.material.icons.filled.ViewTimeline
// Якщо Android Studio не бачить List, переконайтеся, що імпортуєте саме цей варіант:
import androidx.compose.material.icons.automirrored.filled.List
// Або, якщо ви використовуєте старішу версію Compose:
// import androidx.compose.material.icons.filled.List

// Утиліти (якщо вони не підтягнулись автоматично)
import com.nikidayn.taskbox.utils.minutesToTime
import com.nikidayn.taskbox.utils.formatDuration
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.ui.unit.sp

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

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            // Показуємо нижню панель тільки на головних екранах
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route

            // Ховаємо навігацію, якщо ми на екрані редагування нотатки
            if (currentRoute?.startsWith("note_detail") != true) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentRoute == "timeline",
                        onClick = { navController.navigate("timeline") { launchSingleTop = true; popUpTo("timeline") } },
                        icon = { Icon(Icons.Default.DateRange, contentDescription = "Календар") },
                        label = { Text("Справи") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "notes",
                        onClick = { navController.navigate("notes") { launchSingleTop = true; popUpTo("timeline") } },
                        icon = { Icon(Icons.Default.Edit, contentDescription = "Нотатки") },
                        label = { Text("Нотатки") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "templates",
                        onClick = { navController.navigate("templates") { launchSingleTop = true; popUpTo("timeline") } },
                        icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Шаблони") },
                        label = { Text("Шаблони") }
                    )
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { launchSingleTop = true; popUpTo("timeline") } },
                        icon = { Icon(Icons.Default.Settings, contentDescription = "Налаштування") },
                        label = { Text("Опції") }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "timeline",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("timeline") { TaskScreen(viewModel) }

            // Передаємо navController у NotesScreen
            composable("notes") {
                com.nikidayn.taskbox.ui.NotesScreen(viewModel, navController)
            }

            composable("templates") { TemplatesScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }

            // НОВИЙ ЕКРАН: Деталі нотатки (на весь екран)
            // noteId = -1 означає створення нової нотатки
            composable(
                route = "note_detail/{noteId}",
                arguments = listOf(androidx.navigation.navArgument("noteId") { type = androidx.navigation.NavType.IntType })
            ) { backStackEntry ->
                val noteId = backStackEntry.arguments?.getInt("noteId") ?: -1
                com.nikidayn.taskbox.ui.NoteDetailScreen(
                    noteId = noteId,
                    viewModel = viewModel,
                    navController = navController
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskScreen(viewModel: TaskViewModel) {
    val taskList by viewModel.tasks.collectAsState()
    val notesList by viewModel.notes.collectAsState()

    val workHours by viewModel.workHours.collectAsState()
    val startH = workHours.first.toInt()
    val endH = workHours.second.toInt()

    // --- СТАН ВИГЛЯДУ (False = Таймлайн, True = Список) ---
    var isListView by remember { mutableStateOf(false) } // <--- НОВЕ

    // --- ЛОГІКА СКРОЛУ ТАЙМЛАЙНУ ---
    val timelineScrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerHour = with(density) { DayViewHourHeight.toPx() }
    val topSpacerPx = with(density) { 32.dp.toPx() }

    // --- СТАНИ ДЛЯ DRAG & DROP ---
    var draggingTask by remember { mutableStateOf<Task?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }

    // Координати зони Таймлайну (куди кидати)
    var dayViewBoundsInWindow by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }

    val visibleTimeMinutes by remember {
        derivedStateOf {
            val scrollY = timelineScrollState.value
            val effectiveScroll = (scrollY - topSpacerPx).coerceAtLeast(0f)
            val hoursScrolled = effectiveScroll / pxPerHour
            val minutesScrolled = (hoursScrolled * 60).toInt()
            (startH * 60 + minutesScrolled).coerceIn(0, 1439)
        }
    }

    // --- ЛОГІКА КАЛЕНДАРЯ ---
    val initialDate = remember { LocalDate.now() }
    val initialPage = Int.MAX_VALUE / 2
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { Int.MAX_VALUE })
    val scope = rememberCoroutineScope()

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

    // --- СТАНИ РЕДАГУВАННЯ ---
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
                    actions = {
                        // --- КНОПКА ПЕРЕМИКАННЯ ВИГЛЯДУ ---
                        IconButton(onClick = { isListView = !isListView }) {
                            // Переконайтеся, що імпортували Icons.AutoMirrored.Filled.List або Icons.Default.List
                            val icon = if (isListView) Icons.Default.ViewTimeline else Icons.AutoMirrored.Filled.List
                            Icon(imageVector = icon, contentDescription = "Switch View")
                        }

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
                    onCreateNote = { autoTitle ->
                        viewModel.addNote(title = autoTitle, content = "", taskId = taskToEdit!!.id)
                    },
                    onDismiss = { taskToEdit = null },
                    onConfirm = { newTitle, newDuration, newStart, newParentId, newIsLocked, newDate, newColor, newEmoji ->
                        val updatedTask = taskToEdit!!.copy(
                            title = newTitle,
                            durationMinutes = newDuration,
                            startTimeMinutes = newStart,
                            linkedParentId = newParentId,
                            isLocked = newIsLocked,
                            date = newDate,
                            colorHex = newColor,
                            iconEmoji = newEmoji
                        )
                        viewModel.updateTask(updatedTask)
                        taskToEdit = null
                    },
                    onDelete = {
                        viewModel.deleteTask(taskToEdit!!)
                        taskToEdit = null
                    }
                )
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.padding(innerPadding).fillMaxSize()
            ) { page ->
                val pageDate = initialDate.plusDays((page - initialPage).toLong())
                val dateString = pageDate.toString()
                val tasksForDay = taskList.filter { it.date == dateString }
                val timelineTasks = tasksForDay.filter { it.startTimeMinutes != null }
                    .sortedBy { it.startTimeMinutes }
                val inboxTasks = tasksForDay.filter { it.startTimeMinutes == null }

                Column(modifier = Modifier.fillMaxSize()) {

                    // --- ВХІДНІ (INBOX) ---
                    if (inboxTasks.isNotEmpty()) {
                        var isInboxExpanded by remember { mutableStateOf(true) }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isInboxExpanded = !isInboxExpanded }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Inbox, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Вхідні", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text("${inboxTasks.size}", modifier = Modifier.padding(horizontal = 4.dp), color = MaterialTheme.colorScheme.onPrimary, style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                                Icon(if (isInboxExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        AnimatedVisibility(visible = isInboxExpanded) {
                            LazyColumn(
                                modifier = Modifier
                                    .heightIn(max = 220.dp)
                                    .fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                itemsIndexed(inboxTasks) { _, task ->
                                    val isDraggingThis = draggingTask?.id == task.id
                                    Box(modifier = Modifier.alpha(if (isDraggingThis) 0.0f else 1f)) {
                                        InboxItem(
                                            task = task,
                                            onCheck = { viewModel.toggleComplete(task) },
                                            onClick = { taskToEdit = task },
                                            onDragStart = { _ ->
                                                draggingTask = task
                                                dragOffset = Offset.Zero
                                            },
                                            onDrag = { change -> dragOffset += change },
                                            onDragEnd = {
                                                // --- ВАЖЛИВО: Дозволяємо дроп на таймлайн тільки якщо він видимий ---
                                                if (!isListView) {
                                                    val dropY = dragOffset.y
                                                    if (dropY > 100) {
                                                        val scrollY = timelineScrollState.value.toFloat()
                                                        val relativeDropY = dropY + scrollY - 200
                                                        val hoursFromStart = relativeDropY / pxPerHour
                                                        val minutesFromStart = (hoursFromStart * 60).toInt()
                                                        val newStartMinutes = (startH * 60 + minutesFromStart).coerceIn(0, 1439)
                                                        if (draggingTask != null) {
                                                            viewModel.changeTaskStartTime(draggingTask!!, newStartMinutes)
                                                        }
                                                    }
                                                }
                                                draggingTask = null
                                                dragOffset = Offset.Zero
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }

                    // --- ПЕРЕМИКАННЯ: СПИСОК АБО ТАЙМЛАЙН ---
                    if (isListView) {
                        // === РЕЖИМ СПИСКУ ===
                        if (timelineTasks.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("На сьогодні запланованих справ немає", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(timelineTasks) { _, task ->
                                    ScheduledTaskListItem(
                                        task = task,
                                        onCheck = { viewModel.toggleComplete(task) },
                                        onClick = { taskToEdit = task }
                                    )
                                }
                                item { Spacer(modifier = Modifier.height(80.dp)) } // Відступ під FAB
                            }
                        }
                    } else {
                        // === РЕЖИМ ТАЙМЛАЙНУ (Ваш старий DayView) ===
                        DayView(
                            tasks = timelineTasks,
                            startHour = startH,
                            endHour = endH,
                            modifier = Modifier
                                .weight(1f)
                                .onGloballyPositioned { coordinates ->
                                    val position = coordinates.positionInWindow()
                                    val size = coordinates.size
                                    dayViewBoundsInWindow = androidx.compose.ui.geometry.Rect(
                                        offset = position,
                                        size = androidx.compose.ui.geometry.Size(size.width.toFloat(), size.height.toFloat())
                                    )
                                },
                            scrollState = timelineScrollState,
                            onTaskCheck = { viewModel.toggleComplete(it) },
                            onTaskClick = { taskToEdit = it },
                            onTaskTimeChange = { task, newTime ->
                                viewModel.changeTaskStartTime(task, newTime)
                            }
                        )
                    }
                }
            }
        }

        // --- GHOST CARD (Показуємо тільки якщо ми НЕ в режимі списку) ---
        if (draggingTask != null && !isListView) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .padding(start = 64.dp, top = 120.dp)
                    .width(200.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(8.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = draggingTask?.title ?: "",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// === ДОДАЙТЕ ЦЮ НОВУ ФУНКЦІЮ В КІНЕЦЬ ФАЙЛУ MainActivity.kt ===
@Composable
fun ScheduledTaskListItem(
    task: Task,
    onCheck: () -> Unit,
    onClick: () -> Unit
) {
    val taskColor = try {
        Color(android.graphics.Color.parseColor(task.colorHex))
    } catch (e: Exception) {
        MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clickable(onClick = onClick),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Час (зліва на темнішому фоні)
            Column(
                modifier = Modifier
                    .width(80.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = com.nikidayn.taskbox.utils.minutesToTime(task.startTimeMinutes ?: 0),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                if (task.durationMinutes > 0) {
                    Text(
                        text = com.nikidayn.taskbox.utils.formatDuration(task.durationMinutes),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Кольорова лінія
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(if (task.isCompleted) Color.LightGray else taskColor)
            )

            // Основна інформація
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (task.iconEmoji.isNotEmpty()) {
                    Text(
                        text = task.iconEmoji,
                        fontSize = 24.sp,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }

                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
                    color = if (task.isCompleted) Color.Gray else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = { onCheck() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = taskColor,
                        uncheckedColor = taskColor.copy(alpha = 0.5f)
                    )
                )
            }
        }
    }
}
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

    Scaffold(
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route

                NavigationBarItem(
                    selected = currentRoute == "timeline",
                    onClick = {
                        navController.navigate("timeline") {
                            popUpTo("timeline") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.DateRange, contentDescription = "Календар") },
                    label = { Text("Справи") }
                )

                NavigationBarItem(
                    selected = currentRoute == "notes",
                    onClick = {
                        navController.navigate("notes") {
                            popUpTo("timeline") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Edit, contentDescription = "Нотатки") },
                    label = { Text("Нотатки") }
                )

                NavigationBarItem(
                    selected = currentRoute == "templates",
                    onClick = {
                        navController.navigate("templates") {
                            popUpTo("timeline") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Шаблони") },
                    label = { Text("Шаблони") }
                )

                NavigationBarItem(
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo("timeline") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Налаштування") },
                    label = { Text("Опції") }
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
            composable("notes") { com.nikidayn.taskbox.ui.NotesScreen(viewModel) }
            composable("templates") { TemplatesScreen(viewModel) }
            composable("settings") { SettingsScreen(viewModel) }
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

    // --- ЛОГІКА СКРОЛУ ТАЙМЛАЙНУ ---
    val timelineScrollState = rememberScrollState()
    val density = LocalDensity.current
    val pxPerHour = with(density) { DayViewHourHeight.toPx() }
    val topSpacerPx = with(density) { 32.dp.toPx() }

    // --- СТАНИ ДЛЯ DRAG & DROP ---
    var draggingTask by remember { mutableStateOf<Task?>(null) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) } // Поточна зміна позиції пальця

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

    // Головний контейнер Box, що дозволяє малювати "Примару" поверх усього
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
                            iconEmoji = newEmoji // <--- ЗБЕРІГАЄМО В БАЗУ ПРАВИЛЬНО
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

                        // Header
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
                                    Icon(
                                        Icons.Default.Inbox,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Вхідні",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                                        Text(
                                            text = "${inboxTasks.size}",
                                            modifier = Modifier.padding(horizontal = 4.dp),
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isInboxExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isInboxExpanded) "Collapse" else "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Список Вхідних з підтримкою Drag & Drop
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

                                    // Ховаємо оригінальну картку, коли тягнемо її копію
                                    Box(modifier = Modifier.alpha(if (isDraggingThis) 0.0f else 1f)) {
                                        InboxItem(
                                            task = task,
                                            onCheck = { viewModel.toggleComplete(task) },
                                            onClick = { taskToEdit = task },

                                            // --- ЛОГІКА DRAG START ---
                                            onDragStart = { _ ->
                                                draggingTask = task
                                                dragOffset = Offset.Zero // Скидаємо зміщення
                                            },
                                            // --- ЛОГІКА DRAG MOVE ---
                                            onDrag = { change ->
                                                dragOffset += change
                                            },
                                            // --- ЛОГІКА DRAG END ---
                                            onDragEnd = {
                                                val dropY = dragOffset.y

                                                // Евристика: Якщо потягнули вниз більше ніж на 100px (з Вхідних на Таймлайн)
                                                if (dropY > 100) {
                                                    val scrollY = timelineScrollState.value.toFloat()

                                                    // Розрахунок часу (приблизний, відносно верху екрану під вхідними)
                                                    // -200 - це приблизна поправка на висоту хедера та заголовка вхідних
                                                    val relativeDropY = dropY + scrollY - 200
                                                    val hoursFromStart = relativeDropY / pxPerHour
                                                    val minutesFromStart = (hoursFromStart * 60).toInt()

                                                    val newStartMinutes = (startH * 60 + minutesFromStart).coerceIn(0, 1439)

                                                    if (draggingTask != null) {
                                                        viewModel.changeTaskStartTime(draggingTask!!, newStartMinutes)
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

                    // --- ТАЙМЛАЙН ---
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

        // --- ВІЗУАЛІЗАЦІЯ ПЕРЕТЯГУВАННЯ (GHOST CARD) ---
        if (draggingTask != null) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(dragOffset.x.roundToInt(), dragOffset.y.roundToInt()) }
                    .padding(start = 64.dp, top = 120.dp) // Початковий зсув, щоб картка була під пальцем
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
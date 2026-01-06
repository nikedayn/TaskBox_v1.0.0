package com.nikidayn.taskbox.viewmodel

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikidayn.taskbox.TaskBoxApplication
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.model.Note
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.google.gson.Gson
import com.nikidayn.taskbox.model.BackupData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import com.nikidayn.taskbox.model.TaskTemplate
import com.nikidayn.taskbox.utils.UserPreferences

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as TaskBoxApplication).database.taskDao()
    private val prefs = UserPreferences(application)
    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, duration: Int, startTime: Int? = null, date: String) {
        viewModelScope.launch {
            dao.insertTask(
                Task(
                    title = title,
                    durationMinutes = duration,
                    startTimeMinutes = startTime,
                    date = date,
                    colorHex = "#FFEB3B"
                )
            )
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch { dao.deleteTask(task) }
    }

    fun toggleComplete(task: Task) {
        viewModelScope.launch { dao.updateTask(task.copy(isCompleted = !task.isCompleted)) }
    }

    fun generateDemoData() {
        viewModelScope.launch {
            val tasks = listOf(
                Task(title = "Прокинутись", startTimeMinutes = 420, durationMinutes = 30, colorHex = "#FFCDD2"),
                Task(title = "Ранкова зарядка", startTimeMinutes = 450, durationMinutes = 45, colorHex = "#C8E6C9"),
                Task(title = "Сніданок", startTimeMinutes = 510, durationMinutes = 30, colorHex = "#BBDEFB")
            )
            tasks.forEach { dao.insertTask(it) }
        }
    }

    // Список шаблонів
    val templates: StateFlow<List<TaskTemplate>> = dao.getAllTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Створити новий шаблон
    fun addTemplate(title: String, duration: Int, emoji: String) {
        viewModelScope.launch {
            dao.insertTemplate(TaskTemplate(title = title, durationMinutes = duration, iconEmoji = emoji))
        }
    }

    fun deleteTemplate(template: TaskTemplate) {
        viewModelScope.launch { dao.deleteTemplate(template) }
    }

    fun applyTemplateToInbox(template: TaskTemplate) {
        viewModelScope.launch {
            dao.insertTask(
                Task(
                    title = template.title,
                    durationMinutes = template.durationMinutes,
                    colorHex = template.colorHex,
                    startTimeMinutes = null
                )
            )
        }
    }

    fun updateTaskDetails(task: Task, newTitle: String, newDuration: Int, newStartTime: Int?) {
        viewModelScope.launch {
            val updatedTask = task.copy(
                title = newTitle,
                durationMinutes = newDuration,
                startTimeMinutes = newStartTime
            )
            dao.updateTask(updatedTask)
        }
    }

    fun deleteTasks(tasksToDelete: List<Task>) {
        viewModelScope.launch {
            dao.deleteTasks(tasksToDelete)
        }
    }

    fun changeTaskStartTime(task: Task, newStartTime: Int) {
        viewModelScope.launch {
            val allTasks = tasks.value.toMutableList()
            val index = allTasks.indexOfFirst { it.id == task.id }
            if (index == -1) return@launch

            // Якщо сама картка заблокована - не рухаємо
            if (allTasks[index].isLocked) return@launch

            val currentTask = allTasks[index]
            val duration = currentTask.durationMinutes

            // Початковий бажаний час
            var proposedStart = newStartTime.coerceIn(0, 1439 - duration)
            var proposedEnd = proposedStart + duration

            // === ЕТАП 1: ЛОГІКА "ВИШТОВХУВАННЯ" (Force Field) ===
            // Перевіряємо, чи ми не "сіли" зверху на заблоковану картку.
            // Якщо так - вона має нас виплюнути в найближчу сторону.

            val lockedTasks = allTasks.filter { it.isLocked && it.id != task.id }

            for (locked in lockedTasks) {
                val lockedStart = locked.startTimeMinutes ?: 0
                val lockedEnd = lockedStart + locked.durationMinutes

                // Перевірка на перетин (Overlap Test)
                if (proposedStart < lockedEnd && proposedEnd > lockedStart) {

                    // Ми налізли на заблокований блок. Куди нас виштовхнути?
                    // Рахуємо відстань до "виходу" зверху і знизу.

                    val distanceToTop = kotlin.math.abs(proposedEnd - lockedStart)    // Щоб вийти вгору
                    val distanceToBottom = kotlin.math.abs(proposedStart - lockedEnd) // Щоб вийти вниз

                    if (distanceToTop < distanceToBottom) {
                        // Ближче вискочити вгору (перед заблокованим блоком)
                        proposedStart = lockedStart - duration
                    } else {
                        // Ближче вискочити вниз (після заблокованого блоку)
                        // Тобто ми "пройшли крізь стіну"
                        proposedStart = lockedEnd
                    }

                    // Оновлюємо кінець, бо старт змінився
                    proposedStart = proposedStart.coerceIn(0, 1439 - duration)
                    proposedEnd = proposedStart + duration
                }
            }

            // Якщо після "виштовхування" час не змінився від початкового положення в базі - виходимо
            if (proposedStart == (currentTask.startTimeMinutes ?: 0)) return@launch

            // === ЕТАП 2: ОНОВЛЕННЯ ТА ФІЗИКА ДОМІНО ===
            // Тепер, коли ми знаємо "безпечний" час (proposedStart), який точно не всередині стіни,
            // запускаємо звичайну фізику, щоб розштовхати незаблоковані картки.

            allTasks[index] = currentTask.copy(startTimeMinutes = proposedStart)
            val tasksToUpdate = mutableListOf<Task>()
            tasksToUpdate.add(allTasks[index])

            val isMovingDown = proposedStart > (task.startTimeMinutes ?: 0)

            if (isMovingDown) {
                // Рух ВНИЗ -> Штовхаємо нижні
                val timelineTasks = allTasks.filter { it.startTimeMinutes != null }
                    .sortedBy { it.startTimeMinutes }
                    .toMutableList()

                for (i in 0 until timelineTasks.size - 1) {
                    val current = timelineTasks[i]
                    val next = timelineTasks[i + 1]
                    val currentEnd = (current.startTimeMinutes ?: 0) + current.durationMinutes
                    val nextStart = next.startTimeMinutes ?: 0

                    if (nextStart < currentEnd) {
                        // Якщо наступна - це СТІНА, то ми вперлися і зупиняємось перед нею
                        // (Але Етап 1 вже гарантував, що МИ не в стіні. Це для ланцюгової реакції)
                        if (next.isLocked) {
                            val correctedCurrent = current.copy(startTimeMinutes = nextStart - current.durationMinutes)
                            tasksToUpdate.add(correctedCurrent)
                            timelineTasks[i] = correctedCurrent
                        } else {
                            // Якщо коробка - штовхаємо далі
                            val newNextStart = currentEnd.coerceAtMost(1439 - next.durationMinutes)
                            if (newNextStart != nextStart) {
                                val updatedNext = next.copy(startTimeMinutes = newNextStart)
                                timelineTasks[i + 1] = updatedNext
                                tasksToUpdate.add(updatedNext)
                            }
                        }
                    }
                }
            } else {
                // Рух ВГОРУ -> Штовхаємо верхні
                val timelineTasks = allTasks.filter { it.startTimeMinutes != null }
                    .sortedByDescending { it.startTimeMinutes }
                    .toMutableList()

                for (i in 0 until timelineTasks.size - 1) {
                    val current = timelineTasks[i]
                    val previous = timelineTasks[i + 1]
                    val currentStart = current.startTimeMinutes ?: 0
                    val prevStart = previous.startTimeMinutes ?: 0
                    val prevEnd = prevStart + previous.durationMinutes

                    if (currentStart < prevEnd) {
                        if (previous.isLocked) {
                            // Вперлися в стіну знизу
                            val correctedCurrent = current.copy(startTimeMinutes = prevEnd)
                            tasksToUpdate.add(correctedCurrent)
                            timelineTasks[i] = correctedCurrent
                        } else {
                            // Штовхаємо коробку вгору
                            val newPrevStart = (currentStart - previous.durationMinutes).coerceAtLeast(0)
                            if (newPrevStart != prevStart) {
                                val updatedPrev = previous.copy(startTimeMinutes = newPrevStart)
                                timelineTasks[i + 1] = updatedPrev
                                tasksToUpdate.add(updatedPrev)
                            }
                        }
                    }
                }
            }

            if (tasksToUpdate.isNotEmpty()) {
                dao.updateTasks(tasksToUpdate.distinctBy { it.id })
            }
        }
    }

    // --- НОТАТКИ ---
    val notes: StateFlow<List<Note>> = dao.getAllNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            dao.insertNote(Note(title = title, content = content))
        }
    }

    // !!! ДОДАНО ПРОПУЩЕНУ ФУНКЦІЮ !!!
    fun updateNote(note: Note, title: String, content: String) {
        viewModelScope.launch {
            dao.updateNote(note.copy(title = title, content = content))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { dao.deleteNote(note) }
    }

    // Функція оновлення завдання (для MainActivity)
    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task)
        }
    }

    // --- НАЛАШТУВАННЯ ---

    private val _themeMode = MutableStateFlow(prefs.getThemeMode())
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    fun updateTheme(mode: Int) {
        prefs.setThemeMode(mode)
        _themeMode.value = mode
    }

    private val _workHours = MutableStateFlow(prefs.getStartHour() to prefs.getEndHour())
    val workHours: StateFlow<Pair<Float, Float>> = _workHours.asStateFlow()

    fun updateWorkHours(start: Float, end: Float) {
        prefs.setStartHour(start)
        prefs.setEndHour(end)
        _workHours.value = start to end
    }

    // --- РОБОТА З ДАНИМИ ---

    fun deleteAllData() {
        viewModelScope.launch {
            dao.clearAllData()
        }
    }

    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        val backup = BackupData(
            tasks = tasks.value,
            templates = templates.value,
            notes = notes.value
        )
        return@withContext Gson().toJson(backup)
    }

    fun restoreBackup(inputStream: InputStream, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                val backup = Gson().fromJson(jsonString, BackupData::class.java)

                dao.clearAllData()

                backup.tasks.forEach { dao.insertTask(it) }
                backup.templates.forEach { dao.insertTemplate(it) }
                backup.notes.forEach { dao.insertNote(it) }

                withContext(Dispatchers.Main) { onSuccess() }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onError() }
            }
        }
    }
}
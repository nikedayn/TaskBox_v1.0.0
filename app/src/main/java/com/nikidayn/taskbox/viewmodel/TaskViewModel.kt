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
            try {
                val allTasks = tasks.value
                val index = allTasks.indexOfFirst { it.id == task.id }
                if (index == -1) return@launch

                val currentTask = allTasks[index]
                if (currentTask.isLocked) return@launch

                val updatesMap = allTasks.associateBy { it.id }.toMutableMap()
                val duration = currentTask.durationMinutes
                val oldStart = currentTask.startTimeMinutes ?: 0
                val proposedStart = newStartTime.coerceIn(0, 1439 - duration)
                val diff = proposedStart - oldStart

                if (diff == 0) return@launch

                // 1. Рухаємо ланцюжок (прив'язані картки)
                val processedIds = mutableSetOf<Int>()
                fun moveTaskAndChildren(taskId: Int, timeDiff: Int) {
                    if (processedIds.contains(taskId)) return
                    processedIds.add(taskId)
                    val t = updatesMap[taskId] ?: return
                    if (t.isLocked) return

                    val currentStart = t.startTimeMinutes ?: 0
                    val newStart = (currentStart + timeDiff).coerceIn(0, 1439 - t.durationMinutes)
                    updatesMap[taskId] = t.copy(startTimeMinutes = newStart)

                    allTasks.filter { it.linkedParentId == taskId }.forEach { child ->
                        moveTaskAndChildren(child.id, timeDiff)
                    }
                }
                moveTaskAndChildren(currentTask.id, diff)

                // 2. ФІЗИКА З УРАХУВАННЯМ "АГРЕСОРА" (Того, кого тягнуть)
                repeat(3) {
                    val sortedList = updatesMap.values
                        .filter { it.startTimeMinutes != null }
                        .sortedBy { it.startTimeMinutes }
                        .toMutableList()

                    // Прохід по парах сусідів
                    for (i in 0 until sortedList.size - 1) {
                        val top = sortedList[i]       // Верхня картка
                        val bottom = sortedList[i + 1] // Нижня картка

                        val topStart = top.startTimeMinutes ?: 0
                        val topEnd = topStart + top.durationMinutes
                        val bottomStart = bottom.startTimeMinutes ?: 0

                        // Є накладання?
                        if (bottomStart < topEnd) {
                            val overlap = topEnd - bottomStart
                            // Поріг для проходження наскрізь (50% від меншої картки)
                            val threshold = minOf(top.durationMinutes, bottom.durationMinutes) / 2

                            // Визначаємо, хто "Агресор" (кого ми тягнемо)
                            val isTopAggressor = top.id == task.id
                            val isBottomAggressor = bottom.id == task.id

                            if (isTopAggressor) {
                                // === ТЯГНЕМО ВЕРХНЮ ВНИЗ ===
                                // Вона штовхає нижню.
                                if (bottom.isLocked) {
                                    // Нижня - стіна. Верхня відскакує назад.
                                    val correctStart = (bottomStart - top.durationMinutes).coerceAtLeast(0)
                                    val updatedTop = top.copy(startTimeMinutes = correctStart)
                                    updatesMap[top.id] = updatedTop
                                    sortedList[i] = updatedTop
                                } else if (overlap > threshold) {
                                    // SWAP: Нижня стрибає вгору (пропускає нас)
                                    val jumpStart = (topStart - bottom.durationMinutes).coerceAtLeast(0)
                                    val updatedBottom = bottom.copy(startTimeMinutes = jumpStart)
                                    updatesMap[bottom.id] = updatedBottom
                                    sortedList[i + 1] = updatedBottom
                                } else {
                                    // PUSH: Штовхаємо нижню вниз
                                    val pushStart = topEnd.coerceAtMost(1439 - bottom.durationMinutes)
                                    val updatedBottom = bottom.copy(startTimeMinutes = pushStart)
                                    updatesMap[bottom.id] = updatedBottom
                                    sortedList[i + 1] = updatedBottom
                                }

                            } else if (isBottomAggressor) {
                                // === ТЯГНЕМО НИЖНЮ ВГОРУ ===
                                // Вона штовхає верхню.
                                if (top.isLocked) {
                                    // Верхня - стіна. Нижня відскакує вниз.
                                    val correctStart = (topEnd).coerceAtMost(1439 - bottom.durationMinutes)
                                    val updatedBottom = bottom.copy(startTimeMinutes = correctStart)
                                    updatesMap[bottom.id] = updatedBottom
                                    sortedList[i + 1] = updatedBottom
                                } else if (overlap > threshold) {
                                    // SWAP: Верхня стрибає вниз (пропускає нас)
                                    val jumpStart = (bottomStart + bottom.durationMinutes).coerceAtMost(1439 - top.durationMinutes)
                                    val updatedTop = top.copy(startTimeMinutes = jumpStart)
                                    updatesMap[top.id] = updatedTop
                                    sortedList[i] = updatedTop
                                } else {
                                    // PUSH: Штовхаємо верхню вгору
                                    val pushStart = (bottomStart - top.durationMinutes).coerceAtLeast(0)
                                    val updatedTop = top.copy(startTimeMinutes = pushStart)
                                    updatesMap[top.id] = updatedTop
                                    sortedList[i] = updatedTop
                                }

                            } else {
                                // === ЕФЕКТ ДОМІНО (Ніхто з них не є активним) ===
                                // Тут працює проста гравітація: верхні тиснуть на нижніх.
                                if (!bottom.isLocked) {
                                    val pushStart = topEnd.coerceAtMost(1439 - bottom.durationMinutes)
                                    if (pushStart != bottomStart) {
                                        val updatedBottom = bottom.copy(startTimeMinutes = pushStart)
                                        updatesMap[bottom.id] = updatedBottom
                                        sortedList[i + 1] = updatedBottom
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Зберігаємо зміни
                val tasksToSave = updatesMap.values.filter { t ->
                    val original = allTasks.find { it.id == t.id }
                    original?.startTimeMinutes != t.startTimeMinutes
                }

                if (tasksToSave.isNotEmpty()) {
                    dao.updateTasks(tasksToSave.toList())
                }

            } catch (e: Exception) {
                e.printStackTrace()
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
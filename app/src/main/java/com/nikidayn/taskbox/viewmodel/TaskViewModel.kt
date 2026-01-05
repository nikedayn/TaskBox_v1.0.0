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
                    date = date, // Зберігаємо дату
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

    // ГОЛОВНА МАГІЯ: Створити завдання на основі шаблону
    fun applyTemplateToInbox(template: TaskTemplate) {
        viewModelScope.launch {
            dao.insertTask(
                Task(
                    title = template.title,
                    durationMinutes = template.durationMinutes,
                    colorHex = template.colorHex,
                    startTimeMinutes = null // Падає у вхідні
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
            val clampedTime = newStartTime.coerceIn(0, 1439) // Обмежуємо (00:00 - 23:59)
            dao.updateTask(task.copy(startTimeMinutes = clampedTime))
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

    fun updateNote(note: Note, newTitle: String, newContent: String) {
        viewModelScope.launch {
            dao.updateNote(note.copy(title = newTitle, content = newContent))
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch { dao.deleteNote(note) }
    }

    // --- НАЛАШТУВАННЯ ---

    // Стан теми
    private val _themeMode = MutableStateFlow(prefs.getThemeMode())
    val themeMode: StateFlow<Int> = _themeMode.asStateFlow()

    fun updateTheme(mode: Int) {
        prefs.setThemeMode(mode)
        _themeMode.value = mode
    }

    // Робочі години
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

    // 1. Генеруємо JSON з усіх даних
    suspend fun createBackupJson(): String = withContext(Dispatchers.IO) {
        // Збираємо поточні дані напряму з бази (через flow.first() або додайте прості suspend методи в DAO)
        // Для простоти, оскільки у нас Flow, ми можемо взяти поточні значення зі StateFlow, якщо вони завантажені,
        // але надійніше додати в DAO методи getList...
        // Давайте використаємо поточні значення зі StateFlow, які ми вже маємо в пам'яті:
        val backup = BackupData(
            tasks = tasks.value,
            templates = templates.value,
            notes = notes.value
        )
        return@withContext Gson().toJson(backup)
    }

    // 2. Відновлюємо дані з JSON
    fun restoreBackup(inputStream: InputStream, onSuccess: () -> Unit, onError: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val reader = BufferedReader(InputStreamReader(inputStream))
                val jsonString = reader.readText()
                val backup = Gson().fromJson(jsonString, BackupData::class.java)

                // Очищаємо базу і записуємо нове
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

    fun exportData() { /* Логіка експорту JSON */ }
    fun importData() { /* Логіка імпорту JSON */ }
}
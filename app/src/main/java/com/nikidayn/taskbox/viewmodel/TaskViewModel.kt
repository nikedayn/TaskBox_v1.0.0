package com.nikidayn.taskbox.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nikidayn.taskbox.TaskBoxApplication
import com.nikidayn.taskbox.model.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import com.nikidayn.taskbox.model.TaskTemplate

class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = (application as TaskBoxApplication).database.taskDao()

    val tasks: StateFlow<List<Task>> = dao.getAllTasks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addTask(title: String, duration: Int, startTime: Int? = null) {
        viewModelScope.launch {
            dao.insertTask(
                Task(
                    title = title,
                    durationMinutes = duration,
                    startTimeMinutes = startTime,
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
}
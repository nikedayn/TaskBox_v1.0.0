package com.nikidayn.taskbox.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nikidayn.taskbox.model.Category // <--- Перевірте імпорт
import com.nikidayn.taskbox.model.Note
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.model.TaskTemplate

// 👇 ДОДАЙТЕ Category::class СЮДИ
@Database(
    entities = [Task::class, TaskTemplate::class, Note::class, Category::class],
    version = 9, // Також перевірте, що версія піднята (наприклад, 9)
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}


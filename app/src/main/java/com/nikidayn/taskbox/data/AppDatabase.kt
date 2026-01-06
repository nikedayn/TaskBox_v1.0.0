package com.nikidayn.taskbox.data

import androidx.room.Database
import androidx.room.RoomDatabase
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.model.Note
import com.nikidayn.taskbox.model.TaskTemplate

// Додаємо TaskTemplate::class у entities
@Database(entities = [Task::class, TaskTemplate::class, Note::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
}


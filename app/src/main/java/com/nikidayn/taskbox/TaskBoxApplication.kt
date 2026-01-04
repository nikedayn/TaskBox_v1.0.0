package com.nikidayn.taskbox

import android.app.Application
import androidx.room.Room
import com.nikidayn.taskbox.data.AppDatabase

class TaskBoxApplication : Application() {
    // Створюємо базу даних
    val database: AppDatabase by lazy {
        Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "taskbox_db"
        )
            .fallbackToDestructiveMigration() // <--- ДОДАЙТЕ ЦЕЙ РЯДОК
            .build()
    }
}
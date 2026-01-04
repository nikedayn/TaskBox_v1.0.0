package com.nikidayn.taskbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "templates")
data class TaskTemplate(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val durationMinutes: Int,
    val iconEmoji: String = "📝", // Будемо використовувати емодзі як іконку
    val colorHex: String = "#FFEB3B"
)
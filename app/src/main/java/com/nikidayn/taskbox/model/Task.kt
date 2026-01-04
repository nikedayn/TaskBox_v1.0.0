package com.nikidayn.taskbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",

    val startTimeMinutes: Int? = null,
    val durationMinutes: Int = 30,

    val isCompleted: Boolean = false,
    val linkedParentId: Int? = null,

    // Обов'язково має бути це поле!
    val colorHex: String = "#FFEB3B"
) {
    val endTimeMinutes: Int?
        get() = startTimeMinutes?.plus(durationMinutes)
}
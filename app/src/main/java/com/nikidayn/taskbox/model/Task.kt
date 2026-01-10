package com.nikidayn.taskbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val startTimeMinutes: Int? = null,
    val durationMinutes: Int = 30,
    val date: String = LocalDate.now().toString(),
    val isCompleted: Boolean = false,
    val linkedParentId: Int? = null,
    val isLocked: Boolean = false,
    val colorHex: String = "#FFEB3B",
    val iconEmoji: String = "⚡"
) {
    val endTimeMinutes: Int?
        get() = startTimeMinutes?.plus(durationMinutes)
}
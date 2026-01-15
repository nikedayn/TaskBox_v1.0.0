package com.nikidayn.taskbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val colorHex: String = "#808080", // Сірий за замовчуванням
    val iconEmoji: String = "🏷️"
)
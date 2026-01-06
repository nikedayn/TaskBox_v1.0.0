package com.nikidayn.taskbox.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val content: String,
    val date: String = LocalDate.now().toString(),
    val colorHex: String = "#FFF9C4",
    val taskId: Int? = null // <--- НОВЕ ПОЛЕ: ID завдання, до якого прив'язана нотатка
)
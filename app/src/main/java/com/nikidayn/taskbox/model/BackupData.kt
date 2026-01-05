package com.nikidayn.taskbox.model

data class BackupData(
    val tasks: List<Task>,
    val templates: List<TaskTemplate>,
    val notes: List<Note>
)
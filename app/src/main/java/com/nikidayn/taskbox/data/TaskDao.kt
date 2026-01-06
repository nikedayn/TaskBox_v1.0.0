package com.nikidayn.taskbox.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.nikidayn.taskbox.model.Task
import com.nikidayn.taskbox.model.Note
import kotlinx.coroutines.flow.Flow
import com.nikidayn.taskbox.model.TaskTemplate

@Dao
interface TaskDao {
    // Отримуємо всі завдання. Flow означає, що список оновиться автоматично, якщо щось зміниться.
    @Query("SELECT * FROM tasks ORDER BY startTimeMinutes ASC")
    fun getAllTasks(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Int): Task?

    // --- МЕТОДИ ДЛЯ ШАБЛОНІВ ---
    @Query("SELECT * FROM templates")
    fun getAllTemplates(): Flow<List<TaskTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: TaskTemplate)

    @Delete
    suspend fun deleteTemplate(template: TaskTemplate)

    @Delete
    suspend fun deleteTasks(tasks: List<Task>)

    @Query("SELECT * FROM notes ORDER BY id DESC")
    fun getAllNotes(): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note)

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    @Query("DELETE FROM templates")
    suspend fun deleteAllTemplates()

    @Query("DELETE FROM notes")
    suspend fun deleteAllNotes()

    @Transaction
    suspend fun clearAllData() {
        deleteAllTasks()
        deleteAllTemplates()
        deleteAllNotes()
    }

    @Transaction
    @Update
    suspend fun updateTasks(tasks: List<Task>)


}
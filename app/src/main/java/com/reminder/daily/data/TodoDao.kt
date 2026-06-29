package com.reminder.daily.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("""
        SELECT * FROM todos WHERE listId = :listId
        ORDER BY isCompleted ASC,
        CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END ASC,
        dueDate ASC,
        createdAt DESC
    """)
    fun getTodosForList(listId: Long): Flow<List<Todo>>

    @Insert
    suspend fun insert(todo: Todo)

    @Update
    suspend fun update(todo: Todo)

    @Delete
    suspend fun delete(todo: Todo)

    @Query("SELECT COUNT(*) FROM todos WHERE isCompleted = 0")
    suspend fun getTotalPendingCount(): Int
}

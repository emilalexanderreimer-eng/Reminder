package com.reminder.daily.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoListDao {
    @Query("""
        SELECT todo_lists.*, COUNT(CASE WHEN todos.isCompleted = 0 THEN 1 END) as pendingCount
        FROM todo_lists
        LEFT JOIN todos ON todos.listId = todo_lists.id
        GROUP BY todo_lists.id
        ORDER BY todo_lists.createdAt ASC
    """)
    fun getAllListsWithCounts(): Flow<List<TodoListWithCount>>

    @Insert
    suspend fun insert(list: TodoList): Long

    @Delete
    suspend fun delete(list: TodoList)
}

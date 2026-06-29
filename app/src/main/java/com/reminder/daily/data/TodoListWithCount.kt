package com.reminder.daily.data

import androidx.room.Embedded

data class TodoListWithCount(
    @Embedded val todoList: TodoList,
    val pendingCount: Int
)

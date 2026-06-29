package com.reminder.daily.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "todo_lists")
data class TodoList(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

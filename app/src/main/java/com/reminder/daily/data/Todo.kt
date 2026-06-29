package com.reminder.daily.data

data class Todo(
    val id: String = "",
    val title: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val addedBy: String = ""
)

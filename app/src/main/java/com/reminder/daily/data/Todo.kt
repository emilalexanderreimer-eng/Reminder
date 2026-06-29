package com.reminder.daily.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "todos",
    foreignKeys = [ForeignKey(
        entity = TodoList::class,
        parentColumns = ["id"],
        childColumns = ["listId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("listId")]
)
data class Todo(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,
    val title: String,
    val isCompleted: Boolean = false,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

package com.reminder.daily.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reminder.daily.data.TodoListWithCount
import com.reminder.daily.databinding.ItemTodoListBinding

class TodoListAdapter(
    private val onOpen: (TodoListWithCount) -> Unit,
    private val onDelete: (TodoListWithCount) -> Unit
) : ListAdapter<TodoListWithCount, TodoListAdapter.ListViewHolder>(ListDiffCallback()) {

    inner class ListViewHolder(private val binding: ItemTodoListBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: TodoListWithCount) {
            binding.textListName.text = item.todoList.name
            binding.textPendingCount.text = when (item.pendingCount) {
                0 -> "Alle erledigt ✓"
                1 -> "1 offene Aufgabe"
                else -> "${item.pendingCount} offene Aufgaben"
            }
            binding.root.setOnClickListener { onOpen(item) }
            binding.buttonDeleteList.setOnClickListener { onDelete(item) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val binding = ItemTodoListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ListViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class ListDiffCallback : DiffUtil.ItemCallback<TodoListWithCount>() {
    override fun areItemsTheSame(oldItem: TodoListWithCount, newItem: TodoListWithCount) =
        oldItem.todoList.id == newItem.todoList.id
    override fun areContentsTheSame(oldItem: TodoListWithCount, newItem: TodoListWithCount) =
        oldItem == newItem
}

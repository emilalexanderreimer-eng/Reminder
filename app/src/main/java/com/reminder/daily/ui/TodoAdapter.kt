package com.reminder.daily.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reminder.daily.data.Todo
import com.reminder.daily.databinding.ItemTodoBinding

class TodoAdapter(
    private val onToggle: (Todo) -> Unit,
    private val onDelete: (Todo) -> Unit
) : ListAdapter<Todo, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    inner class TodoViewHolder(private val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: Todo) {
            binding.textTodoTitle.text = todo.title
            binding.checkboxTodo.isChecked = todo.isCompleted

            if (todo.isCompleted) {
                binding.textTodoTitle.paintFlags =
                    binding.textTodoTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.textTodoTitle.alpha = 0.5f
            } else {
                binding.textTodoTitle.paintFlags =
                    binding.textTodoTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.textTodoTitle.alpha = 1.0f
            }

            binding.checkboxTodo.setOnCheckedChangeListener(null)
            binding.checkboxTodo.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != todo.isCompleted) {
                    onToggle(todo.copy(isCompleted = isChecked))
                }
            }

            binding.buttonDelete.setOnClickListener {
                onDelete(todo)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoViewHolder {
        val binding = ItemTodoBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TodoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TodoViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class TodoDiffCallback : DiffUtil.ItemCallback<Todo>() {
    override fun areItemsTheSame(oldItem: Todo, newItem: Todo) = oldItem.id == newItem.id
    override fun areContentsTheSame(oldItem: Todo, newItem: Todo) = oldItem == newItem
}

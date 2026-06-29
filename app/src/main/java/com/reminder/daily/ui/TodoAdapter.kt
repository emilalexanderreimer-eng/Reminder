package com.reminder.daily.ui

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.reminder.daily.R
import com.reminder.daily.data.Todo
import com.reminder.daily.databinding.ItemTodoBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodoAdapter(
    private val onToggle: (Todo) -> Unit,
    private val onDelete: (Todo) -> Unit
) : ListAdapter<Todo, TodoAdapter.TodoViewHolder>(TodoDiffCallback()) {

    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    inner class TodoViewHolder(private val binding: ItemTodoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(todo: Todo) {
            binding.textTodoTitle.text = todo.title
            binding.checkboxTodo.isChecked = todo.isCompleted

            val strikeFlag = Paint.STRIKE_THRU_TEXT_FLAG
            if (todo.isCompleted) {
                binding.textTodoTitle.paintFlags = binding.textTodoTitle.paintFlags or strikeFlag
                binding.textTodoTitle.alpha = 0.45f
            } else {
                binding.textTodoTitle.paintFlags = binding.textTodoTitle.paintFlags and strikeFlag.inv()
                binding.textTodoTitle.alpha = 1.0f
            }

            if (todo.dueDate != null && !todo.isCompleted) {
                val todayStart = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                val tomorrowStart = todayStart + 86_400_000L

                val (label, colorRes) = when {
                    todo.dueDate < todayStart  -> "Überfällig!" to R.color.error
                    todo.dueDate < tomorrowStart -> "Heute fällig" to R.color.due_today
                    todo.dueDate < tomorrowStart + 86_400_000L -> "Morgen fällig" to R.color.colorPrimary
                    else -> "bis ${dateFormat.format(todo.dueDate)}" to R.color.text_secondary
                }
                binding.textDueDate.text = label
                binding.textDueDate.setTextColor(ContextCompat.getColor(binding.root.context, colorRes))
                binding.textDueDate.visibility = View.VISIBLE
            } else {
                binding.textDueDate.visibility = View.GONE
            }

            binding.checkboxTodo.setOnCheckedChangeListener(null)
            binding.checkboxTodo.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked != todo.isCompleted) onToggle(todo.copy(isCompleted = isChecked))
            }
            binding.buttonDelete.setOnClickListener { onDelete(todo) }
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

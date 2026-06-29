package com.reminder.daily

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.reminder.daily.data.AppDatabase
import com.reminder.daily.data.Todo
import com.reminder.daily.databinding.ActivityTodoBinding
import com.reminder.daily.ui.TodoAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LIST_ID = "list_id"
        const val EXTRA_LIST_NAME = "list_name"
    }

    private lateinit var binding: ActivityTodoBinding
    private lateinit var adapter: TodoAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }
    private var listId: Long = 0
    private val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.GERMANY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTodoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listId = intent.getLongExtra(EXTRA_LIST_ID, 0)
        val listName = intent.getStringExtra(EXTRA_LIST_NAME) ?: "Aufgaben"

        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            title = listName
            setDisplayHomeAsUpEnabled(true)
        }

        setupRecyclerView()
        setupFab()
        observeTodos()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onToggle = { todo -> lifecycleScope.launch { db.todoDao().update(todo) } },
            onDelete = { todo -> lifecycleScope.launch { db.todoDao().delete(todo) } }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTodo.setOnClickListener { showAddTodoDialog() }
    }

    private fun observeTodos() {
        lifecycleScope.launch {
            db.todoDao().getTodosForList(listId).collectLatest { todos ->
                adapter.submitList(todos)
                binding.textEmpty.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddTodoDialog() {
        var selectedDueDate: Long? = null
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_todo, null)
        val editTitle = dialogView.findViewById<EditText>(R.id.editTodoTitle)
        val checkDueDate = dialogView.findViewById<CheckBox>(R.id.checkDueDate)
        val buttonPickDate = dialogView.findViewById<MaterialButton>(R.id.buttonPickDate)
        val textSelectedDate = dialogView.findViewById<TextView>(R.id.textSelectedDate)

        buttonPickDate.visibility = View.GONE
        textSelectedDate.visibility = View.GONE

        checkDueDate.setOnCheckedChangeListener { _, isChecked ->
            buttonPickDate.visibility = if (isChecked) View.VISIBLE else View.GONE
            if (!isChecked) {
                selectedDueDate = null
                textSelectedDate.visibility = View.GONE
            }
        }

        buttonPickDate.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val cal2 = Calendar.getInstance().apply {
                        set(year, month, day, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    selectedDueDate = cal2.timeInMillis
                    textSelectedDate.text = dateFormat.format(cal2.time)
                    textSelectedDate.visibility = View.VISIBLE
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).apply { datePicker.minDate = System.currentTimeMillis() }.show()
        }

        AlertDialog.Builder(this)
            .setTitle("Neue Aufgabe")
            .setView(dialogView)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val title = editTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.todoDao().insert(Todo(listId = listId, title = title, dueDate = selectedDueDate))
                    }
                } else {
                    Toast.makeText(this, "Bitte einen Text eingeben", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
            .also { editTitle.requestFocus() }
    }
}

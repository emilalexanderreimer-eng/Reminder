package com.reminder.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.reminder.daily.data.AppDatabase
import com.reminder.daily.data.Todo
import com.reminder.daily.databinding.ActivityMainBinding
import com.reminder.daily.notification.NotificationReceiver
import com.reminder.daily.notification.NotificationScheduler
import com.reminder.daily.ui.TodoAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TodoAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationScheduler.scheduleAll(this)
            Toast.makeText(this, "Benachrichtigungen aktiviert ✓", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Ohne Berechtigung keine Benachrichtigungen möglich.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        observeTodos()
        ensureNotificationChannel()
        requestNotificationPermissionAndSchedule()
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onToggle = { todo ->
                lifecycleScope.launch { db.todoDao().update(todo) }
            },
            onDelete = { todo ->
                lifecycleScope.launch { db.todoDao().delete(todo) }
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTodo.setOnClickListener { showAddTodoDialog() }
    }

    private fun observeTodos() {
        lifecycleScope.launch {
            db.todoDao().getAllTodos().collectLatest { todos ->
                adapter.submitList(todos)
                binding.textEmpty.visibility = if (todos.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showAddTodoDialog() {
        val editText = EditText(this).apply {
            hint = "Neue Aufgabe eingeben…"
            setPadding(64, 32, 64, 16)
        }

        AlertDialog.Builder(this)
            .setTitle("Neue Aufgabe")
            .setView(editText)
            .setPositiveButton("Hinzufügen") { _, _ ->
                val title = editText.text.toString().trim()
                if (title.isNotEmpty()) {
                    lifecycleScope.launch {
                        db.todoDao().insert(Todo(title = title))
                    }
                } else {
                    Toast.makeText(this, "Bitte einen Text eingeben", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
            .also { editText.requestFocus() }
    }

    private fun ensureNotificationChannel() {
        val notificationManager = getSystemService(NotificationManager::class.java)
        if (notificationManager.getNotificationChannel(NotificationReceiver.CHANNEL_ID) == null) {
            val channel = NotificationChannel(
                NotificationReceiver.CHANNEL_ID,
                "Tägliche Erinnerungen",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Morgens (8:00), mittags (12:00) und abends (19:00)"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    NotificationScheduler.scheduleAll(this)
                }
                else -> requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationScheduler.scheduleAll(this)
        }
    }
}

package com.reminder.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
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
import com.reminder.daily.data.TodoList
import com.reminder.daily.databinding.ActivityMainBinding
import com.reminder.daily.notification.NotificationReceiver
import com.reminder.daily.notification.NotificationScheduler
import com.reminder.daily.ui.TodoListAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TodoListAdapter
    private val db by lazy { AppDatabase.getDatabase(this) }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) NotificationScheduler.scheduleAll(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        setupRecyclerView()
        setupFab()
        observeLists()
        ensureNotificationChannel()
        requestNotificationPermissionAndSchedule()
    }

    private fun setupRecyclerView() {
        adapter = TodoListAdapter(
            onOpen = { item ->
                startActivity(
                    Intent(this, TodoActivity::class.java).apply {
                        putExtra(TodoActivity.EXTRA_LIST_ID, item.todoList.id)
                        putExtra(TodoActivity.EXTRA_LIST_NAME, item.todoList.name)
                    }
                )
            },
            onDelete = { item ->
                AlertDialog.Builder(this)
                    .setTitle("Liste löschen")
                    .setMessage("\"${item.todoList.name}\" und alle Aufgaben darin löschen?")
                    .setPositiveButton("Löschen") { _, _ ->
                        lifecycleScope.launch { db.todoListDao().delete(item.todoList) }
                    }
                    .setNegativeButton("Abbrechen", null)
                    .show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTodo.setOnClickListener { showCreateListDialog() }
    }

    private fun observeLists() {
        lifecycleScope.launch {
            db.todoListDao().getAllListsWithCounts().collectLatest { lists ->
                adapter.submitList(lists)
                binding.textEmpty.visibility = if (lists.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    private fun showCreateListDialog() {
        val editText = EditText(this).apply {
            hint = "Name der Liste…"
            setPadding(64, 32, 64, 16)
        }
        AlertDialog.Builder(this)
            .setTitle("Neue Liste")
            .setView(editText)
            .setPositiveButton("Erstellen") { _, _ ->
                val name = editText.text.toString().trim()
                if (name.isNotEmpty()) {
                    lifecycleScope.launch { db.todoListDao().insert(TodoList(name = name)) }
                } else {
                    Toast.makeText(this, "Bitte einen Namen eingeben", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
            .also { editText.requestFocus() }
    }

    private fun ensureNotificationChannel() {
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(NotificationReceiver.CHANNEL_ID) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    NotificationReceiver.CHANNEL_ID,
                    "Tägliche Erinnerungen",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Morgens (8:00), mittags (12:00), abends (19:00)" }
            )
        }
    }

    private fun requestNotificationPermissionAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED
            ) {
                NotificationScheduler.scheduleAll(this)
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            NotificationScheduler.scheduleAll(this)
        }
    }
}

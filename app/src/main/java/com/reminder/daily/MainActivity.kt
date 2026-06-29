package com.reminder.daily

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.reminder.daily.data.FirestoreRepository
import com.reminder.daily.data.Prefs
import com.reminder.daily.databinding.ActivityMainBinding
import com.reminder.daily.notification.NotificationReceiver
import com.reminder.daily.notification.NotificationScheduler
import com.reminder.daily.ui.TodoAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_NEW_LIST_CODE = "newListCode"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TodoAdapter
    private lateinit var repo: FirestoreRepository
    private val auth by lazy { FirebaseAuth.getInstance() }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) NotificationScheduler.scheduleAll(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val listId = Prefs.getListId(this)
        if (listId == null || auth.currentUser == null) {
            startActivity(Intent(this, AuthActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        repo = FirestoreRepository(listId)

        // Show code dialog when a new list was just created
        intent.getStringExtra(EXTRA_NEW_LIST_CODE)?.let { showNewListCodeDialog(it) }

        setupRecyclerView()
        setupFab()
        observeTodos()
        ensureNotificationChannel()
        requestNotificationPermissionAndSchedule()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_show_code -> {
            Prefs.getListId(this)?.let { showCodeDialog(it) }
            true
        }
        R.id.action_sign_out -> {
            showSignOutDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        adapter = TodoAdapter(
            onToggle = { todo -> lifecycleScope.launch { repo.updateTodo(todo) } },
            onDelete = { todo -> lifecycleScope.launch { repo.deleteTodo(todo) } }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFab() {
        binding.fabAddTodo.setOnClickListener { showAddTodoDialog() }
    }

    private fun observeTodos() {
        lifecycleScope.launch {
            repo.getTodos().collectLatest { todos ->
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
                    val name = auth.currentUser?.displayName ?: "Unbekannt"
                    lifecycleScope.launch { repo.addTodo(title, name) }
                } else {
                    Toast.makeText(this, "Bitte einen Text eingeben", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Abbrechen", null)
            .show()
            .also { editText.requestFocus() }
    }

    private fun showNewListCodeDialog(code: String) {
        AlertDialog.Builder(this)
            .setTitle("Liste erstellt!")
            .setMessage("Dein Code: $code\n\nTeile diesen Code mit deiner Mutter, damit sie der Liste beitreten kann.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showCodeDialog(code: String) {
        AlertDialog.Builder(this)
            .setTitle("Listen-Code")
            .setMessage("Code: $code\n\nMit diesem Code können andere der Liste beitreten.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showSignOutDialog() {
        AlertDialog.Builder(this)
            .setTitle("Abmelden")
            .setMessage("Möchtest du dich wirklich abmelden? Deine Liste bleibt gespeichert.")
            .setPositiveButton("Abmelden") { _, _ ->
                auth.signOut()
                Prefs.setListId(this, "")
                startActivity(Intent(this, AuthActivity::class.java))
                finish()
            }
            .setNegativeButton("Abbrechen", null)
            .show()
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

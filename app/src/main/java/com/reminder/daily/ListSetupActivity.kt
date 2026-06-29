package com.reminder.daily

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.reminder.daily.data.FirestoreRepository
import com.reminder.daily.data.Prefs
import com.reminder.daily.databinding.ActivityListSetupBinding
import kotlinx.coroutines.launch

class ListSetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListSetupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListSetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonCreateList.setOnClickListener { createList() }
        binding.buttonJoinList.setOnClickListener { joinList() }
    }

    private fun createList() {
        setLoading(true)
        lifecycleScope.launch {
            try {
                val code = FirestoreRepository.createList()
                Prefs.setListId(this@ListSetupActivity, code)
                goToMain(showCode = code)
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@ListSetupActivity, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun joinList() {
        val code = binding.editListCode.text.toString().trim().uppercase()
        if (code.length != 6) {
            Toast.makeText(this, "Bitte einen 6-stelligen Code eingeben", Toast.LENGTH_SHORT).show()
            return
        }
        setLoading(true)
        lifecycleScope.launch {
            try {
                if (FirestoreRepository.listExists(code)) {
                    Prefs.setListId(this@ListSetupActivity, code)
                    goToMain()
                } else {
                    setLoading(false)
                    Toast.makeText(this@ListSetupActivity, "Liste nicht gefunden. Code prüfen.", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                setLoading(false)
                Toast.makeText(this@ListSetupActivity, "Fehler: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun goToMain(showCode: String? = null) {
        val intent = Intent(this, MainActivity::class.java).apply {
            showCode?.let { putExtra(MainActivity.EXTRA_NEW_LIST_CODE, it) }
        }
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.buttonCreateList.isEnabled = !loading
        binding.buttonJoinList.isEnabled = !loading
        binding.editListCode.isEnabled = !loading
    }
}

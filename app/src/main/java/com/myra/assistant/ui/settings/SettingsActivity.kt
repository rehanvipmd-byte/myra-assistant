package com.myra.assistant.ui.settings

import android.content.Context
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import com.myra.assistant.R

class SettingsActivity : AppCompatActivity() {

    private lateinit var apiKeyInput: EditText
    private lateinit var personalitySpinner: Spinner
    private lateinit var saveButton: Button
    private lateinit var backButton: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        apiKeyInput = findViewById(R.id.apiKeyInput)
        personalitySpinner = findViewById(R.id.personalitySpinner)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        // Setup spinner
        val personalities = arrayOf("Girlfriend", "Professional", "Assistant")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, personalities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        personalitySpinner.adapter = adapter

        // Load saved settings
        loadSettings()

        // Save button
        saveButton.setOnClickListener {
            saveSettings()
            finish()
        }

        // Back button
        backButton.setOnClickListener {
            finish()
        }
    }

    private fun loadSettings() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val apiKey = prefs.getString("api_key", "") ?: ""
        val personality = prefs.getString("personality_mode", "gf") ?: "gf"

        apiKeyInput.setText(apiKey)

        val personalities = arrayOf("gf", "professional", "assistant")
        val personalitiesDisplay = arrayOf("Girlfriend", "Professional", "Assistant")
        val index = personalities.indexOf(personality)
        personalitySpinner.setSelection(if (index >= 0) index else 0)
    }

    private fun saveSettings() {
        val prefs = getSharedPreferences("myra_prefs", Context.MODE_PRIVATE)
        val apiKey = apiKeyInput.text.toString().trim()
        val personalities = arrayOf("gf", "professional", "assistant")
        val personality = personalities[personalitySpinner.selectedItemPosition]

        prefs.edit().apply {
            putString("api_key", apiKey)
            putString("personality_mode", personality)
            apply()
        }
    }
}

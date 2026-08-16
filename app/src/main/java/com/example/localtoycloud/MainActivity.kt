package com.example.localtoycloud

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.ai.client.generativeai.GenerativeModel
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var menuButton: Button
    private lateinit var inputEditText: EditText
    private lateinit var generatePromptButton: Button
    private lateinit var promptOutputTextView: TextView
    private lateinit var sendToCloudButton: Button

    private val PREFS_NAME = "CyberPrefs"
    private val KEY_API_KEY = "gemini_api_key"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuButton = findViewById(R.id.menuButton)
        inputEditText = findViewById(R.id.inputEditText)
        generatePromptButton = findViewById(R.id.generatePromptButton)
        promptOutputTextView = findViewById(R.id.promptOutputTextView)
        sendToCloudButton = findViewById(R.id.sendToCloudButton)

        promptOutputTextView.text = "Void gateway online. Open the side drawer menu to configure your Gemini API key."

        menuButton.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_settings -> {
                    drawerLayout.closeDrawer(GravityCompat.START)
                    showApiKeyDialog()
                    true
                }
                else -> false
            }
        }

        generatePromptButton.setOnClickListener {
            val userInput = inputEditText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                val apiKey = getStoredApiKey()
                if (apiKey.isEmpty()) {
                    promptOutputTextView.text = "Error: API key not configured. Open side drawer to set your Gemini API key."
                } else {
                    generateCloudPrompt(userInput, apiKey)
                }
            }
        }

        sendToCloudButton.setOnClickListener {
            val optimizedPrompt = promptOutputTextView.text.toString().trim()
            if (optimizedPrompt.isNotEmpty() && !optimizedPrompt.startsWith("Void gateway") && !optimizedPrompt.startsWith("Error:")) {
                val apiKey = getStoredApiKey()
                if (apiKey.isEmpty()) {
                    promptOutputTextView.text = "Error: API key not configured. Open side drawer to set your Gemini API key."
                } else {
                    generateCloudMedia(optimizedPrompt, apiKey)
                }
            }
        }
    }

    private fun getStoredApiKey(): String {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_API_KEY, "") ?: ""
    }

    private fun saveApiKey(key: String) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_API_KEY, key).apply()
    }

    private fun showApiKeyDialog() {
        val builder = android.app.AlertDialog.Builder(this)
        builder.setTitle("Configure Gemini API Key")

        val input = EditText(this)
        input.hint = "Paste your API key here..."
        input.setText(getStoredApiKey())
        input.setPadding(48, 32, 48, 32)
        builder.setView(input)

        builder.positiveButton("Save") { _, _ ->
            val newKey = input.text.toString().trim()
            saveApiKey(newKey)
            promptOutputTextView.text = "API key securely updated in local storage."
        }
        builder.negativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun android.app.AlertDialog.Builder.positiveButton(text: String, onClick: (android.app.AlertDialog, Int) -> Unit) =
        this.setPositiveButton(text) { dialog, which -> onClick(dialog as android.app.AlertDialog, which) }

    private fun android.app.AlertDialog.Builder.negativeButton(text: String, onClick: (android.app.AlertDialog, Int) -> Unit) =
        this.setNegativeButton(text) { dialog, which -> onClick(dialog as android.app.AlertDialog, which) }

    private fun generateCloudPrompt(input: String, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Querying Gemini 1.5 Pro for prompt refinement..."
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro",
                    apiKey = apiKey
                )
                
                val response = generativeModel.generateContent("Expand this short idea into a highly detailed artistic and technical visual generation prompt: $input")
                
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = response.text ?: "Cloud response was empty."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Cloud Error: ${e.localizedMessage}"
                }
            }
        }
    }

    private fun generateCloudMedia(prompt: String, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Generating media specifications via Gemini cloud..."
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro",
                    apiKey = apiKey
                )
                
                val response = generativeModel.generateContent("Create a comprehensive media generation and rendering breakdown based on this prompt: $prompt")
                
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = response.text ?: "Media generation returned void."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Generation Error: ${e.localizedMessage}"
                }
            }
        }
    }
}

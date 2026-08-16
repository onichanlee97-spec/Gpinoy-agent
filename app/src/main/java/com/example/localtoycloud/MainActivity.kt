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
import com.google.ai.client.generativeai.type.content
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

    private val chatHistory = mutableListOf<Pair<String, String>>()

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

        promptOutputTextView.text = "GP-Noy Agent online. Ready for execution. Configure your Gemini API key in the side drawer."

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
                    inputEditText.setText("")
                    executeAgentTask(userInput, apiKey)
                }
            }
        }

        sendToCloudButton.setOnClickListener {
            val userInput = inputEditText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                val apiKey = getStoredApiKey()
                if (apiKey.isEmpty()) {
                    promptOutputTextView.text = "Error: API key not configured. Open side drawer to set your Gemini API key."
                } else {
                    inputEditText.setText("")
                    executeAgentTask(userInput, apiKey)
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

    private fun executeAgentTask(userQuery: String, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    val currentText = promptOutputTextView.text.toString()
                    promptOutputTextView.text = "$currentText\n\nUser: $userQuery\n\nGP-Noy Agent: Processing reasoning stream..."
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro",
                    apiKey = apiKey
                )

                val chat = generativeModel.startChat(
                    history = chatHistory.map { (role, text) ->
                        content(role) { text(text) }
                    }
                )

                val response = chat.sendMessage(userQuery)
                val agentResponse = response.text ?: "Agent returned empty neural payload."

                chatHistory.add("user" to userQuery)
                chatHistory.add("model" to agentResponse)

                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = buildString {
                        for ((role, text) in chatHistory) {
                            val prefix = if (role == "user") "User: " else "GP-Noy Agent: "
                            append("$prefix$text\n\n")
                        }
                    }.trimEnd()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    val currentText = promptOutputTextView.text.toString()
                    promptOutputTextView.text = "$currentText\n\nAgent Execution Error: ${e.localizedMessage}"
                }
            }
        }
    }
}

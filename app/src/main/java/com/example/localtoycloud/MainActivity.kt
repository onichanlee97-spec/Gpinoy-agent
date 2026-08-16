package com.example.localtoycloud

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
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
    private lateinit var modelSpinner: Spinner
    private lateinit var inputEditText: EditText
    private lateinit var sendAgentButton: Button
    private lateinit var promptOutputTextView: TextView

    private val PREFS_NAME = "CyberPrefs"
    private val KEY_API_KEY = "gemini_api_key"

    private val chatHistory = mutableListOf<Pair<String, String>>()

    // Gemini 3.x and modern fallback model cascade
    private val modelList = listOf(
        "gemini-3.5-pro",
        "gemini-3.5-flash",
        "gemini-3-pro",
        "gemini-1.5-pro",
        "gemini-1.5-flash"
    )

    private var selectedModel = "gemini-3.5-pro"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuButton = findViewById(R.id.menuButton)
        modelSpinner = findViewById(R.id.modelSpinner)
        inputEditText = findViewById(R.id.inputEditText)
        sendAgentButton = findViewById(R.id.sendAgentButton)
        promptOutputTextView = findViewById(R.id.promptOutputTextView)

        promptOutputTextView.text = "GP-Noy Agent online. Configure your Gemini API key in the side drawer menu."

        setupModelSpinner()

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

        sendAgentButton.setOnClickListener {
            val userInput = inputEditText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                val apiKey = getStoredApiKey()
                if (apiKey.isEmpty()) {
                    promptOutputTextView.text = "Error: API key not configured. Open side drawer to set your Gemini API key."
                } else {
                    inputEditText.setText("")
                    executeAgentTaskWithFallback(userInput, apiKey)
                }
            }
        }
    }

    private fun setupModelSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner.adapter = adapter
        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedModel = modelList[position]
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
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

    private fun executeAgentTaskWithFallback(userQuery: String, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val currentText = promptOutputTextView.text.toString()
                promptOutputTextView.text = "$currentText\n\nUser: $userQuery\n\nGP-Noy Agent: Processing neural stream with $selectedModel..."
            }

            // Determine cascade order starting with the user's selected model
            val startIndex = modelList.indexOf(selectedModel)
            val fallbackChain = if (startIndex >= 0) {
                modelList.subList(startIndex, modelList.size) + modelList.subList(0, startIndex)
            } else {
                modelList
            }

            var success = false
            var finalResponse = ""
            var lastError = ""

            for (model in fallbackChain) {
                try {
                    val generativeModel = GenerativeModel(
                        modelName = model,
                        apiKey = apiKey
                    )

                    val chat = generativeModel.startChat(
                        history = chatHistory.map { (role, text) ->
                            content(role) { text(text) }
                        }
                    )

                    val response = chat.sendMessage(userQuery)
                    finalResponse = response.text ?: "Agent returned empty neural payload."
                    success = true
                    break
                } catch (e: Exception) {
                    lastError = e.localizedMessage ?: "Unknown error"
                    // Detect quota limit or rate-limit HTTP errors (429, RESOURCE_EXHAUSTED) to trigger auto-switch
                    if (lastError.contains("429") || lastError.contains("RESOURCE_EXHAUSTED") || lastError.contains("Quota")) {
                        withContext(Dispatchers.Main) {
                            val currentText = promptOutputTextView.text.toString()
                            promptOutputTextView.text = "$currentText\n\n[Quota reached on $model. Auto-switching fallback...]"
                        }
                        continue
                    } else {
                        // For other errors, break and report
                        break
                    }
                }
            }

            if (success) {
                chatHistory.add("user" to userQuery)
                chatHistory.add("model" to finalResponse)

                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = buildString {
                        for ((role, text) in chatHistory) {
                            val prefix = if (role == "user") "User: " else "GP-Noy Agent: "
                            append("$prefix$text\n\n")
                        }
                    }.trimEnd()
                }
            } else {
                withContext(Dispatchers.Main) {
                    val currentText = promptOutputTextView.text.toString()
                    promptOutputTextView.text = "$currentText\n\nAgent Execution Error across all fallback models: $lastError"
                }
            }
        }
    }
}

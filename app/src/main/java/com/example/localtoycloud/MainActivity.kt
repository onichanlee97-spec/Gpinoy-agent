package com.example.localtoycloud

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
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
    private lateinit var chatContainer: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var promptOutputTextView: TextView

    private val PREFS_NAME = "CyberPrefs"
    private val KEY_API_KEY = "gemini_api_key"

    private val chatHistory = mutableListOf<Pair<String, String>>()

    private val modelList = listOf(
        "gemini-3.7-flash",
        "gemini-3.6-flash",
        "gemini-3.5-flash-lite",
        "gemini-3.1-pro"
    )

    private var selectedModel = "gemini-3.7-flash"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        menuButton = findViewById(R.id.menuButton)
        modelSpinner = findViewById(R.id.modelSpinner)
        inputEditText = findViewById(R.id.inputEditText)
        sendAgentButton = findViewById(R.id.sendAgentButton)
        chatContainer = findViewById(R.id.chatContainer)
        scrollView = findViewById(R.id.scrollView)
        promptOutputTextView = findViewById(R.id.promptOutputTextView)

        promptOutputTextView.text = "GP-Noy Agent online.\nConfigure your Gemini API key in the side drawer menu."

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
            if (userInput.isEmpty()) {
                return@setOnClickListener
            }

            val apiKey = getStoredApiKey()
            if (apiKey.isEmpty()) {
                addMessageBubble("Error: Gemini API key not configured. Open the side drawer to set your key.", false)
            } else {
                inputEditText.setText("")
                addMessageBubble(userInput, true)
                executeAgentTaskWithFallback(userInput, apiKey)
            }
        }
    }

    private fun setupModelSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, modelList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        modelSpinner.adapter = adapter
        modelSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (position in modelList.indices) {
                    selectedModel = modelList[position]
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedModel = modelList.first()
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

        builder.setPositiveButton("Save") { _, _ ->
            val newKey = input.text.toString().trim()
            saveApiKey(newKey)
            addMessageBubble("Gemini API key updated in local storage.", false)
        }
        builder.setNegativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun addMessageBubble(text: String, isUser: Boolean): TextView {
        val bubbleTextView = TextView(this).apply {
            this.text = text
            textSize = 15f
            setPadding(32, 24, 32, 24)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 8, 0, 16)
                if (isUser) {
                    gravity = android.view.Gravity.END
                } else {
                    gravity = android.view.Gravity.START
                }
            }
            maxWidth = (resources.displayMetrics.widthPixels * 0.8f).toInt()
            if (isUser) {
                setBackgroundResource(R.drawable.user_bubble)
                setTextColor(resources.getColor(android.R.color.white, theme))
                setShadowLayer(8f, 0f, 0f, resources.getColor(android.R.color.holo_blue_light, theme))
            } else {
                setBackgroundResource(R.drawable.agent_bubble)
                setTextColor(resources.getColor(android.R.color.white, theme))
                setShadowLayer(8f, 0f, 0f, resources.getColor(android.R.color.holo_purple, theme))
            }
        }

        chatContainer.addView(bubbleTextView)
        
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }

        return bubbleTextView
    }

    private fun executeAgentTaskWithFallback(userQuery: String, apiKey: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Display transient thinking status message
            val thinkingBubble = withContext(Dispatchers.Main) {
                addMessageBubble("GP-Noy Agent is thinking...", false)
            }

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
                    finalResponse = response.text ?: "Agent returned an empty response."
                    success = true
                    selectedModel = model
                    break
                } catch (e: Exception) {
                    lastError = e.localizedMessage ?: e.message ?: "Unknown error"
                    continue
                }
            }

            withContext(Dispatchers.Main) {
                // Remove transient thinking bubble
                chatContainer.removeView(thinkingBubble)

                if (success) {
                    chatHistory.add("user" to userQuery)
                    chatHistory.add("model" to finalResponse)
                    addMessageBubble(finalResponse, false)
                } else {
                    addMessageBubble("Agent Execution Error across all fallback models: $lastError", false)
                }
            }
        }
    }
}

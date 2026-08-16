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

    /*
     * Current Gemini model configuration.
     *
     * Newest/high-speed model first.
     * The app will automatically try the next model if
     * the selected model fails.
     */
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
        promptOutputTextView = findViewById(R.id.promptOutputTextView)

        promptOutputTextView.text =
            "GP-Noy Agent online.\nConfigure your Gemini API key in the side drawer menu."

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

            val userInput = inputEditText.text
                .toString()
                .trim()

            if (userInput.isEmpty()) {
                return@setOnClickListener
            }

            val apiKey = getStoredApiKey()

            if (apiKey.isEmpty()) {

                promptOutputTextView.text =
                    "Error: Gemini API key not configured.\n\n" +
                    "Open the side drawer and configure your Gemini API key."

            } else {

                inputEditText.setText("")

                executeAgentTaskWithFallback(
                    userInput,
                    apiKey
                )
            }
        }
    }

    /**
     * Configure the model-selection spinner.
     */
    private fun setupModelSpinner() {

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            modelList
        )

        adapter.setDropDownViewResource(
            android.R.layout.simple_spinner_dropdown_item
        )

        modelSpinner.adapter = adapter

        modelSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {

                    if (position in modelList.indices) {
                        selectedModel = modelList[position]
                    }
                }

                override fun onNothingSelected(
                    parent: AdapterView<*>
                ) {
                    selectedModel = modelList.first()
                }
            }
    }

    /**
     * Retrieve the locally stored API key.
     */
    private fun getStoredApiKey(): String {

        val prefs = getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        return prefs.getString(
            KEY_API_KEY,
            ""
        ) ?: ""
    }

    /**
     * Save the Gemini API key locally.
     */
    private fun saveApiKey(key: String) {

        val prefs = getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putString(KEY_API_KEY, key)
            .apply()
    }

    /**
     * Show the Gemini API key configuration dialog.
     */
    private fun showApiKeyDialog() {

        val builder =
            android.app.AlertDialog.Builder(this)

        builder.setTitle(
            "Configure Gemini API Key"
        )

        val input = EditText(this)

        input.hint = "Paste your API key here..."

        input.setText(
            getStoredApiKey()
        )

        input.setPadding(
            48,
            32,
            48,
            32
        )

        builder.setView(input)

        builder.positiveButton("Save") { _, _ ->

            val newKey =
                input.text
                    .toString()
                    .trim()

            saveApiKey(newKey)

            promptOutputTextView.text =
                "Gemini API key updated in local storage."
        }

        builder.negativeButton("Cancel") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    /**
     * Execute the user's request.
     *
     * The selected model is attempted first.
     * If it fails, the remaining models are attempted.
     */
    private fun executeAgentTaskWithFallback(
        userQuery: String,
        apiKey: String
    ) {

        CoroutineScope(Dispatchers.IO).launch {

            withContext(Dispatchers.Main) {

                val currentText =
                    promptOutputTextView.text.toString()

                promptOutputTextView.text =
                    "$currentText\n\n" +
                    "User: $userQuery\n\n" +
                    "GP-Noy Agent: Processing with $selectedModel..."
            }

            /*
             * Build fallback order.
             *
             * Example:
             *
             * Selected:
             * gemini-3.6-flash
             *
             * Order:
             * gemini-3.6-flash
             * gemini-3.5-flash-lite
             * gemini-3.1-pro
             * gemini-3.7-flash
             */
            val startIndex =
                modelList.indexOf(selectedModel)

            val fallbackChain =
                if (startIndex >= 0) {

                    modelList.subList(
                        startIndex,
                        modelList.size
                    ) + modelList.subList(
                        0,
                        startIndex
                    )

                } else {

                    modelList
                }

            var success = false
            var finalResponse = ""
            var lastError = ""

            /*
             * Try each model.
             */
            for (model in fallbackChain) {

                try {

                    withContext(Dispatchers.Main) {

                        val currentText =
                            promptOutputTextView.text.toString()

                        promptOutputTextView.text =
                            "$currentText\n\n" +
                            "[Trying $model...]"
                    }

                    val generativeModel =
                        GenerativeModel(
                            modelName = model,
                            apiKey = apiKey
                        )

                    /*
                     * Restore the previous conversation.
                     */
                    val chat =
                        generativeModel.startChat(
                            history = chatHistory.map { (role, text) ->

                                content(role) {
                                    text(text)
                                }
                            }
                        )

                    /*
                     * Send the current user request.
                     */
                    val response =
                        chat.sendMessage(userQuery)

                    finalResponse =
                        response.text
                            ?: "Agent returned an empty response."

                    success = true

                    /*
                     * Remember which model actually succeeded.
                     */
                    selectedModel = model

                    break

                } catch (e: Exception) {

                    lastError =
                        e.localizedMessage
                            ?: e.message
                            ?: "Unknown Gemini API error"

                    withContext(Dispatchers.Main) {

                        val currentText =
                            promptOutputTextView.text.toString()

                        promptOutputTextView.text =
                            "$currentText\n\n" +
                            "[Model $model failed: $lastError]\n" +
                            "[Trying next available model...]"
                    }

                    /*
                     * Continue to the next model.
                     *
                     * This is intentionally not restricted only
                     * to quota errors. If a model is unavailable,
                     * deprecated, unsupported, or returns another
                     * API error, the next configured model gets a chance.
                     */
                    continue
                }
            }

            /*
             * Successful response.
             */
            if (success) {

                chatHistory.add(
                    "user" to userQuery
                )

                chatHistory.add(
                    "model" to finalResponse
                )

                withContext(Dispatchers.Main) {

                    promptOutputTextView.text =
                        buildString {

                            append(
                                "GP-Noy Agent • Model: $selectedModel\n\n"
                            )

                            for ((role, text) in chatHistory) {

                                val prefix =
                                    if (role == "user") {
                                        "User: "
                                    } else {
                                        "GP-Noy Agent: "
                                    }

                                append(prefix)
                                append(text)
                                append("\n\n")
                            }
                        }.trimEnd()
                }

            } else {

                /*
                 * Every configured model failed.
                 */
                withContext(Dispatchers.Main) {

                    val currentText =
                        promptOutputTextView.text.toString()

                    promptOutputTextView.text =
                        "$currentText\n\n" +
                        "Agent Execution Error.\n\n" +
                        "All configured Gemini models failed.\n\n" +
                        "Last error:\n$lastError"
                }
            }
        }
    }

    /**
     * Convenience extension for AlertDialog buttons.
     */
    private fun android.app.AlertDialog.Builder.positiveButton(
        text: String,
        onClick: (
            android.app.AlertDialog,
            Int
        ) -> Unit
    ) =
        this.setPositiveButton(text) { dialog, which ->

            onClick(
                dialog as android.app.AlertDialog,
                which
            )
        }

    /**
     * Convenience extension for AlertDialog buttons.
     */
    private fun android.app.AlertDialog.Builder.negativeButton(
        text: String,
        onClick: (
            android.app.AlertDialog,
            Int
        ) -> Unit
    ) =
        this.setNegativeButton(text) { dialog, which ->

            onClick(
                dialog as android.app.AlertDialog,
                which
            )
        }
}

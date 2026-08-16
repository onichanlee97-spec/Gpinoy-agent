package com.example.localtoycloud

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var generatePromptButton: Button
    private lateinit var promptOutputTextView: TextView
    private lateinit var sendToCloudButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.inputEditText)
        generatePromptButton = findViewById(R.id.generatePromptButton)
        promptOutputTextView = findViewById(R.id.promptOutputTextView)
        sendToCloudButton = findViewById(R.id.sendToCloudButton)

        generatePromptButton.setOnClickListener {
            val userInput = inputEditText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                generateLocalPrompt(userInput)
            }
        }

        sendToCloudButton.setOnClickListener {
            val optimizedPrompt = promptOutputTextView.text.toString().trim()
            if (optimizedPrompt.isNotEmpty() && optimizedPrompt != "Local prompt output will appear here...") {
                sendToGoogleCloud(optimizedPrompt)
            }
        }
    }

    private fun generateLocalPrompt(input: String) {
        CoroutineScope(Dispatchers.IO).launch {
            // Simulated local offline LLM expansion step
            // For true offline on-device inference, integrate MediaPipe LLM Inference here
            val expandedPrompt = "High resolution cinematic photography, detailed textures, dramatic studio lighting, professional color grading, $input"

            withContext(Dispatchers.Main) {
                promptOutputTextView.text = expandedPrompt
            }
        }
    }

    private fun sendToGoogleCloud(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Sending prompt to Google Gemini Cloud API..."
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro",
                    apiKey = "YOUR_GEMINI_API_KEY"
                )
                
                val response = generativeModel.generateContent("Create a comprehensive media generation specification based on this prompt: $prompt")
                
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = response.text ?: "Cloud generation returned an empty response."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Cloud Execution Error: ${e.localizedMessage}"
                }
            }
        }
    }
}

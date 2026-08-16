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

        promptOutputTextView.text = "Void gateway online. Ready for Gemini cloud generation."

        generatePromptButton.setOnClickListener {
            val userInput = inputEditText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                generateCloudPrompt(userInput)
            }
        }

        sendToCloudButton.setOnClickListener {
            val optimizedPrompt = promptOutputTextView.text.toString().trim()
            if (optimizedPrompt.isNotEmpty() && !optimizedPrompt.startsWith("Void gateway")) {
                generateCloudMedia(optimizedPrompt)
            }
        }
    }

    private fun generateCloudPrompt(input: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Querying Gemini 1.5 Pro for prompt refinement..."
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro",
                    apiKey = "YOUR_GEMINI_API_KEY"
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

    private fun generateCloudMedia(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Generating media specifications via Gemini cloud..."
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro",
                    apiKey = "AQ.Ab8RN6L6HtLJZy36Y2NkZjoykNde5rePXffIEy1BtFRVKJ8Odw"
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

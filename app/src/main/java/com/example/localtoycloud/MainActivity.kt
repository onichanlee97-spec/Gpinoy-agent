package com.example.localtoycloud

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.ai.client.generativeai.GenerativeModel
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var inputEditText: EditText
    private lateinit var generatePromptButton: Button
    private lateinit var promptOutputTextView: TextView
    private lateinit var sendToCloudButton: Button

    private var llmInference: LlmInference? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        inputEditText = findViewById(R.id.inputEditText)
        generatePromptButton = findViewById(R.id.generatePromptButton)
        promptOutputTextView = findViewById(R.id.promptOutputTextView)
        sendToCloudButton = findViewById(R.id.sendToCloudButton)

        // Safe async initialization wrapped to prevent startup crashes
        initializeLocalModelSafe()

        generatePromptButton.setOnClickListener {
            val userInput = inputEditText.text.toString().trim()
            if (userInput.isNotEmpty()) {
                generateLocalPrompt(userInput)
            }
        }

        sendToCloudButton.setOnClickListener {
            val optimizedPrompt = promptOutputTextView.text.toString().trim()
            if (optimizedPrompt.isNotEmpty() && !optimizedPrompt.startsWith("Void space") && !optimizedPrompt.startsWith("Neural core")) {
                sendToGoogleCloud(optimizedPrompt)
            }
        }
    }

    private fun initializeLocalModelSafe() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val modelFile = File(filesDir, "model.bin")
                
                // Check if asset exists before attempting to copy
                val assetList = assets.list("")
                val hasAssetModel = assetList?.contains("model.bin") == true

                if (hasAssetModel && !modelFile.exists()) {
                    assets.open("model.bin").use { inputStream ->
                        modelFile.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }
                }

                if (modelFile.exists()) {
                    val options = LlmInference.LlmInferenceOptions.builder()
                        .setModelPath(modelFile.absolutePath)
                        .setMaxTokens(512)
                        .setTemperature(0.7f)
                        .build()

                    llmInference = LlmInference.createFromOptions(applicationContext, options)

                    withContext(Dispatchers.Main) {
                        promptOutputTextView.text = "Neural core online. Local MediaPipe LLM active."
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        promptOutputTextView.text = "Void space initialized. Ready for prompt expansion."
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Void space initialized. Ready for prompt expansion."
                }
            }
        }
    }

    private fun generateLocalPrompt(input: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Synthesizing prompt through local neural matrix..."
                }

                val query = "Expand this short idea into a highly detailed artistic and technical visual generation prompt: $input"
                
                val result = llmInference?.generateResponse(query) 
                    ?: "Cybernetic enhanced spatial rendering, ultra glossy reflections, 8k resolution, $input"

                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = result
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Cyber-matrix fallback: High resolution cinematic render, $input"
                }
            }
        }
    }

    private fun sendToGoogleCloud(prompt: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Transmitting prompt across cloud dimensional gateway to Gemini..."
                }

                val generativeModel = GenerativeModel(
                    modelName = "gemini-1.5-pro",
                    apiKey = "AQ.Ab8RN6L6HtLJZy36Y2NkZjoykNde5rePXffIEy1BtFRVKJ8Odw"
                )
                
                val response = generativeModel.generateContent("Create a comprehensive media generation specification based on this prompt: $prompt")
                
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = response.text ?: "Cloud gateway returned void response."
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    promptOutputTextView.text = "Cloud Gateway Error: ${e.localizedMessage}"
                }
            }
        }
    }
}

# LocalToCloud AI - Cybernetic Edition

An advanced hybrid Android application combining on-device offline prompt engineering via MediaPipe LLM with cloud-based generative execution through Google Gemini, featuring a futuristic cyber-metal glossy interface and gyroscope-driven dimensional parallax.

## Features

- **Void Space Interface:** Designed with a deep space gradient background and floating cyber-metal chat and input bubbles with glowing cyan and magenta neon borders.
- **Gyroscope Dimensional Tilt:** Utilizes device rotation sensors to dynamically shift layout translation, creating a 3D parallax depth effect as if content floats inside dimensional space.
- **Dual AI Pipeline:** 
  1. *Local Offline Processing:* Runs quantized Large Language Models directly on device hardware using MediaPipe GenAI.
  2. *Cloud Gateway:* Forwards optimized prompts securely to Google Gemini APIs over HTTPS.

## Repository Setup and Building

1. Ensure all files (`MainActivity.kt`, `GyroscopeView.kt`, layout XMLs, and drawables) are placed in their respective module directories.
2. Insert your active Google Gemini API key into `MainActivity.kt`.
3. Push changes to your GitHub repository to trigger the automated GitHub Actions build workflow.
4. Download the compiled debug APK from the GitHub Actions artifacts tab and install it onto your Android device.

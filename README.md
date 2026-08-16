# LocalToCloud AI Android Application

This project is a hybrid Android application designed to combine on-device text processing with cloud-based generative AI workflows. It serves as a proof-of-concept for offline prompt engineering coupled with remote cloud execution.

## Architecture and Pipeline

The application operates through a sequential two-step pipeline:

1. **Local Prompt Expansion:** The user inputs a simple descriptive concept into the user interface. An on-device background task processes the text to refine and expand it into a detailed generative prompt. In production builds, this step leverages local inference runtimes like MediaPipe or ONNX Runtime executing quantized models offline.
2. **Cloud Generation Request:** Once the prompt is structured, pressing the cloud execution button triggers an asynchronous network call via the Google Generative AI SDK, transmitting the prompt securely over HTTPS to Google Gemini cloud models for advanced media reasoning.

## Project Structure

- `app/src/main/java/com/example/localtoycloud/MainActivity.kt`: Core activity managing UI interactions, asynchronous coroutine scopes, and API communication.
- `app/src/main/res/layout/activity_main.xml`: ConstraintLayout UI definition containing input fields, display containers, and trigger buttons.
- `app/build.gradle.kts` and root `build.gradle.kts`: Gradle build scripts configuring dependencies, compilation SDKs, and plugin versions.
- `.github/workflows/`: Automated CI/CD pipelines configured via GitHub Actions to compile debug and release APKs upon code pushes and version tags.

## Setup and Building

1. Clone the repository into your local environment or open it directly in Android Studio.
2. Insert your valid Google Gemini API key into the `MainActivity.kt` file where indicated by the placeholder string.
3. Push changes to your GitHub repository to trigger the automated GitHub Actions workflow, which will compile the APK artifact automatically.
4. Download the compiled `app-debug.apk` from the GitHub Actions artifacts tab and install it onto your Android device.

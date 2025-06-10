package com.harshithh.llmapp

import android.content.Context
import android.util.Log
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class LLMInferenceManager(private val context: Context) {

    private var llmInference: LlmInference? = null
    private val _isModelLoaded = MutableStateFlow(false)
    val isModelLoaded = _isModelLoaded.asStateFlow()

    private val modelFileName = "gemma-1b-it-int4.task"

    fun loadModel(): Boolean {
        if (llmInference != null) {
            Log.d("LLMInferenceManager", "Model already loaded.")
            _isModelLoaded.value = true
            return true
        }

        try {
            val modelFile = File(context.cacheDir, modelFileName)
            if (!modelFile.exists()) {
                context.assets.open(modelFileName).use { input ->
                    FileOutputStream(modelFile).use { output ->
                        input.copyTo(output)
                    }
                }
                Log.d("LLMInferenceManager", "Copied model to: ${modelFile.absolutePath}")
            }

            val options = LlmInferenceOptions.builder()
                .setModelPath(modelFile.absolutePath)      // ✅ Use setModelPath here
                // .setLoraPath(...)                       // Optional: LoRA model
                .build()

            llmInference = LlmInference.createFromOptions(context, options)
            _isModelLoaded.value = true
            Log.d("LLMInferenceManager", "Model loaded successfully!")
            return true

        } catch (e: IOException) {
            Log.e("LLMInferenceManager", "Model copy failed", e)
        } catch (e: Exception) {
            Log.e("LLMInferenceManager", "Model load failed", e)
        }

        _isModelLoaded.value = false
        return false
    }

    fun generateResponse(prompt: String): Flow<String> = flow {
        if (llmInference == null || !_isModelLoaded.value) {
            emit("Model not loaded. Please wait or retry.")
            return@flow
        }

        try {
            Log.d("LLMInferenceManager", "Prompt: $prompt")
            val result = llmInference?.generateResponse(prompt)
            emit(result ?: "No response generated.")
        } catch (e: Exception) {
            Log.e("LLMInferenceManager", "Inference error", e)
            emit("Error during inference: ${e.message}")
        }
    }

    fun close() {
        llmInference?.close()
        llmInference = null
        _isModelLoaded.value = false
        Log.d("LLMInferenceManager", "LLM inference engine closed.")
    }
}

package com.example.myapplication

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * Data structure representing the full cognitive response from the AI.
 * Matches the <output> schema defined in your prompt.
 */
@Serializable
data class AgentDecision(
    val thinking: String,
    val evaluationPreviousGoal: String,
    val memory: String,
    val nextGoal: String,
    val action: JsonArray // List of actions to execute sequentially
)

class LLMClient {

    private val apiKey = "" // Replace with your actual key
    private val modelName = "gemini-2.5-pro" // Or gemini-1.5-pro for better reasoning

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * Sends the full ReAct prompt and optional screenshot to the LLM.
     * Returns a structured AgentDecision object.
     */
    fun getAgentDecision(fullPrompt: String, screenshot: Bitmap?): AgentDecision? {
        Log.d("Executor", "Sending Prompt: $fullPrompt")
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        // 1. Construct the Request Body
        val requestJson = JSONObject().apply {
            val contents = JSONArray()
            val content = JSONObject()
            val parts = JSONArray()

            // Add the Text Prompt
            parts.put(JSONObject().put("text", fullPrompt))

            // Add the Screenshot if available
            screenshot?.let {
                val imagePart = JSONObject().apply {
                    val inlineData = JSONObject().apply {
                        put("mimeType", "image/jpeg")
                        put("data", encodeBitmapToBase64(it))
                    }
                    put("inlineData", inlineData)
                }
                parts.put(imagePart)
            }

            content.put("parts", parts)
            contents.put(content)
            put("contents", contents)

            // Configuration for JSON output
            val generationConfig = JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.2) // Lower temperature for stricter JSON adherence
                put("max_output_tokens", 2048)
            }
            put("generationConfig", generationConfig)
        }

        val request = Request.Builder()
            .url(url)
            .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
            .build()

        return try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: return null

            if (!response.isSuccessful) {
                Log.e("LLMClient", "API Error: ${response.code} - $body")
                return null
            }

            // 2. Extract JSON from the Gemini response wrapper
            val jsonResponse = JSONObject(body)
            val candidates = jsonResponse.getJSONArray("candidates")
            val textOutput = candidates.getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")

            // 3. Surgical JSON Extraction (Removes markdown if the AI hallucinated it)
            val startIndex = textOutput.indexOf('{')
            val endIndex = textOutput.lastIndexOf('}')

            if (startIndex != -1 && endIndex != -1) {
                val cleanJson = textOutput.substring(startIndex, endIndex + 1)
                Log.d("LLMClient", "Parsed JSON: $cleanJson")

                // Decode into our data class
                json.decodeFromString<AgentDecision>(cleanJson)
            } else {
                Log.e("LLMClient", "No JSON found in AI output: $textOutput")
                null
            }

        } catch (e: Exception) {
            Log.e("LLMClient", "Decision Request Failed", e)
            null
        }
    }

    /**
     * Converts Bitmap to Base64 JPEG string for API transmission.
     */
    private fun encodeBitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Compression at 40-50% is standard for multimodal LLMs (balance quality/speed)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.NO_WRAP)
    }
}

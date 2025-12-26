package com.example.myapplication

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.channels.Channel
import okhttp3.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

class WebSocketLLMClient(private val actionChannel: Channel<AgentCommand>) : WebSocketListener() {

    // 1. API KEY & URL
    private val apiKey = "" // <-- VERIFY THIS IS CORRECT
    private val wsUrl = "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1alpha.GenerativeService/BidiGenerateContent?key=$apiKey"

    private var webSocket: WebSocket? = null
    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // Crucial: Disable timeout for sockets
        .build()

    fun connect() {
        if (webSocket != null) return
        val request = Request.Builder().url(wsUrl).build()
        webSocket = client.newWebSocket(request, this)
        Log.d("AgentSocket", "Attempting connection...")
    }

    fun disconnect() {
        webSocket?.close(1000, "User Stopped")
        webSocket = null
    }

    // --- CRITICAL: THE HANDSHAKE ---
    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.d("AgentSocket", "Connection OPEN! Sending Setup...")

        // 2. SEND SETUP MESSAGE (Required for Bidi)
        // If you skip this, the server will ignore all your future messages.
        val msg = JSONObject()
        val setup = JSONObject()

        // Use the experimental model for Bidi
        setup.put("model", "models/gemini-2.0-flash-exp")

        val generationConfig = JSONObject()
        generationConfig.put("response_mime_type", "application/json")
        setup.put("generation_config", generationConfig)

        msg.put("setup", setup)

        webSocket.send(msg.toString())
        Log.d("AgentSocket", "Setup Sent: $msg")
    }

    // --- SEND DATA ---
    fun sendScreenContext(goal: String, screenText: String, screenshot: Bitmap?) {
        if (webSocket == null) {
            Log.e("AgentSocket", "Socket is null. Cannot send.")
            return
        }

        val msg = JSONObject()
        val clientContent = JSONObject()
        val turns = JSONArray()
        val turn = JSONObject()
        val parts = JSONArray()

        // Construct Prompt
        val textPart = JSONObject()
        textPart.put("text", "GOAL: $goal\nSCREEN: $screenText\nReturn JSON command.")
        parts.put(textPart)

        // Add Image
        if (screenshot != null) {
            val imagePart = JSONObject()
            val inlineData = JSONObject()
            inlineData.put("mime_type", "image/jpeg")
            inlineData.put("data", bitmapToBase64(screenshot))
            imagePart.put("inline_data", inlineData) // Note: snake_case for Bidi API
            parts.put(imagePart)
        }

        turn.put("parts", parts)
        turn.put("role", "user")
        turns.put(turn)

        clientContent.put("turns", turns)
        clientContent.put("turn_complete", true) // Tells AI "I am done talking, your turn"

        msg.put("client_content", clientContent)

        // Log.d("AgentSocket", "Sending Frame...")
        webSocket?.send(msg.toString())
    }

    // --- RECEIVE DATA ---
    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.d("AgentSocket", "RECEIVED RAW: $text") // <--- LOOK AT THIS LOG

        try {
            val json = JSONObject(text)

            // Check for Server Content (The actual reply)
            val serverContent = json.optJSONObject("server_content")
            if (serverContent != null) {
                val modelTurn = serverContent.optJSONObject("model_turn")
                val parts = modelTurn?.optJSONArray("parts")
                val responseText = parts?.getJSONObject(0)?.optString("text")

                if (!responseText.isNullOrEmpty()) {
                    Log.d("AgentSocket", "AI SAYS: $responseText")
                    parseAndSendAction(responseText)
                }
            }

            // Check for Setup Complete
            // Note: Currently setupComplete might be empty, but it confirms connection.
            if (json.has("setupComplete")) {
                Log.d("AgentSocket", "Handshake Confirmed by Server.")
            }

        } catch (e: Exception) {
            Log.e("AgentSocket", "Parse Error: ${e.message}")
        }
    }

    private fun parseAndSendAction(jsonString: String) {
        try {
            val clean = jsonString.replace("```json", "").replace("```", "").trim()
            val obj = JSONObject(clean)
            val cmd = AgentCommand(
                obj.optString("action", "wait"),
                obj.optInt("elementId", -1),
                obj.optString("text")
            )
            actionChannel.trySend(cmd)
        } catch (e: Exception) {
            Log.e("AgentSocket", "JSON Parse Failed: $jsonString")
        }
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        Log.w("AgentSocket", "Closing: $code / $reason")
        webSocket.close(1000, null)
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("AgentSocket", "FAILURE: ${t.message}")
        if (response != null) {
            Log.e("AgentSocket", "Response Code: ${response.code} Message: ${response.message}")
        }
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val output = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, output)
        return Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }
}

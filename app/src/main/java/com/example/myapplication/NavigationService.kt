package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class NavigationService : AccessibilityService() {

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val executor by lazy { ActionExecutor(this) }
//    private val llmClient = LLMClient()
    private val actionHistory = mutableListOf<String>()

    companion object {
        var currentGoal: String? = null
        var isRunning = false
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        scope.launch {
            while (isActive) {
                if (currentGoal != null && !isRunning) {
                    isRunning = true
                    startLoop(currentGoal!!)
                }
                delay(1000)
            }
        }
    }

    private suspend fun startLoop(goal: String) {
        val prefs = PreferenceManager(this)
        val apiKey = prefs.getApiKey()

        if (apiKey.isNullOrEmpty()) {
            Log.e("NavigationService", "API Key missing. Please set it in the app.")
            // Optional: You could dispatch a Toast on the main thread here to warn the user
            isRunning = false
            currentGoal = null
            return
        }
        val llmClient = LLMClient(apiKey)
        var todo = "- [ ] Start task"
        var results = ""
        val history = mutableListOf<String>()
        var step = 1

        // Define the JSON parser
        val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true; isLenient = true }

        while (step < 105 && isRunning) {
            // 1. Get Screen Dimensions
            val metrics = resources.displayMetrics
            val width = metrics.widthPixels
            val height = metrics.heightPixels

            // 2. Capture Screen Text
            val screenText = withContext(Dispatchers.Main) {
                val root = rootInActiveWindow
                if (root != null) {
                    ScreenParser.parse(root, width, height)
                } else {
                    "Error: No active window"
                }
            }

            // 3. Capture Screenshot
            val screenshot: Bitmap? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                takeScreenshotSuspend()
            } else {
                null
            }

            // 4. Build Prompt and Get Decision
            val fullPrompt = PromptManager.buildPrompt(goal, screenText, history, todo, results, step)
            val decision = llmClient.getAgentDecision(fullPrompt, screenshot)

            screenshot?.recycle() // Clean up memory

            if (decision == null) {
                Log.e("Agent", "AI Decision was null. Retrying...")
                delay(2000)
                continue
            }

            // 5. Log Thinking
            Log.d("AgentThinking", decision.thinking)

            // 6. Execute Actions Sequentially
            decision.action.forEach { actionJson ->
                try {
                    val action = json.decodeFromJsonElement(Action.ActionSerializer, actionJson)

                    when (action) {
                        is Action.WriteFile -> {
                            if (action.fileName == "todo.md") todo = action.text
                            if (action.fileName == "results.md") results = action.text
                        }
                        is Action.AppendFile -> {
                            if (action.fileName == "results.md") results += "\n${action.text}"
                        }
                        is Action.Done -> {
                            // 1. Execute the "Done" logic (e.g., Show Toast / Speak)
                            executor.execute(action)

                            // 2. Log it
                            Log.i("Agent", "Agent decided task is Done. Success: ${action.success}")
                            history.add("Step $step: COMPLETED. ${action.text}")

                            // 3. KILL THE LOOP
                            isRunning = false
                            currentGoal = null
                            return  // Exit the startLoop function immediately
                        }
                        else -> {
                            val result = executor.execute(action)
                            history.add("Step $step: Tried ${action::class.simpleName}. Success: $result")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("Executor", "Failed to parse or execute action: ${e.message}")
                }
            }

            step++
            delay(2000)
        }

        isRunning = false
        currentGoal = null
    }

    // --- SCREENSHOT UTILITY ---
    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun takeScreenshotSuspend(): Bitmap? = suspendCancellableCoroutine { continuation ->
        val executor = androidx.core.content.ContextCompat.getMainExecutor(this)

        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            executor,
            object : TakeScreenshotCallback {
                override fun onSuccess(result: ScreenshotResult) {
                    val hardwareBuffer = result.hardwareBuffer
                    val colorSpace = result.colorSpace
                    try {
                        val bitmap = Bitmap.wrapHardwareBuffer(hardwareBuffer, colorSpace)
                        val softwareBitmap = bitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBuffer.close()
                        continuation.resume(softwareBitmap)
                    } catch (e: Exception) {
                        hardwareBuffer.close()
                        continuation.resume(null)
                    }
                }
                override fun onFailure(errorCode: Int) {
                    continuation.resume(null)
                }
            }
        )
    }

    // --- REQUIRED OVERRIDES (Fixed Signatures) ---

    // FIX: Removed nullable '?' from AccessibilityEvent
    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        // We don't use real-time events, we poll the screen in the loop.
    }

    override fun onInterrupt() {
        Log.e("NavigationService", "Service Interrupted")
        job.cancel()
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }
}
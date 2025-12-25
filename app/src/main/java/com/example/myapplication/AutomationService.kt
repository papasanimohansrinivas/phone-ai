package com.example.zerotapclone

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class AutomationService : AccessibilityService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // --- YOUR API KEY ---
    private val generativeModel = GenerativeModel(
        modelName = "gemini-3-pro-preview",
        apiKey = "AIzaSyC9NG_1lZWByAGM1P-XkYQ_NBiCtPkK59o\n" // <--- PASTE KEY HERE
    )

    private val commandReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val prompt = intent?.getStringExtra("USER_PROMPT") ?: return
            logToUI("Processing: $prompt")
            processCommand(prompt)
        }
    }

    override fun onServiceConnected() {
        logToUI("Service Connected")
        val filter = IntentFilter("com.example.zerotap.ACTION_AI_COMMAND")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(commandReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(commandReceiver, filter)
        }
    }

    override fun onDestroy() {
        unregisterReceiver(commandReceiver)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    private fun processCommand(userPrompt: String) {
        // 1. Get Screen Content
        val screenXml = dumpScreenToXml()

        // 2. Get List of Installed Apps (so AI knows what it can open)
        val appList = getInstalledAppsList()

        serviceScope.launch {
            try {
                // 3. Construct the Brain
                val systemPrompt = """
                    You are an Android automation agent.
                    
                    USER GOAL: "$userPrompt"
                    
                    AVAILABLE TOOLS:
                    1. LAUNCH_APP: Use this if the user wants to open an app that is NOT on the screen.
                       Available Apps: $appList
                       
                    2. CLICK: Use this if the element is visible in the SCREEN XML below.
                    
                    3. HOME / BACK: Global navigation.
                    
                    RESPONSE FORMAT (JSON ONLY):
                    { "action": "LAUNCH", "package": "com.package.name" }
                    OR
                    { "action": "CLICK", "viewId": "id", "text": "text" }
                    OR
                    { "action": "HOME" }
                """.trimIndent()

                val chat = generativeModel.startChat()
                val response = chat.sendMessage(
                    content {
                        text(systemPrompt)
                        text("CURRENT SCREEN XML:\n$screenXml")
                    }
                )

                val rawText = response.text ?: ""
                val jsonString = rawText.replace("```json", "").replace("```", "").trim()

                logToUI("AI Decision: $jsonString")
                executeAction(jsonString)

            } catch (e: Exception) {
                logToUI("Error: ${e.message}")
            }
        }
    }

    private fun executeAction(jsonString: String) {
        try {
            val json = JSONObject(jsonString)
            when (json.optString("action")) {
                "HOME" -> performGlobalAction(GLOBAL_ACTION_HOME)
                "BACK" -> performGlobalAction(GLOBAL_ACTION_BACK)

                "LAUNCH" -> {
                    val pkg = json.optString("package")
                    logToUI("Launching: $pkg")
                    launchApp(pkg)
                }

                "CLICK" -> {
                    val targetId = json.optString("viewId")
                    val targetText = json.optString("text")

                    val root = rootInActiveWindow ?: return

                    // Try ID Match
                    if (targetId.isNotEmpty()) {
                        val nodes = root.findAccessibilityNodeInfosByViewId(targetId)
                        if (nodes.isNotEmpty()) {
                            nodes[0].performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return
                        }
                    }
                    // Try Text Match
                    if (targetText.isNotEmpty()) {
                        val nodes = root.findAccessibilityNodeInfosByText(targetText)
                        for (node in nodes) {
                            if (node.isClickable) {
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                return
                            }
                            node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logToUI("Exec Failed: ${e.message}")
        }
    }

    // --- HELPER: Get Installed Apps ---
    private fun getInstalledAppsList(): String {
        val pm = packageManager
        // Get list of installed apps
        val packages = pm.getInstalledPackages(PackageManager.GET_META_DATA)

        val sb = StringBuilder()
        var count = 0
        for (pack in packages) {
            // Only list user apps (skip system apps to save tokens, usually)
            // Or just list all if you want system apps too.
            // Here we check if it has a launch intent (means it's an app you can open)
            if (pm.getLaunchIntentForPackage(pack.packageName) != null) {
                val label = pack.applicationInfo.loadLabel(pm).toString()
                sb.append("[$label : ${pack.packageName}], ")
                count++
                if (count > 200) break // Limit to prevent hitting token limits
            }
        }
        return sb.toString()
    }

    // --- HELPER: Launch App ---
    private fun launchApp(packageName: String) {
        try {
            val intent = packageManager.getLaunchIntentForPackage(packageName)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                logToUI("App not found: $packageName")
            }
        } catch (e: Exception) {
            logToUI("Launch Error: ${e.message}")
        }
    }

    // --- EXISTING XML DUMP CODE ---
    private fun dumpScreenToXml(): String {
        val root = rootInActiveWindow ?: return "<screen>Empty</screen>"
        val sb = StringBuilder("<screen>\n")
        serialize(root, sb, 0)
        sb.append("</screen>")
        return sb.toString()
    }

    private fun serialize(node: AccessibilityNodeInfo, sb: StringBuilder, depth: Int) {
        if (!node.isVisibleToUser) return
        val isUseful = node.isClickable || !node.text.isNullOrEmpty() || !node.contentDescription.isNullOrEmpty()
        if (isUseful) {
            val indent = " ".repeat(depth)
            sb.append("$indent<node")
            if (node.text != null) sb.append(" text=\"${node.text}\"")
            if (node.viewIdResourceName != null) sb.append(" id=\"${node.viewIdResourceName}\"")
            if (node.contentDescription != null) sb.append(" desc=\"${node.contentDescription}\"")
            if (node.isClickable) sb.append(" clickable=\"true\"")
            sb.append(" />\n")
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                serialize(child, sb, depth + 1)
                child.recycle()
            }
        }
    }

    private fun logToUI(msg: String) {
        Log.d("ZeroTap", msg)
        val intent = Intent("com.example.zerotap.ACTION_LOG")
        intent.putExtra("LOG_MESSAGE", msg)
        sendBroadcast(intent)
    }
}
//package com.example.myapplication
//
//import android.accessibilityservice.AccessibilityService
//import android.accessibilityservice.GestureDescription
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Path
//import android.graphics.Rect
//import android.net.Uri
//import android.os.Bundle
//import android.util.Log
//import android.view.accessibility.AccessibilityNodeInfo
//import android.widget.Toast
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.withContext
//import kotlin.system.measureTimeMillis
//
//class ActionExecutor(private val service: AccessibilityService) {
//
//    suspend fun execute(action: Action): Boolean {
//        Log.d("Executor", "Executing: $action")
//
//        return when (action) {
//            is Action.TapElement -> handleRobustTap(action.elementId)
//            is Action.InputText -> handleType(action.text)
//            is Action.ScrollDown -> handleSwipe(true, action.amount)
//            is Action.ScrollUp -> handleSwipe(false, action.amount)
//            is Action.Back -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
//            is Action.Home -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
//            is Action.SwitchApp -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
//
//            // --- UPDATED: Open App Logic ---
//            is Action.OpenApp -> handleOpenApp(action.appName)
//
//            is Action.SearchGoogle -> launchUrl("https://www.google.com/search?q=${action.query}")
//            is Action.LaunchIntent -> handleIntent(action.intentName, action.parameters)
//            is Action.Done -> {
//                withContext(Dispatchers.Main) { Toast.makeText(service, action.text, Toast.LENGTH_LONG).show() }
//                true
//            }
//            is Action.Error -> {
//                Log.e("Executor", "Error: ${action.reason}")
//                false
//            }
//            else -> {
//                Log.w("Executor", "Action not implemented: $action")
//                false
//            }
//        }
//    }
//
//    // --- 1. ROBUST APP OPENER ---
//    private fun handleOpenApp(appName: String): Boolean {
//        val pm = service.packageManager
//
//        // 1. Get list of all installed apps
//        // Note: Requires QUERY_ALL_PACKAGES permission in Manifest
//        val installedApps = pm.getInstalledPackages(0)
//
//        // 2. Find the best match (Case-insensitive)
//        // e.g., "youtube" matches "YouTube", "settings" matches "Settings"
//        val targetPackage = installedApps.find {
//            val label = it.applicationInfo.loadLabel(pm).toString()
//            label.contains(appName, ignoreCase = true)
//        }
//
//        if (targetPackage == null) {
//            Log.e("Executor", "App not found: $appName")
//            return false
//        }
//
//        // 3. Launch the App
//        return try {
//            val intent = pm.getLaunchIntentForPackage(targetPackage.packageName)
//            if (intent != null) {
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Required when starting from Service
//                service.startActivity(intent)
//                Log.d("Executor", "Launched: ${targetPackage.packageName}")
//                true
//            } else {
//                Log.e("Executor", "No launch intent for: ${targetPackage.packageName}")
//                false
//            }
//        } catch (e: Exception) {
//            Log.e("Executor", "Failed to launch app", e)
//            false
//        }
//    }
//
//    // --- 2. ROBUST TAP STRATEGY ---
//    private suspend fun handleRobustTap(id: Int): Boolean {
//        val node = ScreenParser.nodeMap[id] ?: return false
//        val bounds = Rect()
//        node.getBoundsInScreen(bounds)
//
//        // 1. Capture State
//        val signatureBefore = getWindowHierarchySignature()
//
//        // 2. Try Semantic Click
//        if (node.isClickable) {
//            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//        } else {
//            node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//        }
//
//        // 3. Wait & Verify
//        delay(400)
//        val signatureAfter = getWindowHierarchySignature()
//
//        if (signatureBefore != signatureAfter) return true // Success
//
//        // 4. Escalate to Physical Tap
//        if (bounds.isEmpty) return false
//
//        // Add jitter to look human
//        val x = bounds.centerX().toFloat() + (Math.random() * 4 - 2).toFloat()
//        val y = bounds.centerY().toFloat() + (Math.random() * 4 - 2).toFloat()
//        return gesture(x, y)
//    }
//
//    private fun getWindowHierarchySignature(): String {
//        val root = service.rootInActiveWindow ?: return "null"
//        val sb = StringBuilder()
//        fun traverse(node: AccessibilityNodeInfo) {
//            sb.append(node.className).append(node.viewIdResourceName).append("|")
//            for (i in 0 until node.childCount) node.getChild(i)?.let { traverse(it) }
//        }
//        try { traverse(root) } catch (e: Exception) { return "error" }
//        return sb.toString().hashCode().toString()
//    }
//
//    // --- 3. OTHER HANDLERS ---
//    private fun handleType(text: String): Boolean {
//        val root = service.rootInActiveWindow ?: return false
//        var focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
//        if (focused == null) focused = ScreenParser.nodeMap.values.find { it.isEditable && it.isFocused }
//
//        if (focused != null) {
//            val args = Bundle()
//            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
//            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
//        }
//        return false
//    }
//
//    private fun handleSwipe(down: Boolean, amount: Int): Boolean {
//        val d = service.resources.displayMetrics
//        val cx = d.widthPixels / 2f
//        val cy = d.heightPixels / 2f
//        val dist = amount.coerceAtMost((d.heightPixels * 0.7).toInt()).toFloat()
//
//        val sy = if (down) cy + (dist/2) else cy - (dist/2)
//        val ey = if (down) cy - (dist/2) else cy + (dist/2)
//        return swipe(cx, sy, cx, ey)
//    }
//
//    private fun launchUrl(url: String): Boolean {
//        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
//        service.startActivity(i)
//        return true
//    }
//
//    private fun handleIntent(name: String, params: Map<String, String>): Boolean {
//        if (name == "dial") {
//            val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${params["number"]}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
//            service.startActivity(i)
//            return true
//        }
//        return false
//    }
//
//    // --- PHYSICS ENGINE ---
//    private fun gesture(x: Float, y: Float): Boolean {
//        val path = Path().apply { moveTo(x, y) }
//        val builder = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 50))
//        return service.dispatchGesture(builder.build(), null, null)
//    }
//
//    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
//        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
//        val builder = GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(path, 0, 400))
//        return service.dispatchGesture(builder.build(), null, null)
//    }
//}

//package com.example.myapplication
//
//import android.accessibilityservice.AccessibilityService
//import android.accessibilityservice.GestureDescription
//import android.content.Context
//import android.content.Intent
//import android.content.pm.PackageManager
//import android.graphics.Path
//import android.graphics.Rect
//import android.net.Uri
//import android.os.Build
//import android.os.Bundle
//import android.util.Log
//import android.view.accessibility.AccessibilityNodeInfo
//import android.widget.Toast
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.withContext
//import kotlin.system.measureTimeMillis
//
//class ActionExecutor(private val service: AccessibilityService) {
//
//    suspend fun execute(action: Action): Boolean {
//        Log.d("Executor", "Executing: $action")
//
//        return when (action) {
//            is Action.LongPressElement -> handleRobustLongPress(action.elementId)
//            is Action.TapElement -> handleRobustTap(action.elementId)
//            is Action.InputText -> handleType(action.text)
//            is Action.ScrollDown -> handleSwipe(true, action.amount)
//            is Action.ScrollUp -> handleSwipe(false, action.amount)
//            is Action.Back -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
//            is Action.Home -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
//            is Action.SwitchApp -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
//            is Action.OpenApp -> handleOpenApp(action.appName)
//            is Action.SearchGoogle -> launchUrl("https://www.google.com/search?q=${action.query}")
//            is Action.LaunchIntent -> handleIntent(action.intentName, action.parameters)
//            is Action.Done -> {
//                withContext(Dispatchers.Main) { Toast.makeText(service, action.text, Toast.LENGTH_LONG).show() }
//                true
//            }
//            is Action.Error -> {
//                Log.e("Executor", "Error: ${action.reason}")
//                false
//            }
//            else -> {
//                Log.w("Executor", "Action not implemented: $action")
//                false
//            }
//        }
//    }
//
//    /**
//     * THE "VERIFY & ESCALATE" TAP STRATEGY
//     * 1. Capture Screen Signature.
//     * 2. Try Semantic Click (Accessibility API).
//     * 3. Wait & Check Diff.
//     * 4. If no change, force Physical Click (Gesture API).
//     */
//    private suspend fun handleRobustTap(id: Int): Boolean {
//        // 1. Find the node
//        val node = ScreenParser.nodeMap[id]
//        if (node == null) {
//            Log.e("Executor", "Node $id not found in map")
//            return false
//        }
//
//        val bounds = Rect()
//        node.getBoundsInScreen(bounds)
//        val text = getVisibleText(node)
//
//        // --- STEP 1: CAPTURE STATE BEFORE ---
//        // We use a simplified signature (hash of the UI tree) to detect changes
//        var signatureBefore = ""
//        val timeToSnapshot = measureTimeMillis {
//            signatureBefore = getWindowHierarchySignature()
//        }
//
//        // --- STEP 2: ATTEMPT SEMANTIC CLICK ---
//        // This is the "Polite" way. It works on standard Android buttons.
//        var apiSuccess = false
//        if (node.isClickable) {
//            apiSuccess = node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//        } else {
//            // Try parent if child isn't clickable (common in Lists)
//            apiSuccess = node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
//        }
//
//        // --- STEP 3: WAIT & VERIFY ---
//        // Give app time to react (Blurr suggests ~100ms - 500ms)
//        delay(300)
//
//        val signatureAfter = getWindowHierarchySignature()
//        val screenChanged = signatureBefore != signatureAfter
//
//        Log.d("Executor", "Tap verification for '$text': Changed=$screenChanged (Snapshot took ${timeToSnapshot}ms)")
//
//        if (screenChanged) {
//            // The screen updated, so the semantic click worked!
//            return true
//        }
//
//        // --- STEP 4: ESCALATE (BRUTE FORCE) ---
//        // The screen didn't change. Either the app ignored the click,
//        // or it's a non-standard view (Game, WebView, Flutter, Custom View).
//        Log.w("Executor", "Semantic click failed for '$text'. Escalating to Physical Tap.")
//
//        if (bounds.isEmpty) {
//            Log.e("Executor", "Cannot physically tap: Bounds are empty.")
//            return false
//        }
//
//        // Perform physical hardware gesture at the center
//        val x = bounds.centerX().toFloat()
//        val y = bounds.centerY().toFloat()
//
//        // Optional: Add tiny random jitter to look human (Anti-Bot detection)
//        // val jitterX = x + (Math.random() * 4 - 2).toFloat()
//        // val jitterY = y + (Math.random() * 4 - 2).toFloat()
//
//        return gesture(x, y)
//    }
//    private suspend fun handleRobustLongPress(id: Int): Boolean {
//        val node = ScreenParser.nodeMap[id] ?: return false
//        val bounds = Rect()
//        node.getBoundsInScreen(bounds)
//        val text = getVisibleText(node)
//
//        // 1. Capture State
//        val signatureBefore = getWindowHierarchySignature()
//
//        // 2. Try Semantic Long Click (The "Polite" way)
//        var apiSuccess = false
//        if (node.isLongClickable) {
//            apiSuccess = node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
//        } else {
//            // Try parent if the text view itself isn't long-clickable
//            apiSuccess = node.parent?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK) ?: false
//        }
//
//        // 3. Wait & Verify
//        // Long presses usually trigger popups/menus. Give it 600ms to appear.
//        delay(600)
//
//        val signatureAfter = getWindowHierarchySignature()
//        val screenChanged = signatureBefore != signatureAfter
//
//        Log.d("Executor", "LongPress verification for '$text': Changed=$screenChanged")
//
//        if (screenChanged) return true // Success!
//
//        // 4. Escalate to Physical Long Press (Brute Force)
//        Log.w("Executor", "Semantic LongPress failed for '$text'. Escalating to Physical.")
//
//        if (bounds.isEmpty) return false
//
//        val x = bounds.centerX().toFloat()
//        val y = bounds.centerY().toFloat()
//
//        // Pass 1000ms (1 second) duration
//        return gesture(x, y)
//    }
//    /**
//     * Generates a string signature of the current screen state.
//     * If the string changes, the screen has updated.
//     */
//    private fun getWindowHierarchySignature(): String {
//        val root = service.rootInActiveWindow ?: return "null"
//        val sb = StringBuilder()
//
//        // DFS Traversal to build a lightweight structure string
//        fun traverse(node: AccessibilityNodeInfo) {
//            sb.append(node.className)
//            sb.append(node.viewIdResourceName)
//            // Only include stateful properties that might change after a click
//            if (node.isChecked) sb.append(":checked")
//            if (node.isEnabled) sb.append(":enabled")
//            sb.append("|") // Separator
//
//            for (i in 0 until node.childCount) {
//                node.getChild(i)?.let { traverse(it) }
//            }
//        }
//
//        try {
//            traverse(root)
//        } catch (e: Exception) {
//            return "error_${System.currentTimeMillis()}"
//        }
//        return sb.toString().hashCode().toString()
//    }
//
//    private fun getVisibleText(node: AccessibilityNodeInfo): String {
//        val text = node.text?.toString() ?: ""
//        val contentDesc = node.contentDescription?.toString() ?: ""
//        return (if (text.isNotBlank()) text else contentDesc).replace("\n", " ")
//    }
//
//    // --- OTHER HANDLERS ---
//
//    private fun handleOpenApp(appName: String): Boolean {
//        val pm = service.packageManager
//        // Requires QUERY_ALL_PACKAGES in Manifest
//        val installedApps = pm.getInstalledPackages(0)
//
//        // Fuzzy match app name
//        val targetPackage = installedApps.find {
//            val label = it.applicationInfo.loadLabel(pm).toString()
//            label.contains(appName, ignoreCase = true)
//        }
//
//        if (targetPackage == null) {
//            Log.e("Executor", "App not found: $appName")
//            return false
//        }
//
//        return try {
//            val intent = pm.getLaunchIntentForPackage(targetPackage.packageName)
//            if (intent != null) {
//                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                service.startActivity(intent)
//                true
//            } else false
//        } catch (e: Exception) {
//            Log.e("Executor", "Failed to launch app", e)
//            false
//        }
//    }
//
//    private fun handleType(text: String): Boolean {
//        val root = service.rootInActiveWindow ?: return false
//        var focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
//
//        if (focused == null) {
//            focused = ScreenParser.nodeMap.values.find { it.isEditable && it.isFocused }
//        }
//
//        if (focused != null) {
//            val args = Bundle()
//            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
//            // Try standard input
//            var ok = focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
//            // If standard fails (some webviews), try generic paste
//            if (!ok) {
//                // You could add ClipboardManager paste logic here as a fallback
//            }
//            return ok
//        }
//        return false
//    }
//
//    private fun handleSwipe(down: Boolean, amount: Int): Boolean {
//        val d = service.resources.displayMetrics
//        val cx = d.widthPixels / 2f
//        val cy = d.heightPixels / 2f
//        // Limit swipe distance to ensure reliability
//        val dist = amount.coerceAtMost((d.heightPixels * 0.7).toInt()).toFloat()
//
//        val sy = if (down) cy + (dist/2) else cy - (dist/2)
//        val ey = if (down) cy - (dist/2) else cy + (dist/2)
//        return swipe(cx, sy, cx, ey)
//    }
//
//    private fun launchUrl(url: String): Boolean {
//        val i = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
//        service.startActivity(i)
//        return true
//    }
//
//    private fun handleIntent(name: String, params: Map<String, String>): Boolean {
//        if (name == "dial") {
//            val i = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${params["number"]}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
//            service.startActivity(i)
//            return true
//        }
//        return false
//    }
//
//    // --- PHYSICS ENGINE ---
//
//    private fun gesture(x: Float, y: Float): Boolean {
//        val path = Path().apply { moveTo(x, y) }
//        val builder = GestureDescription.Builder()
//        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 50)) // 50ms tap duration
//        return service.dispatchGesture(builder.build(), null, null)
//    }
//
//    private fun swipe(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
//        val path = Path().apply { moveTo(x1, y1); lineTo(x2, y2) }
//        val builder = GestureDescription.Builder()
//        builder.addStroke(GestureDescription.StrokeDescription(path, 0, 400)) // 400ms swipe duration
//        return service.dispatchGesture(builder.build(), null, null)
//    }
//}

package com.example.myapplication

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.Intent
import android.graphics.Path
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.system.measureTimeMillis

class ActionExecutor(private val service: AccessibilityService) {

    private val DEBUG_SHOW_TAPS = true

    suspend fun execute(action: Action): Boolean {
        Log.d("Executor", "Executing: $action")

        return when (action) {
            // --- CORE INTERACTIONS ---
            is Action.TapElement -> handleRobustTap(action.elementId)
            is Action.LongPressElement -> handleRobustLongPress(action.elementId)
            is Action.InputText -> handleType(action.text)
            is Action.TapElementInputTextPressEnter -> handleTapTypeEnter(action.index, action.text)
            is Action.ScrollDown -> handleSwipe(true, action.amount)
            is Action.ScrollUp -> handleSwipe(false, action.amount)

            // --- SYSTEM & NAV ---
            is Action.Back -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            is Action.Home -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            is Action.SwitchApp -> service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            is Action.OpenApp -> handleOpenApp(action.appName)
            is Action.LaunchIntent -> handleIntent(action.intentName, action.parameters)
            is Action.SearchGoogle -> launchUrl("https://www.google.com/search?q=${action.query}")

            // --- FLOW ---
            is Action.Wait -> { delay(2000); true }
            is Action.Done -> {
                withContext(Dispatchers.Main) { Toast.makeText(service, action.text, Toast.LENGTH_LONG).show() }
                true
            }
            is Action.Error -> {
                Log.e("Executor", "Error: ${action.reason}")
                false
            }

            // --- PLACEHOLDERS (Exhaustive when requirement) ---
            is Action.Speak -> true
            is Action.Ask -> true
            is Action.ReadFile -> true
            is Action.WriteFile -> true
            is Action.AppendFile -> true
        }
    }

    // ==========================================
    // 1. THE "VERIFY & ESCALATE" TAP LOGIC
    // ==========================================
    private fun findSurfaceViewCenter(): Pair<Float, Float>? {
        val root = service.rootInActiveWindow ?: return null
        val rect = Rect()

        fun traverse(node: AccessibilityNodeInfo?): Rect? {
            if (node == null) return null

            val cls = node.className?.toString() ?: ""

            if (cls.contains("SurfaceView", true) ||
                cls.contains("TextureView", true)
            ) {
                node.getBoundsInScreen(rect)
                if (!rect.isEmpty) return Rect(rect)
            }

            for (i in 0 until node.childCount) {
                traverse(node.getChild(i))?.let { return it }
            }
            return null
        }

        val surface = traverse(root) ?: return null
        return surface.centerX().toFloat() to surface.centerY().toFloat()
    }
//    private suspend fun handleRobustTap(id: Int): Boolean {
//        val node = ScreenParser.nodeMap[id] ?: return false
//        val bounds = Rect()
//        node.getBoundsInScreen(bounds)
//        val text = getVisibleText(node)
//        Log.d("Executor", "TARGET: '$text' | ID: $id | BOUNDS: $bounds | CENTER: ${bounds.centerX()},${bounds.centerY()}")
//        // A. CAPTURE STATE (SIGNATURE)
//        val signatureBefore = getWindowHierarchySignature()
//
//        // B. ATTEMPT 1: POLITE API CLICK
////        Log.d(tag="is node clickable",msg=)
////        Log.d("Executor", node.isClickable)
//
//        if (node.isClickable) {
//            Log.d("Executor","yes")
//            node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//        } else {
//            Log.d("Executor","no")
//            node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//        }
//
//        // C. WAIT & VERIFY
//        // Wait 300ms for UI to update (Animations, Popups, Icon changes)
//        delay(100)
//
//        val signatureAfter = getWindowHierarchySignature()
//        val screenChanged = signatureBefore != signatureAfter
//
//        Log.d("Executor", "Tap verification for '$text': Changed=$screenChanged")
//
//        if (screenChanged) {
//            return true // Success!
//        }
//
//        // D. ESCALATE: BRUTE FORCE PHYSICAL TAP
//        Log.w("Executor", "Semantic click failed for '$text'. Escalating to Physical Tap.")
//
//        if (bounds.isEmpty) {
//            Log.e("Executor", "Cannot physically tap: Bounds are empty.")
//            return false
//        }
//        val surfaceCenter = findSurfaceViewCenter()
//
//        if (surfaceCenter != null) {
//            val (x, y) = surfaceCenter
//            Log.d("Executor", "Physical Tap on SurfaceView at ($x, $y)")
////            return
//            clickOnPoint(x, y)
//            return  true
//        }
////        else{
////
////        }
////        val dm = service.resources.displayMetrics
////        val x = dm.widthPixels / 2f
////        val y = dm.heightPixels / 2f
//
////        val x = bounds.centerX().toFloat()
////        val y = bounds.centerY().toFloat()
//
////        clickOnPoint(x, y) // Uses Gesture API
//        return true
//    }
//private suspend fun handleRobustTap(id: Int): Boolean {
//    val node = ScreenParser.nodeMap[id] ?: return false
//    val bounds = Rect()
//    node.getBoundsInScreen(bounds)
//    val text = getVisibleText(node)
//
//    Log.d("Executor", "TARGET: '$text' | ID: $id | BOUNDS: $bounds | CENTER: ${bounds.centerX()},${bounds.centerY()}")
//
//    // A. CAPTURE STATE
//    val signatureBefore = getWindowHierarchySignature()
//
//    // B. ATTEMPT 1: POLITE API CLICK
//    if (node.isClickable) {
//        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//    } else {
//        node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK)
//    }
//
//    // C. WAIT & VERIFY
//    delay(300)  // Increased from 100ms for better detection
//    val signatureAfter = getWindowHierarchySignature()
//    val screenChanged = signatureBefore != signatureAfter
//
//    Log.d("Executor", "Tap verification for '$text': Changed=$screenChanged")
//
//    if (screenChanged) return true
//
//    // D. ESCALATE: TAP THE ACTUAL BUTTON COORDINATES
//    Log.w("Executor", "Semantic click failed for '$text'. Escalating to Physical Tap.")
//
//    if (bounds.isEmpty) {
//        Log.e("Executor", "Cannot physically tap: Bounds are empty.")
//        return false
//    }
//
//    // ✅ Use the BUTTON'S center, not the SurfaceView center
//    val x = bounds.centerX().toFloat()
//    val y = bounds.centerY().toFloat()
//
//    Log.d("Executor", "Physical Tap at button center ($x, $y)")
//    clickOnPoint(x, y)
//    return true
//}
private suspend fun handleRobustTap(id: Int): Boolean {
    val node = ScreenParser.nodeMap[id] ?: return false
    val bounds = Rect()
    node.getBoundsInScreen(bounds)
    val text = getVisibleText(node)
    val nodemap_ = ScreenParser.nodeMap

    Log.d("Executor", "TARGET: '$text' | ID: $id | BOUNDS: $bounds | $nodemap_")

    // A. CAPTURE STATE
    val signatureBefore = getWindowHierarchySignature()

    // B. ATTEMPT 1: SEMANTIC CLICK
    val semanticSuccess = if (node.isClickable) {
        node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    } else {
        node.parent?.performAction(AccessibilityNodeInfo.ACTION_CLICK) ?: false
    }

    // C. WAIT & VERIFY
    delay(400)  // Increased to 400ms for better detection
    val signatureAfter = getWindowHierarchySignature()
    val screenChanged = signatureBefore != signatureAfter

    Log.d("Executor", "Semantic tap for '$text': API_SUCCESS=$semanticSuccess, UI_CHANGED=$screenChanged")

    if (screenChanged) return true

    // D. ESCALATE TO PHYSICAL TAP
    Log.w("Executor", "Escalating to Physical Tap for '$text'")

    if (bounds.isEmpty) {
        Log.e("Executor", "Cannot tap: bounds are empty")
        return false
    }

    val x = bounds.centerX().toFloat()
    val y = bounds.centerY().toFloat()

    // Use suspending version that waits for completion
    return clickOnPointSync(x, y)
}

    // Synchronous version that waits for gesture completion
    private suspend fun clickOnPointSync(x: Float, y: Float, duration: Long = 1000): Boolean {
        if (DEBUG_SHOW_TAPS) Log.d("Executor", "Physical Tap at ($x, $y)")

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        // Use CompletableDeferred to wait for gesture completion
        return withContext(Dispatchers.Main) {
            var success = false
            val callback = object : AccessibilityService.GestureResultCallback() {
                override fun onCompleted(gestureDescription: GestureDescription) {
                    success = true
                    Log.d("Executor", "Gesture completed successfully")
                }

                override fun onCancelled(gestureDescription: GestureDescription) {
                    Log.w("Executor", "Gesture was cancelled")
                    success = false
                }
            }

            service.dispatchGesture(gesture, callback, null)

            // Wait for callback
            delay(duration + 200)
            success
        }
    }

    // ==========================================
    // 2. LONG PRESS LOGIC
    // ==========================================
    private suspend fun handleRobustLongPress(id: Int): Boolean {
        val node = ScreenParser.nodeMap[id] ?: return false
        val bounds = Rect()
        node.getBoundsInScreen(bounds)

        val signatureBefore = getWindowHierarchySignature()

        // Try API
        if (node.isLongClickable) node.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)
        else node.parent?.performAction(AccessibilityNodeInfo.ACTION_LONG_CLICK)

        delay(600) // Long press takes longer
        if (signatureBefore != getWindowHierarchySignature()) return true

        // Escalate
        if (bounds.isEmpty) return false
        Log.w("Executor", "Escalating to Physical Long Press.")

        // 1000ms hold
        clickOnPoint(bounds.centerX().toFloat(), bounds.centerY().toFloat(), 1000L)
        return true
    }

    // ==========================================
    // 3. PHYSICAL GESTURE IMPLEMENTATION
    // ==========================================
    private fun clickOnPoint(x: Float, y: Float, duration: Long = 100) {
        if (DEBUG_SHOW_TAPS) Log.d("Executor", "Physical Tap at ($x, $y)")

        val path = Path().apply { moveTo(x, y) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, duration))
            .build()

        // Dispatch asynchronously
        service.dispatchGesture(gesture, null, null)
    }

    // ==========================================
    // 4. HELPERS
    // ==========================================
    private fun getWindowHierarchySignature(): String {
        val root = service.rootInActiveWindow ?: return "null"
        val sb = StringBuilder()
        fun traverse(node: AccessibilityNodeInfo) {
            sb.append(node.className).append(node.viewIdResourceName).append("|")
            for (i in 0 until node.childCount) node.getChild(i)?.let { traverse(it) }
        }
        try { traverse(root) } catch (e: Exception) { return "error" }
        return sb.toString().hashCode().toString()
    }

    private fun getVisibleText(node: AccessibilityNodeInfo): String {
        return (node.text ?: node.contentDescription ?: "").toString()
    }

    private suspend fun handleTapTypeEnter(id: Int, text: String): Boolean {
        if (!handleRobustTap(id)) return false
        delay(500)
        if (!handleType(text)) return false
        delay(300)
        // Hit Enter (Bottom Right corner estimation)
        val d = service.resources.displayMetrics
        clickOnPoint(d.widthPixels * 0.9f, d.heightPixels * 0.9f)
        return true
    }

    private fun handleType(text: String): Boolean {
        val root = service.rootInActiveWindow ?: return false
        var focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused == null) focused = ScreenParser.nodeMap.values.find { it.isEditable && it.isFocused }

        if (focused != null) {
            val args = Bundle()
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text)
            return focused.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
        }
        return false
    }

    private fun handleSwipe(down: Boolean, amount: Int): Boolean {
        val d = service.resources.displayMetrics
        val cx = d.widthPixels / 2f
        val cy = d.heightPixels / 2f
        val dist = amount.coerceAtMost((d.heightPixels * 0.7).toInt()).toFloat()

        val sy = if (down) cy + (dist/2) else cy - (dist/2)
        val ey = if (down) cy - (dist/2) else cy + (dist/2)

        val path = Path().apply { moveTo(cx, sy); lineTo(cx, ey) }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 400))
            .build()
        return service.dispatchGesture(gesture, null, null)
    }

    private fun handleOpenApp(appName: String): Boolean {
        val pm = service.packageManager
        val installedApps = pm.getInstalledPackages(0)
        val target = installedApps.find { it.applicationInfo.loadLabel(pm).toString().contains(appName, true) } ?: return false
        val intent = pm.getLaunchIntentForPackage(target.packageName)?.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) } ?: return false
        service.startActivity(intent)
        return true
    }

    private fun launchUrl(url: String): Boolean {
        service.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
        return true
    }

    private fun handleIntent(name: String, params: Map<String, String>): Boolean {
        if (name == "dial") {
            service.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${params["number"]}")).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            return true
        }
        return false
    }
}
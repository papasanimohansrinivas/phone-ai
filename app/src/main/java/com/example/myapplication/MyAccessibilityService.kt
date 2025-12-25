import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class MyAccessibilityService : AccessibilityService() {

    // Store nodes to reference them later by ID
    private val nodeMap = mutableMapOf<Int, AccessibilityNodeInfo>()

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // You usually trigger the "Agent" manually (e.g., floating button),
        // not on every event, to save battery/cost.
    }

    override fun onInterrupt() {}

    // CALL THIS when you want to send the screen to the LLM
    fun scanScreen(): String {
        val rootNode = rootInActiveWindow ?: return "Screen is empty"
        nodeMap.clear()

        val sb = StringBuilder()
        traverseNode(rootNode, sb, depth = 0)

        return sb.toString()
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, sb: StringBuilder, depth: Int) {
        if (node == null) return
        if (depth > 20) return // Safety break for deep trees

        // Heuristic: We only care about things we can Click, Type in, or Scroll
        // OR things that have text (labels)
        val isInteractive = node.isClickable || node.isEditable || node.isScrollable
        val hasText = !node.text.isNullOrBlank() || !node.contentDescription.isNullOrBlank()

        if (isInteractive || hasText) {
            val id = nodeMap.size + 1
            nodeMap[id] = node // Save reference

            val text = node.text ?: node.contentDescription ?: ""
            val className = node.className.toString().substringAfterLast('.')

            // Format: [ID] Type: "Text" (status)
            sb.append("[$id] $className")
            if (text.isNotEmpty()) sb.append(": \"$text\"")
            if (node.isClickable) sb.append(" [Clickable]")
            if (node.isEditable) sb.append(" [Editable]")
            if (node.isScrollable) sb.append(" [Scrollable]")
            sb.append("\n")
        }

        // Recursively visit children
        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), sb, depth + 1)
        }
    }
}
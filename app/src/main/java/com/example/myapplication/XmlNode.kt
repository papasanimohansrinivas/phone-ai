package com.example.myapplication

import android.graphics.Rect

/**
 * A smart wrapper for AccessibilityNodeInfo.
 * It creates a "Virtual DOM" that is easier to manipulate than the raw Android tree.
 */
data class XmlNode(
    val attributes: MutableMap<String, String> = mutableMapOf(),
    val children: MutableList<XmlNode> = mutableListOf(),
    var parent: XmlNode? = null
) {
    // Check if the node is within the visible screen area
    fun isVisibleOnScreen(screenWidth: Int, screenHeight: Int): Boolean {
        val boundsStr = attributes["bounds"] ?: return false
        try {
            // Parse "[0,0][1080,200]"
            val parts = boundsStr.replace("][", ",").removePrefix("[").removeSuffix("]").split(",")
            if (parts.size != 4) return false

            val left = parts[0].toInt()
            val top = parts[1].toInt()
            val right = parts[2].toInt()
            val bottom = parts[3].toInt()

            // Logic: Is it strictly OUTSIDE the screen?
            if (right <= 0 || left >= screenWidth || bottom <= 0 || top >= screenHeight) return false

            // Logic: Does it have 0 area?
            if (left == right || top == bottom) return false

            return true
        } catch (e: Exception) {
            return false
        }
    }

    // The "Brain" of the filter: Should the AI see this?
    fun isInteractive(): Boolean {
        // If disabled, ignore (unless it has text, handled separately)
        if (attributes["enabled"] == "false") return false

        // Check interactive flags
        return attributes["clickable"] == "true" ||
                attributes["long-clickable"] == "true" ||
                attributes["scrollable"] == "true" ||
                attributes["class"]?.contains("EditText") == true || // Input fields
                attributes["focusable"] == "true"
    }

    fun isSemanticallyImportant(): Boolean {
        return (attributes["text"]?.isNotBlank() == true) ||
                (attributes["content-desc"]?.isNotBlank() == true) ||
                (attributes["resource-id"]?.isNotBlank() == true)
    }
}
//package com.example.myapplication
//
//import android.graphics.Rect
//import android.view.accessibility.AccessibilityNodeInfo
//
//object ScreenParser {
//
//    // Global map to store interactive nodes for execution
//    val nodeMap = mutableMapOf<Int, AccessibilityNodeInfo>()
//    private var counter = 0
//
//    // Main entry point
//    fun parse(root: AccessibilityNodeInfo, width: Int, height: Int): String {
//        nodeMap.clear()
//        counter = 0
//
//        val builder = StringBuilder()
//
//        // 1. Convert Android Tree -> XmlNode Tree (Virtual DOM)
//        val virtualRoot = buildVirtualTree(root)
//
//        // 2. Prune the Tree (Remove invisible/useless wrappers)
//        val prunedNodes = pruneTree(virtualRoot, width, height)
//
//        // 3. Generate String for LLM
//        for (node in prunedNodes) {
//            buildStringRecursive(node, 0, builder)
//        }
//
//        return if (builder.isEmpty()) "Screen appears empty." else builder.toString()
//    }
//
//    private fun buildVirtualTree(node: AccessibilityNodeInfo): XmlNode {
//        val xmlNode = XmlNode()
//
//        // Extract attributes
//        val bounds = Rect()
//        node.getBoundsInScreen(bounds)
//        xmlNode.attributes["bounds"] = "[${bounds.left},${bounds.top}][${bounds.right},${bounds.bottom}]"
//        xmlNode.attributes["text"] = node.text?.toString() ?: ""
//        xmlNode.attributes["content-desc"] = node.contentDescription?.toString() ?: ""
//        xmlNode.attributes["resource-id"] = node.viewIdResourceName ?: ""
//        xmlNode.attributes["class"] = node.className?.toString() ?: ""
//
//        // Flags
//        xmlNode.attributes["clickable"] = node.isClickable.toString()
//        xmlNode.attributes["enabled"] = node.isEnabled.toString()
//        xmlNode.attributes["scrollable"] = node.isScrollable.toString()
//        xmlNode.attributes["checked"] = node.isChecked.toString()
//
//        // Recursion
//        for (i in 0 until node.childCount) {
//            val child = node.getChild(i)
//            if (child != null) {
//                val childXml = buildVirtualTree(child)
//                childXml.parent = xmlNode
//                xmlNode.children.add(childXml)
//                // Important: We don't recycle 'child' here because we might need it for nodeMap later.
//                // In a production app, you need a strategy to map IDs back to live nodes safely.
//            }
//        }
//        return xmlNode
//    }
//
//    /**
//     * The Magic Logic: Flattens the tree.
//     * If a node is useless, we return its CHILDREN instead of the node itself.
//     */
//    private fun pruneTree(node: XmlNode, width: Int, height: Int): List<XmlNode> {
//        // 1. Recursively prune children first
//        val newChildren = node.children.flatMap { pruneTree(it, width, height) }
//        node.children.clear()
//        node.children.addAll(newChildren)
//
//        // 2. Visibility Check
//        if (!node.isVisibleOnScreen(width, height)) {
//            return emptyList() // Kill this branch
//        }
//
//        // 3. Keep or Discard?
//        val isImportant = node.isSemanticallyImportant()
//        val isInteractive = node.isInteractive()
//        val hasChildren = node.children.isNotEmpty()
//
//        return if (isImportant || isInteractive || hasChildren) {
//            // Keep this node
//            listOf(node)
//        } else {
//            // Discard this node, but promote its children up
//            node.children
//        }
//    }
//
//    private fun buildStringRecursive(node: XmlNode, indent: Int, sb: StringBuilder) {
//        val spaces = " ".repeat(indent)
//        val text = node.attributes["text"] ?: ""
//        val desc = node.attributes["content-desc"] ?: ""
//        val visibleText = if (text.isNotBlank()) text else desc
//        val id = node.attributes["resource-id"] ?: ""
//
//        // Logic to assign an ID for the LLM
//        var prefix = ""
//        if (node.isInteractive()) {
//            counter++
//            // In a real implementation, you need to map this 'counter' back to the actual AccessibilityNodeInfo
//            // For now, we are simulating the structure generation
//            prefix = "[$counter] "
//        }
//
//        if (prefix.isNotEmpty() || visibleText.isNotBlank()) {
//            sb.append("$spaces$prefix")
//            if (visibleText.isNotBlank()) sb.append("text:\"$visibleText\" ")
//            if (id.isNotBlank()) sb.append("id:<$id> ")
//
//            // Add extra context
//            if (node.attributes["checked"] == "true") sb.append("[CHECKED] ")
//            if (node.attributes["scrollable"] == "true") sb.append("[SCROLLABLE] ")
//
//            sb.append("\n")
//        }
//
//        for (child in node.children) {
//            buildStringRecursive(child, indent + 1, sb)
//        }
//    }
//}

package com.example.myapplication

import android.view.accessibility.AccessibilityNodeInfo

object ScreenParser {
    // This map allows ActionExecutor to find the REAL node using the ID the LLM picks
    val nodeMap = mutableMapOf<Int, AccessibilityNodeInfo>()
    private var counter = 0

    fun parse(root: AccessibilityNodeInfo, width: Int, height: Int): String {
        nodeMap.clear()
        counter = 0
        val sb = StringBuilder()

        // Recursive function to build the string and map nodes
        fun walk(node: AccessibilityNodeInfo, indent: Int) {
            if (!node.isVisibleToUser) return

            val isInteractive = node.isClickable || node.isLongClickable || node.isEditable
            val text = node.text?.toString() ?: node.contentDescription?.toString() ?: ""

            if (isInteractive || text.isNotBlank()) {
                val id = ++counter
                nodeMap[id] = node // Save reference

                sb.append("\t".repeat(indent))
                sb.append("[$id] ")
                if (text.isNotBlank()) sb.append("text:\"$text\" ")
                sb.append("<${node.className.toString().removePrefix("android.widget.")}>\n")
            }

            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { walk(it, indent + 1) }
            }
        }

        walk(root, 0)
        return sb.toString()
    }
}
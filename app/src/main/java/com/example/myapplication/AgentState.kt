package com.example.myapplication

data class AgentCommand(
    val action: String,      // "tap", "type", "enter", "scroll", "wait", "done", "back", "home"
    val elementId: Int?,     // ID from ScreenParser
    val text: String? = null // For typing
)
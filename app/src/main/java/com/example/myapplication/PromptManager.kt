//package com.example.myapplication
//
//object PromptManager {
//
//    fun buildPrompt(
//        userRequest: String,
//        screenContext: String,
//        history: List<String>,
//        todoContent: String,
//        resultsContent: String,
//        step: Int
//    ): String {
//        // Generate the dynamic action documentation from our Action.kt Specs
//        val availableActionsDoc = Action.getAllSpecs().joinToString("\n") { spec ->
//            val params = spec.params.joinToString(", ") { "${it.name}: ${it.type.simpleName}" }
//            "- ${spec.name}: ${spec.description} (Params: $params)"
//        }
//
//        return """
//<user_request>
//USER REQUEST: $userRequest
//</user_request>
//
//<agent_history>
//${history.joinToString("\n")}
//</agent_history>
//
//<agent_state>
//File System:
//- todo.md (${todoContent.lines().size} lines)
//- results.md (${resultsContent.lines().size} lines)
//
//Current Step: $step
//Timestamp: ${java.util.Date()}
//</agent_state>
//
//<android_state>
//Interactive Elements:
//$screenContext
//</android_state>
//
//<available_actions>
//$availableActionsDoc
//</available_actions>
//
//<output_instruction>
//You must respond in the following JSON format:
//{
//  "thinking": "Your step-by-step reasoning...",
//  "evaluationPreviousGoal": "Did the last action work?",
//  "memory": "What to remember for next time...",
//  "nextGoal": "What are we doing next?",
//  "action": [
//    { "action_name": { "param": "value" } }
//  ]
//}
//</output_instruction>
//        """.trimIndent()
//    }
//}

package com.example.myapplication

object PromptManager {

    fun buildPrompt(
        userRequest: String,
        screenContext: String,
        history: List<String>,
        todoContent: String,
        resultsContent: String,
        step: Int
    ): String {
        // Generate the dynamic action documentation from our Action.kt Specs
        val availableActionsDoc = Action.getAllSpecs().joinToString("\n") { spec ->
            val params = spec.params.joinToString(", ") { "${it.name}: ${it.type.simpleName}" }
            "- ${spec.name}: ${spec.description} (Params: $params)"
        }

        // Analyze history for loops
        val loopDetection = detectLoops(history)

        return """
<user_request>
USER REQUEST: $userRequest
</user_request>

<agent_history>
${history.joinToString("\n")}
</agent_history>

<agent_state>
File System:
- todo.md (${todoContent.lines().size} lines)
- results.md (${resultsContent.lines().size} lines)

Current Step: $step
Timestamp: ${java.util.Date()}
</agent_state>

<android_state>
Interactive Elements:
$screenContext
</android_state>

<available_actions>
$availableActionsDoc
</available_actions>

<adaptive_action_strategy>
You are an autonomous Android agent that must be adaptive and self-correcting. Follow these critical rules:

<failure_detection>
${loopDetection.warning}

Track your action history carefully:
- If you've tried the SAME action on the SAME element more than 2 times and the UI hasn't changed meaningfully, YOU ARE STUCK IN A LOOP
- Signs of being stuck:
  * "Success: true" but no UI change (same elements visible)
  * Repeating identical actions (e.g., tap_element with same element_id)
  * Media progress continues but controls don't appear
  * Same error/result multiple times in a row
</failure_detection>

<loop_breaking_strategy>
When you detect you're in a loop (same action 3+ times with no progress):

1. **STOP immediately** - Do not repeat the same action again
2. **Acknowledge the failure** - In "evaluationPreviousGoal", state: "LOOP DETECTED: tried [action] on element [X] [N] times with no progress"
3. **Analyze alternatives** - Look at ALL interactive elements in the current state
4. **Try a different approach:**

   For video/audio control (YouTube, media apps):
   a. If tapping video player container failed → Try unlabeled <Button> elements near the video (often element 4 or nearby)
   b. If that fails → Try tapping child elements within video player (elements 2-10 under the player)
   c. If that fails → Try long_press_element instead of tap
   d. If that fails → Try back button to exit video (stops playback)
   e. If that fails → Try home button to background app
   f. If that fails → Use launch_intent with media controls
   
   For navigation tasks:
   a. Try scrolling to reveal hidden elements (scroll_down or scroll_up)
   b. Try back button to return to previous screen
   c. Look for "Options", "More", "Menu" buttons (often unlabeled)
   
   For text input:
   a. Verify correct input field is being tapped
   b. Try tap_element_input_text_press_enter as single action
   c. Check if keyboard is blocking elements

5. **Execute different action** - Never repeat the same failing action more than twice
</loop_breaking_strategy>

<element_analysis_guide>
When examining interactive elements, pay special attention to:

**Unlabeled elements often ARE the controls you need:**
- `<Button>` with no text/contentDescription → Usually pause/play, mute, close, or toggle controls
- Buttons positioned near SeekBars → Typically playback controls (play/pause)
- Buttons at top-right corner → Usually settings, options, or close
- Buttons at bottom of screen → Usually navigation or primary actions

**Element hierarchy matters:**
- If tapping parent container (e.g., element 1 "Video player") doesn't work → Try its children (elements 2-10)
- Look at indentation in the element tree to understand parent-child relationships
- Child elements are often the actual interactive controls

**Position and context clues:**
- Near a SeekBar + unlabeled Button = likely play/pause control
- "Options", "More", "Action menu" = opens additional actions
- "Expand", "Collapse", "Minimize" = changes view state
- No description + clickable + near media = media control button

**YouTube-specific patterns:**
- Video player container (element 1) often doesn't respond to taps
- Pause/play button is usually:
  * An unlabeled <Button> (no contentDescription)
  * Positioned after the SeekBar in hierarchy
  * Wide bounds (spans screen width or centered)
  * Often element 4 or nearby in the tree
</element_analysis_guide>

<critical_rules>
- NEVER repeat the same failing action more than 2 times
- ALWAYS honestly assess if previous action made progress
- If no UI change after 2 attempts → State "FAILED - no progress" not "Success"
- ALWAYS try a different strategy after 2 failed attempts
- Your "evaluationPreviousGoal" must reflect reality, not optimism
</critical_rules>

</adaptive_action_strategy>

<output_instruction>
You must respond in the following JSON format:
{
  "thinking": "Your step-by-step reasoning. If you detected a loop, start with 'LOOP DETECTED:' and explain what pattern you noticed and why you're changing strategy.",
  "evaluationPreviousGoal": "Honest assessment: Did the last action work? If you've tried the same thing multiple times, state 'FAILED - no progress after N attempts'",
  "memory": "What to remember for next time, especially patterns that don't work...",
  "nextGoal": "What are we doing next? If breaking out of a loop, explain the new approach.",
  "action": [
    { "action_name": { "param": "value" } }
  ]
}
</output_instruction>
        """.trimIndent()
    }

    private data class LoopDetection(
        val isLooping: Boolean,
        val repeatedAction: String?,
        val repeatCount: Int,
        val warning: String
    )

    private fun detectLoops(history: List<String>): LoopDetection {
        if (history.size < 3) {
            return LoopDetection(false, null, 0, "")
        }

        // Get last 6 actions to check for patterns
        val recentHistory = history.takeLast(6)

        // Extract action patterns (e.g., "TapElement(1)")
        val actionPattern = Regex("""Tried (\w+).*?element[:\s]+(\d+)|Tried (\w+)""")
        val recentActions = recentHistory.mapNotNull { line ->
            actionPattern.find(line)?.let { match ->
                val action = match.groupValues[1].ifEmpty { match.groupValues[3] }
                val element = match.groupValues[2]
                if (element.isNotEmpty()) "$action:$element" else action
            }
        }

        // Check for repeated actions
        if (recentActions.size >= 3) {
            val lastAction = recentActions.last()
            val repeatCount = recentActions.takeLastWhile { it == lastAction }.size

            if (repeatCount >= 3) {
                val warning = """
⚠️ LOOP DETECTED: You have tried "$lastAction" $repeatCount times in a row!
The same action is repeating with no progress. You MUST try a different approach.
Look for alternative elements or use a different action type.
                """.trimIndent()

                return LoopDetection(true, lastAction, repeatCount, warning)
            }
        }

        return LoopDetection(false, null, 0, "")
    }
}

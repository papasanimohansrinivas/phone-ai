//package com.example.myapplication
//
//import kotlinx.serialization.KSerializer
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.descriptors.SerialDescriptor
//import kotlinx.serialization.descriptors.buildClassSerialDescriptor
//import kotlinx.serialization.encoding.Decoder
//import kotlinx.serialization.encoding.Encoder
//import kotlinx.serialization.json.*
//import kotlin.reflect.KClass
//
//data class ParamSpec(val name: String, val type: KClass<*>, val description: String)
//
//@Serializable(with = Action.ActionSerializer::class)
//sealed class Action {
//    // --- UI ACTIONS ---
//    data class TapElement(val elementId: Int) : Action()
//    data class LongPressElement(val elementId: Int) : Action()
//    data class InputText(val text: String) : Action()
//    data class TapElementInputTextPressEnter(val index: Int, val text: String) : Action()
//    data class ScrollDown(val amount: Int) : Action()
//    data class ScrollUp(val amount: Int) : Action()
//
//    // --- SYSTEM ACTIONS ---
//    data object Back : Action()
//    data object Home : Action()
//    data object SwitchApp : Action()
//    data class OpenApp(val appName: String) : Action()
//    data class LaunchIntent(val intentName: String, val parameters: Map<String, String>) : Action()
//    data class SearchGoogle(val query: String) : Action()
//
//    // --- FLOW CONTROL ---
//    data object Wait : Action()
//    data class Done(val success: Boolean, val text: String) : Action()
//
//    // --- PLACEHOLDERS (To match Blurr schema & prevent errors) ---
//    data class Speak(val message: String) : Action()
//    data class Ask(val question: String) : Action()
//    data class ReadFile(val fileName: String) : Action()
//    data class WriteFile(val fileName: String) : Action()
//    data class AppendFile(val fileName: String) : Action()
//
//    // --- ERROR STATE ---
//    data class Error(val reason: String) : Action()
//
//    object ActionSerializer : KSerializer<Action> {
//        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Action")
//
//        override fun serialize(encoder: Encoder, value: Action) {
//            throw NotImplementedError("Serialization not supported")
//        }
//
//        override fun deserialize(decoder: Decoder): Action {
//            try {
//                val jsonInput = (decoder as JsonDecoder).decodeJsonElement().jsonObject
//                val root = if (jsonInput.containsKey("action")) jsonInput["action"]!!.jsonObject else jsonInput
//
//                if (root.isEmpty()) return Error("Empty JSON")
//
//                val actionName = root.keys.first()
//                val paramsJson = root[actionName]?.jsonObject
//
//                val spec = allSpecs[actionName] ?: return Error("Unknown action: $actionName")
//
//                val args = mutableMapOf<String, Any?>()
//                paramsJson?.let {
//                    for (paramSpec in spec.params) {
//                        val jsonValue = it[paramSpec.name] ?: continue
//                        val value = when (paramSpec.type) {
//                            Int::class -> jsonValue.jsonPrimitive.int
//                            String::class -> jsonValue.jsonPrimitive.content
//                            Boolean::class -> jsonValue.jsonPrimitive.boolean
//                            Map::class -> jsonValue.jsonObject.mapValues { e -> e.value.jsonPrimitive.content }
//                            else -> ""
//                        }
//                        args[paramSpec.name] = value
//                    }
//                }
//                return spec.build(args)
//            } catch (e: Exception) {
//                return Error("Parse Error: ${e.message}")
//            }
//        }
//    }
//
//    companion object {
//        data class Spec(
//            val name: String,
//            val description: String,
//            val params: List<ParamSpec>,
//            val build: (args: Map<String, Any?>) -> Action
//        )
//
//        val allSpecs: Map<String, Spec> = mapOf(
//            "tap_element" to Spec("tap_element", "Tap ID.", listOf(ParamSpec("element_id", Int::class, ""))) { TapElement(it["element_id"] as Int) },
//            "long_press_element" to Spec("long_press_element", "Long Press ID.", listOf(ParamSpec("element_id", Int::class, ""))) { LongPressElement(it["element_id"] as Int) },
//            "input_text" to Spec("input_text", "Type text.", listOf(ParamSpec("text", String::class, ""))) { InputText(it["text"] as String) },
//            "tap_element_input_text_press_enter" to Spec("tap_element_input_text_press_enter", "Tap, Type, Enter.", listOf(ParamSpec("index", Int::class, ""), ParamSpec("text", String::class, ""))) { TapElementInputTextPressEnter(it["index"] as Int, it["text"] as String) },
//            "scroll_down" to Spec("scroll_down", "Scroll Down.", listOf(ParamSpec("amount", Int::class, ""))) { ScrollDown(it["amount"] as Int) },
//            "scroll_up" to Spec("scroll_up", "Scroll Up.", listOf(ParamSpec("amount", Int::class, ""))) { ScrollUp(it["amount"] as Int) },
//
//            "back" to Spec("back", "Go Back.", emptyList()) { Back },
//            "home" to Spec("home", "Go Home.", emptyList()) { Home },
//            "switch_app" to Spec("switch_app", "App Switcher.", emptyList()) { SwitchApp },
//            "open_app" to Spec("open_app", "Open App.", listOf(ParamSpec("app_name", String::class, ""))) { OpenApp(it["app_name"] as String) },
//            "launch_intent" to Spec("launch_intent", "Launch Intent.", listOf(ParamSpec("intent_name", String::class, ""), ParamSpec("parameters", Map::class, ""))) { @Suppress("UNCHECKED_CAST") LaunchIntent(it["intent_name"] as String, it["parameters"] as Map<String, String>) },
//            "search_google" to Spec("search_google", "Google Search.", listOf(ParamSpec("query", String::class, ""))) { SearchGoogle(it["query"] as String) },
//
//            "wait" to Spec("wait", "Wait.", emptyList()) { Wait },
//            "done" to Spec("done", "Task Done.", listOf(ParamSpec("success", Boolean::class, ""), ParamSpec("text", String::class, ""))) { Done(it["success"] as Boolean, it["text"] as String) }
//        )
//
//        fun getAllSpecs(): Collection<Spec> = allSpecs.values
//    }
//}

package com.example.myapplication

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*
import kotlin.reflect.KClass

data class ParamSpec(val name: String, val type: KClass<*>, val description: String)

@Serializable(with = Action.ActionSerializer::class)
sealed class Action {
    // --- UI ACTIONS ---
    data class TapElement(val elementId: Int) : Action()
    data class LongPressElement(val elementId: Int) : Action()
    data class InputText(val text: String) : Action()
    data class TapElementInputTextPressEnter(val index: Int, val text: String) : Action()
    data class ScrollDown(val amount: Int) : Action()
    data class ScrollUp(val amount: Int) : Action()

    // --- SYSTEM ACTIONS ---
    data object Back : Action()
    data object Home : Action()
    data object SwitchApp : Action()
    data class OpenApp(val appName: String) : Action()
    data class LaunchIntent(val intentName: String, val parameters: Map<String, String>) : Action()
    data class SearchGoogle(val query: String) : Action()

    // --- FILE SYSTEM ACTIONS ---
    data class WriteFile(val fileName: String, val text: String) : Action()
    data class AppendFile(val fileName: String, val text: String) : Action()
    data class ReadFile(val fileName: String) : Action() // Added based on your usage

    // --- AGENT INTERACTION ACTIONS (Fixing your error) ---
    data class Speak(val text: String) : Action()
    data class Ask(val question: String) : Action()

    // --- FLOW CONTROL ---
    data object Wait : Action()
    data class Done(val success: Boolean, val text: String) : Action()

    // --- ERROR STATE ---
    data class Error(val reason: String) : Action()

    // --- SERIALIZER ---
    object ActionSerializer : KSerializer<Action> {
        override val descriptor: SerialDescriptor = buildClassSerialDescriptor("Action")

        override fun serialize(encoder: Encoder, value: Action) {
            throw NotImplementedError("Serialization not supported")
        }

        override fun deserialize(decoder: Decoder): Action {
            try {
                val jsonInput = (decoder as JsonDecoder).decodeJsonElement().jsonObject

                // Handle different JSON structures (wrapped vs flat)
                val root = if (jsonInput.keys.size == 1 && allSpecs.containsKey(jsonInput.keys.first())) {
                    jsonInput
                } else {
                    jsonInput
                }

                if (root.isEmpty()) return Error("Empty JSON")

                val actionName = root.keys.first()
                val paramsJson = root[actionName]?.jsonObject

                val spec = allSpecs[actionName] ?: return Error("Unknown action: $actionName")

                val args = mutableMapOf<String, Any?>()
                paramsJson?.let {
                    for (paramSpec in spec.params) {
                        val jsonValue = it[paramSpec.name] ?: continue
                        val value = when (paramSpec.type) {
                            Int::class -> jsonValue.jsonPrimitive.int
                            String::class -> jsonValue.jsonPrimitive.content
                            Boolean::class -> jsonValue.jsonPrimitive.boolean
                            Map::class -> jsonValue.jsonObject.mapValues { e -> e.value.jsonPrimitive.content }
                            else -> ""
                        }
                        args[paramSpec.name] = value
                    }
                }
                return spec.build(args)
            } catch (e: Exception) {
                return Error("Parse Error: ${e.message}")
            }
        }
    }

    companion object {
        data class Spec(
            val name: String,
            val description: String,
            val params: List<ParamSpec>,
            val build: (args: Map<String, Any?>) -> Action
        )

        val allSpecs: Map<String, Spec> = mapOf(
            "tap_element" to Spec("tap_element", "Tap ID.", listOf(ParamSpec("element_id", Int::class, ""))) { TapElement(it["element_id"] as Int) },
            "long_press_element" to Spec("long_press_element", "Long Press ID.", listOf(ParamSpec("element_id", Int::class, ""))) { LongPressElement(it["element_id"] as Int) },
            "input_text" to Spec("input_text", "Type text.", listOf(ParamSpec("text", String::class, ""))) { InputText(it["text"] as String) },
            "tap_element_input_text_press_enter" to Spec("tap_element_input_text_press_enter", "Tap, Type, Enter.", listOf(ParamSpec("index", Int::class, ""), ParamSpec("text", String::class, ""))) { TapElementInputTextPressEnter(it["index"] as Int, it["text"] as String) },
            "scroll_down" to Spec("scroll_down", "Scroll Down.", listOf(ParamSpec("amount", Int::class, ""))) { ScrollDown(it["amount"] as Int) },
            "scroll_up" to Spec("scroll_up", "Scroll Up.", listOf(ParamSpec("amount", Int::class, ""))) { ScrollUp(it["amount"] as Int) },

            "back" to Spec("back", "Go Back.", emptyList()) { Back },
            "home" to Spec("home", "Go Home.", emptyList()) { Home },
            "switch_app" to Spec("switch_app", "App Switcher.", emptyList()) { SwitchApp },
            "open_app" to Spec("open_app", "Open App.", listOf(ParamSpec("app_name", String::class, ""))) { OpenApp(it["app_name"] as String) },
            "launch_intent" to Spec("launch_intent", "Launch Intent.", listOf(ParamSpec("intent_name", String::class, ""), ParamSpec("parameters", Map::class, ""))) { @Suppress("UNCHECKED_CAST") LaunchIntent(it["intent_name"] as String, it["parameters"] as Map<String, String>) },
            "search_google" to Spec("search_google", "Google Search.", listOf(ParamSpec("query", String::class, ""))) { SearchGoogle(it["query"] as String) },

            // --- FILE SPECS ---
            "write_file" to Spec("write_file", "Write file.", listOf(ParamSpec("file_name", String::class, ""), ParamSpec("text", String::class, ""))) { WriteFile(it["file_name"] as String, it["text"] as String) },
            "append_file" to Spec("append_file", "Append file.", listOf(ParamSpec("file_name", String::class, ""), ParamSpec("text", String::class, ""))) { AppendFile(it["file_name"] as String, it["text"] as String) },
            "read_file" to Spec("read_file", "Read file.", listOf(ParamSpec("file_name", String::class, ""))) { ReadFile(it["file_name"] as String) },

            // --- INTERACTION SPECS ---
            "speak" to Spec("speak", "Speak text.", listOf(ParamSpec("text", String::class, ""))) { Speak(it["text"] as String) },
            "ask" to Spec("ask", "Ask user.", listOf(ParamSpec("question", String::class, ""))) { Ask(it["question"] as String) },

            "wait" to Spec("wait", "Wait.", emptyList()) { Wait },
            "done" to Spec("done", "Task Done.", listOf(ParamSpec("success", Boolean::class, ""), ParamSpec("text", String::class, ""))) { Done(it["success"] as Boolean, it["text"] as String) }
        )

        fun getAllSpecs(): Collection<Spec> = allSpecs.values
    }
}
//package com.example.myapplication
//
//import android.content.Intent
//import android.os.Bundle
//import android.provider.Settings
//import android.widget.Button
//import android.widget.EditText
//import androidx.appcompat.app.AppCompatActivity
//import com.example.myapplication.NavigationService
//import com.example.myapplication.PreferenceManager
//
//class MainActivity : AppCompatActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        setContentView(R.layout.activity_main)
//
//        val input = findViewById<EditText>(R.id.goalInput)
//        val startBtn = findViewById<Button>(R.id.startBtn)
//        val permBtn = findViewById<Button>(R.id.permissionBtn)
//
//        permBtn.setOnClickListener {
//            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
//        }
//
//        startBtn.setOnClickListener {
//            NavigationService.currentGoal = input.text.toString()
//            moveTaskToBack(true) // Minimize app
//        }
//    }
//}

package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.NavigationService
// import com.example.myapplication.data.PreferenceManager // <--- Uncomment if you put PreferenceManager in a sub-package

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. Initialize PreferenceManager
        val prefs = PreferenceManager(this)

        // 2. Check if API Key is missing on launch
        if (prefs.getApiKey().isNullOrEmpty()) {
            showApiKeyDialog(prefs)
        }

        val input = findViewById<EditText>(R.id.goalInput)
        val startBtn = findViewById<Button>(R.id.startBtn)
        val permBtn = findViewById<Button>(R.id.permissionBtn)

        permBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        startBtn.setOnClickListener {
            // 3. Prevent starting if key is still missing
            if (prefs.getApiKey().isNullOrEmpty()) {
                Toast.makeText(this, "API Key required!", Toast.LENGTH_SHORT).show()
                showApiKeyDialog(prefs)
                return@setOnClickListener
            }

            NavigationService.currentGoal = input.text.toString()
            moveTaskToBack(true) // Minimize app
        }
    }

    /**
     * Shows a popup dialog forcing the user to enter their API Key.
     */
    private fun showApiKeyDialog(prefs: PreferenceManager) {
        val inputField = EditText(this)
        inputField.hint = "Paste your Gemini API Key here"
        // Add some padding to the input field so it looks nice
        inputField.setPadding(50, 50, 50, 50)

        AlertDialog.Builder(this)
            .setTitle("Setup Required")
            .setMessage("To use Phone AI, you must provide a Google Gemini API Key.")
            .setView(inputField)
            .setCancelable(false) // User cannot click away; they MUST enter a key
            .setPositiveButton("Save") { _, _ ->
                val key = inputField.text.toString().trim()
                if (key.isNotEmpty()) {
                    prefs.saveApiKey(key)
                    Toast.makeText(this, "Key saved successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Key cannot be empty.", Toast.LENGTH_LONG).show()
                    showApiKeyDialog(prefs) // Show it again if they failed
                }
            }
            .setNegativeButton("Exit") { _, _ ->
                finish() // Close app if they refuse
            }
            .show()
    }
}
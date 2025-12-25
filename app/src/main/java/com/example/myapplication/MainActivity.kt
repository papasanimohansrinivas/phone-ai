package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.NavigationService

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val input = findViewById<EditText>(R.id.goalInput)
        val startBtn = findViewById<Button>(R.id.startBtn)
        val permBtn = findViewById<Button>(R.id.permissionBtn)

        permBtn.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        startBtn.setOnClickListener {
            NavigationService.currentGoal = input.text.toString()
            moveTaskToBack(true) // Minimize app
        }
    }
}
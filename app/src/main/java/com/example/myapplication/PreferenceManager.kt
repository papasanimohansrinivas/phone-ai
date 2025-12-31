package com.example.myapplication
import android.content.Context
import android.content.SharedPreferences

class PreferenceManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveApiKey(key: String) {
        prefs.edit().putString("gemini_api_key", key).apply()
    }

    fun getApiKey(): String? {
        return prefs.getString("gemini_api_key", null)
    }

    fun isApiKeySet(): Boolean {
        return !getApiKey().isNullOrEmpty()
    }
}
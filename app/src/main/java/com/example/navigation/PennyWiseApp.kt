package com.example.navigation

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class PennyWiseApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Apply dark mode preference globally at startup
        val prefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("DARK_MODE", false)
        
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}

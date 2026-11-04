package com.example.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class ManualActivity : BaseActivity() {

    private lateinit var sideMenuDrawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_manual)

        // Setting up the side menu.
        sideMenuDrawer = findViewById(R.id.drawer_layout)
        val sideNavigationView = findViewById<NavigationView>(R.id.nav_view)
        val menuIconButton = findViewById<ImageView>(R.id.btn_menu)

        menuIconButton.setOnClickListener {
            sideMenuDrawer.openDrawer(GravityCompat.START)
        }

        // side menu navigation.
        sideNavigationView.setNavigationItemSelectedListener { item ->
            val destination = when (item.itemId) {
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                R.id.nav_reports -> Intent(this, ReportActivity::class.java)
                R.id.nav_settings -> Intent(this, SettingsActivity::class.java)
                R.id.nav_about -> Intent(this, AboutActivity::class.java)
                R.id.nav_manual -> null // already here
                R.id.nav_logout -> {
                    onLogoutClick(View(this))
                    null
                }
                else -> null
            }
            
            destination?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(it)
            }
            sideMenuDrawer.closeDrawer(GravityCompat.START)
            true
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Bottom navigation setup.
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = 0 
        
        bottomNav.setOnItemSelectedListener {
            val intent = when (it.itemId) {
                R.id.nav_home -> Intent(this, ThirdActivity::class.java)
                R.id.nav_add -> Intent(this, BudgetActivity::class.java)
                R.id.nav_add_expense -> Intent(this, ForthActivity::class.java)
                R.id.nav_progress -> Intent(this, ProgressActivity::class.java)
                else -> null
            }
            
            intent?.let { i ->
                i.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(i)
                finish()
                true
            } ?: false
        }
    }

    fun onLogoutClick(view: View) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPref.edit().putBoolean("IS_LOGGED_IN", false).apply()
        val intent = Intent(this, SecondActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    fun onBackClick(view: View) {
        finish()
    }
}

package com.example.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import android.provider.Settings
import android.content.ComponentName
import android.text.TextUtils
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView

class SettingsActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private lateinit var sideMenuDrawer: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_settings)
        
        dbHelper = DatabaseHelper(this)

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
                R.id.nav_settings -> null // already here
                R.id.nav_about -> Intent(this, AboutActivity::class.java)
                R.id.nav_manual -> Intent(this, ManualActivity::class.java)
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
        bottomNav.selectedItemId = 0 // Settings is not in bottom nav, but we can clear selection or leave as is.
        
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

        setupSpinners()
        setupSwitches()
        setupNavigationLinks()
        setupDataManagement()
    }

    private fun setupSpinners() {
        val prefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)

        // Currency Spinner.
        val spinnerCurrency = findViewById<Spinner>(R.id.spinnerCurrency)
        val currencies = arrayOf("ZAR (R)", "USD ($)", "EUR (€)", "GBP (£)")
        val currencyAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, currencies)
        currencyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCurrency.adapter = currencyAdapter
        
        val savedCurrency = prefs.getString("CURRENCY", "ZAR (R)")
        spinnerCurrency.setSelection(currencies.indexOf(savedCurrency))
        
        spinnerCurrency.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("CURRENCY", currencies[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Start Date Spinner.
        val spinnerStartDate = findViewById<Spinner>(R.id.spinnerStartDate)
        val days = (1..31).map { it.toString() }.toTypedArray()
        val dateAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, days)
        dateAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStartDate.adapter = dateAdapter
        
        val savedDay = prefs.getString("START_DAY", "1")
        spinnerStartDate.setSelection(days.indexOf(savedDay))

        spinnerStartDate.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                prefs.edit().putString("START_DAY", days[position]).apply()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupSwitches() {
        val prefs = getSharedPreferences("SettingsPrefs", MODE_PRIVATE)
        
        val switchDarkMode = findViewById<SwitchCompat>(R.id.switchDarkMode)
        val isDark = prefs.getBoolean("DARK_MODE", false)
        
        // Set the initial state without triggering any listener logic.
        switchDarkMode.isChecked = isDark
        
        // Use setOnClickListener instead of setOnCheckedChangeListener.
        switchDarkMode.setOnClickListener {
            val isChecked = switchDarkMode.isChecked
            prefs.edit().putBoolean("DARK_MODE", isChecked).apply()
            
            val targetMode = if (isChecked) {
                AppCompatDelegate.MODE_NIGHT_YES
            } else {
                AppCompatDelegate.MODE_NIGHT_NO
            }
            
            if (AppCompatDelegate.getDefaultNightMode() != targetMode) {
                AppCompatDelegate.setDefaultNightMode(targetMode)
            }
        }

        val switchNotifications = findViewById<SwitchCompat>(R.id.switchNotifications)
        switchNotifications.isChecked = isNotificationServiceEnabled()
        
        switchNotifications.setOnClickListener {
            if (!isNotificationServiceEnabled()) {
                // Redirect user to system settings to enable the listener
                val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                startActivity(intent)
                Toast.makeText(this, "Please enable PennyWise in the list", Toast.LENGTH_LONG).show()
            } else {
                // If already enabled, we just toggle the preference
                prefs.edit().putBoolean("NOTIFICATIONS", switchNotifications.isChecked).apply()
            }
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (!TextUtils.isEmpty(flat)) {
            val names = flat.split(":".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            for (name in names) {
                val cn = ComponentName.unflattenFromString(name)
                if (cn != null && TextUtils.equals(pkgName, cn.packageName)) {
                    return true
                }
            }
        }
        return false
    }

    private fun setupNavigationLinks() {
        findViewById<TextView>(R.id.tvEditProfile).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        
        findViewById<TextView>(R.id.tvAbout).setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun setupDataManagement() {
        findViewById<Button>(R.id.btnClearData).setOnClickListener {
            showClearDataConfirmation()
        }
    }

    private fun showClearDataConfirmation() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_clear_title))
            .setMessage(getString(R.string.dialog_clear_message))
            .setPositiveButton(getString(R.string.btn_delete_everything)) { _, _ ->
                val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
                val tvLoadingMessage = findViewById<TextView>(R.id.tvLoadingMessage)
                
                tvLoadingMessage.text = getString(R.string.loading_clearing_data)
                loadingOverlay.visibility = View.VISIBLE

                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    dbHelper.clearAllExpenses()
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, getString(R.string.toast_data_cleared), Toast.LENGTH_SHORT).show()
                }, 800)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
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

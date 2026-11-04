package com.example.navigation

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.content.ContextCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.android.material.textfield.TextInputEditText
import android.util.Log

class ProfileActivity : BaseActivity() {

    private val TAG = "ProfileActivity"
    private lateinit var ivProfileImage: ImageView
    private lateinit var etUsername: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var tvXpLevel: TextView
    private lateinit var tvXpValue: TextView
    private lateinit var pbXp: ProgressBar
    private lateinit var sideMenuDrawer: DrawerLayout
    private var selectedImageUri: Uri? = null

    // Register the photo picker activity launcher.
    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            ivProfileImage.setImageURI(uri)
            
            // Request persistable URI permission to ensure access after app restarts.
            val flag = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(uri, flag)
        }
    }

    private lateinit var layoutAchievementReceipt: LinearLayout
    private lateinit var layoutAchievementBudget: LinearLayout
    private lateinit var layoutAchievementTracker: LinearLayout
    private lateinit var ivAchievementReceipt: ImageView
    private lateinit var ivAchievementBudget: ImageView
    private lateinit var ivAchievementTracker: ImageView
    private lateinit var tvAchievementReceipt: TextView
    private lateinit var tvAchievementBudget: TextView
    private lateinit var tvAchievementTracker: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Profile")
        enableEdgeToEdge()
        setContentView(R.layout.activity_profile)

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
                R.id.nav_profile -> null // already here
                R.id.nav_reports -> Intent(this, ReportActivity::class.java)
                R.id.nav_settings -> Intent(this, SettingsActivity::class.java)
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
        bottomNav.selectedItemId = -1 // Clear selection since Profile is not in the bottom nav
        
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

        // Initialize views.
        ivProfileImage = findViewById(R.id.iv_profile_image)
        etUsername = findViewById(R.id.et_username)
        etEmail = findViewById(R.id.et_email)
        etPassword = findViewById(R.id.et_password)
        tvXpLevel = findViewById(R.id.tv_xp_level)
        tvXpValue = findViewById(R.id.tv_xp_value)
        pbXp = findViewById(R.id.pb_xp)
        layoutAchievementReceipt = findViewById(R.id.layout_achievement_receipt)
        layoutAchievementBudget = findViewById(R.id.layout_achievement_budget)
        layoutAchievementTracker = findViewById(R.id.layout_achievement_tracker)
        ivAchievementReceipt = findViewById(R.id.iv_achievement_receipt)
        ivAchievementBudget = findViewById(R.id.iv_achievement_budget)
        ivAchievementTracker = findViewById(R.id.iv_achievement_tracker)
        tvAchievementReceipt = findViewById(R.id.tv_achievement_receipt)
        tvAchievementBudget = findViewById(R.id.tv_achievement_budget)
        tvAchievementTracker = findViewById(R.id.tv_achievement_tracker)

        val fabEditPhoto = findViewById<FloatingActionButton>(R.id.fab_edit_photo)
        val btnSave = findViewById<Button>(R.id.btn_save_profile)
        val loadingOverlay = findViewById<RelativeLayout>(R.id.loadingOverlay)

        // Load existing data.
        val userPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val currentName = userPref.getString("REG_USER", "")
        val currentEmail = userPref.getString("REG_EMAIL", "user@example.com")
        val currentImageUri = userPref.getString("PROFILE_IMAGE_URI", null)

        etUsername.setText(currentName)
        etEmail.setText(currentEmail)
        
        if (currentImageUri != null) {
            try {
                ivProfileImage.setImageURI(Uri.parse(currentImageUri))
            } catch (e: Exception) {
                e.printStackTrace()
                //default image.
                ivProfileImage.setImageResource(R.drawable.pennywise5)
            }
        }

        updateXpDisplay()

        // Set up photo picker.
        fabEditPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Save logic.
        btnSave.setOnClickListener {
            val newName = etUsername.text.toString().trim()
            val newEmail = etEmail.text.toString().trim()
            val newPassword = etPassword.text.toString().trim()

            if (newName.isEmpty()) {
                etUsername.error = "Username cannot be empty"
                return@setOnClickListener
            }

            // Validate Username (Letters only).
            if (!newName.matches(Regex("^[a-zA-Z]+$"))) {
                etUsername.error = "Username must contain only letters"
                return@setOnClickListener
            }

            // Validate Password (only if the user is trying to change it).
            if (newPassword.isNotEmpty()) {
                val passwordPattern = Regex("^(?=.*[a-zA-Z])(?=.*\\d)(?=.*[^a-zA-Z0-9]).+$")
                if (!newPassword.matches(passwordPattern)) {
                    etPassword.error = "Password must include letters, numbers, and special characters"
                    return@setOnClickListener
                }
            }

            // Validate Email format.
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                etEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            loadingOverlay.visibility = View.VISIBLE
            Log.d(TAG, "Saving profile changes for user: $newName")

            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                val editor = userPref.edit()
                editor.putString("REG_USER", newName)
                editor.putString("REG_EMAIL", newEmail)
                
                if (newPassword.isNotEmpty()) {
                    editor.putString("REG_PASS", newPassword)
                }
                
                if (selectedImageUri != null) {
                    editor.putString("PROFILE_IMAGE_URI", selectedImageUri.toString())
                }
                
                editor.apply()
                loadingOverlay.visibility = View.GONE
                Log.i(TAG, "Profile updated successfully")
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
            }, 600)
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

    private fun updateXpDisplay() {
        val userStats = getSharedPreferences("UserData", MODE_PRIVATE)
        val xp = userStats.getInt("xp", 0)
        
        // Thresholds: 10 (Novice), 100 (Saver), 200 (Strategist), 500 (Wealth Builder), 1000 (Generational Wealth Builder)
        val levelName = when {
            xp >= 1000 -> getString(R.string.label_generational_wealth_builder)
            xp >= 500 -> getString(R.string.label_wealth_builder)
            xp >= 200 -> getString(R.string.label_strategist)
            xp >= 100 -> getString(R.string.label_saver)
            xp >= 10 -> getString(R.string.label_financial_novice)
            else -> "Financial Apprentice"
        }

        tvXpLevel.text = levelName
        
        tvXpValue.text = getString(R.string.label_current_xp, xp, 1000)
        
        if (xp >= 1000) {
            pbXp.progress = 100
        } else {
            // XP progress relative to the 1000 XP total goal
            val totalProgress = (xp.toFloat() / 1000f * 100).toInt()
            pbXp.max = 100
            pbXp.progress = totalProgress
        }

        // --- Milestone Badge Logic ---
        val db = DatabaseHelper(this)
        val financePrefs = getSharedPreferences("FinancePrefs", MODE_PRIVATE)
        
        // 1. Receipt Ninja: Logged 5 photo receipts
        val receiptCount = db.getCountWithReceipt()
        updateBadgeUI(receiptCount >= 5, ivAchievementReceipt, tvAchievementReceipt)

        // 2. Budget Warrior: Set a monthly spending limit
        val hasBudget = financePrefs.contains("MAX_BUDGET") && financePrefs.getFloat("MAX_BUDGET", 0f) > 0
        updateBadgeUI(hasBudget, ivAchievementBudget, tvAchievementBudget)

        // 3. Consistent Tracker: Logged 10+ transactions
        val totalTransactions = db.getCountTotal()
        updateBadgeUI(totalTransactions >= 10, ivAchievementTracker, tvAchievementTracker)
    }

    private fun updateBadgeUI(isUnlocked: Boolean, icon: ImageView, text: TextView) {
        // Force fully visible badges as requested
        icon.alpha = 1.0f
        icon.clearColorFilter()
        if (isUnlocked) {
            text.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        } else {
            text.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
        }
    }
}

package com.example.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale


class BudgetActivity : AppCompatActivity() {

    // A function to make the numbers look like currency and not random numbers
    private fun formatCurrency(amount: Float): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,##0.00", symbols)
        return formatter.format(amount)
    }

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        

        enableEdgeToEdge()
        setContentView(R.layout.activity_budget)

        // Setting up our sidebar navigation for users convinience
        drawerLayout = findViewById(R.id.drawer_layout)
        val sideNavigationView = findViewById<NavigationView>(R.id.nav_view)
        val hamburgerMenuIcon = findViewById<ImageView>(R.id.btn_menu)

        hamburgerMenuIcon.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.START)
        }

        sideNavigationView.setNavigationItemSelectedListener { menuItem ->
            val destinationIntent = when (menuItem.itemId) {
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                R.id.nav_reports -> Intent(this, ReportActivity::class.java)
                R.id.nav_settings -> Intent(this, SettingsActivity::class.java)
                R.id.nav_about -> Intent(this, AboutActivity::class.java)
                R.id.nav_manual -> Intent(this, ManualActivity::class.java)
                R.id.nav_logout -> {
                    logoutUser()
                    null
                }
                else -> null
            }
            
            destinationIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(it)
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // making sure the UI elements don't clash with the status and navigation bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // Connecting input fields for Income and Budget limits
        val incomeInput = findViewById<EditText>(R.id.etPocket)
        val minBudgetInput = findViewById<EditText>(R.id.etMinBudget)
        val maxBudgetInput = findViewById<EditText>(R.id.etMaxBudget)
        val saveSettingsButton = findViewById<Button>(R.id.btnSaveBudget)

        // Loading saved preferences to see what the user already set
        val financialPrefs = getSharedPreferences("FinancePrefs", MODE_PRIVATE)
        val savedIncome = financialPrefs.getFloat("BUDGET", 0f)
        val savedMinBudget = financialPrefs.getFloat("MIN_BUDGET", 0f)
        val savedMaxBudget = financialPrefs.getFloat("MAX_BUDGET", 0f)

        // showing saved values in the correct format
        if (savedIncome > 0) incomeInput.setText(formatCurrency(savedIncome))
        if (savedMinBudget > 0) minBudgetInput.setText(formatCurrency(savedMinBudget))
        if (savedMaxBudget > 0) maxBudgetInput.setText(formatCurrency(savedMaxBudget))

        // stripping the format for when the user clicks an input
        val allInputs = listOf(incomeInput, minBudgetInput, maxBudgetInput)
        for (input in allInputs) {
            input.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val rawValue = input.text.toString().replace(" ", "").replace(",", ".")
                    input.setText(rawValue)
                }
            }
        }

        // taking the user's input and storing it locally
        saveSettingsButton.setOnClickListener {
            val incomeStr = incomeInput.text.toString().trim().replace(" ", "").replace(",", ".")
            val minStr = minBudgetInput.text.toString().trim().replace(" ", "").replace(",", ".")
            val maxStr = maxBudgetInput.text.toString().trim().replace(" ", "").replace(",", ".")


            if (incomeStr.isEmpty()) {
                Toast.makeText(this, "Wait! We need to know your income first.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val incomeValue = incomeStr.toFloatOrNull() ?: 0f
            val minBudgetValue = minStr.toFloatOrNull() ?: 0f
            val maxBudgetValue = maxStr.toFloatOrNull() ?: 0f

            // income can't be zero or negative
            if (incomeValue <= 0) {
                Toast.makeText(this, "Please enter a valid amount for your monthly income.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // logic Check: Does the minimum budget make sense compared to the maximum?
            if (minBudgetValue > 0f || maxBudgetValue > 0f) {
                if (maxBudgetValue == 0f) {
                    Toast.makeText(this, "If you set a minimum, you'll need a maximum too!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (minBudgetValue >= maxBudgetValue) {
                    Toast.makeText(this, "Your minimum budget should be lower than your maximum.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Storing the data so the rest of the app can use it
            financialPrefs.edit()
                .putFloat("BUDGET", incomeValue)
                .putFloat("MIN_BUDGET", minBudgetValue)
                .putFloat("MAX_BUDGET", maxBudgetValue)
                .apply()

            // Award XP for being proactive with budget goals
            val userGameStats = getSharedPreferences("UserData", MODE_PRIVATE)
            val currentXP = userGameStats.getInt("xp", 0)
            userGameStats.edit().putInt("xp", currentXP + 50).apply()

            Toast.makeText(this, "Goals locked in! You earned +50 XP.", Toast.LENGTH_SHORT).show()

            // navigation back to the main dashboard
            val backToDashboard = Intent(this, ThirdActivity::class.java)
            backToDashboard.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            startActivity(backToDashboard)
            finish()
        }

        // bottom navigation buttons
        val bottomNavigationBar = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationBar.selectedItemId = R.id.nav_add 

        bottomNavigationBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val homeIntent = Intent(this, ThirdActivity::class.java)
                    homeIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(homeIntent)
                    finish()
                    true
                }
                R.id.nav_add -> true
                R.id.nav_progress -> {
                    val progressIntent = Intent(this, ProgressActivity::class.java)
                    progressIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(progressIntent)
                    finish()
                    true
                }
                R.id.nav_add_expense -> {
                    val addExpenseIntent = Intent(this, ForthActivity::class.java)
                    addExpenseIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(addExpenseIntent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    // logout button. clearing the session and returning back to the login screen.
    private fun logoutUser() {
        val userAuthPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        userAuthPrefs.edit().putBoolean("IS_LOGGED_IN", false).apply()

        val loginIntent = Intent(this, SecondActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)
        finish()
    }
    

    fun login_page(view: View?) {
        logoutUser()
    }
}

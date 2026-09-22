package com.example.navigation

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import java.util.*

class ForthActivity : AppCompatActivity() {

    private var selectedReceiptUri: Uri? = null
    private lateinit var receiptPreviewImage: ImageView
    private lateinit var sideDrawerLayout: DrawerLayout

    // picking an image from the phone's gallery.
    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            selectedReceiptUri = it
            // "lock in" permission to read this file later if the app restarts.
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                e.printStackTrace()
            }
            // Showing the user a tiny preview of their receipt.
            receiptPreviewImage.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // making the app look modern by drawing behind the system bars
        enableEdgeToEdge()
        setContentView(R.layout.activity_forth)

        sideDrawerLayout = findViewById(R.id.drawer_layout)
        val sideNavView = findViewById<NavigationView>(R.id.nav_view)
        val menuToggleIcon = findViewById<ImageView>(R.id.btn_menu)

        menuToggleIcon.setOnClickListener {
            sideDrawerLayout.openDrawer(GravityCompat.START)
        }

        // side menu navigation.
        sideNavView.setNavigationItemSelectedListener { item ->
            val targetIntent = when (item.itemId) {
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                R.id.nav_reports -> Intent(this, ReportActivity::class.java)
                R.id.nav_settings -> Intent(this, SettingsActivity::class.java)
                R.id.nav_about -> Intent(this, AboutActivity::class.java)
                R.id.nav_manual -> Intent(this, ManualActivity::class.java)
                R.id.nav_logout -> {
                    processUserLogout()
                    null
                }
                else -> null
            }
            
            targetIntent?.let {
                it.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                startActivity(it)
            }
            sideDrawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // Adjusting layout padding.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // declaring all our input fields.
        val amountInputField = findViewById<EditText>(R.id.etAmount)
        val descriptionInputField = findViewById<EditText>(R.id.etDescription)
        val categoryDropdown = findViewById<Spinner>(R.id.spCategory)
        val customCategoryContainer = findViewById<View>(R.id.cardCustomCategory)
        val customCategoryInput = findViewById<EditText>(R.id.etCustomCategory)
        val dateInputField = findViewById<EditText>(R.id.etDate)
        val startTimeInput = findViewById<EditText>(R.id.etStartTime)
        val endTimeInput = findViewById<EditText>(R.id.etEndTime)
        val uploadReceiptButton = findViewById<Button>(R.id.btnUpload)
        receiptPreviewImage = findViewById(R.id.imgReceipt)
        val saveExpenseButton = findViewById<Button>(R.id.btnSave)

        // Populating the category list
        val expenseCategories = mutableListOf("Food", "Transport", "Bills", "Shopping", "Other")
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, expenseCategories)
        categoryDropdown.adapter = categoryAdapter

        // custom category for when the user selects "other" as category.
        categoryDropdown.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                customCategoryContainer.visibility = if (expenseCategories[position] == "Other") View.VISIBLE else View.GONE
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // calendar pop-up for expense entry.
        dateInputField.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                dateInputField.setText(String.format("%02d/%02d/%d", day, month + 1, year))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        // time picker for expense entry.
        fun pickTime(targetField: EditText) {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this, { _, hour, minute ->
                targetField.setText(String.format("%02d:%02d", hour, minute))
            }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show()
        }

        startTimeInput.setOnClickListener { pickTime(startTimeInput) }
        endTimeInput.setOnClickListener { pickTime(endTimeInput) }

        uploadReceiptButton.setOnClickListener {
            imagePickerLauncher.launch(arrayOf("image/*"))
        }

        // save button for an expense entry.
        saveExpenseButton.setOnClickListener {
            val amountText = amountInputField.text.toString()
            if (amountText.isEmpty()) {
                Toast.makeText(this, "Wait, how much did you spend?", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val spentAmount = amountText.toFloat()

            // Checking if the user has enough money left in their "Pocket".
            val financeStorage = getSharedPreferences("FinancePrefs", MODE_PRIVATE)
            val monthlyBudget = financeStorage.getFloat("BUDGET", 0f)
            val database = DatabaseHelper(this)
            val totalAlreadySpent = database.getTotalExpenses()
            val remainingBalance = monthlyBudget - totalAlreadySpent

            if (spentAmount > remainingBalance) {
                Toast.makeText(this, "Oops! You only have ${String.format("%.2f", remainingBalance)} left in your budget.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // Figure out the final category name.
            var finalCategory = categoryDropdown.selectedItem.toString()
            if (finalCategory == "Other") {
                finalCategory = customCategoryInput.text.toString().ifEmpty { "Other" }
            }

            // Convert the date to a database-friendly format.
            val rawDate = dateInputField.text.toString()
            val formattedDbDate = if (rawDate.isNotEmpty()) {
                val segments = rawDate.split("/")
                if (segments.size == 3) "${segments[2]}-${segments[1]}-${segments[0]}" else rawDate
            } else {
                java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }

            // Save the data to our local SQLite database.
            database.addExpense(
                spentAmount,
                finalCategory,
                descriptionInputField.text.toString(),
                formattedDbDate,
                startTimeInput.text.toString(),
                endTimeInput.text.toString(),
                selectedReceiptUri?.toString() ?: ""
            )

            // Update our quick-access total expense counter.
            financeStorage.edit()
                .putFloat("TOTAL_EXPENSE", financeStorage.getFloat("TOTAL_EXPENSE", 0f) + spentAmount)
                .apply()

            // Rewarding the user.
            val userGameData = getSharedPreferences("UserData", MODE_PRIVATE)
            userGameData.edit()
                .putInt("xp", userGameData.getInt("xp", 0) + 10)
                .apply()

            Toast.makeText(this, "Logged! +10 XP earned.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Set up the bottom bar so it knows we're on the "Add Expense" screen
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_add_expense

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> navigateAway(ThirdActivity::class.java)
                R.id.nav_add -> navigateAway(BudgetActivity::class.java)
                R.id.nav_progress -> navigateAway(ProgressActivity::class.java)
                R.id.nav_add_expense -> true
                else -> false
            }
        }
    }

    private fun navigateAway(activity: Class<*>) : Boolean {
        val intent = Intent(this, activity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        finish()
        return true
    }

    private fun processUserLogout() {
        val authPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        authPrefs.edit().putBoolean("IS_LOGGED_IN", false).apply()

        val loginIntent = Intent(this, SecondActivity::class.java)
        loginIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(loginIntent)
        finish()
    }
    
    // click listener for login button.
    fun login_page(view: View?) {
        processUserLogout()
    }
}

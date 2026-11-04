package com.example.navigation

import android.util.Log
import android.graphics.Color
import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.*
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject


class ThirdActivity : BaseActivity() {

    // Logging tag for debugging and lifecycle tracking
    private val TAG = "ThirdActivity"

    // These variables connect to the text on the screen.
    private lateinit var pocketBalanceDisplay: TextView
    private lateinit var monthlyIncomeDisplay: TextView
    private lateinit var totalSpentDisplay: TextView
    private lateinit var minBudgetDisplay: TextView
    private lateinit var maxBudgetDisplay: TextView
    private lateinit var welcomeUserText: TextView
    private lateinit var transactionListContainer: LinearLayout
    private lateinit var sideMenuDrawer: DrawerLayout
    private lateinit var tvDailyAdvice: TextView
    
    // Progress bar to show monthly budget progression.
    private lateinit var monthlyUsageProgressBar: ProgressBar
    private lateinit var usagePercentageText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: Initializing Dashboard")
        
        // This makes the app content flow nicely under the status bar.
        enableEdgeToEdge()
        setContentView(R.layout.activity_third)

        // Ensure status bar is blue to match our new theme
        window.statusBarColor = ContextCompat.getColor(this, R.color.primary_blue)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.primary_blue)

        // Setting up the side menu.
        sideMenuDrawer = findViewById(R.id.drawer_layout)
        val sideNavigationView = findViewById<NavigationView>(R.id.nav_view)
        val menuIconButton = findViewById<ImageView>(R.id.btn_menu)

        menuIconButton.setOnClickListener {
            Log.d(TAG, "Opening side menu")
            sideMenuDrawer.openDrawer(GravityCompat.START)
        }

        // side menu navigation logic: we switch activities based on user selection.
        sideNavigationView.setNavigationItemSelectedListener { item ->
            Log.d(TAG, "Side nav item clicked: ${item.title}")
            val destination = when (item.itemId) {
                R.id.nav_profile -> Intent(this, ProfileActivity::class.java)
                R.id.nav_reports -> Intent(this, ReportActivity::class.java)
                R.id.nav_settings -> Intent(this, SettingsActivity::class.java)
                R.id.nav_about -> Intent(this, AboutActivity::class.java)
                R.id.nav_manual -> Intent(this, ManualActivity::class.java)
                R.id.nav_logout -> {
                    signOutUser()
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

        // Adjusting padding.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(bars.left, bars.top, bars.right, 0)
            insets
        }

        // Linking code to the actual visual elements in the layout.
        pocketBalanceDisplay = findViewById(R.id.tvPocket)
        monthlyIncomeDisplay = findViewById(R.id.tvIncome)
        totalSpentDisplay = findViewById(R.id.tvExpenses)
        minBudgetDisplay = findViewById(R.id.tvMinBudget)
        maxBudgetDisplay = findViewById(R.id.tvMaxBudget)
        welcomeUserText = findViewById(R.id.tv_username)
        transactionListContainer = findViewById(R.id.transactionsContainer)
        monthlyUsageProgressBar = findViewById(R.id.progressMonthlyUsage)
        usagePercentageText = findViewById(R.id.tvUsagePercent)
        tvDailyAdvice = findViewById(R.id.tvDailyAdvice)

        // Fetch daily advice
        fetchDailyAdvice()

        // bottom navigation.
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
                R.id.nav_add -> navigateTo(BudgetActivity::class.java)
                R.id.nav_add_expense -> navigateTo(ForthActivity::class.java)
                R.id.nav_progress -> navigateTo(ProgressActivity::class.java)
                else -> false
            }
        }
    }

    private fun navigateTo(activity: Class<*>) : Boolean {
        val intent = Intent(this, activity)
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
        return true
    }

    // Every time the user comes back to this screen, refresh the numbers.
    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume: Refreshing dashboard data")
        updateDashboardData()
    }

    // using currency format.
    private fun formatAsCurrency(amount: Double): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = ' '
            decimalSeparator = ','
        }
        val formatter = DecimalFormat("#,##0.00", symbols)
        return "R " + formatter.format(amount)
    }

    // pulling all the latest financial info and displays it.
    private fun updateDashboardData() {
        Log.i(TAG, "Updating Dashboard: Fetching stats from DB and Prefs")
        val userPrefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val username = userPrefs.getString("REG_USER", "Valued User")
        val avatarUriString = userPrefs.getString("PROFILE_IMAGE_URI", null)

        welcomeUserText.text = getString(R.string.msg_welcome_user, username)

        // Update the side menu header with user info.
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val header = navView.getHeaderView(0)
        val headerName = header.findViewById<TextView>(R.id.nav_header_name)
        val headerAvatar = header.findViewById<ImageView>(R.id.nav_header_image)

        headerName.text = username
        if (avatarUriString != null) {
            try {
                headerAvatar.setImageURI(Uri.parse(avatarUriString))
            } catch (e: Exception) {
                headerAvatar.setImageResource(R.drawable.pennywise5)
            }
        }

        // Fetching the spending data from our local database.
        val db = DatabaseHelper(this)
        val totalSpent = db.getTotalExpenses().toDouble()

        // Fetching our goal settings.
        val financePrefs = getSharedPreferences("FinancePrefs", MODE_PRIVATE)
        val income = financePrefs.getFloat("BUDGET", 0f).toDouble()
        val minGoal = financePrefs.getFloat("MIN_BUDGET", 0f).toDouble()
        val maxLimit = financePrefs.getFloat("MAX_BUDGET", 0f).toDouble()
        val leftInPocket = income - totalSpent

        // Pushing the values to the UI.
        pocketBalanceDisplay.text = formatAsCurrency(leftInPocket)
        monthlyIncomeDisplay.text = formatAsCurrency(income)
        totalSpentDisplay.text = formatAsCurrency(totalSpent)
        minBudgetDisplay.text = formatAsCurrency(minGoal)
        maxBudgetDisplay.text = formatAsCurrency(maxLimit)

        // Update the visual progress circles/bars.
        refreshVisualProgress(totalSpent, minGoal, maxLimit)

        // list out the most recent transactions.
        refreshTransactionList()
    }

    private fun refreshVisualProgress(spent: Double, goal: Double, limit: Double) {
        // Main Monthly Usage Progress Bar (Threshold-based coloring)
        if (limit > 0) {
            val usagePercent = ((spent / limit) * 100).toInt()
            monthlyUsageProgressBar.progress = usagePercent.coerceAtMost(100)
            usagePercentageText.text = "$usagePercent%"

            // Threshold logic for colors
            when {
                spent >= limit -> {
                    // Over the max limit - RED
                    monthlyUsageProgressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_red)
                    usagePercentageText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
                }
                spent >= goal -> {
                    // Between Min Goal and Max Limit - YELLOW
                    monthlyUsageProgressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_yellow)
                    usagePercentageText.setTextColor(ContextCompat.getColor(this, R.color.warning_yellow))
                }
                else -> {
                    // Below Min Goal - Use white for consistency with blue card theme
                    monthlyUsageProgressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_white)
                    usagePercentageText.setTextColor(ContextCompat.getColor(this, R.color.white))
                }
            }
        } else {
            monthlyUsageProgressBar.progress = 0
            usagePercentageText.text = "0%"
            monthlyUsageProgressBar.progressDrawable = ContextCompat.getDrawable(this, R.drawable.progress_bar_white)
            usagePercentageText.setTextColor(ContextCompat.getColor(this, R.color.white))
        }
    }

    private fun refreshTransactionList() {
        Log.d(TAG, "Refreshing Transaction List UI")
        val db = DatabaseHelper(this)
        val transactionCursor = db.getAllExpenses()

        transactionListContainer.removeAllViews()

        if (!transactionCursor.moveToFirst()) {
            val emptyNotice = TextView(this).apply {
                text = getString(R.string.msg_no_transactions)
                setPadding(16, 16, 16, 16)
                textAlignment = View.TEXT_ALIGNMENT_CENTER
            }
            transactionListContainer.addView(emptyNotice)
            transactionCursor.close()
            return
        }

        // Loop through everything found in the database.
        do {
            val amount = transactionCursor.getDouble(transactionCursor.getColumnIndexOrThrow("amount"))
            val category = transactionCursor.getString(transactionCursor.getColumnIndexOrThrow("category"))
            val note = transactionCursor.getString(transactionCursor.getColumnIndexOrThrow("description"))
            val receiptPath = transactionCursor.getString(transactionCursor.getColumnIndexOrThrow("receipt_uri"))

            // Create a row for this transaction.
            val transactionRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(32, 32, 32, 32)
                background = android.graphics.drawable.GradientDrawable().apply {
                        setColor(Color.TRANSPARENT)
                        cornerRadius = 16f
                    }
                
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 16)
                layoutParams = params
            }

            val textDetails = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val categoryLabel = TextView(this).apply {
                text = category
                textSize = 16f
                setTextColor(ContextCompat.getColor(this@ThirdActivity, R.color.text_primary))
                setTypeface(null, Typeface.BOLD)
            }
            
            val noteLabel = TextView(this).apply {
                text = note
                textSize = 12f
                setTextColor(ContextCompat.getColor(this@ThirdActivity, R.color.text_secondary))
            }

            textDetails.addView(categoryLabel)
            textDetails.addView(noteLabel)

            val amountLabel = TextView(this).apply {
                text = getString(R.string.label_amount_negative, formatAsCurrency(amount))
                setTextColor(ContextCompat.getColor(this@ThirdActivity, R.color.error_red))
                setTypeface(null, Typeface.BOLD)
                textSize = 16f
            }

            // A button to see the receipt or details.
            val receiptButton = Button(this).apply {
                text = getString(R.string.label_receipt)
                textSize = 10f
                setTextColor(Color.WHITE)
                setBackgroundColor(ContextCompat.getColor(this@ThirdActivity, R.color.primary_blue))
                val btnParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, 100).apply {
                    setMargins(24, 0, 0, 0)
                }
                layoutParams = btnParams
                setPadding(20, 0, 20, 0)
                setOnClickListener {
                    showTransactionDetails(category, amount, note, receiptPath)
                }
            }

            transactionRow.addView(textDetails)
            transactionRow.addView(amountLabel)
            transactionRow.addView(receiptButton)

            transactionListContainer.addView(transactionRow, 0)
            
            // Add a separator view
            val separator = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 1).apply {
                    setMargins(32, 0, 32, 0)
                }
                setBackgroundColor(ContextCompat.getColor(this@ThirdActivity, R.color.gray_light))
            }
            transactionListContainer.addView(separator, 0)
        } while (transactionCursor.moveToNext())
        
        transactionCursor.close()
    }

    private fun showTransactionDetails(category: String, amount: Double, note: String, path: String) {
        val details = "Category: $category\nAmount: ${formatAsCurrency(amount)}\nNote: $note"
        val hasImage = path.isNotEmpty()
        
        val choiceList = if (hasImage) {
            arrayOf(getString(R.string.option_view_summary), getString(R.string.option_open_image), getString(R.string.option_save_file))
        } else {
            arrayOf(getString(R.string.option_view_summary), getString(R.string.option_save_file))
        }

        AlertDialog.Builder(this)
            .setTitle("Manage Transaction")
            .setItems(choiceList) { _, index ->
                when (choiceList[index]) {
                    getString(R.string.option_view_summary) -> {
                        AlertDialog.Builder(this)
                            .setTitle("Details")
                            .setMessage(details)
                            .setPositiveButton("Got it", null)
                            .show()
                    }
                    getString(R.string.option_open_image) -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(Uri.parse(path), "image/*")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, getString(R.string.toast_error_open_image), Toast.LENGTH_SHORT).show()
                        }
                    }
                    getString(R.string.option_save_file) -> {
                        exportReceiptToFile("Receipt_${System.currentTimeMillis()}.txt", details)
                    }
                }
            }
            .show()
    }

    private fun exportReceiptToFile(name: String, text: String) {
        try {
            val file = File(getExternalFilesDir(null), name)
            FileOutputStream(file).use { it.write(text.toByteArray()) }
            Toast.makeText(this, "Saved to: ${file.name}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Error saving the file.", Toast.LENGTH_SHORT).show()
        }
    }

    fun openAddExpensePage(view: View) {
        navigateTo(ForthActivity::class.java)
    }

    private fun signOutUser() {
        val prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        prefs.edit().putBoolean("IS_LOGGED_IN", false).apply()

        val intent = Intent(this, SecondActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    // on click lister for the login button.
    fun login_page(view: View?) {
        signOutUser()
    }

    private fun fetchDailyAdvice() {
        val advicePrefs = getSharedPreferences("DailyAdvicePrefs", MODE_PRIVATE)
        val lastFetchedDate = advicePrefs.getString("last_fetched_date", "")
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = sdf.format(Date())

        if (lastFetchedDate == currentDate) {
            val savedAdvice = advicePrefs.getString("daily_advice", null)
            if (savedAdvice != null) {
                tvDailyAdvice.text = savedAdvice
                return
            }
        }

        Log.d(TAG, "Fetching new daily financial tip from API")
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://api.adviceslip.com/advice")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Advice API call failed", e)
                runOnUiThread {
                    val fallbackAdvice = advicePrefs.getString("daily_advice", getString(R.string.advice_load_error))
                    tvDailyAdvice.text = fallbackAdvice
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val jsonString = response.body?.string()
                Log.d(TAG, "Advice API Response received")
                if (jsonString != null) {
                    try {
                        val json = JSONObject(jsonString)
                        val advice = "\"${json.getJSONObject("slip").getString("advice")}\""
                        runOnUiThread {
                            tvDailyAdvice.text = advice
                            advicePrefs.edit().apply {
                                putString("last_fetched_date", currentDate)
                                putString("daily_advice", advice)
                                apply()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing advice JSON", e)
                        runOnUiThread {
                            tvDailyAdvice.text = getString(R.string.advice_parse_error)
                        }
                    }
                }
            }
        })
    }

    fun openBankFinder(view: View) {
        Log.d(TAG, "Opening ATM finder intent")
        // Use geo intent to find ATMs nearby using OpenStreetMap style query or Google Maps
        // The user specifically mentioned Overpass/OSM, so we can link to an OSM search
        val gmmIntentUri = Uri.parse("geo:0,0?q=ATM")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        // Try to specifically use a map app
        mapIntent.setPackage("com.google.android.apps.maps")
        
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            // Fallback to browser with OpenStreetMap
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.openstreetmap.org/search?query=atm"))
            startActivity(browserIntent)
        }
    }
}

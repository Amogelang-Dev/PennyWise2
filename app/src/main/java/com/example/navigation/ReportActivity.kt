package com.example.navigation

import android.app.DatePickerDialog
import android.content.Intent
import android.database.Cursor
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

class ReportActivity : AppCompatActivity() {

    private lateinit var etFromDate: EditText
    private lateinit var etToDate: EditText
    private lateinit var btnFilter: Button
    private lateinit var breakdownContainer: LinearLayout
    private lateinit var transactionsListContainer: LinearLayout
    private lateinit var barChart: BarChart
    private lateinit var loadingOverlay: RelativeLayout
    private lateinit var sideMenuDrawer: DrawerLayout
    
    // Budget Status UI
    private lateinit var cardBudgetStatus: View
    private lateinit var ivStatusIcon: ImageView
    private lateinit var tvBudgetStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_report)

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
                R.id.nav_reports -> null // already here
                R.id.nav_settings -> Intent(this, SettingsActivity::class.java)
                R.id.nav_about -> Intent(this, AboutActivity::class.java)
                R.id.nav_manual -> Intent(this, ManualActivity::class.java)
                R.id.nav_logout -> {
                    login_page(View(this))
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

        etFromDate = findViewById(R.id.etFromDate)
        etToDate = findViewById(R.id.etToDate)
        btnFilter = findViewById(R.id.btnFilter)
        breakdownContainer = findViewById(R.id.breakdownContainer)
        transactionsListContainer = findViewById(R.id.transactionsListContainer)
        barChart = findViewById(R.id.barChart)
        loadingOverlay = findViewById(R.id.loadingOverlay)
        
        cardBudgetStatus = findViewById(R.id.cardBudgetStatus)
        ivStatusIcon = findViewById(R.id.ivStatusIcon)
        tvBudgetStatus = findViewById(R.id.tvBudgetStatus)

        // setting up date pickers for the period selection.
        setupDatePickers()

        // initially loading all expenses.
        loadData(null, null)

        btnFilter.setOnClickListener {
            val fromStr = etFromDate.text.toString()
            val toStr = etToDate.text.toString()

            if (fromStr.isEmpty() || toStr.isEmpty()) {
                Toast.makeText(this, "Please select both dates", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // converting dates to YYYY-MM-DD for database query.
            val fromDb = formatToDbDate(fromStr)
            val toDb = formatToDbDate(toStr)
            loadData(fromDb, toDb)
        }

        //bottom navigation.
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        
        bottomNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, ThirdActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_add -> {
                    val intent = Intent(this, BudgetActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_add_expense -> {
                    val intent = Intent(this, ForthActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_progress -> {
                    val intent = Intent(this, ProgressActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun setupDatePickers() {
        val calendar = Calendar.getInstance()
        val dateSetListener = { view: View, editText: EditText ->
            DatePickerDialog(this, { _, year, month, day ->
                editText.setText(String.format("%02d/%02d/%d", day, month + 1, year))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        }

        etFromDate.setOnClickListener { dateSetListener(it, etFromDate) }
        etToDate.setOnClickListener { dateSetListener(it, etToDate) }
    }

    private fun formatToDbDate(dateStr: String): String {
        val parts = dateStr.split("/")
        return "${parts[2]}-${parts[1]}-${parts[0]}"
    }

    // loading data from the database.
    private fun loadData(startDate: String?, endDate: String?) {
        loadingOverlay.visibility = View.VISIBLE
        
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            val db = DatabaseHelper(this)
            val cursor = if (startDate != null && endDate != null) {
                db.getExpensesByDate(startDate, endDate)
            } else {
                db.getAllExpenses()
            }

            val categoryTotals = mutableMapOf<String, Float>()
            var totalAmount = 0f
            
            breakdownContainer.removeAllViews()
            transactionsListContainer.removeAllViews()

            if (cursor.moveToFirst()) {
                do {
                    val amount = cursor.getFloat(cursor.getColumnIndexOrThrow("amount"))
                    val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                    val desc = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                    val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                    val receiptUri = cursor.getString(cursor.getColumnIndexOrThrow("receipt_uri"))

                    // adding up totals for the pie chart.
                    categoryTotals[category] = categoryTotals.getOrDefault(category, 0f) + amount
                    totalAmount += amount

                    // adding individual transaction row to the list.
                    addTransactionRow(date, category, amount, desc, receiptUri)

                } while (cursor.moveToNext())
            }
            cursor.close()

            updateBarChart(categoryTotals, totalAmount)
            updateBreakdownList(categoryTotals, totalAmount)
            updateBudgetStatus(totalAmount)
            
            loadingOverlay.visibility = View.GONE
        }, 500)
    }

    private fun formatCurrency(amount: Float): String {
        val symbols = DecimalFormatSymbols(Locale.getDefault())
        symbols.groupingSeparator = ' '
        symbols.decimalSeparator = ','
        val df = DecimalFormat("#,##0.00", symbols)
        return "R " + df.format(amount)
    }

    private fun addTransactionRow(date: String, category: String, amount: Float, desc: String, receiptUri: String) {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.VERTICAL
        row.setPadding(32, 32, 32, 32)
        
        // adding an elevation and rounded background effect.
        val shape = android.graphics.drawable.GradientDrawable()
        shape.setColor(ContextCompat.getColor(this, R.color.surface_light))
        shape.cornerRadius = 15f
        shape.setStroke(2, ContextCompat.getColor(this, R.color.gray_light))
        row.background = shape
        
        // top row: Category and Amount.
        val header = LinearLayout(this)
        header.orientation = LinearLayout.HORIZONTAL
        header.gravity = android.view.Gravity.CENTER_VERTICAL

        val catText = TextView(this)
        catText.text = category
        catText.textSize = 18f
        catText.setTextColor(ContextCompat.getColor(this, R.color.text_primary))
        catText.setTypeface(null, android.graphics.Typeface.BOLD)
        catText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        
        val amtText = TextView(this)
        amtText.text = formatCurrency(amount)
        amtText.textSize = 18f
        amtText.setTextColor(ContextCompat.getColor(this, R.color.error_red))
        amtText.setTypeface(null, android.graphics.Typeface.BOLD)

        header.addView(catText)
        header.addView(amtText)
        row.addView(header)

        // Middle row: Description.
        if (desc.isNotEmpty()) {
            val descText = TextView(this)
            descText.text = desc
            descText.textSize = 14f
            descText.setPadding(0, 8, 0, 8)
            descText.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
            row.addView(descText)
        }

        // Bottom row: Date and Receipt button.
        val footer = LinearLayout(this)
        footer.orientation = LinearLayout.HORIZONTAL
        footer.gravity = android.view.Gravity.CENTER_VERTICAL
        footer.setPadding(0, 8, 0, 0)

        val dateText = TextView(this)
        dateText.text = date
        dateText.textSize = 12f
        dateText.setTextColor(ContextCompat.getColor(this, R.color.gray_medium))
        dateText.layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        footer.addView(dateText)

        val btnAction = Button(this)
        btnAction.text = "Receipt"
        btnAction.textSize = 12f
        btnAction.setTextColor(ContextCompat.getColor(this, R.color.white))
        btnAction.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_blue))
        
        val btnParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            100 
        )
        btnAction.layoutParams = btnParams
        btnAction.setPadding(20, 0, 20, 0)
        
        btnAction.setOnClickListener {
            showReceiptOptions(category, amount.toDouble(), desc, receiptUri)
        }
        footer.addView(btnAction)

        row.addView(footer)

        // Adding margin between items.
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 10, 0, 10)
        row.layoutParams = params

        transactionsListContainer.addView(row)
    }

    private fun spToPx(sp: Int): Int {
        return (sp * resources.displayMetrics.scaledDensity).toInt()
    }

    private fun updateBarChart(categoryTotals: Map<String, Float>, totalAmount: Float) {
        if (categoryTotals.isEmpty()) {
            barChart.visibility = View.GONE
            return
        }
        barChart.visibility = View.VISIBLE

        // Retrieve budget goals from shared preferences.
        val sharedPref = getSharedPreferences("FinancePrefs", MODE_PRIVATE)
        val minBudget = sharedPref.getFloat("MIN_BUDGET", 0f)
        val maxBudget = sharedPref.getFloat("MAX_BUDGET", 0f)

        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()
        var index = 0f
        for ((category, amount) in categoryTotals) {
            entries.add(BarEntry(index, amount))
            labels.add(category)
            index += 1f
        }

        val dataSet = BarDataSet(entries, "Spending by Category")
        
        val colors = ArrayList<Int>()
        for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
        dataSet.colors = colors
        dataSet.valueTextColor = Color.BLACK
        dataSet.valueTextSize = 10f

        val data = BarData(dataSet)
        data.barWidth = 0.7f
        barChart.data = data

        // show category labels.
        val xAxis = barChart.xAxis
        xAxis.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                val i = value.toInt()
                return if (i >= 0 && i < labels.size) labels[i] else ""
            }
        }
        xAxis.position = XAxis.XAxisPosition.BOTTOM
        xAxis.granularity = 1f
        xAxis.setDrawGridLines(false)
        xAxis.labelRotationAngle = -45f
        xAxis.yOffset = 10f

        // Add Limit Lines for Min/Max goals.
        val leftAxis = barChart.axisLeft
        leftAxis.removeAllLimitLines()
        
        if (minBudget > 0) {
            val minLine = LimitLine(minBudget, "Min Goal")
            minLine.lineColor = Color.parseColor("#4CAF50") // Greenish
            minLine.lineWidth = 2f
            minLine.textColor = Color.parseColor("#4CAF50")
            minLine.textSize = 12f
            minLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            leftAxis.addLimitLine(minLine)
        }

        if (maxBudget > 0) {
            val maxLine = LimitLine(maxBudget, "Max Limit")
            maxLine.lineColor = Color.RED
            maxLine.lineWidth = 2f
            maxLine.textColor = Color.RED
            maxLine.textSize = 12f
            maxLine.labelPosition = LimitLine.LimitLabelPosition.RIGHT_TOP
            leftAxis.addLimitLine(maxLine)
        }

        leftAxis.axisMinimum = 0f
        // Set axis maximum to comfortably fit the lines and bars.
        val highestPoint = maxOf(categoryTotals.values.maxOrNull() ?: 0f, maxBudget, minBudget)
        leftAxis.axisMaximum = highestPoint * 1.3f
        leftAxis.setDrawGridLines(true)

        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        

        val legend = barChart.legend
        legend.isEnabled = true
        legend.verticalAlignment = com.github.mikephil.charting.components.Legend.LegendVerticalAlignment.BOTTOM
        legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
        legend.orientation = com.github.mikephil.charting.components.Legend.LegendOrientation.HORIZONTAL
        legend.setDrawInside(false)
        legend.yOffset = 10f
        legend.isWordWrapEnabled = true


        barChart.extraBottomOffset = 45f

        barChart.animateY(1000)
        barChart.invalidate()
    }

    private fun updateBreakdownList(categoryTotals: Map<String, Float>, totalAmount: Float) {
        val colors = ArrayList<Int>()
        for (c in ColorTemplate.JOYFUL_COLORS) colors.add(c)
        for (c in ColorTemplate.COLORFUL_COLORS) colors.add(c)
        for (c in ColorTemplate.LIBERTY_COLORS) colors.add(c)
        for (c in ColorTemplate.PASTEL_COLORS) colors.add(c)
        for (c in ColorTemplate.MATERIAL_COLORS) colors.add(c)

        var colorIndex = 0
        for ((category, amount) in categoryTotals) {
            val row = LinearLayout(this)
            row.orientation = LinearLayout.HORIZONTAL
            row.gravity = android.view.Gravity.CENTER_VERTICAL
            row.setPadding(0, 8, 0, 8)

            // Box key for chart clarity.
            val colorBox = View(this)
            val boxSize = spToPx(14)
            val params = LinearLayout.LayoutParams(boxSize, boxSize)
            params.setMargins(0, 0, 20, 0)
            colorBox.layoutParams = params
            colorBox.setBackgroundColor(colors[colorIndex % colors.size])

            val item = TextView(this)
            val percentage = if (totalAmount > 0) (amount / totalAmount) * 100 else 0f
            item.text = String.format(Locale.getDefault(), "%s: %s (%.1f%%)", category, formatCurrency(amount), percentage)
            item.textSize = 16f
            item.setTextColor(ContextCompat.getColor(this, R.color.text_primary))

            row.addView(colorBox)
            row.addView(item)
            breakdownContainer.addView(row)
            colorIndex++
        }
    }

    private fun showReceiptOptions(category: String, amount: Double, desc: String, receiptUri: String) {
        val receiptContent = """
            TRANSACTION RECEIPT
            -----------------------------
            Category: $category
            Amount: ${formatCurrency(amount.toFloat())}
            Description: $desc
            Date: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(java.util.Date())}
            -----------------------------
            Thank you for using Pennywise!
        """.trimIndent()

        val hasImage = receiptUri.isNotEmpty()
        val options = if (hasImage) {
            arrayOf("View Details", "View Photo Receipt", "Download Receipt")
        } else {
            arrayOf("View Details", "Download Receipt")
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Transaction Options")
            .setItems(options) { _, which ->
                when {
                    options[which] == "View Details" -> {
                        androidx.appcompat.app.AlertDialog.Builder(this)
                            .setTitle("Receipt Details")
                            .setMessage(receiptContent)
                            .setPositiveButton("Close", null)
                            .show()
                    }
                    options[which] == "View Photo Receipt" -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setDataAndType(Uri.parse(receiptUri), "image/*")
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(this, "Cannot open image", Toast.LENGTH_SHORT).show()
                        }
                    }
                    options[which] == "Download Receipt" -> {
                        saveToFile("Receipt_${System.currentTimeMillis()}.txt", receiptContent)
                    }
                }
            }
            .show()
    }

    // this function creates a text file report of all filtered transactions.
    fun downloadStatement(view: View) {
        val fromStr = etFromDate.text.toString()
        val toStr = etToDate.text.toString()
        
        val db = DatabaseHelper(this)
        val cursor = if (fromStr.isNotEmpty() && toStr.isNotEmpty()) {
            db.getExpensesByDate(formatToDbDate(fromStr), formatToDbDate(toStr))
        } else {
            db.getAllExpenses()
        }

        val sb = StringBuilder()
        sb.append("PENNYWISE FINANCIAL STATEMENT\n")
        sb.append("Generated on: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())}\n")
        if (fromStr.isNotEmpty() && toStr.isNotEmpty()) {
            sb.append("Period: $fromStr to $toStr\n")
        }
        sb.append("-------------------------------------------\n\n")

        var total = 0.0
        if (cursor.moveToFirst()) {
            do {
                val date = cursor.getString(cursor.getColumnIndexOrThrow("date"))
                val category = cursor.getString(cursor.getColumnIndexOrThrow("category"))
                val amount = cursor.getDouble(cursor.getColumnIndexOrThrow("amount"))
                val desc = cursor.getString(cursor.getColumnIndexOrThrow("description"))
                
                sb.append("Date: $date\n")
                sb.append("Category: $category\n")
                sb.append("Amount: ${formatCurrency(amount.toFloat())}\n")
                if (desc.isNotEmpty()) sb.append("Description: $desc\n")
                sb.append("-------------------------------------------\n")
                total += amount
            } while (cursor.moveToNext())
        }
        cursor.close()

        sb.append("\nTOTAL EXPENSES: ${formatCurrency(total.toFloat())}\n")
        
        saveToFile("Statement_${System.currentTimeMillis()}.txt", sb.toString())
    }

    private fun saveToFile(fileName: String, content: String) {
        try {
            val file = File(getExternalFilesDir(null), fileName)
            val fos = FileOutputStream(file)
            fos.write(content.toByteArray())
            fos.close()
            Toast.makeText(this, "File saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to save file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateBudgetStatus(totalSpent: Float) {
        val sharedPref = getSharedPreferences("FinancePrefs", MODE_PRIVATE)
        val minGoal = sharedPref.getFloat("MIN_BUDGET", 0f)
        val maxLimit = sharedPref.getFloat("MAX_BUDGET", 0f)

        if (maxLimit <= 0) {
            ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
            ivStatusIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.darker_gray))
            tvBudgetStatus.text = "Set a budget limit to track your goals!"
            tvBudgetStatus.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray))
            cardBudgetStatus.visibility = View.VISIBLE
            return
        }

        cardBudgetStatus.visibility = View.VISIBLE
        
        when {
            totalSpent >= maxLimit -> {
                // EXCEEDED MAX LIMIT
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_alert)
                ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.error_red))
                tvBudgetStatus.text = getString(R.string.msg_budget_critical, formatCurrency(maxLimit))
                tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.error_red))
            }
            totalSpent >= minGoal -> {
                // EXCEEDED MIN GOAL (WARNING)
                ivStatusIcon.setImageResource(android.R.drawable.ic_dialog_info)
                ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.warning_yellow))
                tvBudgetStatus.text = getString(R.string.msg_budget_warning, formatCurrency(minGoal))
                tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.warning_yellow))
            }
            else -> {
                // WITHIN BUDGET
                ivStatusIcon.setImageResource(android.R.drawable.ic_menu_save) // simple check-like icon
                ivStatusIcon.setColorFilter(ContextCompat.getColor(this, R.color.success_green))
                tvBudgetStatus.text = getString(R.string.msg_budget_healthy)
                tvBudgetStatus.setTextColor(ContextCompat.getColor(this, R.color.success_green))
            }
        }
    }

    // logging out the user and returning to the login screen.
    fun login_page(view: View) {
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

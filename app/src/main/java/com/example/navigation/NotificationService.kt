package com.example.navigation

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.content.Context
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

class NotificationService : NotificationListenerService() {

    // Unique tag for our logging system to track background events
    private val TAG = "PennyWise_NotifService"

    // List of major South African Bank package names. 
    // We filter for these to ensure we only process relevant financial data.
    private val saBankPackages = listOf(
        "za.co.fnb.connect.android",       // First National Bank
        "za.co.capitec.bank",               // Capitec Bank
        "za.co.standardbank.mobilebanking", // Standard Bank
        "com.absa.banking",                 // ABSA Bank
        "za.co.nedbank.money",              // Nedbank
        "com.tyme.bank",                    // TymeBank
        "za.co.discovery.bank"              // Discovery Bank
    )

    /**
     * This method triggers whenever a new notification appears on the phone.
     * We act as a silent listener, waiting for bank transactions.
     */
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        
        val packageName = sbn?.packageName ?: return
        
        // Safety check: Is this notification from one of our supported banks?
        if (!saBankPackages.contains(packageName)) {
            // Log.v(TAG, "Ignoring notification from $packageName") // Too noisy for debug logs
            return
        }

        Log.i(TAG, "Detected financial notification from bank app: $packageName")

        val extras = sbn.notification.extras
        val title = extras.getString("android.title") ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        Log.d(TAG, "Notification content: [Title: $title] [Text: $text]")

        // We use regex to hunt for currency patterns (e.g., R 150.00) in the text.
        val amount = extractAmount(text)
        
        if (amount > 0) {
            Log.i(TAG, "Successfully extracted transaction amount: R$amount")
            recordAutomatedTransaction(amount, title, packageName)
            awardXP()
        } else {
            Log.w(TAG, "No transaction amount could be parsed from the notification text.")
        }
    }

    /**
     * A clever regex-based helper to find monetary values in unstructured strings.
     * It accounts for common South African formatting like "R 1,234.56" or "R123.45".
     */
    private fun extractAmount(text: String): Float {
        // Look for 'R' followed by digits, spaces, commas, or dots
        val pattern = Pattern.compile("R\\s?([\\d\\s,]+\\.\\d{2}|[\\d\\s.]+,\\d{2})")
        val matcher = pattern.matcher(text)
        
        return if (matcher.find()) {
            try {
                // Clean the string: remove whitespace and normalize commas to dots
                val cleaned = matcher.group(1)
                    ?.replace("\\s".toRegex(), "")
                    ?.replace(",", ".") ?: "0"
                
                // Handling cases with multiple separators (e.g., 1.000.00)
                val finalAmount = if (cleaned.count { it == '.' } > 1) {
                    val lastDotIndex = cleaned.lastIndexOf('.')
                    cleaned.substring(0, lastDotIndex).replace(".", "") + cleaned.substring(lastDotIndex)
                } else {
                    cleaned
                }
                
                finalAmount.toFloat()
            } catch (e: Exception) {
                Log.e(TAG, "Regex cleanup failed for amount extraction", e)
                0f
            }
        } else {
            0f
        }
    }

    /**
     * Automatically inserts the detected transaction into our local SQLite database.
     * This saves the user the effort of manual logging!
     */
    private fun recordAutomatedTransaction(amount: Float, bankName: String, pkg: String) {
        val db = DatabaseHelper(this)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val timeSdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentDate = sdf.format(Date())
        val currentTime = timeSdf.format(Date())

        Log.d(TAG, "Saving automated expense to DB: R$amount for $bankName")
        
        db.addExpense(
            amount = amount,
            category = "Automated ($bankName)",
            description = "Transaction detected via $bankName app",
            date = currentDate,
            startTime = currentTime,
            endTime = currentTime,
            receiptUri = ""
        )
    }

    /**
     * Gamification logic: Award 5 XP for every automated transaction detected.
     * We sync this both locally and to the cloud (Firestore).
     */
    private fun awardXP() {
        Log.d(TAG, "Awarding XP for automated transaction")
        val statsPrefs = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val currentXP = statsPrefs.getInt("xp", 0)
        val newXP = currentXP + 5 
        
        statsPrefs.edit().putInt("xp", newXP).apply()
        
        // Sync to Firestore if the user is logged in
        val userPrefs = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val email = userPrefs.getString("REG_EMAIL", null)
        
        if (email != null) {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("users").document(email)
                .update("xp", newXP)
                .addOnSuccessListener { Log.d(TAG, "XP cloud sync successful") }
                .addOnFailureListener { Log.e(TAG, "XP cloud sync failed") }
        }
    }
}

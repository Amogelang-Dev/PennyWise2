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

        // Keywords that suggest a spending transaction
        val spendingKeywords = listOf("purchased", "spent", "paid", "withdrawal", "debit", "payment", "bought")
        val isSpending = spendingKeywords.any { text.lowercase().contains(it) } || 
                        spendingKeywords.any { title.lowercase().contains(it) }

        if (!isSpending) {
            Log.d(TAG, "Notification does not seem to be a spending transaction. Skipping.")
            return
        }

        // We use our dedicated parser to hunt for currency patterns.
        val amount = NotificationParser.extractAmount(text)
        
        if (amount > 0) {
            Log.i(TAG, "Successfully extracted transaction amount: R$amount")
            recordAutomatedTransaction(amount, title, packageName)
            awardXP()
        } else {
            Log.w(TAG, "No transaction amount could be parsed from the notification text.")
        }
    }

    /**
     * Automatically inserts the detected transaction into our local SQLite database.
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


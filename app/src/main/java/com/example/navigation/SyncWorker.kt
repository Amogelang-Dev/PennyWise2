package com.example.navigation

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class SyncWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val dbHelper = DatabaseHelper(applicationContext)
        val unsyncedExpenses = dbHelper.getUnsyncedExpenses()
        val firestore = FirebaseFirestore.getInstance()
        val userPrefs = applicationContext.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val userEmail = userPrefs.getString("REG_EMAIL", null) ?: return Result.failure()

        Log.d("SyncWorker", "Starting sync for $userEmail")

        if (unsyncedExpenses.moveToFirst()) {
            do {
                val id = unsyncedExpenses.getLong(unsyncedExpenses.getColumnIndexOrThrow("id"))
                val amount = unsyncedExpenses.getFloat(unsyncedExpenses.getColumnIndexOrThrow("amount"))
                val category = unsyncedExpenses.getString(unsyncedExpenses.getColumnIndexOrThrow("category"))
                val description = unsyncedExpenses.getString(unsyncedExpenses.getColumnIndexOrThrow("description"))
                val date = unsyncedExpenses.getString(unsyncedExpenses.getColumnIndexOrThrow("date"))
                val startTime = unsyncedExpenses.getString(unsyncedExpenses.getColumnIndexOrThrow("start_time"))
                val endTime = unsyncedExpenses.getString(unsyncedExpenses.getColumnIndexOrThrow("end_time"))

                val expenseMap = hashMapOf(
                    "amount" to amount,
                    "category" to category,
                    "description" to description,
                    "date" to date,
                    "startTime" to startTime,
                    "endTime" to endTime,
                    "userEmail" to userEmail
                )

                try {
                    firestore.collection("expenses").add(expenseMap).await()
                    dbHelper.markAsSynced(id)
                    Log.d("SyncWorker", "Synced expense ID: $id")
                } catch (e: Exception) {
                    Log.e("SyncWorker", "Failed to sync expense ID: $id", e)
                    return Result.retry()
                }
            } while (unsyncedExpenses.moveToNext())
        }
        unsyncedExpenses.close()

        return Result.success()
    }
}

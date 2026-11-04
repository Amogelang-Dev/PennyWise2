package com.example.navigation

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "Pennywise.db"
        private const val DATABASE_VERSION = 2
        private const val EXPENSES_TABLE = "expenses"

        // Column names for our spreadsheet-like table.
        private const val ID_COL = "id"
        private const val AMOUNT_COL = "amount"
        private const val CATEGORY_COL = "category"
        private const val DESC_COL = "description"
        private const val DATE_COL = "date"
        private const val START_TIME_COL = "start_time"
        private const val END_TIME_COL = "end_time"
        private const val RECEIPT_COL = "receipt_uri"
        private const val SYNCED_COL = "synced"
    }

    // This runs the very first time the app is opened to build the vault structure.
    override fun onCreate(db: SQLiteDatabase?) {
        val createQuery = ("CREATE TABLE " + EXPENSES_TABLE + "("
                + ID_COL + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + AMOUNT_COL + " REAL,"
                + CATEGORY_COL + " TEXT,"
                + DESC_COL + " TEXT,"
                + DATE_COL + " TEXT,"
                + START_TIME_COL + " TEXT,"
                + END_TIME_COL + " TEXT,"
                + RECEIPT_COL + " TEXT,"
                + SYNCED_COL + " INTEGER DEFAULT 0" + ")")
        db?.execSQL(createQuery)
    }

    // handling vault structure updates for any changes the user makes.
    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $EXPENSES_TABLE")
        onCreate(db)
    }

    //recording a new expense entry.
    fun addExpense(amount: Float, category: String, description: String, date: String, startTime: String, endTime: String, receiptUri: String): Long {
        val db = this.writableDatabase
        val dataRow = ContentValues().apply {
            put(AMOUNT_COL, amount)
            put(CATEGORY_COL, category)
            put(DESC_COL, description)
            put(DATE_COL, date)
            put(START_TIME_COL, startTime)
            put(END_TIME_COL, endTime)
            put(RECEIPT_COL, receiptUri)
        }

        // closing the screen after an entry is made.
        val generatedId = db.insert(EXPENSES_TABLE, null, dataRow)
        db.close()
        return generatedId
    }

    //checking the vault structure for specific dates.
    fun getExpensesByDate(startDate: String, endDate: String): Cursor {
        val db = this.readableDatabase
        return db.rawQuery(
            "SELECT * FROM $EXPENSES_TABLE WHERE $DATE_COL BETWEEN ? AND ? ORDER BY $DATE_COL DESC", 
            arrayOf(startDate, endDate)
        )
    }

    //sorts out saved transactions in descending order.
    fun getAllExpenses(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $EXPENSES_TABLE ORDER BY $DATE_COL DESC", null)
    }

    //calculates the grand total of all the expenses.
    fun getTotalExpenses(): Float {
        val db = this.readableDatabase
        val result = db.rawQuery("SELECT SUM($AMOUNT_COL) FROM $EXPENSES_TABLE", null)
        var total = 0f
        if (result.moveToFirst()) {
            total = result.getFloat(0)
        }
        result.close()
        return total
    }

    //the reset button for clearing all the expense records.
    fun clearAllExpenses() {
        val db = this.writableDatabase
        db.delete(EXPENSES_TABLE, null, null)
        db.close()
    }

    fun clearAllData() {
        clearAllExpenses()
    }

    //counting how many expense entries have a receipt attached to them.
    fun getCountWithReceipt(): Int {
        val db = this.readableDatabase
        val result = db.rawQuery("SELECT COUNT(*) FROM $EXPENSES_TABLE WHERE $RECEIPT_COL IS NOT NULL AND $RECEIPT_COL != ''", null)
        var count = 0
        if (result.moveToFirst()) count = result.getInt(0)
        result.close()
        return count
    }

    fun getUnsyncedExpenses(): Cursor {
        val db = this.readableDatabase
        return db.rawQuery("SELECT * FROM $EXPENSES_TABLE WHERE $SYNCED_COL = 0", null)
    }

    fun markAsSynced(id: Long) {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(SYNCED_COL, 1)
        }
        db.update(EXPENSES_TABLE, values, "$ID_COL = ?", arrayOf(id.toString()))
        db.close()
    }

    //returns the total number of the logs in the vault.
    fun getCountTotal(): Int {
        val db = this.readableDatabase
        val result = db.rawQuery("SELECT COUNT(*) FROM $EXPENSES_TABLE", null)
        var count = 0
        if (result.moveToFirst()) count = result.getInt(0)
        result.close()
        return count
    }
}

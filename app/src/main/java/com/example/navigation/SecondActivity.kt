package com.example.navigation

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.snackbar.Snackbar

class SecondActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    // handling the user login attempt when the login button is clicked.
    fun home(view: View) {
        val etUser = findViewById<EditText>(R.id.et_login_username)
        val etEmail = findViewById<EditText>(R.id.et_login_email)
        val etPass = findViewById<EditText>(R.id.et_login_password)
        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        val rootView = findViewById<View>(android.R.id.content)

        val inputUser = etUser.text.toString().trim()
        val inputEmail = etEmail.text.toString().trim()
        val inputPass = etPass.text.toString().trim()

        // Check for empty fields.
        if (inputUser.isEmpty()) {
            etUser.error = "Username is required"
            return
        }
        if (inputEmail.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }
        if (inputPass.isEmpty()) {
            etPass.error = "Password is required"
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(inputEmail).matches()) {
            etEmail.error = "Please enter a valid email address"
            return
        }

        loadingOverlay.visibility = View.VISIBLE

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("email", inputEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    loadingOverlay.visibility = View.GONE
                    etEmail.error = "Account not found"
                    Snackbar.make(rootView, "No account found with this email", Snackbar.LENGTH_LONG).show()
                } else {
                    val document = documents.documents[0]
                    val dbPassword = document.getString("password")
                    val dbUsername = document.getString("username")
                    val dbXp = document.getLong("xp")?.toInt() ?: 0

                    if (dbPassword == inputPass) {
                        // saving the login state and user data locally.
                        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
                        val editor = sharedPref.edit()
                        editor.putString("REG_USER", dbUsername)
                        editor.putString("REG_EMAIL", inputEmail)
                        editor.putString("REG_PASS", dbPassword)
                        editor.putBoolean("IS_LOGGED_IN", true)
                        editor.apply()

                        // Sync XP to UserData prefs
                        val userDataPref = getSharedPreferences("UserData", MODE_PRIVATE)
                        userDataPref.edit().putInt("xp", dbXp).apply()

                        loadingOverlay.visibility = View.GONE
                        Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, ThirdActivity::class.java))
                        finish()
                    } else {
                        loadingOverlay.visibility = View.GONE
                        etPass.error = "Incorrect password"
                        Snackbar.make(rootView, "Invalid credentials", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                loadingOverlay.visibility = View.GONE
                Snackbar.make(rootView, "Error: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    // navigating to the registration page if the user doesn't have an account.
    fun register_page(view: View) {
        val intent = Intent(this, FirstActivity::class.java)
        startActivity(intent)
    }
}

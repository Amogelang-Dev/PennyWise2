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
//import this , put them at the top of the code
import com.google.android.material.textfield.TextInputEditText
import android.util.Patterns
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.snackbar.Snackbar
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader
import android.util.Log
import com.example.navigation.utils.SecurityUtils


class FirstActivity : BaseActivity() {
    private val TAG = "FirstActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_first)
        //setting the screen to not clash with the emulator status bar.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        // checking if the user is already logged into the app
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val isLoggedIn = sharedPref.getBoolean("IS_LOGGED_IN", false)

        //taking the user straight to the dashboard uf they have already logged in.
        if (isLoggedIn) {
            startActivity(Intent(this, ThirdActivity::class.java))
            finish()
            return
        }
    }

    // registration handling.
    fun home(view: View) {
        val etUsername = findViewById<TextInputEditText>(R.id.et_username)
        val etEmail = findViewById<TextInputEditText>(R.id.et_email)
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        val etConfirmPassword = findViewById<TextInputEditText>(R.id.et_confirm_password)

        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString()
        val confirmPassword = etConfirmPassword.text.toString()

        var valid = true

        if (username.isEmpty()) {
            etUsername.error = "Name is required"
            valid = false
        } else if (username.length < 2) {
            etUsername.error = "Name must be at least 2 characters long"
            valid = false
        } else {
            etUsername.error = null
        }

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            valid = false
        } else if (!isValidEmail(email)) {
            etEmail.error = "Please enter a valid email address (e.g., name@example.com)"
            valid = false
        } else {
            etEmail.error = null
        }

        if (!validatePassword(password)) {
            valid = false
        }

        if (valid && password != confirmPassword) {
            etConfirmPassword.error = "Passwords do not match"
            valid = false
        }

        if (!valid) return

        // clearing old data when a new user registers.
        getSharedPreferences("FinancePrefs", MODE_PRIVATE).edit().clear().apply()
        getSharedPreferences("UserData", MODE_PRIVATE).edit().clear().apply()

        // clearing the local database so the new user starts fresh.
        val dbHelper = DatabaseHelper(this)
        dbHelper.clearAllData()

        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()

        // sending the registration data to Firebase Firestore.
        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        loadingOverlay.visibility = View.VISIBLE

        val db = FirebaseFirestore.getInstance()
        val rootView = findViewById<View>(android.R.id.content)

        Log.d(TAG, "Starting registration for user: $username")

        db.collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    loadingOverlay.visibility = View.GONE
                    Log.w(TAG, "Registration failed: Email $email already exists")
                    Snackbar.make(rootView, "Email already registered", Snackbar.LENGTH_LONG).show()
                } else {
                    val user = hashMapOf(
                        "username" to username,
                        "email" to email,
                        "password" to SecurityUtils.hashPassword(password),
                        "xp" to 0,
                        "streak" to 0,
                        "last_login" to ""
                    )

                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener {
                            loadingOverlay.visibility = View.GONE
                            Log.i(TAG, "User registered successfully: $username")
                            // saving the user registration details locally.
                            editor.putString("REG_USER", username)
                            editor.putString("REG_EMAIL", email)
                            editor.putString("REG_PASS", password)
                            editor.putBoolean("IS_LOGGED_IN", true)
                            editor.apply()

                            Snackbar.make(rootView, "Registration successful!", Snackbar.LENGTH_LONG).show()
                            
                            // redirecting the user to the dashboard.
                            startActivity(Intent(this, ThirdActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { exception ->
                            loadingOverlay.visibility = View.GONE
                            Log.e(TAG, "Firestore error during registration", exception)
                            Snackbar.make(rootView, "Error: ${exception.message}", Snackbar.LENGTH_LONG).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                loadingOverlay.visibility = View.GONE
                Log.e(TAG, "Firestore error checking email existence", exception)
                Snackbar.make(rootView, "Error: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
    }


    // switching to the login page.
    fun login_page(view: View) {
        startActivity(Intent(this, SecondActivity::class.java))
    }

    private fun validatePassword(password: String): Boolean {
        val etPassword = findViewById<TextInputEditText>(R.id.et_password)
        if (password.isEmpty()) {
            etPassword.error = "Password is required"
            return false
        }

        if (!password.matches(Regex(".*[A-Z].*"))) {
            etPassword.error = "Password must contain at least one uppercase letter"
            return false
        }

        if (!password.matches(Regex(".*[a-z].*"))) {
            etPassword.error = "Password must contain at least one lowercase letter"
            return false
        }

        if (!password.matches(Regex(".*[0-9].*"))) {
            etPassword.error = "Password must contain at least one number"
            return false
        }

        if (!password.matches(Regex(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*"))) {
            etPassword.error = "Password must contain at least one special character"
            return false
        }

        if (password.length < 8) {
            etPassword.error = "Password must be at least 8 characters long"
            return false
        }

        etPassword.error = null
        return true
    }

    fun isValidEmail(email: String): Boolean {
        if (email.isEmpty()) return false
        if (email.contains(" ")) return false
        if (email.length > 100) return false
        if (email.contains("..")) return false
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}

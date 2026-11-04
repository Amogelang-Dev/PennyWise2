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
import android.util.Log
import com.google.android.material.textfield.TextInputEditText
import com.example.navigation.utils.SecurityUtils
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

class SecondActivity : BaseActivity() {
    private val TAG = "SecondActivity"
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        findViewById<MaterialButton>(R.id.btn_google_login).setOnClickListener {
            signIn()
        }

        val mainView = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    // handling the user login attempt when the login button is clicked.
    fun home(view: View) {
        val etUser = findViewById<TextInputEditText>(R.id.et_login_username)
        val etEmail = findViewById<TextInputEditText>(R.id.et_login_email)
        val etPass = findViewById<TextInputEditText>(R.id.et_login_password)
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
        Log.d(TAG, "Attempting login for email: $inputEmail")

        val db = FirebaseFirestore.getInstance()
        db.collection("users")
            .whereEqualTo("email", inputEmail)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    loadingOverlay.visibility = View.GONE
                    Log.w(TAG, "Login failed: Email not found")
                    etEmail.error = "Account not found"
                    Snackbar.make(rootView, "No account found with this email", Snackbar.LENGTH_LONG).show()
                } else {
                    val document = documents.documents[0]
                    val dbPassword = document.getString("password")
                    val dbUsername = document.getString("username")
                    val dbXp = document.getLong("xp")?.toInt() ?: 0

                    val hashedInputPass = SecurityUtils.hashPassword(inputPass)
                    if (dbPassword == hashedInputPass) {
                        Log.i(TAG, "Login successful for user: $dbUsername")
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
                        Log.w(TAG, "Login failed: Incorrect password")
                        etPass.error = "Incorrect password"
                        Snackbar.make(rootView, "Invalid credentials", Snackbar.LENGTH_LONG).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                loadingOverlay.visibility = View.GONE
                Log.e(TAG, "Firestore error during login", exception)
                Snackbar.make(rootView, "Error: ${exception.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    // navigating to the registration page if the user doesn't have an account.
    fun register_page(view: View) {
        val intent = Intent(this, FirstActivity::class.java)
        startActivity(intent)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        loadingOverlay.visibility = View.VISIBLE
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    syncGoogleUser(user?.displayName, user?.email)
                } else {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun syncGoogleUser(name: String?, email: String?) {
        val db = FirebaseFirestore.getInstance()
        val loadingOverlay = findViewById<View>(R.id.loadingOverlay)
        
        email?.let { e ->
            db.collection("users").whereEqualTo("email", e).get()
                .addOnSuccessListener { docs ->
                    if (docs.isEmpty) {
                        // Create new user for Google SSO
                        val newUser = hashMapOf(
                            "username" to (name ?: "User"),
                            "email" to e,
                            "xp" to 0,
                            "streak" to 0,
                            "last_login" to "",
                            "auth_provider" to "google"
                        )
                        db.collection("users").add(newUser).addOnSuccessListener {
                            completeLogin(name ?: "User", e, 0)
                        }
                    } else {
                        val doc = docs.documents[0]
                        val username = doc.getString("username") ?: (name ?: "User")
                        val xp = doc.getLong("xp")?.toInt() ?: 0
                        completeLogin(username, e, xp)
                    }
                }
                .addOnFailureListener {
                    loadingOverlay.visibility = View.GONE
                    Toast.makeText(this, "Failed to sync user data", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun completeLogin(username: String, email: String, xp: Int) {
        val sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("REG_USER", username)
            putString("REG_EMAIL", email)
            putBoolean("IS_LOGGED_IN", true)
            apply()
        }
        
        val userDataPref = getSharedPreferences("UserData", MODE_PRIVATE)
        userDataPref.edit().putInt("xp", xp).apply()
        
        findViewById<View>(R.id.loadingOverlay).visibility = View.GONE
        Toast.makeText(this, "Welcome $username!", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this, ThirdActivity::class.java))
        finish()
    }
}

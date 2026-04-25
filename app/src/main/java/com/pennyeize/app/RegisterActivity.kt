package com.pennywize.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.User
import kotlinx.coroutines.launch

// Activity for user registration
class RegisterActivity : AppCompatActivity() {

    private lateinit var etFullName: EditText
    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnCreateAccount: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize database
        db = AppDatabase.getDatabase(this)

        // Initialize views
        etFullName = findViewById(R.id.etFullName)
        etUsername = findViewById(R.id.etRegUsername)
        etPassword = findViewById(R.id.etRegPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnCreateAccount = findViewById(R.id.btnCreateAccount)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        // Create account button click
        btnCreateAccount.setOnClickListener {
            registerUser()
        }

        // Back to login button click
        btnBackToLogin.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun registerUser() {
        val fullName = etFullName.text.toString().trim()
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validate inputs
        if (fullName.isEmpty() || username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        // Insert user into database
        lifecycleScope.launch {
            try {
                val existingUser = db.userDao().getUserByUsername(username)
                if (existingUser != null) {
                    runOnUiThread {
                        Toast.makeText(this@RegisterActivity, "Username already exists", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val user = User(username = username, password = password)
                db.userDao().insertUser(user)
                Log.d("RegisterActivity", "User registered: $username")

                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Account created successfully!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                    finish()
                }
            } catch (e: Exception) {
                Log.e("RegisterActivity", "Error registering user: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity, "Error creating account", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
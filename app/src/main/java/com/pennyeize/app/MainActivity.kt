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
import kotlinx.coroutines.launch

// Main Activity - Login Screen for PennyWize app
class MainActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize database
        db = AppDatabase.getDatabase(this)

        // Initialize views
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        // Login button click
        btnLogin.setOnClickListener {
            loginUser()
        }

        // Register button click
        btnRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun loginUser() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate inputs
        if (username.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Check credentials in database
        lifecycleScope.launch {
            try {
                val user = db.userDao().login(username, password)
                if (user != null) {
                    Log.d("MainActivity", "User logged in: $username")
                    val intent = Intent(this@MainActivity, HomeActivity::class.java)
                    intent.putExtra("userId", user.id)
                    intent.putExtra("username", user.username)
                    runOnUiThread {
                        startActivity(intent)
                        finish()
                    }
                } else {
                    Log.w("MainActivity", "Login failed for: $username")
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Invalid username or password", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Login error: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "Login error occurred", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
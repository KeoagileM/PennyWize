package com.pennywize.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Home Dashboard Activity for PennyWize app
class HomeActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvMinGoal: TextView
    private lateinit var tvMaxGoal: TextView
    private lateinit var btnAddExpense: Button
    private lateinit var btnViewExpenses: Button
    private lateinit var btnCategories: Button
    private lateinit var btnGoals: Button
    private lateinit var btnCategoryTotals: Button
    private lateinit var btnLogout: Button
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        // Get user details from intent
        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: ""

        Log.d("HomeActivity", "User logged in: $username with id: $userId")

        // Initialize database
        db = AppDatabase.getDatabase(this)

        // Initialize views
        tvUsername = findViewById(R.id.tvUsername)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvMinGoal = findViewById(R.id.tvMinGoal)
        tvMaxGoal = findViewById(R.id.tvMaxGoal)
        btnAddExpense = findViewById(R.id.btnAddExpense)
        btnViewExpenses = findViewById(R.id.btnViewExpenses)
        btnCategories = findViewById(R.id.btnCategories)
        btnGoals = findViewById(R.id.btnGoals)
        btnCategoryTotals = findViewById(R.id.btnCategoryTotals)
        btnLogout = findViewById(R.id.btnLogout)

        tvUsername.text = "Welcome, $username"

        // Load dashboard data
        loadDashboardData()

        // Button click listeners
        btnAddExpense.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnViewExpenses.setOnClickListener {
            val intent = Intent(this, ViewExpensesActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnCategories.setOnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnGoals.setOnClickListener {
            val intent = Intent(this, GoalsActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnCategoryTotals.setOnClickListener {
            val intent = Intent(this, CategoryTotalsActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            Log.d("HomeActivity", "User logged out: $username")
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                // Get current month
                val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonth = sdf.format(Date())

                // Get current month expenses
                val startDate = "$currentMonth-01"
                val endDate = "$currentMonth-31"
                val expenses = db.expenseDao().getExpensesByPeriod(userId, startDate, endDate)
                val total = expenses.sumOf { it.amount }

                // Get current month goal
                val goal = db.goalDao().getGoalByMonth(userId, currentMonth)

                runOnUiThread {
                    tvTotalExpenses.text = "R %.2f".format(total)
                    if (goal != null) {
                        tvMinGoal.text = "R %.2f".format(goal.minimumGoal)
                        tvMaxGoal.text = "R %.2f".format(goal.maximumGoal)
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeActivity", "Error loading dashboard: ${e.message}")
            }
        }
    }
}
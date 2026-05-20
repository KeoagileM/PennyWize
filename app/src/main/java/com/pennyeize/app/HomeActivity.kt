package com.pennywize.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    //text views
    private lateinit var tvWelcomeUser: TextView
    private lateinit var tvRemainingBalance: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvMinGoal: TextView
    private lateinit var tvMaxGoal: TextView
    private lateinit var tvMaxBudget: TextView

    //buttons
    private lateinit var btnViewExpenses: Button
    private lateinit var btnViewCategory: Button
    private lateinit var btnLogout: Button
    private lateinit var btnViewGoals: Button

    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        userId = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: ""

        Log.d("HomeActivity", "User logged in: $username with id: $userId")

        db = AppDatabase.getDatabase(this)

        //text view initialization
        tvWelcomeUser = findViewById(R.id.tvWelcomeUser)
        tvMaxBudget = findViewById(R.id.tvMaxBudget)
        tvRemainingBalance = findViewById(R.id.tvRemainingBalance)
        tvTotalExpenses = findViewById(R.id.tvTotalExpenses)
        tvMinGoal = findViewById(R.id.tvMinGoal)
        tvMaxGoal = findViewById(R.id.tvMaxGoal)
        tvWelcomeUser.text = username

        //buttons initialization
        btnViewExpenses = findViewById(R.id.btnViewExpenses)
        btnViewGoals = findViewById(R.id.btnViewGoals)
        btnViewCategory = findViewById<Button>(R.id.btnViewCategory)
        btnLogout = findViewById(R.id.btnLogout)


        loadDashboardData()


        //set button on click activity for navigation
        btnViewExpenses.setOnClickListener {
            val intent = Intent(this, ViewExpensesActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnViewCategory.setOnClickListener {
            val intent = Intent(this, CategoryTotalsActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnViewGoals.setOnClickListener {
            val intent = Intent(this, GoalsActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        btnLogout.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }



        setupBottomNavigation()
    }

    //function to set on click activity for nagvigation bar buttons
    private fun setupBottomNavigation() {
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navExpenses = findViewById<LinearLayout>(R.id.navExpenses)
        val navCategories = findViewById<LinearLayout>(R.id.navCategories)
        val navGoals = findViewById<LinearLayout>(R.id.navGoals)
        val navAnalytics = findViewById<LinearLayout>(R.id.navAnalytics)

        setActiveTab(navHome)
        setInactiveTab(navExpenses)
        setInactiveTab(navCategories)
        setInactiveTab(navGoals)
        setInactiveTab(navAnalytics)


        navHome.setOnClickListener {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show()
        }

        navExpenses.setOnClickListener {
            val intent = Intent(this, AddExpenseActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        navCategories.setOnClickListener {
            val intent = Intent(this, CategoriesActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        navGoals.setOnClickListener {
            val intent = Intent(this, GoalsActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        navAnalytics.setOnClickListener {
            val intent = Intent(this, AnalyticsActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }
    }

    private fun setActiveTab(tab: LinearLayout) {
        tab.setBackgroundColor(android.graphics.Color.parseColor("#DCD3EA"))
        for (i in 0 until tab.childCount) {
            val child = tab.getChildAt(i)
            when (child) {
                is TextView -> {
                    child.setTextColor(android.graphics.Color.parseColor("#1F2937"))
                    child.setTypeface(null, android.graphics.Typeface.BOLD)
                }
            }
        }
    }

    private fun setInactiveTab(tab: LinearLayout) {
        tab.setBackgroundColor(android.graphics.Color.TRANSPARENT)
        for (i in 0 until tab.childCount) {
            val child = tab.getChildAt(i)
            when (child) {
                is TextView -> {
                    child.setTextColor(android.graphics.Color.parseColor("#5A9BFF"))
                    child.setTypeface(null, android.graphics.Typeface.NORMAL)
                }
            }
        }
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonth = sdf.format(Date())

                val startDate = "$currentMonth-01"
                val calendar = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                val expenses = db.expenseDao().getExpensesByPeriod(userId, startDate, endDate)
                Log.d("DEBUG", "Expenses size: ${expenses.size}")

                val totalExpense = expenses.sumOf { it.amount }
                val goal = db.goalDao().getGoalByMonth(userId, currentMonth)

                runOnUiThread {


                    tvTotalExpenses.text = "R %.2f".format(totalExpense)

                    if (goal != null) {

                        val totalBalance = goal.maximumGoal - totalExpense

                        tvMaxBudget.text = "R %.2f".format(goal.maximumGoal)
                        tvMinGoal.text = "R %.2f".format(goal.minimumGoal)
                        tvMaxGoal.text = "R %.2f".format(goal.maximumGoal)


                        val budgetUsedPercent =
                            if (goal.maximumGoal > 0)
                                (totalExpense / goal.maximumGoal) * 100
                            else 0.0

                        val (ratingText, ratingColor, amountColor) = when {
                            budgetUsedPercent <= 50 ->
                                Triple("Excellent", "#4CAF50", "#4CAF50")

                            budgetUsedPercent <= 80 ->
                                Triple("On Track", "#4A9EFF", "#4A9EFF")

                            budgetUsedPercent <= 100 ->
                                Triple("Caution Zone", "#FF9800", "#FF9800")

                            else ->
                                Triple("Over Budget", "#F44336", "#F44336")
                        }

                        val fullText = "R %.2f   %s".format(totalBalance, ratingText)

                        val spannable = android.text.SpannableString(fullText)

                        val ratingStartIndex = fullText.indexOf(ratingText)


                        spannable.setSpan(
                            android.text.style.ForegroundColorSpan(
                                android.graphics.Color.parseColor(amountColor)
                            ),
                            0,
                            ratingStartIndex,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )


                        spannable.setSpan(
                            android.text.style.ForegroundColorSpan(
                                android.graphics.Color.parseColor(ratingColor)
                            ),
                            ratingStartIndex,
                            fullText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        spannable.setSpan(
                            android.text.style.RelativeSizeSpan(0.50f),
                            ratingStartIndex,
                            fullText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )

                        spannable.setSpan(
                            android.text.style.StyleSpan(android.graphics.Typeface.ITALIC),
                            ratingStartIndex,
                            fullText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                        )


                        tvRemainingBalance.text = spannable
                    }
                }

            } catch (e: Exception) {
                Log.e("HomeActivity", "Error loading dashboard: ${e.message}")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }
}
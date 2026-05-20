package com.pennywize.app

import android.content.Intent
import android.graphics.*
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class AnalyticsActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var userId: Int = 0

    private lateinit var pieChartView: PieChartView
    private lateinit var tvTotalSpent: TextView
    private lateinit var tvMonth: TextView
    private lateinit var tvBudgetStatus: TextView
    private lateinit var tvTopCategory: TextView
    private lateinit var tvCategoryCount: TextView
    private lateinit var legendContainer: LinearLayout
    private lateinit var tvNoBudget: TextView
    private lateinit var tvProgressLabel: TextView
    private lateinit var progressBar: ProgressBar

    // Pie chart colours that match the app palette
    private val chartColors = listOf(
        "#4A9EFF", "#10B981", "#F59E0B", "#EF4444",
        "#8B5CF6", "#EC4899", "#14B8A6", "#F97316",
        "#06B6D4", "#84CC16"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_analytics)

        userId = intent.getIntExtra("userId", 0)
        db = AppDatabase.getDatabase(this)

        initViews()
        loadAnalytics()
        setupBottomNavigation()

        findViewById<android.widget.Button>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun initViews() {
        pieChartView    = findViewById(R.id.pieChartView)
        tvTotalSpent    = findViewById(R.id.tvTotalSpent)
        tvMonth         = findViewById(R.id.tvMonth)
        tvBudgetStatus  = findViewById(R.id.tvBudgetStatus)
        tvTopCategory   = findViewById(R.id.tvTopCategory)
        tvCategoryCount = findViewById(R.id.tvCategoryCount)
        legendContainer = findViewById(R.id.legendContainer)
        tvNoBudget      = findViewById(R.id.tvNoBudget)
        tvProgressLabel = findViewById(R.id.tvProgressLabel)
        progressBar     = findViewById(R.id.budgetProgressBar)
    }

    private fun loadAnalytics() {
        lifecycleScope.launch {
            try {
                val sdf          = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonth = sdf.format(Date())
                val startDate    = "$currentMonth-01"
                val calendar     = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH,
                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endDate = SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()
                ).format(calendar.time)

                val categories   = db.categoryDao().getCategoriesByUser(userId)
                val goal         = db.goalDao().getGoalByMonth(userId, currentMonth)
                val allExpenses  = db.expenseDao().getExpensesByPeriod(userId, startDate, endDate)
                val totalSpent   = allExpenses.sumOf { it.amount }

                // Build (name, total) pairs
                val categoryTotals = mutableListOf<Pair<String, Double>>()
                for (cat in categories) {
                    val total = db.expenseDao().getTotalByCategory(
                        userId, cat.id, startDate, endDate
                    ) ?: 0.0
                    if (total > 0) categoryTotals.add(Pair(cat.name, total))
                }
                categoryTotals.sortByDescending { it.second }

                runOnUiThread {
                    // Month label
                    tvMonth.text = "📅 $currentMonth"
                    tvTotalSpent.text = "R %.2f".format(totalSpent)
                    tvCategoryCount.text = "${categoryTotals.size} active categories"

                    // Top category
                    if (categoryTotals.isNotEmpty()) {
                        tvTopCategory.text = "🏆 Top: ${categoryTotals[0].first}  " +
                                "R %.2f".format(categoryTotals[0].second)
                    } else {
                        tvTopCategory.text = "No expenses this month"
                    }

                    // Budget progress
                    if (goal != null) {
                        tvNoBudget.visibility = android.view.View.GONE
                        progressBar.visibility = android.view.View.VISIBLE
                        tvProgressLabel.visibility = android.view.View.VISIBLE

                        val pct = if (goal.maximumGoal > 0)
                            ((totalSpent / goal.maximumGoal) * 100).toInt().coerceIn(0, 100)
                        else 0

                        progressBar.progress = pct

                        val (statusText, statusColor) = when {
                            pct <= 50  -> Pair("✅ Excellent — $pct% used", "#10B981")
                            pct <= 80  -> Pair("🔵 On Track — $pct% used",  "#4A9EFF")
                            pct <= 100 -> Pair("⚠️ Caution — $pct% used",   "#F59E0B")
                            else       -> Pair("🔴 Over Budget — $pct% used","#EF4444")
                        }
                        tvBudgetStatus.text = statusText
                        tvBudgetStatus.setTextColor(Color.parseColor(statusColor))
                        tvProgressLabel.text =
                            "R %.2f  /  R %.2f budget".format(totalSpent, goal.maximumGoal)
                    } else {
                        tvNoBudget.visibility    = android.view.View.VISIBLE
                        progressBar.visibility   = android.view.View.GONE
                        tvProgressLabel.visibility = android.view.View.GONE
                        tvBudgetStatus.text      = "No budget set for $currentMonth"
                        tvBudgetStatus.setTextColor(Color.parseColor("#94A3B8"))
                    }

                    // Pie chart
                    if (categoryTotals.isNotEmpty()) {
                        pieChartView.setData(categoryTotals, chartColors)
                        buildLegend(categoryTotals)
                    } else {
                        pieChartView.setEmpty()
                    }
                }
            } catch (e: Exception) {
                Log.e("AnalyticsActivity", "Error loading analytics: ${e.message}")
            }
        }
    }

    private fun buildLegend(data: List<Pair<String, Double>>) {
        legendContainer.removeAllViews()
        val total = data.sumOf { it.second }

        data.forEachIndexed { index, (name, amount) ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity     = android.view.Gravity.CENTER_VERTICAL
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lp.bottomMargin = 10
                layoutParams = lp
            }

            // Colour dot
            val dot = android.view.View(this).apply {
                val size = resources.getDimensionPixelSize(android.R.dimen.app_icon_size) / 3
                layoutParams = LinearLayout.LayoutParams(size, size).also {
                    it.marginEnd = 12
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(chartColors[index % chartColors.size]))
                }
            }

            // Name + percentage
            val pct = if (total > 0) (amount / total * 100) else 0.0
            val label = TextView(this).apply {
                text    = "${name}  —  R %.2f  (%.1f%%)".format(amount, pct)
                setTextColor(Color.parseColor("#D1D5DB"))
                textSize = 13f
                layoutParams = LinearLayout.LayoutParams(0,
                    LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            row.addView(dot)
            row.addView(label)
            legendContainer.addView(row)
        }
    }

    // ── Bottom navigation ──────────────────────────────────────────────────

    private fun setupBottomNavigation() {
        val navHome       = findViewById<LinearLayout>(R.id.navHome)
        val navExpenses   = findViewById<LinearLayout>(R.id.navExpenses)
        val navCategories = findViewById<LinearLayout>(R.id.navCategories)
        val navGoals      = findViewById<LinearLayout>(R.id.navGoals)
        val navAnalytics  = findViewById<LinearLayout>(R.id.navAnalytics)

        setInactiveTab(navHome)
        setInactiveTab(navExpenses)
        setInactiveTab(navCategories)
        setInactiveTab(navGoals)
        setActiveTab(navAnalytics)

        navHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java)
                .putExtra("userId", userId)); finish()
        }
        navExpenses.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java)
                .putExtra("userId", userId))
        }
        navCategories.setOnClickListener {
            startActivity(Intent(this, CategoriesActivity::class.java)
                .putExtra("userId", userId))
        }
        navGoals.setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java)
                .putExtra("userId", userId))
        }
        navAnalytics.setOnClickListener {
            Toast.makeText(this, "Already on Analytics", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setActiveTab(tab: LinearLayout) {
        tab.setBackgroundColor(Color.parseColor("#DCD3EA"))
        for (i in 0 until tab.childCount) {
            val child = tab.getChildAt(i)
            if (child is TextView) {
                child.setTextColor(Color.parseColor("#1F2937"))
                child.setTypeface(null, Typeface.BOLD)
            }
        }
    }

    private fun setInactiveTab(tab: LinearLayout) {
        tab.setBackgroundColor(Color.TRANSPARENT)
        for (i in 0 until tab.childCount) {
            val child = tab.getChildAt(i)
            if (child is TextView) {
                child.setTextColor(Color.parseColor("#5A9BFF"))
                child.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadAnalytics()
    }
}
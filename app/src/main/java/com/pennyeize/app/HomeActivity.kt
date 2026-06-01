package com.pennywize.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.Achievement
import com.pennywize.app.model.AchievementManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeActivity : AppCompatActivity() {

    // ── Views ──────────────────────────────────────────────────────────────
    private lateinit var tvWelcomeUser: TextView
    private lateinit var tvRemainingBalance: TextView
    private lateinit var tvTotalExpenses: TextView
    private lateinit var tvMinGoal: TextView
    private lateinit var tvMaxGoal: TextView
    private lateinit var tvMaxBudget: TextView
    private lateinit var btnViewExpenses: Button
    private lateinit var btnViewCategory: Button
    private lateinit var btnLogout: Button
    private lateinit var btnViewGoals: Button

    // Achievement views
    private lateinit var achievementsContainer: LinearLayout
    private lateinit var tvAchievementCount: TextView
    private lateinit var achievementProgressBar: ProgressBar

    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private var username: String = ""

    // ── Lifecycle ──────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)

        userId   = intent.getIntExtra("userId", 0)
        username = intent.getStringExtra("username") ?: ""
        db       = AppDatabase.getDatabase(this)

        initViews()
        loadDashboardData()

        btnViewExpenses.setOnClickListener {
            startActivity(Intent(this, AddExpenseActivity::class.java)
                .putExtra("userId", userId))
        }
        btnViewCategory.setOnClickListener {
            startActivity(Intent(this, CategoryTotalsActivity::class.java)
                .putExtra("userId", userId))
        }
        btnViewGoals.setOnClickListener {
            startActivity(Intent(this, GoalsActivity::class.java)
                .putExtra("userId", userId))
        }
        btnLogout.setOnClickListener {
            // Clear saved session on logout
            getSharedPreferences("pennywize_prefs", MODE_PRIVATE)
                .edit().clear().apply()

            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        loadDashboardData()
    }

    // ── View init ──────────────────────────────────────────────────────────
    private fun initViews() {
        tvWelcomeUser        = findViewById(R.id.tvWelcomeUser)
        tvMaxBudget          = findViewById(R.id.tvMaxBudget)
        tvRemainingBalance   = findViewById(R.id.tvRemainingBalance)
        tvTotalExpenses      = findViewById(R.id.tvTotalExpenses)
        tvMinGoal            = findViewById(R.id.tvMinGoal)
        tvMaxGoal            = findViewById(R.id.tvMaxGoal)
        btnViewExpenses      = findViewById(R.id.btnViewExpenses)
        btnViewGoals         = findViewById(R.id.btnViewGoals)
        btnViewCategory      = findViewById(R.id.btnViewCategory)
        btnLogout            = findViewById(R.id.btnLogout)
        achievementsContainer  = findViewById(R.id.achievementsContainer)
        tvAchievementCount     = findViewById(R.id.tvAchievementCount)
        achievementProgressBar = findViewById(R.id.achievementProgressBar)

        // Read from SharedPreferences so it survives navigation back to this screen
        val prefs = getSharedPreferences("pennywize_prefs", MODE_PRIVATE)
        userId   = prefs.getInt("userId", intent.getIntExtra("userId", 0))
        username = prefs.getString("username", intent.getStringExtra("username") ?: "") ?: ""

        tvWelcomeUser.text = username
    }

    // ── Dashboard + achievements data load ────────────────────────────────
    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val sdf          = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonth = sdf.format(Date())
                val startDate    = "$currentMonth-01"
                val calendar     = Calendar.getInstance()
                calendar.set(Calendar.DAY_OF_MONTH,
                    calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
                val endDate = SimpleDateFormat(
                    "yyyy-MM-dd", Locale.getDefault()).format(calendar.time)

                val expenses     = db.expenseDao().getExpensesByPeriod(userId, startDate, endDate)
                val allExpenses  = db.expenseDao().getAllExpenses(userId)
                val totalExpense = expenses.sumOf { it.amount }
                val goal         = db.goalDao().getGoalByMonth(userId, currentMonth)
                val categories   = db.categoryDao().getCategoriesByUser(userId)

                // Evaluate achievements
                val achievements = AchievementManager.evaluate(
                    categoryCount = categories.size,
                    expenseCount  = allExpenses.size,
                    totalSpent    = totalExpense,
                    minGoal       = goal?.minimumGoal,
                    maxGoal       = goal?.maximumGoal
                )

                runOnUiThread {
                    // ── Balance card ────────────────────────────────────
                    tvTotalExpenses.text = "R %.2f".format(totalExpense)

                    if (goal != null) {
                        val totalBalance    = goal.maximumGoal - totalExpense
                        tvMaxBudget.text    = "R %.2f".format(goal.maximumGoal)
                        tvMinGoal.text      = "R %.2f".format(goal.minimumGoal)
                        tvMaxGoal.text      = "R %.2f".format(goal.maximumGoal)

                        val pct = if (goal.maximumGoal > 0)
                            (totalExpense / goal.maximumGoal) * 100 else 0.0

                        val (ratingText, ratingColor, amountColor) = when {
                            pct <= 50  -> Triple("Excellent",    "#4CAF50", "#4CAF50")
                            pct <= 80  -> Triple("On Track",     "#4A9EFF", "#4A9EFF")
                            pct <= 100 -> Triple("Caution Zone", "#FF9800", "#FF9800")
                            else       -> Triple("Over Budget",  "#F44336", "#F44336")
                        }

                        val fullText = "R %.2f   %s".format(totalBalance, ratingText)
                        val spannable = android.text.SpannableString(fullText)
                        val ratingStart = fullText.indexOf(ratingText)
                        spannable.setSpan(android.text.style.ForegroundColorSpan(
                            Color.parseColor(amountColor)), 0, ratingStart,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(android.text.style.ForegroundColorSpan(
                            Color.parseColor(ratingColor)), ratingStart, fullText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(android.text.style.RelativeSizeSpan(0.50f),
                            ratingStart, fullText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        spannable.setSpan(android.text.style.StyleSpan(Typeface.ITALIC),
                            ratingStart, fullText.length,
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                        tvRemainingBalance.text = spannable
                    }

                    // ── Achievements ────────────────────────────────────
                    renderAchievements(achievements)
                }

            } catch (e: Exception) {
                Log.e("HomeActivity", "Error loading dashboard: ${e.message}")
            }
        }
    }

    // ── Render achievement cards ───────────────────────────────────────────
    private fun renderAchievements(achievements: List<Achievement>) {
        achievementsContainer.removeAllViews()

        val unlocked = achievements.count { it.isUnlocked }
        val total    = achievements.size
        val pct      = if (total > 0) (unlocked * 100) / total else 0

        tvAchievementCount.text     = "$unlocked / $total"
        achievementProgressBar.progress = pct

        // Show unlocked first, locked dimmed at the bottom
        val sorted = achievements.sortedByDescending { it.isUnlocked }

        sorted.forEach { achievement ->
            val card = buildAchievementCard(achievement)
            achievementsContainer.addView(card)
        }
    }

    private fun buildAchievementCard(a: Achievement): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity     = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(14))
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = dp(10) }
            layoutParams = lp
            elevation    = 4f

            // Background: glowing border if unlocked, dim if locked
            background = if (a.isUnlocked) {
                GradientDrawable().apply {
                    setColor(Color.parseColor("#1E293B"))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(2), tierBorderColor(a.tier))
                }
            } else {
                GradientDrawable().apply {
                    setColor(Color.parseColor("#111827"))
                    cornerRadius = dp(12).toFloat()
                    setStroke(dp(1), Color.parseColor("#2D3748"))
                }
            }
            alpha = if (a.isUnlocked) 1f else 0.45f
        }

        // ── Icon circle ───────────────────────────────────────────────────
        val iconCircle = FrameLayout(this).apply {
            val size = dp(52)
            layoutParams = LinearLayout.LayoutParams(size, size).also {
                it.marginEnd = dp(14)
            }
            background = GradientDrawable().apply {
                shape        = GradientDrawable.OVAL
                setColor(Color.parseColor("#0F172A"))
                setStroke(dp(2), if (a.isUnlocked) tierBorderColor(a.tier)
                else Color.parseColor("#334155"))
            }
        }
        val iconTv = TextView(this).apply {
            text     = a.icon
            textSize = 24f
            gravity  = Gravity.CENTER
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
        }
        iconCircle.addView(iconTv)
        card.addView(iconCircle)

        // ── Text block ────────────────────────────────────────────────────
        val textBlock = LinearLayout(this).apply {
            orientation  = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val titleTv = TextView(this).apply {
            text      = a.title
            textSize  = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(if (a.isUnlocked) Color.WHITE
            else Color.parseColor("#64748B"))
        }
        val descTv = TextView(this).apply {
            text      = a.description
            textSize  = 12f
            setTextColor(Color.parseColor(if (a.isUnlocked) "#94A3B8" else "#374151"))
            setPadding(0, dp(2), 0, 0)
        }
        textBlock.addView(titleTv)
        textBlock.addView(descTv)
        card.addView(textBlock)

        // ── Right badge: tier medal or lock ───────────────────────────────
        val badgeTv = TextView(this).apply {
            text     = if (a.isUnlocked) tierEmoji(a.tier) else "🔒"
            textSize = 22f
            gravity  = Gravity.CENTER
            setPadding(dp(8), 0, 0, 0)
        }
        card.addView(badgeTv)

        return card
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private fun tierBorderColor(tier: String): Int = when (tier) {
        "gold"   -> Color.parseColor("#F59E0B")
        "silver" -> Color.parseColor("#94A3B8")
        else     -> Color.parseColor("#CD7F32")   // bronze
    }

    private fun tierEmoji(tier: String): String = when (tier) {
        "gold"   -> "🥇"
        "silver" -> "🥈"
        else     -> "🥉"
    }

    /** Convert dp to pixels */
    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    // ── Bottom Navigation ─────────────────────────────────────────────────
    private fun setupBottomNavigation() {
        val navHome       = findViewById<LinearLayout>(R.id.navHome)
        val navExpenses   = findViewById<LinearLayout>(R.id.navExpenses)
        val navCategories = findViewById<LinearLayout>(R.id.navCategories)
        val navGoals      = findViewById<LinearLayout>(R.id.navGoals)
        val navAnalytics  = findViewById<LinearLayout>(R.id.navAnalytics)

        setActiveTab(navHome)
        setInactiveTab(navExpenses)
        setInactiveTab(navCategories)
        setInactiveTab(navGoals)
        setInactiveTab(navAnalytics)

        navHome.setOnClickListener {
            Toast.makeText(this, "Already on Home", Toast.LENGTH_SHORT).show()
        }
        navExpenses.setOnClickListener {
            startActivity(Intent(this, ViewExpensesActivity::class.java)
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
            startActivity(Intent(this, AnalyticsActivity::class.java)
                .putExtra("userId", userId))
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
}
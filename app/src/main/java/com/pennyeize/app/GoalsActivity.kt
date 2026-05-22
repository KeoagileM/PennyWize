package com.pennywize.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.Goal
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class GoalsActivity : AppCompatActivity() {

    private lateinit var etMonth: EditText
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var seekBarMin: SeekBar
    private lateinit var seekBarMax: SeekBar
    private lateinit var btnSaveGoal: Button
    private lateinit var btnBack: Button
    private lateinit var tvCurrentGoals: TextView
    private lateinit var btnLogout: Button
    private lateinit var db: AppDatabase
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        userId = intent.getIntExtra("userId", 0)
        db = AppDatabase.getDatabase(this)

        etMonth = findViewById(R.id.etMonth)
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)
        seekBarMin = findViewById(R.id.seekBarMin)
        seekBarMax = findViewById(R.id.seekBarMax)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        btnBack = findViewById(R.id.btnBack)
        tvCurrentGoals = findViewById(R.id.tvCurrentGoals)
        btnLogout = findViewById(R.id.btnLogout)

        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        etMonth.setText(sdf.format(Date()))

        setupSeekBars()
        loadCurrentGoals()

        btnSaveGoal.setOnClickListener {
            saveGoal()
        }

        btnBack.setOnClickListener {
            finish()
        }

        btnLogout.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        setupBottomNavigation()
    }

    private fun setupSeekBars() {
        seekBarMin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    etMinGoal.setText(progress.toString())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarMax.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    etMaxGoal.setText(progress.toString())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        etMinGoal.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = etMinGoal.text.toString().toIntOrNull() ?: 0
                seekBarMin.progress = minOf(value, 10000)
            }
        }

        etMaxGoal.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = etMaxGoal.text.toString().toIntOrNull() ?: 0
                seekBarMax.progress = minOf(value, 10000)
            }
        }
    }

    //function to set on click activity for nagvigation bar buttons
    private fun setupBottomNavigation() {
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navExpenses = findViewById<LinearLayout>(R.id.navExpenses)
        val navCategories = findViewById<LinearLayout>(R.id.navCategories)
        val navGoals = findViewById<LinearLayout>(R.id.navGoals)
        val navAnalytics = findViewById<LinearLayout>(R.id.navAnalytics)


        setInactiveTab(navHome)
        setInactiveTab(navExpenses)
        setInactiveTab(navCategories)
        setActiveTab(navGoals)
        setInactiveTab(navAnalytics)

        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
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
            Toast.makeText(this, "Already on Goals", Toast.LENGTH_SHORT).show()
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

    private fun loadCurrentGoals() {
        lifecycleScope.launch {
            try {
                val goals = db.goalDao().getAllGoals(userId)
                runOnUiThread {
                    if (goals.isEmpty()) {
                        tvCurrentGoals.text = "📭 No goals set yet"
                    } else {
                        val goalsText = goals.joinToString("\n\n") {
                            "📅 Month: ${it.month}\n⬇️ Min: R%.2f\n⬆️ Max: R%.2f".format(
                                it.minimumGoal, it.maximumGoal
                            )
                        }
                        tvCurrentGoals.text = goalsText
                    }
                }
            } catch (e: Exception) {
                Log.e("GoalsActivity", "Error loading goals: ${e.message}")
            }
        }
    }

    private fun saveGoal() {
        val month = etMonth.text.toString().trim()
        val minStr = etMinGoal.text.toString().trim()
        val maxStr = etMaxGoal.text.toString().trim()

        if (month.isEmpty() || minStr.isEmpty() || maxStr.isEmpty()) {
            Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val minGoal = minStr.toDoubleOrNull()
        val maxGoal = maxStr.toDoubleOrNull()

        if (minGoal == null || maxGoal == null) {
            Toast.makeText(this, "Please enter valid amounts", Toast.LENGTH_SHORT).show()
            return
        }

        if (minGoal >= maxGoal) {
            Toast.makeText(this, "Maximum goal must be greater than minimum goal", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            val existingGoal = db.goalDao().getGoalByMonth(userId, month)

            if (existingGoal != null) {

                runOnUiThread {
                    androidx.appcompat.app.AlertDialog.Builder(this@GoalsActivity)
                        .setTitle("Goal Already Exists")
                        .setMessage("You already have a goal set for $month. What would you like to do?")
                        .setPositiveButton("✏️ Edit Goal") { _, _ ->
                            loadExistingGoal(existingGoal)
                        }
                        .setNeutralButton("🔁 Replace") { _, _ ->
                            overwriteGoalMode(existingGoal)
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }

                return@launch
            }

            val goal = Goal(
                userId = userId,
                minimumGoal = minGoal,
                maximumGoal = maxGoal,
                month = month
            )

            db.goalDao().insertGoal(goal)

            runOnUiThread {
                Toast.makeText(this@GoalsActivity, "Goal saved!", Toast.LENGTH_SHORT).show()
                loadCurrentGoals()
            }
        }
    }

    private fun overwriteGoalMode(existingGoal: Goal) {
        lifecycleScope.launch {
            db.goalDao().deleteGoal(existingGoal)
            runOnUiThread {
                Toast.makeText(this@GoalsActivity, "You can now create a new goal", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun loadExistingGoal(goal: Goal) {
        etMonth.setText(goal.month)
        etMinGoal.setText(goal.minimumGoal.toString())
        etMaxGoal.setText(goal.maximumGoal.toString())

        seekBarMin.progress = goal.minimumGoal.toInt()
        seekBarMax.progress = goal.maximumGoal.toInt()

        Toast.makeText(this, "You can now edit your existing goal", Toast.LENGTH_SHORT).show()
    }
}
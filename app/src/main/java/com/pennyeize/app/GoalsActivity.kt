package com.pennywize.app

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

// Activity for setting minimum and maximum monthly budget goals
class GoalsActivity : AppCompatActivity() {

    private lateinit var etMonth: EditText
    private lateinit var etMinGoal: EditText
    private lateinit var etMaxGoal: EditText
    private lateinit var seekBarMin: SeekBar
    private lateinit var seekBarMax: SeekBar
    private lateinit var btnSaveGoal: Button
    private lateinit var btnBack: Button
    private lateinit var tvCurrentGoals: TextView
    private lateinit var db: AppDatabase
    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_goals)

        userId = intent.getIntExtra("userId", 0)
        db = AppDatabase.getDatabase(this)

        // Initialize views
        etMonth = findViewById(R.id.etMonth)
        etMinGoal = findViewById(R.id.etMinGoal)
        etMaxGoal = findViewById(R.id.etMaxGoal)
        seekBarMin = findViewById(R.id.seekBarMin)
        seekBarMax = findViewById(R.id.seekBarMax)
        btnSaveGoal = findViewById(R.id.btnSaveGoal)
        btnBack = findViewById(R.id.btnBack)
        tvCurrentGoals = findViewById(R.id.tvCurrentGoals)

        // Set current month as default
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        etMonth.setText(sdf.format(Date()))

        // SeekBar for minimum goal
        seekBarMin.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    etMinGoal.setText(progress.toString())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // SeekBar for maximum goal
        seekBarMax.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    etMaxGoal.setText(progress.toString())
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        // Sync EditText to SeekBar for min goal
        etMinGoal.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = etMinGoal.text.toString().toIntOrNull() ?: 0
                seekBarMin.progress = minOf(value, 10000)
            }
        }

        // Sync EditText to SeekBar for max goal
        etMaxGoal.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = etMaxGoal.text.toString().toIntOrNull() ?: 0
                seekBarMax.progress = minOf(value, 10000)
            }
        }

        // Load current goals
        loadCurrentGoals()

        btnSaveGoal.setOnClickListener {
            saveGoal()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCurrentGoals() {
        lifecycleScope.launch {
            try {
                val goals = db.goalDao().getAllGoals(userId)
                runOnUiThread {
                    if (goals.isEmpty()) {
                        tvCurrentGoals.text = "No goals set yet"
                    } else {
                        val goalsText = goals.joinToString("\n") {
                            "Month: ${it.month}\nMin: R%.2f | Max: R%.2f".format(
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

        // Validate inputs
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
            try {
                val goal = Goal(
                    userId = userId,
                    minimumGoal = minGoal,
                    maximumGoal = maxGoal,
                    month = month
                )
                db.goalDao().insertGoal(goal)
                Log.d("GoalsActivity", "Goal saved for $month: min=$minGoal, max=$maxGoal")
                runOnUiThread {
                    Toast.makeText(this@GoalsActivity, "Goals saved!", Toast.LENGTH_SHORT).show()
                    loadCurrentGoals()
                }
            } catch (e: Exception) {
                Log.e("GoalsActivity", "Error saving goal: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@GoalsActivity, "Error saving goals", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
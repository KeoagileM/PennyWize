package com.pennywize.app

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class CategoryTotalsActivity : AppCompatActivity() {

    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnBack: Button
    private lateinit var lvCategoryTotals: ListView
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private val categoryTotalsList = mutableListOf<Pair<String, Double>>()
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_category_totals)

        userId = intent.getIntExtra("userId", 0)
        db = AppDatabase.getDatabase(this)

        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        btnFilter = findViewById(R.id.btnFilter)
        btnBack = findViewById(R.id.btnBack)
        lvCategoryTotals = findViewById(R.id.lvCategoryTotals)
        btnLogout = findViewById(R.id.btnLogout)

        setupDatePickers()
        loadCategoryTotals()

        btnFilter.setOnClickListener {
            loadCategoryTotals()
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

    private fun setupDatePickers() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()

        // Set default dates (current month)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        etStartDate.setText(sdf.format(calendar.time))
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        etEndDate.setText(sdf.format(calendar.time))

        etStartDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                    etStartDate.setText(selectedDate)
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etEndDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, day)
                    etEndDate.setText(selectedDate)
                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
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
        setActiveTab(navCategories)
        setInactiveTab(navGoals)
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

    private fun loadCategoryTotals() {
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val categories = db.categoryDao().getCategoriesByUser(userId)
                categoryTotalsList.clear()

                if (categories.isEmpty()) {
                    runOnUiThread {
                        Toast.makeText(this@CategoryTotalsActivity, "No categories found. Please add categories first.", Toast.LENGTH_LONG).show()
                        val adapter = ArrayAdapter<String>(
                            this@CategoryTotalsActivity,
                            android.R.layout.simple_list_item_1,
                            listOf("No categories available")
                        )
                        lvCategoryTotals.adapter = adapter
                    }
                    return@launch
                }

                for (category in categories) {
                    val total = db.expenseDao().getTotalByCategory(
                        userId, category.id, startDate, endDate
                    ) ?: 0.0
                    categoryTotalsList.add(Pair(category.name, total))
                    Log.d("CategoryTotals", "${category.name}: R$total")
                }

                // Sort by total amount (highest first)
                categoryTotalsList.sortByDescending { it.second }

                runOnUiThread {
                    if (categoryTotalsList.isEmpty()) {
                        Toast.makeText(this@CategoryTotalsActivity, "No data found", Toast.LENGTH_SHORT).show()
                    }

                    val adapter = object : ArrayAdapter<Pair<String, Double>>(
                        this@CategoryTotalsActivity,
                        R.layout.item_category_total,
                        categoryTotalsList
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = convertView ?: layoutInflater.inflate(
                                R.layout.item_category_total, parent, false
                            )
                            val item = categoryTotalsList[position]
                            val categoryName = view.findViewById<TextView>(R.id.tvCategoryName)
                            val categoryTotal = view.findViewById<TextView>(R.id.tvCategoryTotal)

                            // Choose icon based on category name
                            val icon = when (item.first.lowercase()) {
                                "food" -> "🍔"
                                "transport", "transportation" -> "🚗"
                                "rent" -> "🏠"
                                "utilities" -> "⚡"
                                "entertainment" -> "🎬"
                                "shopping" -> "🛍️"
                                "health" -> "💊"
                                "salary" -> "💰"
                                else -> "📁"
                            }

                            categoryName.text = "$icon ${item.first}"
                            categoryTotal.text = "R %.2f".format(item.second)

                            // Color code based on amount
                            if (item.second > 1000) {
                                categoryTotal.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                            } else if (item.second > 500) {
                                categoryTotal.setTextColor(android.graphics.Color.parseColor("#F59E0B"))
                            } else {
                                categoryTotal.setTextColor(android.graphics.Color.parseColor("#10B981"))
                            }

                            return view
                        }
                    }
                    lvCategoryTotals.adapter = adapter
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("CategoryTotalsActivity", "Error loading category totals: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@CategoryTotalsActivity, "Error loading data: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
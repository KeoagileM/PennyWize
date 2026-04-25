package com.pennywize.app

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

// Activity for viewing total spending per category during a selected period
class CategoryTotalsActivity : AppCompatActivity() {

    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnBack: Button
    private lateinit var lvCategoryTotals: ListView
    private lateinit var db: AppDatabase
    private var userId: Int = 0

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

        // Set default date range to current month
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        etStartDate.setText(sdf.format(cal.time))
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        etEndDate.setText(sdf.format(cal.time))

        // Load category totals
        loadCategoryTotals()

        btnFilter.setOnClickListener {
            loadCategoryTotals()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadCategoryTotals() {
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please enter start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val categories = db.categoryDao().getCategoriesByUser(userId)
                val categoryTotals = mutableListOf<Pair<String, Double>>()

                for (category in categories) {
                    val total = db.expenseDao().getTotalByCategory(
                        userId, category.id, startDate, endDate
                    ) ?: 0.0
                    categoryTotals.add(Pair(category.name, total))
                }

                Log.d("CategoryTotalsActivity", "Loaded ${categoryTotals.size} category totals")

                runOnUiThread {
                    val adapter = object : ArrayAdapter<Pair<String, Double>>(
                        this@CategoryTotalsActivity,
                        R.layout.item_category_total,
                        categoryTotals
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = convertView ?: layoutInflater.inflate(
                                R.layout.item_category_total, parent, false
                            )
                            val item = categoryTotals[position]
                            view.findViewById<TextView>(R.id.tvCategoryName).text = item.first
                            view.findViewById<TextView>(R.id.tvCategoryTotal).text =
                                "R %.2f".format(item.second)
                            return view
                        }
                    }
                    lvCategoryTotals.adapter = adapter
                }
            } catch (e: Exception) {
                Log.e("CategoryTotalsActivity", "Error loading category totals: ${e.message}")
            }
        }
    }
}
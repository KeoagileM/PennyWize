package com.pennywize.app

import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.Expense
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// Activity for viewing expense entries filtered by period
class ViewExpensesActivity : AppCompatActivity() {

    private lateinit var etStartDate: EditText
    private lateinit var etEndDate: EditText
    private lateinit var btnFilter: Button
    private lateinit var btnBack: Button
    private lateinit var lvExpenses: ListView
    private lateinit var tvTotal: TextView
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_expenses)

        userId = intent.getIntExtra("userId", 0)
        db = AppDatabase.getDatabase(this)

        etStartDate = findViewById(R.id.etStartDate)
        etEndDate = findViewById(R.id.etEndDate)
        btnFilter = findViewById(R.id.btnFilter)
        btnBack = findViewById(R.id.btnBack)
        lvExpenses = findViewById(R.id.lvExpenses)
        tvTotal = findViewById(R.id.tvTotal)

        // Set default date range to current month
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.set(Calendar.DAY_OF_MONTH, 1)
        etStartDate.setText(sdf.format(cal.time))
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        etEndDate.setText(sdf.format(cal.time))

        // Load expenses for current month
        loadExpenses()

        btnFilter.setOnClickListener {
            loadExpenses()
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadExpenses() {
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please enter start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val expenses = db.expenseDao().getExpensesByPeriod(userId, startDate, endDate)
                expenseList.clear()
                expenseList.addAll(expenses)
                val total = expenses.sumOf { it.amount }

                runOnUiThread {
                    tvTotal.text = "Total: R %.2f".format(total)
                    val adapter = object : ArrayAdapter<Expense>(
                        this@ViewExpensesActivity,
                        R.layout.item_expense,
                        expenseList
                    ) {
                        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                            val view = convertView ?: layoutInflater.inflate(
                                R.layout.item_expense, parent, false
                            )
                            val expense = expenseList[position]
                            view.findViewById<TextView>(R.id.tvExpenseTitle).text = expense.title
                            view.findViewById<TextView>(R.id.tvExpenseAmount).text = "R %.2f".format(expense.amount)
                            view.findViewById<TextView>(R.id.tvExpenseDate).text = "Date: ${expense.date}"
                            view.findViewById<TextView>(R.id.tvExpenseDescription).text = "Description: ${expense.description}"
                            view.findViewById<TextView>(R.id.tvExpenseTime).text = "Time: ${expense.startTime} - ${expense.endTime}"

                            // Load photo if available
                            val ivPhoto = view.findViewById<ImageView>(R.id.ivExpensePhoto)
                            if (expense.photoPath != null && File(expense.photoPath).exists()) {
                                ivPhoto.visibility = View.VISIBLE
                                Glide.with(this@ViewExpensesActivity)
                                    .load(File(expense.photoPath))
                                    .into(ivPhoto)
                            } else {
                                ivPhoto.visibility = View.GONE
                            }
                            return view
                        }
                    }
                    lvExpenses.adapter = adapter
                    Log.d("ViewExpensesActivity", "Loaded ${expenses.size} expenses")
                }
            } catch (e: Exception) {
                Log.e("ViewExpensesActivity", "Error loading expenses: ${e.message}")
            }
        }
    }
}
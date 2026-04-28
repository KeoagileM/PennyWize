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
import com.bumptech.glide.Glide
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.Expense
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import android.app.TimePickerDialog
import java.util.*

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

        setupDatePickers()
        loadExpenses()

        btnFilter.setOnClickListener {
            loadExpenses()
        }

        btnBack.setOnClickListener {
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

        setInactiveTab(navHome)
        setInactiveTab(navExpenses)
        setInactiveTab(navCategories)
        setInactiveTab(navGoals)

        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)        }

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


    private fun loadExpenses() {
        val startDate = etStartDate.text.toString().trim()
        val endDate = etEndDate.text.toString().trim()

        if (startDate.isEmpty() || endDate.isEmpty()) {
            Toast.makeText(this, "Please select start and end dates", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val expenses = db.expenseDao().getExpensesByPeriod(userId, startDate, endDate)

                runOnUiThread {
                    expenseList.clear()
                    expenseList.addAll(expenses)

                    val total = expenses.sumOf { it.amount }
                    tvTotal.text = "💰 Total: R %.2f".format(total)

                    Log.d("ViewExpensesActivity", "Loaded ${expenses.size} expenses")

                    if (expenses.isEmpty()) {
                        Toast.makeText(this@ViewExpensesActivity, "No expenses found for this period", Toast.LENGTH_SHORT).show()
                    }

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
                            view.findViewById<TextView>(R.id.tvExpenseDescription).text = "📝 ${expense.description}"
                            view.findViewById<TextView>(R.id.tvExpenseTime).text = "📅  ${expense.date} - ${expense.date}"

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

                        override fun getCount(): Int = expenseList.size
                    }
                    lvExpenses.adapter = adapter
                    adapter.notifyDataSetChanged()

                    lvExpenses.setOnItemLongClickListener { _, _, position, _ ->
                        val selectedExpense = expenseList[position]

                        val options = arrayOf("✏️ Edit", "🗑️ Delete")

                        val builder = androidx.appcompat.app.AlertDialog.Builder(this@ViewExpensesActivity)
                        builder.setTitle("Choose Action")
                        builder.setItems(options) { _, which ->
                            when (which) {
                                0 -> showEditDialog(selectedExpense)
                                1 -> showDeleteDialog(selectedExpense)
                            }
                        }
                        builder.show()

                        true
                    }
                }
            } catch (e: Exception) {
                Log.e("ViewExpensesActivity", "Error loading expenses: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@ViewExpensesActivity, "Error loading expenses", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    //method that allows the user to delete an expense
    private fun showDeleteDialog(expense: Expense) {

        //dialog box to let the user confirm delete
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)

        builder.setTitle("Delete Expense")
        builder.setMessage("Are you sure you want to delete this expense?")

        builder.setPositiveButton("Delete") { _, _ ->
            lifecycleScope.launch {
                db.expenseDao().deleteExpense(expense)

                runOnUiThread {
                    Toast.makeText(this@ViewExpensesActivity, "Expense deleted", Toast.LENGTH_SHORT).show()
                    loadExpenses()
                }
            }
        }

        builder.setNegativeButton("Cancel", null)

        builder.show()
    }

    //methd to allow the user to edit an expense
    private fun showEditDialog(expense: Expense) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_expense, null)

        val etTitle = dialogView.findViewById<EditText>(R.id.etEditTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etEditAmount)
        val etDate = dialogView.findViewById<EditText>(R.id.etEditDate)
        val etTime = dialogView.findViewById<EditText>(R.id.etEditTime)
        val etDescription = dialogView.findViewById<EditText>(R.id.etEditDescription)
        val btnUpdate = dialogView.findViewById<Button>(R.id.btnUpdateExpense)

        // Prefill
        etTitle.setText(expense.title)
        etAmount.setText(expense.amount.toString())
        etDate.setText(expense.date)
        etTime.setText(expense.time)
        etDescription.setText(expense.description)

        // DATE PICKER
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(this,
                { _, year, month, day ->
                    etDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // TIME PICKER
        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(this,
                { _, hour, minute ->
                    etTime.setText(String.format("%02d:%02d", hour, minute))
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }

        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.show()

        btnUpdate.setOnClickListener {
            val updatedExpense = expense.copy(
                title = etTitle.text.toString(),
                amount = etAmount.text.toString().toDoubleOrNull() ?: 0.0,
                date = etDate.text.toString(),
                time = etTime.text.toString(),
                description = etDescription.text.toString()
            )

            lifecycleScope.launch {
                db.expenseDao().updateExpense(updatedExpense)

                runOnUiThread {
                    Toast.makeText(this@ViewExpensesActivity, "Expense updated", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadExpenses()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadExpenses()
    }
}
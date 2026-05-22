package com.pennywize.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.Category
import kotlinx.coroutines.launch

class CategoriesActivity : AppCompatActivity() {

    private lateinit var etCategoryName: EditText
    private lateinit var btnAddCategory: Button
    private lateinit var btnBack: Button
    private lateinit var containerCategories: LinearLayout
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private val categoryList = mutableListOf<Category>()
    private lateinit var btnLogout: Button


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        userId = intent.getIntExtra("userId", 0)
        db = AppDatabase.getDatabase(this)

        etCategoryName = findViewById(R.id.etCategoryName)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        btnBack = findViewById(R.id.btnBack)
        containerCategories = findViewById(R.id.containerCategories)
        btnLogout = findViewById(R.id.btnLogout)

        loadCategories()

        btnAddCategory.setOnClickListener {
            addCategory()
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

            Toast.makeText(this, "Already on Categories", Toast.LENGTH_SHORT).show()
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

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val categories = db.categoryDao().getCategoriesByUser(userId)
                categoryList.clear()
                categoryList.addAll(categories)

                runOnUiThread {
                    displayCategories()
                    Log.d("CategoriesActivity", "Loaded ${categories.size} categories")
                }
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error loading categories: ${e.message}")
            }
        }
    }

    private fun displayCategories() {
        // Clear container
        containerCategories.removeAllViews()

        if (categoryList.isEmpty()) {
            val emptyText = TextView(this)
            emptyText.text = "📭 No categories yet. Add one!"
            emptyText.setTextColor(android.graphics.Color.parseColor("#9CA3AF"))
            emptyText.gravity = android.view.Gravity.CENTER
            emptyText.setPadding(32, 32, 32, 32)
            containerCategories.addView(emptyText)
            return
        }

        for (category in categoryList) {
            val categoryView = LayoutInflater.from(this).inflate(R.layout.item_category, containerCategories, false)

            val tvCategoryName = categoryView.findViewById<TextView>(R.id.tvCategoryName)
            val ivDeleteCategory = categoryView.findViewById<ImageView>(R.id.ivDeleteCategory)

            tvCategoryName.text = category.name

            ivDeleteCategory.setOnClickListener {
                deleteCategory(category)
            }

            containerCategories.addView(categoryView)
        }
    }

    private fun addCategory() {
        val name = etCategoryName.text.toString().trim()
        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter a category name", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val category = Category(name = name, userId = userId)
                db.categoryDao().insertCategory(category)
                Log.d("CategoriesActivity", "Category added: $name")

                runOnUiThread {
                    etCategoryName.text.clear()
                    Toast.makeText(this@CategoriesActivity, "Category added!", Toast.LENGTH_SHORT).show()
                    loadCategories() // Refresh the list
                }
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error adding category: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@CategoriesActivity, "Error adding category", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun deleteCategory(category: Category) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete Category")
            .setMessage("Are you sure you want to delete \"${category.name}\"?")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        db.categoryDao().deleteCategory(category)
                        runOnUiThread {
                            Toast.makeText(this@CategoriesActivity, "Category deleted", Toast.LENGTH_SHORT).show()
                            loadCategories() // Refresh the list
                        }
                    } catch (e: Exception) {
                        Log.e("CategoriesActivity", "Error deleting category: ${e.message}")
                        runOnUiThread {
                            Toast.makeText(this@CategoriesActivity, "Error deleting category", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
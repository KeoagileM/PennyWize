package com.pennywize.app

import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.Category
import kotlinx.coroutines.launch

// Activity for managing expense categories
class CategoriesActivity : AppCompatActivity() {

    private lateinit var etCategoryName: EditText
    private lateinit var btnAddCategory: Button
    private lateinit var btnBack: Button
    private lateinit var lvCategories: ListView
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private val categoryList = mutableListOf<Category>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_categories)

        // Get userId from intent
        userId = intent.getIntExtra("userId", 0)

        // Initialize database
        db = AppDatabase.getDatabase(this)

        // Initialize views
        etCategoryName = findViewById(R.id.etCategoryName)
        btnAddCategory = findViewById(R.id.btnAddCategory)
        btnBack = findViewById(R.id.btnBack)
        lvCategories = findViewById(R.id.lvCategories)

        // Setup list adapter
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        lvCategories.adapter = adapter

        // Load categories
        loadCategories()

        // Add category button
        btnAddCategory.setOnClickListener {
            addCategory()
        }

        // Back button
        btnBack.setOnClickListener {
            finish()
        }

        // Long click to delete category
        lvCategories.setOnItemLongClickListener { _, _, position, _ ->
            deleteCategory(position)
            true
        }
    }

    private fun loadCategories() {
        lifecycleScope.launch {
            try {
                val categories = db.categoryDao().getCategoriesByUser(userId)
                categoryList.clear()
                categoryList.addAll(categories)
                val names = categories.map { it.name }
                runOnUiThread {
                    adapter.clear()
                    adapter.addAll(names)
                    adapter.notifyDataSetChanged()
                }
                Log.d("CategoriesActivity", "Loaded ${categories.size} categories")
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error loading categories: ${e.message}")
            }
        }
    }

    private fun addCategory() {
        val name = etCategoryName.text.toString().trim()

        // Validate input
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
                    loadCategories()
                }
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error adding category: ${e.message}")
            }
        }
    }

    private fun deleteCategory(position: Int) {
        val category = categoryList[position]
        lifecycleScope.launch {
            try {
                db.categoryDao().deleteCategory(category)
                Log.d("CategoriesActivity", "Category deleted: ${category.name}")
                runOnUiThread {
                    Toast.makeText(this@CategoriesActivity, "Category deleted", Toast.LENGTH_SHORT).show()
                    loadCategories()
                }
            } catch (e: Exception) {
                Log.e("CategoriesActivity", "Error deleting category: ${e.message}")
            }
        }
    }
}
package com.pennywize.app

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.pennywize.app.database.AppDatabase
import com.pennywize.app.model.Category
import com.pennywize.app.model.Expense
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText

    private lateinit var etTime: EditText
    private lateinit var etDescription: EditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnTakePhoto: Button
    private lateinit var btnSaveExpense: Button
    private lateinit var btnBack: Button
    private lateinit var ivReceiptPhoto: ImageView
    private lateinit var db: AppDatabase
    private var userId: Int = 0
    private var photoPath: String? = null
    private var categoryList = mutableListOf<Category>()
    private lateinit var photoUri: Uri
    private lateinit var btnLogout: Button

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        userId = intent.getIntExtra("userId", 0)
        db = AppDatabase.getDatabase(this)

        initViews()
        loadCategories()
        setupDatePicker()
        setupTimePicker()
        setupPhotoButton()
        setupSaveButton()
        setupBackButton()
        setupBottomNavigation()


    }

    private fun initViews() {
        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        etDate = findViewById(R.id.etDate)
        etTime = findViewById(R.id.etTime)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        btnBack = findViewById(R.id.btnBack)
        ivReceiptPhoto = findViewById(R.id.ivReceiptPhoto)
        btnLogout = findViewById<Button>(R.id.btnLogout)


        btnLogout.setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
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
                    val adapter = ArrayAdapter(
                        this@AddExpenseActivity,
                        android.R.layout.simple_spinner_item,
                        names
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerCategory.adapter = adapter
                }
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error loading categories: ${e.message}")
            }
        }
    }

    private fun setupDatePicker() {
        etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    val selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    etDate.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupTimePicker() {
        etTime.setOnClickListener {
            val calendar = Calendar.getInstance()
            TimePickerDialog(
                this,
                { _, hourOfDay, minute ->
                    val selectedTime = String.format("%02d:%02d", hourOfDay, minute)
                    etTime.setText(selectedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                true
            ).show()
        }


    }

    private fun setupPhotoButton() {
        btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndCapture()
        }
    }

    private fun setupSaveButton() {
        btnSaveExpense.setOnClickListener {
            saveExpense()
        }
    }

    private fun setupBackButton() {
        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun checkCameraPermissionAndCapture() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            takePhoto()
        }
    }

    //function to set on click activity for nagvigation bar buttons
    private fun setupBottomNavigation() {
        val navHome = findViewById<LinearLayout>(R.id.navHome)
        val navExpenses = findViewById<LinearLayout>(R.id.navExpenses)
        val navCategories = findViewById<LinearLayout>(R.id.navCategories)
        val navGoals = findViewById<LinearLayout>(R.id.navGoals)

        setActiveTab(navExpenses)
        setInactiveTab(navHome)
        setInactiveTab(navCategories)
        setInactiveTab(navGoals)

        navHome.setOnClickListener {
            val intent = Intent(this, HomeActivity::class.java)
            intent.putExtra("userId", userId)
            startActivity(intent)
        }

        navExpenses.setOnClickListener {
            Toast.makeText(this, "Already on Expenses", Toast.LENGTH_SHORT).show()
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
    private fun takePhoto() {
        val photoFile = createImageFile()
        photoUri = FileProvider.getUriForFile(
            this,
            "com.pennywize.app.fileprovider",
            photoFile
        )
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
        startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir).also {
            photoPath = it.absolutePath
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
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            ivReceiptPhoto.visibility = ImageView.VISIBLE
            ivReceiptPhoto.setImageURI(photoUri)
            Log.d("AddExpenseActivity", "Photo captured: $photoPath")
            Toast.makeText(this, "Photo saved!", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA_PERMISSION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            takePhoto()
        } else {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveExpense() {
        val title = etTitle.text.toString().trim()
        val amountStr = etAmount.text.toString().trim()
        val date = etDate.text.toString().trim()
        val time = etTime.text.toString().trim()

        val description = etDescription.text.toString().trim()

        // Validate inputs
        if (title.isEmpty() || amountStr.isEmpty() || date.isEmpty() ||
            time.isEmpty()|| description.isEmpty()) {
            Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (categoryList.isEmpty()) {
            Toast.makeText(this, "Please create a category first", Toast.LENGTH_SHORT).show()
            return
        }

        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedCategory = categoryList[spinnerCategory.selectedItemPosition]

        lifecycleScope.launch {
            try {
                val expense = Expense(
                    title = title,
                    amount = amount,
                    date = date,
                    time = time,
                    description = description,
                    categoryId = selectedCategory.id,
                    userId = userId,
                    photoPath = photoPath
                )
                db.expenseDao().insertExpense(expense)
                Log.d("AddExpenseActivity", "Expense saved: $title - R$amount")
                runOnUiThread {
                    Toast.makeText(this@AddExpenseActivity, "Expense saved!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Log.e("AddExpenseActivity", "Error saving expense: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@AddExpenseActivity, "Error saving expense", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
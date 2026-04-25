package com.pennywize.app

import android.Manifest
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

// Activity for adding new expense entries
class AddExpenseActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etAmount: EditText
    private lateinit var etDate: EditText
    private lateinit var etStartTime: EditText
    private lateinit var etEndTime: EditText
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

    companion object {
        private const val REQUEST_IMAGE_CAPTURE = 1
        private const val REQUEST_CAMERA_PERMISSION = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        // Get userId from intent
        userId = intent.getIntExtra("userId", 0)

        // Initialize database
        db = AppDatabase.getDatabase(this)

        // Initialize views
        etTitle = findViewById(R.id.etTitle)
        etAmount = findViewById(R.id.etAmount)
        etDate = findViewById(R.id.etDate)
        etStartTime = findViewById(R.id.etStartTime)
        etEndTime = findViewById(R.id.etEndTime)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnTakePhoto = findViewById(R.id.btnTakePhoto)
        btnSaveExpense = findViewById(R.id.btnSaveExpense)
        btnBack = findViewById(R.id.btnBack)
        ivReceiptPhoto = findViewById(R.id.ivReceiptPhoto)

        // Set today's date
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDate.setText(sdf.format(Date()))

        // Load categories into spinner
        loadCategories()

        // Take photo button
        btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndCapture()
        }

        // Save expense button
        btnSaveExpense.setOnClickListener {
            saveExpense()
        }

        // Back button
        btnBack.setOnClickListener {
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
        val startTime = etStartTime.text.toString().trim()
        val endTime = etEndTime.text.toString().trim()
        val description = etDescription.text.toString().trim()

        // Validate inputs
        if (title.isEmpty() || amountStr.isEmpty() || date.isEmpty() ||
            startTime.isEmpty() || endTime.isEmpty() || description.isEmpty()) {
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
                    startTime = startTime,
                    endTime = endTime,
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
package com.pennywize.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Expense entity for Room Database
@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val date: String,
    val startTime: String,
    val endTime: String,
    val description: String,
    val categoryId: Int,
    val userId: Int,
    val photoPath: String? = null
)
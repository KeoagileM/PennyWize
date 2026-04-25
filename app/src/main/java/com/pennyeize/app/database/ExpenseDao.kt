package com.pennywize.app.database

import androidx.room.*
import com.pennywize.app.model.Expense

// Data Access Object for Expense operations
@Dao
interface ExpenseDao {
    @Insert
    suspend fun insertExpense(expense: Expense)

    @Query("SELECT * FROM expenses WHERE userId = :userId AND date BETWEEN :startDate AND :endDate")
    suspend fun getExpensesByPeriod(userId: Int, startDate: String, endDate: String): List<Expense>

    @Query("SELECT * FROM expenses WHERE userId = :userId")
    suspend fun getAllExpenses(userId: Int): List<Expense>

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE userId = :userId AND categoryId = :categoryId AND date BETWEEN :startDate AND :endDate")
    suspend fun getTotalByCategory(userId: Int, categoryId: Int, startDate: String, endDate: String): Double?
}
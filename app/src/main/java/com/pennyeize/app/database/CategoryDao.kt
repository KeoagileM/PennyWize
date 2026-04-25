package com.pennywize.app.database

import androidx.room.*
import com.pennywize.app.model.Category

// Data Access Object for Category operations
@Dao
interface CategoryDao {
    @Insert
    suspend fun insertCategory(category: Category)

    @Query("SELECT * FROM categories WHERE userId = :userId")
    suspend fun getCategoriesByUser(userId: Int): List<Category>

    @Delete
    suspend fun deleteCategory(category: Category)

    @Update
    suspend fun updateCategory(category: Category)
}
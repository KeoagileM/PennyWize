package com.pennywize.app.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.pennywize.app.model.User
import com.pennywize.app.model.Category
import com.pennywize.app.model.Expense
import com.pennywize.app.model.Goal

// Main Room Database class for PennyWize app
@Database(
    entities = [User::class, Category::class, Expense::class, Goal::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun categoryDao(): CategoryDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Singleton pattern to prevent multiple instances of database
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pennywize_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
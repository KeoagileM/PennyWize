package com.pennywize.app.database

import androidx.room.*
import com.pennywize.app.model.Goal

// Data Access Object for Goal operations
@Dao
interface GoalDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: Goal)

    @Query("SELECT * FROM goals WHERE userId = :userId AND month = :month")
    suspend fun getGoalByMonth(userId: Int, month: String): Goal?

    @Query("SELECT * FROM goals WHERE userId = :userId")
    suspend fun getAllGoals(userId: Int): List<Goal>

    @Delete
    suspend fun deleteGoal(goal: Goal)


}
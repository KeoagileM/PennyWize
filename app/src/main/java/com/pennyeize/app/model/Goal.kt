package com.pennywize.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Goal entity for Room Database
@Entity(tableName = "goals")
data class Goal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val minimumGoal: Double,
    val maximumGoal: Double,
    val month: String
)
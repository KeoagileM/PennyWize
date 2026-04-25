package com.pennywize.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// Category entity for Room Database
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val userId: Int
)
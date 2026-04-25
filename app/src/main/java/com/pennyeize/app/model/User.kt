package com.pennywize.app.model

import androidx.room.Entity
import androidx.room.PrimaryKey

// User entity for Room Database
@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val username: String,
    val password: String
)
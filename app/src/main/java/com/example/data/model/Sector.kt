package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sectors")
data class Sector(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

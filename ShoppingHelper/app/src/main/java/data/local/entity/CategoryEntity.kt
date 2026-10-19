package com.example.shoppinghelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey
    val name: String,          // Название категории (например, "Молочное")
    val keywords: String       // Ключевые слова для автокатегоризации: "молоко,кефир,сыр"
)
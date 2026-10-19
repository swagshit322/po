package com.example.shoppinghelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_lists")
data class ShoppingListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,              // Уникальный ID, создаётся автоматически
    val name: String,              // Название списка (например, "Продукты")
    val storeName: String? = null, // Привязка к магазину (опционально, требование F-01)
    val createdAt: Long = System.currentTimeMillis(), // Дата создания
    val isArchived: Boolean = false, // true = список в архиве (история, требование F-11)
    val reminderTime: Long? = null   // Время для напоминания (требование F-10)
)
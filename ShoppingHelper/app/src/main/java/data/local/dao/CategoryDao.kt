package com.example.shoppinghelper.data.local.dao

import androidx.room.*
import com.example.shoppinghelper.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories")
    suspend fun getAll(): List<CategoryEntity>

    // Названия категорий в порядке добавления: базовые сначала, свои в конце
    @Query("SELECT name FROM categories ORDER BY rowid")
    fun getAllNames(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(category: CategoryEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(categories: List<CategoryEntity>)
}

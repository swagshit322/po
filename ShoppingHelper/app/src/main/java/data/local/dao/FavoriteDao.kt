package com.example.shoppinghelper.data.local.dao

import androidx.room.*
import com.example.shoppinghelper.data.local.entity.FavoriteItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteDao {
    @Query("SELECT * FROM favorites ORDER BY name ASC")
    fun getAll(): Flow<List<FavoriteItemEntity>>

    // Названия той же длины: сравнение без учёта регистра делается в Kotlin
    // (NOCASE в SQLite работает только для латиницы)
    @Query("SELECT name FROM favorites WHERE length(name) = :length")
    suspend fun getNamesByLength(length: Int): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: FavoriteItemEntity)

    @Delete
    suspend fun delete(item: FavoriteItemEntity)
}

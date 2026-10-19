package com.example.shoppinghelper.data.local.dao

import androidx.room.*
import com.example.shoppinghelper.data.local.entity.FavoriteListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FavoriteListDao {
    @Query("SELECT * FROM favorite_lists ORDER BY name ASC")
    fun getAll(): Flow<List<FavoriteListEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(list: FavoriteListEntity)

    @Delete
    suspend fun delete(list: FavoriteListEntity)
}
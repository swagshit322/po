package com.example.shoppinghelper.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorite_lists")
data class FavoriteListEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val storeName: String? = null
)
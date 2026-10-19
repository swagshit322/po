package com.example.shoppinghelper.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.shoppinghelper.data.local.dao.*
import com.example.shoppinghelper.data.local.entity.*

@Database(
    entities = [
        ShoppingListEntity::class,
        ItemEntity::class,
        FavoriteItemEntity::class,
        FavoriteListEntity::class,
        CategoryEntity::class
    ],
    version = 2,  // Повышать при каждом изменении таблиц
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun itemDao(): ItemDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun favoriteListDao(): FavoriteListDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "shopping_helper_database"
                )
                    // При смене версии БД старые данные стираются. Пока приложение
                    // в разработке это удобно, перед релизом нужно писать миграции.
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
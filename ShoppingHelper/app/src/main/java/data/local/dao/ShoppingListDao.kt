package com.example.shoppinghelper.data.local.dao

import androidx.room.*
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    // Получить все активные (не в архиве) списки. Flow автоматически обновляет UI при изменении БД
    @Query("SELECT * FROM shopping_lists WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getActiveLists(): Flow<List<ShoppingListEntity>>

    // Активные списки вместе с количеством товаров (для прогресса на главном экране)
    @Query("""
        SELECT l.*,
            (SELECT COUNT(*) FROM items i WHERE i.listId = l.id) AS totalCount,
            (SELECT COUNT(*) FROM items i WHERE i.listId = l.id AND i.isBought = 1) AS boughtCount
        FROM shopping_lists l
        WHERE l.isArchived = 0
        ORDER BY l.createdAt DESC
    """)
    fun getActiveListsWithStats(): Flow<List<ListWithStats>>

    // Получить архивные списки (история, требование F-11)
    @Query("SELECT * FROM shopping_lists WHERE isArchived = 1 ORDER BY createdAt DESC")
    fun getArchivedLists(): Flow<List<ShoppingListEntity>>

    @Query("SELECT * FROM shopping_lists WHERE id = :id")
    fun getListById(id: Long): Flow<ShoppingListEntity?>

    // Все магазины, которые пользователь указывал в списках (для экрана карт, требование F-05)
    @Query("""
        SELECT DISTINCT storeName FROM shopping_lists
        WHERE storeName IS NOT NULL AND TRIM(storeName) != ''
        ORDER BY storeName COLLATE NOCASE
    """)
    fun getStoreNames(): Flow<List<String>>

    // Списки с ещё не наступившими напоминаниями (нужны после перезагрузки телефона, F-10)
    @Query("SELECT * FROM shopping_lists WHERE isArchived = 0 AND reminderTime > :now")
    suspend fun getUpcomingReminders(now: Long): List<ShoppingListEntity>

    @Insert
    suspend fun insert(list: ShoppingListEntity): Long

    @Update
    suspend fun update(list: ShoppingListEntity)

    @Delete
    suspend fun delete(list: ShoppingListEntity)

    // Переместить список в архив (завершить покупки)
    @Query("UPDATE shopping_lists SET isArchived = 1, reminderTime = NULL WHERE id = :id")
    suspend fun archive(id: Long)

    @Query("UPDATE shopping_lists SET reminderTime = :time WHERE id = :id")
    suspend fun setReminder(id: Long, time: Long?)

    // Очистка архива (риск из ТЗ: ограничение объёма хранилища). Товары удаляются каскадно
    @Query("DELETE FROM shopping_lists WHERE isArchived = 1")
    suspend fun deleteArchived()
}

// Список + счётчики товаров
data class ListWithStats(
    @Embedded val list: ShoppingListEntity,
    val totalCount: Int,
    val boughtCount: Int
)

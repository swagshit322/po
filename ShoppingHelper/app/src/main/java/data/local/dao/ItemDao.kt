package com.example.shoppinghelper.data.local.dao

import androidx.room.*
import com.example.shoppinghelper.data.local.entity.ItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ItemDao {
    // Товары списка: некупленные сверху, купленные снизу (требование F-04)
    @Query("SELECT * FROM items WHERE listId = :listId ORDER BY isBought ASC, sortOrder ASC")
    fun getItemsForList(listId: Long): Flow<List<ItemEntity>>

    // Получить товары списка один раз (для функции "Повторить заказ")
    @Query("SELECT * FROM items WHERE listId = :listId")
    suspend fun getItemsForListOnce(listId: Long): List<ItemEntity>

    @Insert
    suspend fun insert(item: ItemEntity): Long

    @Update
    suspend fun update(item: ItemEntity)

    @Delete
    suspend fun delete(item: ItemEntity)

    // Быстрое переключение статуса "куплено"
    @Query("UPDATE items SET isBought = :bought WHERE id = :itemId")
    suspend fun setBought(itemId: Long, bought: Boolean)

    // ВАЖНО: COLLATE NOCASE и lower() в SQLite не понимают кириллицу («Молоко» ≠ «молоко»),
    // поэтому сравнение без учёта регистра делается в Kotlin (ShoppingUtils).
    // Чтобы не читать всю таблицу, заранее отбираем строки той же длины.

    // Кандидаты для поиска категории по истории (F-03): товары с названием той же длины, новые сначала
    @Query("SELECT name, category FROM items WHERE length(name) = :length ORDER BY id DESC")
    suspend fun getNameCategoryByLength(length: Int): List<NameCategory>

    // Купленные товары для "Умных подсказок" (F-08). Если задана длина названия магазина,
    // берутся только списки с такой длиной. Текущий список не учитывается.
    @Query("""
        SELECT i.name AS name, i.category AS category, i.unit AS unit, TRIM(l.storeName) AS storeName
        FROM items i
        INNER JOIN shopping_lists l ON i.listId = l.id
        WHERE i.isBought = 1
          AND l.id != :excludeListId
          AND (:storeLength IS NULL OR length(TRIM(l.storeName)) = :storeLength)
    """)
    suspend fun getBoughtItems(excludeListId: Long, storeLength: Int?): List<BoughtItemRow>
}

data class NameCategory(val name: String, val category: String)

data class BoughtItemRow(
    val name: String,
    val category: String,
    val unit: String,
    val storeName: String?
)

// Результат подсчёта частых покупок
data class FrequentItem(
    val name: String,
    val category: String,
    val unit: String,
    val cnt: Int
)
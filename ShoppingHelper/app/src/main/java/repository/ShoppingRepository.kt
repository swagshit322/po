package com.example.shoppinghelper.data.repository

import com.example.shoppinghelper.data.local.dao.*
import com.example.shoppinghelper.data.local.entity.*
import com.example.shoppinghelper.ShoppingUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ShoppingRepository(
    private val listDao: ShoppingListDao,
    private val itemDao: ItemDao,
    private val favoriteDao: FavoriteDao,
    private val favoriteListDao: FavoriteListDao,
    private val categoryDao: CategoryDao
) {
    fun getActiveLists(): Flow<List<ShoppingListEntity>> = listDao.getActiveLists()
    fun getActiveListsWithStats(): Flow<List<ListWithStats>> = listDao.getActiveListsWithStats()
    fun getArchivedLists(): Flow<List<ShoppingListEntity>> = listDao.getArchivedLists()
    fun getList(id: Long): Flow<ShoppingListEntity?> = listDao.getListById(id)
    suspend fun insertList(list: ShoppingListEntity) = listDao.insert(list)
    suspend fun updateList(list: ShoppingListEntity) = listDao.update(list)
    suspend fun archiveList(id: Long) = listDao.archive(id)
    suspend fun deleteList(list: ShoppingListEntity) = listDao.delete(list)
    suspend fun clearArchive() = listDao.deleteArchived()

    // === НАПОМИНАНИЯ ===
    suspend fun setReminder(listId: Long, time: Long?) = listDao.setReminder(listId, time)
    suspend fun getUpcomingReminders(now: Long): List<ShoppingListEntity> = listDao.getUpcomingReminders(now)

    fun getItemsForList(listId: Long): Flow<List<ItemEntity>> = itemDao.getItemsForList(listId)
    suspend fun getItemsForListOnce(listId: Long): List<ItemEntity> = itemDao.getItemsForListOnce(listId)
    suspend fun insertItem(item: ItemEntity) = itemDao.insert(item)
    suspend fun updateItem(item: ItemEntity) = itemDao.update(item)
    suspend fun setItemBought(itemId: Long, bought: Boolean) = itemDao.setBought(itemId, bought)
    suspend fun deleteItem(item: ItemEntity) = itemDao.delete(item)
    // Категория из истории (F-03): последний товар с таким же названием без учёта регистра
    suspend fun getLastCategoryForName(name: String): String? {
        val trimmed = name.trim()
        return itemDao.getNameCategoryByLength(trimmed.length)
            .firstOrNull { ShoppingUtils.sameName(it.name, trimmed) }
            ?.category
    }

    fun getFavorites(): Flow<List<FavoriteItemEntity>> = favoriteDao.getAll()
    suspend fun insertFavorite(item: FavoriteItemEntity) = favoriteDao.insert(item)
    suspend fun deleteFavorite(item: FavoriteItemEntity) = favoriteDao.delete(item)
    suspend fun isFavorite(name: String): Boolean {
        val trimmed = name.trim()
        return favoriteDao.getNamesByLength(trimmed.length).any { ShoppingUtils.sameName(it, trimmed) }
    }

    // === КАТЕГОРИИ ===
    fun getCategoryNames(): Flow<List<String>> = categoryDao.getAllNames()
    suspend fun insertCategory(name: String) = categoryDao.insert(CategoryEntity(name = name, keywords = ""))

    // Базовые категории добавляются только если их ещё нет (IGNORE), поэтому вызывать можно при каждом запуске
    suspend fun ensureDefaultCategories(names: List<String>) =
        categoryDao.insertAll(names.map { CategoryEntity(name = it, keywords = "") })

    // === ИЗБРАННЫЕ СПИСКИ ===
    fun getFavoriteLists(): Flow<List<FavoriteListEntity>> = favoriteListDao.getAll()
    suspend fun insertFavoriteList(list: FavoriteListEntity) = favoriteListDao.insert(list)
    suspend fun deleteFavoriteList(list: FavoriteListEntity) = favoriteListDao.delete(list)

    // «Пятёрочка» и «пятёрочка» показываем один раз
    fun getStoreNames(): Flow<List<String>> = listDao.getStoreNames()
        .map { names -> names.map { it.trim() }.distinctBy { it.lowercase() } }

    suspend fun getFrequentItems(storeName: String?, excludeListId: Long, limit: Int): List<FrequentItem> {
        val store = storeName?.trim()?.ifBlank { null }
        val rows = itemDao.getBoughtItems(excludeListId, store?.length)
        return ShoppingUtils.countFrequentItems(rows, store, limit)
    }
}

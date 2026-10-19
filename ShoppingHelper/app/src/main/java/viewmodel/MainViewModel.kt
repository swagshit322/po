package com.example.shoppinghelper.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.shoppinghelper.CategoryUtils
import com.example.shoppinghelper.ShoppingUtils
import com.example.shoppinghelper.data.local.dao.FrequentItem
import com.example.shoppinghelper.data.local.dao.ListWithStats
import com.example.shoppinghelper.data.local.entity.FavoriteItemEntity
import com.example.shoppinghelper.data.local.entity.FavoriteListEntity
import com.example.shoppinghelper.data.local.entity.ItemEntity
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity
import com.example.shoppinghelper.data.repository.ShoppingRepository
import com.example.shoppinghelper.reminder.ReminderScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ShoppingRepository,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val activeLists: Flow<List<ListWithStats>> = repository.getActiveListsWithStats()
    val archivedLists: Flow<List<ShoppingListEntity>> = repository.getArchivedLists()
    val favorites: Flow<List<FavoriteItemEntity>> = repository.getFavorites()
    val favoriteLists: Flow<List<FavoriteListEntity>> = repository.getFavoriteLists()
    val categories: Flow<List<String>> = repository.getCategoryNames()
    val storeNames: Flow<List<String>> = repository.getStoreNames()

    init {
        viewModelScope.launch { repository.ensureDefaultCategories(CategoryUtils.defaultCategories) }
    }

    // === СПИСКИ (UC-01) ===
    fun getList(id: Long): Flow<ShoppingListEntity?> = repository.getList(id)

    fun addList(name: String, storeName: String? = null) {
        viewModelScope.launch {
            repository.insertList(ShoppingListEntity(name = name, storeName = storeName))
        }
    }

    fun updateList(list: ShoppingListEntity, name: String, storeName: String?) {
        viewModelScope.launch {
            val updated = list.copy(name = name, storeName = storeName)
            repository.updateList(updated)
            // В тексте уведомления есть название и магазин, поэтому перепланируем
            reminderScheduler.schedule(updated)
        }
    }

    // Товары удаляются из БД вместе со списком (CASCADE), поэтому сохраняем их заранее,
    // чтобы кнопка «Отменить» могла вернуть список целиком
    fun deleteList(list: ShoppingListEntity, onDeleted: (DeletedList) -> Unit = {}) {
        viewModelScope.launch {
            val items = repository.getItemsForListOnce(list.id)
            reminderScheduler.cancel(list.id)
            repository.deleteList(list)
            onDeleted(DeletedList(list, items))
        }
    }

    fun restoreList(deleted: DeletedList) {
        viewModelScope.launch {
            repository.insertList(deleted.list)
            deleted.items.forEach { repository.insertItem(it) }
            reminderScheduler.schedule(deleted.list)
        }
    }

    fun archiveList(id: Long) {
        viewModelScope.launch {
            reminderScheduler.cancel(id)
            repository.archiveList(id)
        }
    }

    fun clearArchive() {
        viewModelScope.launch { repository.clearArchive() }
    }

    fun duplicateList(oldList: ShoppingListEntity, oldItems: List<ItemEntity>) {
        viewModelScope.launch {
            val newListId = repository.insertList(
                ShoppingListEntity(
                    name = "${oldList.name} (${android.text.format.DateFormat.format("dd.MM", System.currentTimeMillis())})",
                    storeName = oldList.storeName
                )
            )
            oldItems.forEach { item ->
                repository.insertItem(
                    ItemEntity(
                        listId = newListId,
                        name = item.name,
                        quantity = item.quantity,
                        unit = item.unit,
                        category = item.category,
                        price = item.price,
                        isBought = false
                    )
                )
            }
        }
    }

    fun repeatOrder(list: ShoppingListEntity) {
        viewModelScope.launch {
            val items = repository.getItemsForListOnce(list.id)
            duplicateList(list, items)
        }
    }

    // === НАПОМИНАНИЯ (F-10) ===
    fun setReminder(list: ShoppingListEntity, time: Long) {
        viewModelScope.launch {
            repository.setReminder(list.id, time)
            reminderScheduler.schedule(list.copy(reminderTime = time))
        }
    }

    fun clearReminder(list: ShoppingListEntity) {
        viewModelScope.launch {
            reminderScheduler.cancel(list.id)
            repository.setReminder(list.id, null)
        }
    }

    // === ТОВАРЫ (UC-02) ===
    fun getItemsForList(listId: Long): Flow<List<ItemEntity>> = repository.getItemsForList(listId)

    fun addItem(item: ItemEntity) {
        viewModelScope.launch { repository.insertItem(item) }
    }

    fun updateItem(item: ItemEntity) {
        viewModelScope.launch { repository.updateItem(item) }
    }

    fun toggleItemBought(itemId: Long, isBought: Boolean) {
        viewModelScope.launch { repository.setItemBought(itemId, !isBought) }
    }

    fun deleteItem(item: ItemEntity) {
        viewModelScope.launch { repository.deleteItem(item) }
    }

    fun calculateTotalPrice(items: List<ItemEntity>): Double = ShoppingUtils.totalPrice(items)

    // Автокатегоризация (F-03): сначала локальная история пользователя, потом встроенный справочник
    suspend fun guessCategory(itemName: String): String? {
        val name = itemName.trim()
        if (name.length < 2) return null
        return repository.getLastCategoryForName(name) ?: CategoryUtils.guessCategoryByKeywords(name)
    }

    // === ИЗБРАННЫЕ ТОВАРЫ (F-07) ===
    fun addItemToFavorites(name: String, unit: String, category: String) {
        viewModelScope.launch {
            if (!repository.isFavorite(name)) {
                repository.insertFavorite(FavoriteItemEntity(name = name, unit = unit, category = category))
            }
        }
    }

    fun removeFavoriteItem(item: FavoriteItemEntity) {
        viewModelScope.launch { repository.deleteFavorite(item) }
    }

    fun restoreFavoriteItem(item: FavoriteItemEntity) {
        viewModelScope.launch { repository.insertFavorite(item) }
    }

    // === УМНЫЕ ПОДСКАЗКИ (F-08) ===
    // Берём с запасом: часть товаров может уже быть в списке, экран их отфильтрует
    suspend fun getSmartSuggestions(list: ShoppingListEntity): List<FrequentItem> =
        repository.getFrequentItems(list.storeName?.ifBlank { null }, list.id, limit = 15)

    // === КАТЕГОРИИ ===
    fun addCategory(name: String) {
        viewModelScope.launch { repository.insertCategory(name) }
    }

    // === ИЗБРАННЫЕ СПИСКИ ===
    fun addListToFavorites(list: ShoppingListEntity) {
        viewModelScope.launch {
            repository.insertFavoriteList(
                FavoriteListEntity(name = list.name, storeName = list.storeName)
            )
        }
    }

    fun removeFromFavoriteList(list: FavoriteListEntity) {
        viewModelScope.launch { repository.deleteFavoriteList(list) }
    }

    fun restoreFavoriteList(list: FavoriteListEntity) {
        viewModelScope.launch { repository.insertFavoriteList(list) }
    }

    fun createListFromFavorite(favorite: FavoriteListEntity) {
        viewModelScope.launch {
            repository.insertList(
                ShoppingListEntity(
                    name = favorite.name,  // Копируем название без даты
                    storeName = favorite.storeName
                )
            )
        }
    }
}

// Снимок удалённого списка вместе с товарами, нужен для отмены удаления
data class DeletedList(
    val list: ShoppingListEntity,
    val items: List<ItemEntity>
)

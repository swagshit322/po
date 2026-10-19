package com.example.shoppinghelper

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.shoppinghelper.data.local.AppDatabase
import com.example.shoppinghelper.data.local.entity.ItemEntity
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity
import com.example.shoppinghelper.data.repository.ShoppingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Интеграционные тесты работы с SQLite (раздел 7 ТЗ). База создаётся в памяти и не трогает данные приложения
@RunWith(AndroidJUnit4::class)
class ShoppingDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: ShoppingRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        repository = ShoppingRepository(
            db.shoppingListDao(), db.itemDao(), db.favoriteDao(), db.favoriteListDao(), db.categoryDao()
        )
    }

    @After
    fun tearDown() = db.close()

    private suspend fun newList(name: String, store: String? = null, archived: Boolean = false): Long =
        db.shoppingListDao().insert(ShoppingListEntity(name = name, storeName = store, isArchived = archived))

    @Test
    fun купленные_товары_идут_вниз_списка() = runBlocking { // F-04
        val listId = newList("Продукты")
        val itemDao = db.itemDao()
        val milkId = itemDao.insert(ItemEntity(listId = listId, name = "Молоко"))
        itemDao.insert(ItemEntity(listId = listId, name = "Хлеб"))
        itemDao.setBought(milkId, true)

        val items = itemDao.getItemsForList(listId).first()
        assertEquals(listOf("Хлеб", "Молоко"), items.map { it.name })
    }

    @Test
    fun удаление_списка_удаляет_его_товары() = runBlocking {
        val listId = newList("Продукты")
        db.itemDao().insert(ItemEntity(listId = listId, name = "Молоко"))
        val list = db.shoppingListDao().getListById(listId).first()!!

        db.shoppingListDao().delete(list)

        assertTrue(db.itemDao().getItemsForListOnce(listId).isEmpty())
    }

    @Test
    fun архивирование_и_очистка_архива() = runBlocking { // F-11
        val listId = newList("Продукты")
        db.shoppingListDao().setReminder(listId, System.currentTimeMillis() + 60_000)
        db.shoppingListDao().archive(listId)

        assertTrue(db.shoppingListDao().getActiveLists().first().isEmpty())
        val archived = db.shoppingListDao().getArchivedLists().first()
        assertEquals(1, archived.size)
        assertNull("У архивного списка не должно остаться напоминания", archived.first().reminderTime)

        db.shoppingListDao().deleteArchived()
        assertTrue(db.shoppingListDao().getArchivedLists().first().isEmpty())
    }

    @Test
    fun умные_подсказки_учитывают_магазин() = runBlocking { // F-08
        val itemDao = db.itemDao()
        repeat(3) {
            val old = newList("Старый", store = "Пятёрочка", archived = true)
            itemDao.insert(ItemEntity(listId = old, name = "Молоко", isBought = true))
        }
        val otherStore = newList("Другой", store = "Магнит", archived = true)
        itemDao.insert(ItemEntity(listId = otherStore, name = "Сыр", isBought = true))
        val current = newList("Новый", store = "Пятёрочка")

        // Регистр в названии магазина не важен (в том числе для кириллицы)
        val suggestions = repository.getFrequentItems("пятёрочка", current, 5)
        assertEquals(listOf("Молоко"), suggestions.map { it.name })
        assertEquals(3, suggestions.first().cnt)

        // Без магазина — по всем спискам
        val all = repository.getFrequentItems(null, current, 5)
        assertEquals(setOf("Молоко", "Сыр"), all.map { it.name }.toSet())
    }

    @Test
    fun категория_берётся_из_истории() = runBlocking { // F-03
        val listId = newList("Продукты")
        db.itemDao().insert(ItemEntity(listId = listId, name = "Тофу", category = "Овощи"))
        assertEquals("Овощи", repository.getLastCategoryForName("ТОФУ"))
        assertNull(repository.getLastCategoryForName("Неизвестное"))
    }

    @Test
    fun счётчики_товаров_для_главного_экрана() = runBlocking {
        val listId = newList("Продукты")
        val itemDao = db.itemDao()
        val id = itemDao.insert(ItemEntity(listId = listId, name = "Молоко"))
        itemDao.insert(ItemEntity(listId = listId, name = "Хлеб"))
        itemDao.setBought(id, true)

        val stats = db.shoppingListDao().getActiveListsWithStats().first().single()
        assertEquals(2, stats.totalCount)
        assertEquals(1, stats.boughtCount)
    }

    @Test
    fun магазины_без_повторов_и_пустых() = runBlocking { // F-05
        newList("1", store = "Магнит")
        newList("2", store = "магнит")
        newList("3", store = "  ")
        newList("4")
        assertEquals(1, repository.getStoreNames().first().size)
    }

    @Test
    fun избранное_без_дублей_в_разном_регистре() = runBlocking { // F-07
        db.favoriteDao().insert(com.example.shoppinghelper.data.local.entity.FavoriteItemEntity(name = "Молоко"))
        assertTrue(repository.isFavorite("  молоко "))
        assertTrue(!repository.isFavorite("Кефир"))
    }
}

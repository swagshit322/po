package com.example.shoppinghelper

import com.example.shoppinghelper.data.local.dao.BoughtItemRow
import com.example.shoppinghelper.data.local.dao.FrequentItem
import com.example.shoppinghelper.data.local.entity.ItemEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

class ShoppingUtilsTest {

    private fun item(name: String, price: Double? = null, bought: Boolean = false) =
        ItemEntity(listId = 1, name = name, price = price, isBought = bought)

    // === Количество и числа ===

    @Test
    fun `целое количество без дробной части`() {
        assertEquals("1", ShoppingUtils.formatQuantity(1.0))
        assertEquals("12", ShoppingUtils.formatQuantity(12.0))
        assertEquals("0.5", ShoppingUtils.formatQuantity(0.5))
        assertEquals("1.25", ShoppingUtils.formatQuantity(1.25))
    }

    @Test
    fun `число вводится через точку или запятую`() {
        assertEquals(99.9, ShoppingUtils.parseNumber("99,90")!!, 0.0001)
        assertEquals(99.9, ShoppingUtils.parseNumber("99.90")!!, 0.0001)
        assertEquals(2.0, ShoppingUtils.parseNumber("  2 ")!!, 0.0001)
    }

    @Test
    fun `пустая строка и текст не число`() {
        assertNull(ShoppingUtils.parseNumber(""))
        assertNull(ShoppingUtils.parseNumber("abc"))
    }

    @Test
    fun `цена форматируется по-русски`() {
        assertEquals("1 234,50 ₽", ShoppingUtils.formatPrice(1234.5))
        assertEquals("0,00 ₽", ShoppingUtils.formatPrice(0.0))
    }

    // === Стоимость (F-09) и прогресс ===

    @Test
    fun `сумма списка без цен не ломается`() {
        val items = listOf(item("Молоко", 89.9), item("Хлеб"), item("Сыр", 250.0))
        assertEquals(339.9, ShoppingUtils.totalPrice(items), 0.0001)
        assertEquals(0.0, ShoppingUtils.totalPrice(emptyList()), 0.0)
    }

    @Test
    fun `прогресс покупок`() {
        assertEquals(0f, ShoppingUtils.progress(0, 0), 0f)
        assertEquals(0.5f, ShoppingUtils.progress(2, 4), 0.0001f)
        assertEquals(1f, ShoppingUtils.progress(5, 5), 0f)
        assertEquals(1f, ShoppingUtils.progress(7, 5), 0f) // не больше 100%
    }

    // === Умные подсказки (F-08) ===

    @Test
    fun `подсказки без товаров, которые уже в списке`() {
        val frequent = listOf(
            FrequentItem("Молоко", "Молочное", "л", 10),
            FrequentItem("Хлеб", "Хлеб", "шт", 8),
            FrequentItem("Сыр", "Молочное", "шт", 5)
        )
        val result = ShoppingUtils.filterSuggestions(frequent, listOf(item(" молоко ")))
        assertEquals(listOf("Хлеб", "Сыр"), result.map { it.name })
    }

    @Test
    fun `подсказок не больше пяти`() {
        val frequent = (1..10).map { FrequentItem("Товар $it", "Другое", "шт", 10 - it) }
        val result = ShoppingUtils.filterSuggestions(frequent, emptyList())
        assertEquals(5, result.size)
        assertEquals("Товар 1", result.first().name) // порядок по частоте сохраняется
    }

    // === Напоминания (F-10) ===

    @Test
    fun `дата из календаря и время складываются в часовом поясе телефона`() {
        val moscow = ZoneId.of("Europe/Moscow")
        // DatePicker отдаёт полночь 20 октября 2026 по UTC
        val dateUtc = LocalDateTime.of(2026, 10, 20, 0, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        val result = ShoppingUtils.combineDateAndTime(dateUtc, 18, 30, moscow)
        val expected = LocalDateTime.of(2026, 10, 20, 18, 30).atZone(moscow).toInstant().toEpochMilli()
        assertEquals(expected, result)
    }

    @Test
    fun `время напоминания показывается коротко`() {
        val moscow = ZoneId.of("Europe/Moscow")
        val time = LocalDateTime.of(2026, 10, 20, 9, 5).atZone(moscow).toInstant().toEpochMilli()
        assertEquals("20 окт., 09:05", ShoppingUtils.formatReminder(time, moscow))
    }

    @Test
    fun `названия сравниваются без учёта регистра, и для кириллицы тоже`() {
        assertEquals(true, ShoppingUtils.sameName("Пятёрочка", " пятёрочка "))
        assertEquals(false, ShoppingUtils.sameName("Магнит", "Пятёрочка"))
        assertEquals(false, ShoppingUtils.sameName(null, "Магнит"))
    }

    @Test
    fun `частые покупки считаются по магазину без учёта регистра`() {
        val rows = listOf(
            BoughtItemRow("Молоко", "Молочное", "л", "Пятёрочка"),
            BoughtItemRow("молоко", "Молочное", "л", "пятёрочка"),
            BoughtItemRow("Хлеб", "Хлеб", "шт", "Пятёрочка"),
            BoughtItemRow("Сыр", "Молочное", "шт", "Магнит")
        )
        val inStore = ShoppingUtils.countFrequentItems(rows, "ПЯТЁРОЧКА", 5)
        assertEquals(listOf("Молоко" to 2, "Хлеб" to 1), inStore.map { it.name to it.cnt })

        val everywhere = ShoppingUtils.countFrequentItems(rows, null, 5)
        assertEquals(3, everywhere.size)
        assertEquals(1, ShoppingUtils.countFrequentItems(rows, null, 1).size)
    }
}

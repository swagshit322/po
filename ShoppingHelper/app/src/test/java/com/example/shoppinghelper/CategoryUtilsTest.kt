package com.example.shoppinghelper

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

// Автокатегоризация по встроенному справочнику (F-03)
class CategoryUtilsTest {

    private fun guess(name: String) = CategoryUtils.guessCategoryByKeywords(name)

    @Test
    fun `молочные продукты`() {
        assertEquals("Молочное", guess("Молоко"))
        assertEquals("Молочное", guess("молоко 3,2%"))
        assertEquals("Молочное", guess("Кефир"))
        assertEquals("Молочное", guess("Сыр российский"))
        assertEquals("Молочное", guess("Творог 5%"))
    }

    @Test
    fun `сливки попадают в молочное, а сливы во фрукты`() {
        assertEquals("Молочное", guess("Сливки 20%"))
        assertEquals("Фрукты", guess("Сливы"))
    }

    @Test
    fun `овощи, фрукты и хлеб`() {
        assertEquals("Овощи", guess("Картофель"))
        assertEquals("Овощи", guess("помидоры черри"))
        assertEquals("Фрукты", guess("Бананы"))
        assertEquals("Фрукты", guess("Яблоки зелёные"))
        assertEquals("Хлеб", guess("Батон нарезной"))
    }

    @Test
    fun `мясо, бакалея, напитки, сладости, товары для дома`() {
        assertEquals("Мясо и рыба", guess("Куриное филе"))
        assertEquals("Мясо и рыба", guess("Яйца С1"))
        assertEquals("Бакалея", guess("Гречка"))
        assertEquals("Бакалея", guess("Масло подсолнечное"))
        assertEquals("Напитки", guess("Сок апельсиновый"))
        assertEquals("Сладости", guess("Шоколад молочный"))
        assertEquals("Товары для дома", guess("Туалетная бумага"))
    }

    @Test
    fun `буква ё и регистр не мешают`() {
        assertEquals("Овощи", guess("СВЁКЛА"))
        assertEquals("Овощи", guess("свекла"))
    }

    @Test
    fun `неизвестный товар и пустая строка дают null`() {
        assertNull(guess("Абракадабра"))
        assertNull(guess(""))
        assertNull(guess("   "))
    }

    @Test
    fun `слово ищется по началу, а не по середине`() {
        // «лук» не должен находиться внутри «Колукс»
        assertNull(guess("Колукс"))
    }

    @Test
    fun `у каждой базовой категории есть эмодзи`() {
        CategoryUtils.defaultCategories.forEach { category ->
            assertTrue("Нет эмодзи для $category", CategoryUtils.getEmoji(category) != "🏷️")
        }
        assertEquals("🏷️", CategoryUtils.getEmoji("Моя категория"))
    }

    @Test
    fun `категория по умолчанию входит в базовые`() {
        assertTrue(CategoryUtils.DEFAULT_CATEGORY in CategoryUtils.defaultCategories)
    }
}

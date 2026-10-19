package com.example.shoppinghelper

import com.example.shoppinghelper.data.local.dao.BoughtItemRow
import com.example.shoppinghelper.data.local.dao.FrequentItem
import com.example.shoppinghelper.data.local.entity.ItemEntity
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

// Небольшие функции без Android-зависимостей: их удобно проверять unit-тестами
object ShoppingUtils {

    // «1.0» → «1», «0.5» → «0.5»
    fun formatQuantity(quantity: Double): String =
        if (quantity % 1.0 == 0.0) quantity.toLong().toString() else quantity.toString()

    // Принимает и точку, и запятую: «99,90» → 99.9. Пустая строка или мусор → null
    fun parseNumber(text: String): Double? =
        text.trim().replace(',', '.').toDoubleOrNull()

    // 1234.5 → «1 234,50 ₽»
    fun formatPrice(price: Double): String =
        String.format(Locale("ru"), "%,.2f ₽", price).replace(' ', ' ')

    // Примерная стоимость списка (F-09): товары без цены не учитываются
    fun totalPrice(items: List<ItemEntity>): Double = items.sumOf { it.price ?: 0.0 }

    // Доля купленных товаров от 0 до 1 (для полоски прогресса)
    fun progress(bought: Int, total: Int): Float =
        if (total <= 0) 0f else (bought.toFloat() / total).coerceIn(0f, 1f)

    // Сравнение названий без учёта регистра и лишних пробелов, работает и для кириллицы
    fun sameName(a: String?, b: String?): Boolean =
        a != null && b != null && a.trim().lowercase() == b.trim().lowercase()

    // Подсчёт частых покупок (F-08): только в нужном магазине (если он задан),
    // одинаковые названия в разном регистре считаются одним товаром
    fun countFrequentItems(rows: List<BoughtItemRow>, storeName: String?, limit: Int): List<FrequentItem> =
        rows.asSequence()
            .filter { storeName.isNullOrBlank() || sameName(it.storeName, storeName) }
            .groupBy { it.name.trim().lowercase() }
            .values
            .map { group ->
                val first = group.first()
                FrequentItem(first.name.trim(), first.category, first.unit, group.size)
            }
            .sortedByDescending { it.cnt }
            .take(limit)

    // Умные подсказки (F-08): убираем то, что уже есть в списке, и оставляем не больше limit
    fun filterSuggestions(frequent: List<FrequentItem>, itemsInList: List<ItemEntity>, limit: Int = 5): List<FrequentItem> {
        val namesInList = itemsInList.map { it.name.trim().lowercase() }.toSet()
        return frequent.filter { it.name.trim().lowercase() !in namesInList }.take(limit)
    }

    // DatePicker отдаёт полночь выбранного дня в UTC; добавляем время в часовом поясе телефона
    fun combineDateAndTime(
        dateUtcMillis: Long,
        hour: Int,
        minute: Int,
        zone: ZoneId = ZoneId.systemDefault()
    ): Long {
        val date: LocalDate = Instant.ofEpochMilli(dateUtcMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return LocalDateTime.of(date, LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()
    }

    // 1729245600000 → «18 окт., 13:00»
    fun formatReminder(timeMillis: Long, zone: ZoneId = ZoneId.systemDefault()): String =
        DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale("ru"))
            .format(Instant.ofEpochMilli(timeMillis).atZone(zone))
}

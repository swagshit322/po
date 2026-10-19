package com.example.shoppinghelper.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "items",
    // Связь: товар принадлежит списку. Если список удаляется, товары удаляются автоматически (CASCADE)
    foreignKeys = [
        ForeignKey(
            entity = ShoppingListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("listId")] // Ускоряет поиск товаров по ID списка
)
data class ItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val listId: Long,               // ID списка, к которому относится товар
    val name: String,               // Название товара (требование F-02)
    val quantity: Double = 1.0,     // Количество
    val unit: String = "шт",        // Единица измерения (шт/кг/л)
    val category: String = "Другое",// Категория для группировки (требование F-03)
    val price: Double? = null,      // Примерная цена (требование F-09)
    val isBought: Boolean = false,  // Статус "куплено" (требование F-04)
    val sortOrder: Int = 0          // Для сортировки внутри списка
)
package com.example.shoppinghelper

import android.app.Application
import com.example.shoppinghelper.data.local.AppDatabase
import com.example.shoppinghelper.data.repository.ShoppingRepository
import com.example.shoppinghelper.reminder.ReminderScheduler

class ShoppingApp : Application() {

    // 1. Сначала создаём экземпляр базы данных
    val database by lazy { AppDatabase.getDatabase(this) }

    // 2. Затем создаём репозиторий, используя DAO из этой базы данных
    val repository by lazy {
        ShoppingRepository(
            listDao = database.shoppingListDao(),
            itemDao = database.itemDao(),
            favoriteDao = database.favoriteDao(),
            favoriteListDao = database.favoriteListDao(),
            categoryDao = database.categoryDao()
        )
    }

    // 3. Планировщик напоминаний (F-10)
    val reminderScheduler by lazy { ReminderScheduler(this) }

    override fun onCreate() {
        super.onCreate()
        ReminderScheduler.createNotificationChannel(this)
    }
}

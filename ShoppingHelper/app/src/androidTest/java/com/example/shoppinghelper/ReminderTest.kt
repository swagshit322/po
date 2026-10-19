package com.example.shoppinghelper

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

// Сквозная проверка напоминаний (F-10): будильник срабатывает и показывает уведомление
@RunWith(AndroidJUnit4::class)
class ReminderTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val app get() = context as ShoppingApp

    @Before
    fun grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    @Test
    fun уведомление_приходит_в_назначенное_время() {
        val listId = 987_654L // такого списка в БД нет: ресивер просто не найдёт что обновлять
        val notifications = context.getSystemService(NotificationManager::class.java)
        notifications.cancel(listId.toInt())

        app.reminderScheduler.schedule(
            ShoppingListEntity(
                id = listId,
                name = "Тест",
                storeName = "Магнит",
                reminderTime = System.currentTimeMillis() + 5_000
            )
        )

        // Ждём до 30 секунд: точный будильник должен сработать примерно через 5
        val deadline = System.currentTimeMillis() + 30_000
        var shown = false
        while (!shown && System.currentTimeMillis() < deadline) {
            Thread.sleep(500)
            shown = notifications.activeNotifications.any { it.id == listId.toInt() }
        }
        notifications.cancel(listId.toInt())
        assertTrue("Уведомление не появилось за 30 секунд", shown)
    }
}

package com.example.shoppinghelper.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity

/**
 * Локальные напоминания о покупках (требование F-10).
 * Используется AlarmManager: без интернета, без внешних сервисов.
 */
class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun schedule(list: ShoppingListEntity) {
        val time = list.reminderTime ?: return
        if (time <= System.currentTimeMillis()) return

        val pendingIntent = buildPendingIntent(list.id, list.name, list.storeName)
        // Точный будильник, если система разрешает; иначе обычный (может сработать на пару минут позже)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()
        if (canExact) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pendingIntent)
        }
    }

    fun cancel(listId: Long) {
        alarmManager.cancel(buildPendingIntent(listId, "", null))
    }

    private fun buildPendingIntent(listId: Long, listName: String, storeName: String?): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_LIST_ID, listId)
            putExtra(EXTRA_LIST_NAME, listName)
            putExtra(EXTRA_STORE_NAME, storeName)
        }
        return PendingIntent.getBroadcast(
            context,
            listId.toInt(), // у каждого списка своё напоминание
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val CHANNEL_ID = "shopping_reminders"
        const val EXTRA_LIST_ID = "list_id"
        const val EXTRA_LIST_NAME = "list_name"
        const val EXTRA_STORE_NAME = "store_name"

        fun createNotificationChannel(context: Context) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Напоминания о покупках",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "Напоминания, которые вы ставите для списков покупок" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
}

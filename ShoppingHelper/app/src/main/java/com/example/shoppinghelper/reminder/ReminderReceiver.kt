package com.example.shoppinghelper.reminder

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.shoppinghelper.MainActivity
import com.example.shoppinghelper.R
import com.example.shoppinghelper.ShoppingApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Срабатывает в назначенное время и показывает уведомление (F-10)
class ReminderReceiver : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // разрешение проверяется через areNotificationsEnabled()
    override fun onReceive(context: Context, intent: Intent) {
        val listId = intent.getLongExtra(ReminderScheduler.EXTRA_LIST_ID, -1L)
        if (listId == -1L) return
        val listName = intent.getStringExtra(ReminderScheduler.EXTRA_LIST_NAME).orEmpty()
        val storeName = intent.getStringExtra(ReminderScheduler.EXTRA_STORE_NAME)

        val notifications = NotificationManagerCompat.from(context)
        if (notifications.areNotificationsEnabled()) {
            // Нажатие на уведомление открывает этот список
            val openIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(MainActivity.EXTRA_OPEN_LIST_ID, listId)
            }
            val contentIntent = PendingIntent.getActivity(
                context, listId.toInt(), openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val text = if (storeName.isNullOrBlank()) "Пора за покупками по списку «$listName»"
            else "Пора в «$storeName»: список «$listName»"

            val notification = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🛒 Напоминание о покупках")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent)
                .setAutoCancel(true)
                .build()

            notifications.notify(listId.toInt(), notification)
        }

        // Напоминание сработало: убираем его из списка, чтобы колокольчик погас
        val pending = goAsync()
        val repository = (context.applicationContext as ShoppingApp).repository
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.setReminder(listId, null)
            } finally {
                pending.finish()
            }
        }
    }
}

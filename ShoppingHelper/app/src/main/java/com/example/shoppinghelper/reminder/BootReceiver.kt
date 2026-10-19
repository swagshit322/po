package com.example.shoppinghelper.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.shoppinghelper.ShoppingApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// После перезагрузки телефона Android забывает все будильники, поэтому ставим их заново из БД
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val app = context.applicationContext as ShoppingApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.repository.getUpcomingReminders(System.currentTimeMillis())
                    .forEach { app.reminderScheduler.schedule(it) }
            } finally {
                pending.finish()
            }
        }
    }
}

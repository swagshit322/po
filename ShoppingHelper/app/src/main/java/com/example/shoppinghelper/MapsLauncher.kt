package com.example.shoppinghelper

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Работа с картами через внешние приложения (требования F-05, F-06).
 * Своей карты в приложении нет: поиск магазинов, адреса, часы работы и маршрут
 * показывает установленное приложение карт (Яндекс.Карты, Google Maps, 2ГИС и т. д.).
 * Поэтому не нужны ни SDK карт, ни API-ключи, ни разрешение на геолокацию.
 */
object MapsLauncher {

    // Ближайшие магазины сети на карте (F-05).
    // Схему geo: понимают все популярные приложения карт; если их несколько, Android предложит выбрать.
    fun showStoresNearby(context: Context, storeName: String) {
        val query = Uri.encode(storeName.trim())
        val opened = tryOpen(context, Uri.parse("geo:0,0?q=$query"))
                || tryOpen(context, Uri.parse("https://yandex.ru/maps/?text=$query"))
        if (!opened) showNoAppMessage(context)
    }

    // Маршрут до ближайшего магазина сети (F-06): навигатор сам выберет ближайший филиал.
    fun buildRoute(context: Context, storeName: String) {
        val query = Uri.encode(storeName.trim())
        val opened = tryOpen(context, Uri.parse("google.navigation:q=$query"))
                || tryOpen(context, Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$query"))
        if (!opened) showNoAppMessage(context)
    }

    private fun tryOpen(context: Context, uri: Uri): Boolean = try {
        context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    }

    private fun showNoAppMessage(context: Context) {
        Toast.makeText(context, "Не найдено приложение карт или браузер", Toast.LENGTH_LONG).show()
    }
}

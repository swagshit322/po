package com.example.shoppinghelper.ui.theme

import androidx.compose.ui.graphics.Color

// === ТЁМНО-СЕРАЯ ПАЛИТРА (Premium Dark) ===

// Фоны
val DarkBackground = Color(0xFF121212)      // Глубокий тёмно-серый (почти чёрный)
val DarkSurface = Color(0xFF1E1E1E)         // Чуть светлее для карточек
val DarkSurfaceVariant = Color(0xFF2C2C2E)  // Для выделенных элементов

// Акцентный цвет (Изумрудный зелёный - выглядит дорого на тёмном)
val DarkPrimary = Color(0xFF10B981)         // Яркий изумрудный
val DarkOnPrimary = Color(0xFF000000)       // Чёрный текст на зелёной кнопке
val DarkPrimaryContainer = Color(0xFF064E3B)// Тёмно-зелёный фон для контейнеров
val DarkOnPrimaryContainer = Color(0xFF6EE7B7)// Светло-зелёный текст

// Текст
val DarkTextPrimary = Color(0xFFE4E4E7)     // Почти белый (основной текст)
val DarkTextSecondary = Color(0xFFA1A1AA)   // Серый (вторичный текст, подсказки)

// Прочее
val DarkOutline = Color(0xFF3F3F46)         // Тонкие границы
val DarkError = Color(0xFFEF4444)           // Красный для ошибок/удаления
// === СВЕТЛАЯ ПАЛИТРА (включается, если на телефоне светлая тема) ===
val LightBackground = Color(0xFFF4F6F8)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFE9EDF1)
val LightPrimary = Color(0xFF059669)         // Изумрудный, чуть темнее, чтобы читался на белом
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD1FAE5)
val LightOnPrimaryContainer = Color(0xFF064E3B)
val LightTextPrimary = Color(0xFF18181B)
val LightTextSecondary = Color(0xFF52525B)
val LightOutline = Color(0xFFD4D4D8)
val LightError = Color(0xFFDC2626)

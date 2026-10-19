package com.example.shoppinghelper.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shoppinghelper.ShoppingUtils
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Выбор даты и времени напоминания (F-10): сначала календарь, потом часы.
 * onRemove != null — у списка уже есть напоминание, показываем кнопку «Убрать».
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderPickerDialog(
    initialTime: Long?,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit,
    onRemove: (() -> Unit)?
) {
    val zone = ZoneId.systemDefault()
    // Стартовое значение: текущее напоминание или через час от текущего времени
    val start = initialTime?.let { LocalDateTime.ofInstant(Instant.ofEpochMilli(it), zone) }
        ?: LocalDateTime.now().plusHours(1).withMinute(0)
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

    var pickedDateUtc by remember { mutableStateOf<Long?>(null) }

    if (pickedDateUtc == null) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : SelectableDates {
                // Прошедшие дни выбрать нельзя
                override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis >= todayUtc
            }
        )
        DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(
                    onClick = { pickedDateUtc = dateState.selectedDateMillis },
                    enabled = dateState.selectedDateMillis != null
                ) { Text("Далее") }
            },
            dismissButton = {
                Row {
                    if (onRemove != null) {
                        TextButton(onClick = onRemove) {
                            Text("Убрать", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(onClick = onDismiss) { Text("Отмена") }
                }
            }
        ) {
            DatePicker(state = dateState, title = {
                Text(
                    "Когда напомнить?",
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp)
                )
            })
        }
    } else {
        val timeState = rememberTimePickerState(
            initialHour = start.hour,
            initialMinute = start.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Во сколько?") },
            text = {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TimePicker(state = timeState)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onConfirm(ShoppingUtils.combineDateAndTime(pickedDateUtc!!, timeState.hour, timeState.minute, zone))
                }) { Text("Готово") }
            },
            dismissButton = {
                TextButton(onClick = { pickedDateUtc = null }) { Text("Назад") }
            }
        )
    }
}

// Краткое руководство при первом запуске (раздел 8 ТЗ «Обучение пользователей»)
@Composable
fun WelcomeDialog(onDismiss: () -> Unit) {
    val tips = listOf(
        "📝" to "Создайте список кнопкой «Новый список» и укажите магазин",
        "✅" to "Нажимайте на кружок у товара, чтобы отметить его купленным",
        "💡" to "Приложение само подберёт категорию и подскажет, что вы часто покупаете",
        "🔔" to "Колокольчик в списке поставит напоминание о покупках",
        "🗺️" to "На вкладке «Карты» можно найти магазин рядом и построить маршрут"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Добро пожаловать! 🛒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                tips.forEach { (emoji, text) ->
                    Row(verticalAlignment = Alignment.Top) {
                        Text(emoji, fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(text, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Text(
                    "Все данные хранятся только на вашем телефоне.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Начать") }
        }
    )
}

package com.example.shoppinghelper

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.shoppinghelper.ui.navigation.AppNavigation
import com.example.shoppinghelper.ui.theme.ShoppingHelperTheme
import com.example.shoppinghelper.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    // ViewModel переживает поворот экрана (не создаётся заново при пересоздании Activity)
    private val viewModel: MainViewModel by viewModels {
        viewModelFactory {
            initializer {
                val app = application as ShoppingApp
                MainViewModel(app.repository, app.reminderScheduler)
            }
        }
    }

    // Список, который нужно открыть (приходит из уведомления-напоминания)
    private val listToOpen = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) listToOpen.value = intent.listIdToOpen()

        setContent {
            // Тема как в настройках телефона: светлая или тёмная
            ShoppingHelperTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        viewModel = viewModel,
                        listToOpen = listToOpen.value,
                        onListOpened = { listToOpen.value = null }
                    )
                }
            }
        }
    }

    // Приложение уже открыто, а пользователь нажал на уведомление
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.listIdToOpen()?.let { listToOpen.value = it }
    }

    private fun Intent.listIdToOpen(): Long? =
        getLongExtra(EXTRA_OPEN_LIST_ID, -1L).takeIf { it != -1L }

    companion object {
        const val EXTRA_OPEN_LIST_ID = "open_list_id"
    }
}

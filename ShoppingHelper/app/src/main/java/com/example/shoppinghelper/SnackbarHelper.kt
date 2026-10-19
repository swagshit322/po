package com.example.shoppinghelper

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object SnackbarHelper {
    fun show(
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        message: String
    ) {
        scope.launch {
            // Закрываем предыдущее сообщение, чтобы они не копились в очереди
            snackbarHostState.currentSnackbarData?.dismiss()
            snackbarHostState.showSnackbar(message)
        }
    }

    // Сообщение с кнопкой «Отменить» (например, после удаления)
    fun showWithUndo(
        snackbarHostState: SnackbarHostState,
        scope: CoroutineScope,
        message: String,
        onUndo: () -> Unit
    ) {
        scope.launch {
            snackbarHostState.currentSnackbarData?.dismiss()
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "Отменить",
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) onUndo()
        }
    }
}

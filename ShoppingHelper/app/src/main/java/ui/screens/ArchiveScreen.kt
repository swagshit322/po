package com.example.shoppinghelper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.shoppinghelper.SnackbarHelper
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity
import com.example.shoppinghelper.ui.components.ConfirmDialog
import com.example.shoppinghelper.ui.components.EmptyState
import com.example.shoppinghelper.ui.components.ScreenHeader
import com.example.shoppinghelper.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveScreen(viewModel: MainViewModel) {
    val archivedLists by viewModel.archivedLists.collectAsState(initial = emptyList())
    var showClearDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ScreenHeader(
                title = "История покупок",
                subtitle = if (archivedLists.isEmpty()) null else "Завершённых списков: ${archivedLists.size}",
                actions = {
                    if (archivedLists.isNotEmpty()) {
                        IconButton(onClick = { showClearDialog = true }) {
                            Icon(Icons.Default.DeleteSweep, contentDescription = "Очистить архив")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (archivedLists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "🗂️",
                    title = "Архив пуст",
                    text = "Завершённые списки появятся здесь. Любой из них можно повторить одним нажатием"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(archivedLists, key = { it.id }) { list ->
                    ArchivedListCard(
                        list = list,
                        snackbarHostState = snackbarHostState,
                        scope = scope,
                        viewModel = viewModel,
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }

    if (showClearDialog) {
        ConfirmDialog(
            title = "Очистить архив?",
            text = "Все завершённые списки (${archivedLists.size}) будут удалены без возможности восстановления.",
            confirmText = "Очистить",
            onDismiss = { showClearDialog = false },
            onConfirm = {
                viewModel.clearArchive()
                showClearDialog = false
                SnackbarHelper.show(snackbarHostState, scope, "Архив очищен")
            }
        )
    }
}

@Composable
fun ArchivedListCard(
    list: ShoppingListEntity,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val dateText = remember(list.createdAt) {
        SimpleDateFormat("d MMMM yyyy", Locale("ru")).format(Date(list.createdAt))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = list.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = listOfNotNull(list.storeName?.ifBlank { null }, dateText).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // «Повторить список» (F-11): новый активный список с теми же товарами
            FilledTonalButton(
                onClick = {
                    viewModel.repeatOrder(list)
                    SnackbarHelper.show(snackbarHostState, scope, "Создан новый список по образцу «${list.name}»")
                },
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Повторить")
            }
            IconButton(onClick = {
                viewModel.deleteList(list) { deleted ->
                    SnackbarHelper.showWithUndo(snackbarHostState, scope, "Список «${list.name}» удалён из архива") {
                        viewModel.restoreList(deleted)
                    }
                }
            }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

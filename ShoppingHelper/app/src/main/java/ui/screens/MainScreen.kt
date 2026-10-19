package com.example.shoppinghelper.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.shoppinghelper.ShoppingUtils
import com.example.shoppinghelper.SnackbarHelper
import com.example.shoppinghelper.data.local.dao.ListWithStats
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity
import com.example.shoppinghelper.ui.components.EmptyState
import com.example.shoppinghelper.ui.components.ListEditDialog
import com.example.shoppinghelper.ui.components.ScreenHeader
import com.example.shoppinghelper.viewmodel.MainViewModel

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onListClick: (ShoppingListEntity) -> Unit
) {
    val lists by viewModel.activeLists.collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var listToEdit by remember { mutableStateOf<ShoppingListEntity?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ScreenHeader(
                title = "Мои списки",
                subtitle = when (lists.size) {
                    0 -> null
                    else -> "Активных списков: ${lists.size}"
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Новый список") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        if (lists.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "🛒",
                    title = "Списков пока нет",
                    text = "Нажмите «Новый список», чтобы составить первый список покупок"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(lists, key = { it.list.id }) { stats ->
                    ShoppingListCard(
                        stats = stats,
                        modifier = Modifier.animateItem(),
                        onClick = { onListClick(stats.list) },
                        onEdit = { listToEdit = stats.list },
                        onAddToFavorites = {
                            viewModel.addListToFavorites(stats.list)
                            SnackbarHelper.show(snackbarHostState, scope, "Список «${stats.list.name}» добавлен в избранное")
                        },
                        onDelete = {
                            viewModel.deleteList(stats.list) { deleted ->
                                SnackbarHelper.showWithUndo(snackbarHostState, scope, "Список «${stats.list.name}» удалён") {
                                    viewModel.restoreList(deleted)
                                }
                            }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        ListEditDialog(
            title = "Новый список",
            confirmText = "Создать",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, store ->
                viewModel.addList(name, store)
                SnackbarHelper.show(snackbarHostState, scope, "Список «$name» создан")
                showAddDialog = false
            }
        )
    }

    listToEdit?.let { list ->
        ListEditDialog(
            title = "Изменить список",
            confirmText = "Сохранить",
            initialName = list.name,
            initialStore = list.storeName.orEmpty(),
            onDismiss = { listToEdit = null },
            onConfirm = { name, store ->
                viewModel.updateList(list, name, store)
                listToEdit = null
            }
        )
    }
}

@Composable
fun ShoppingListCard(
    stats: ListWithStats,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onAddToFavorites: () -> Unit,
    onDelete: () -> Unit
) {
    val list = stats.list
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 4.dp)
                ) {
                    Text(text = list.name, style = MaterialTheme.typography.titleMedium)
                    // Строка с магазином и напоминанием — только если есть что показать
                    if (!list.storeName.isNullOrBlank() || list.reminderTime != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!list.storeName.isNullOrBlank()) {
                                MetaLabel(icon = { Icon(Icons.Default.Store, null, Modifier.size(14.dp)) }, text = list.storeName)
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            list.reminderTime?.let { time ->
                                MetaLabel(
                                    icon = { Icon(Icons.Default.Notifications, null, Modifier.size(14.dp)) },
                                    text = ShoppingUtils.formatReminder(time),
                                    highlighted = true
                                )
                            }
                        }
                    }
                }

                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Действия со списком")
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Изменить") },
                            leadingIcon = { Icon(Icons.Default.Edit, null) },
                            onClick = { menuOpen = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("В избранное") },
                            leadingIcon = { Icon(Icons.Default.Favorite, null) },
                            onClick = { menuOpen = false; onAddToFavorites() }
                        )
                        DropdownMenuItem(
                            text = { Text("Удалить", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menuOpen = false; onDelete() }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Column(modifier = Modifier.padding(end = 12.dp)) {
                if (stats.totalCount == 0) {
                    Text(
                        text = "Пока пусто, добавьте товары",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    val done = stats.boughtCount == stats.totalCount
                    LinearProgressIndicator(
                        progress = { ShoppingUtils.progress(stats.boughtCount, stats.totalCount) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(MaterialTheme.shapes.small),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {}
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (done) "Всё куплено ✓" else "Куплено ${stats.boughtCount} из ${stats.totalCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// Маленькая подпись с иконкой: магазин, время напоминания
@Composable
fun MetaLabel(icon: @Composable () -> Unit, text: String, highlighted: Boolean = false) {
    val color = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    CompositionLocalProvider(LocalContentColor provides color) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            icon()
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = color, maxLines = 1)
        }
    }
}

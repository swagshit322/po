package com.example.shoppinghelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shoppinghelper.CategoryUtils
import com.example.shoppinghelper.SnackbarHelper
import com.example.shoppinghelper.data.local.entity.FavoriteItemEntity
import com.example.shoppinghelper.data.local.entity.FavoriteListEntity
import com.example.shoppinghelper.ui.components.EmptyState
import com.example.shoppinghelper.ui.components.ScreenHeader
import com.example.shoppinghelper.ui.components.SectionHeader
import com.example.shoppinghelper.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(viewModel: MainViewModel) {
    val favoriteLists by viewModel.favoriteLists.collectAsState(initial = emptyList())
    val favoriteItems by viewModel.favorites.collectAsState(initial = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            ScreenHeader(title = "Избранное", subtitle = "Для быстрого создания списков и добавления товаров")
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (favoriteLists.isEmpty() && favoriteItems.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                EmptyState(
                    emoji = "⭐",
                    title = "Избранное пусто",
                    text = "Добавьте список через меню ⋮ на главном экране, а товар — сердечком в окне «Новый товар»"
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (favoriteLists.isNotEmpty()) {
                    item(key = "header_lists") { SectionHeader("Списки") }
                    items(favoriteLists, key = { "list_${it.id}" }) { favorite ->
                        FavoriteListCard(
                            favorite = favorite,
                            snackbarHostState = snackbarHostState,
                            scope = scope,
                            viewModel = viewModel
                        )
                    }
                }

                if (favoriteItems.isNotEmpty()) {
                    item(key = "header_items") { SectionHeader("Товары") }
                    items(favoriteItems, key = { "item_${it.id}" }) { item ->
                        FavoriteItemCard(
                            item = item,
                            onDelete = {
                                viewModel.removeFavoriteItem(item)
                                SnackbarHelper.showWithUndo(
                                    snackbarHostState, scope, "«${item.name}» удалён из избранного"
                                ) { viewModel.restoreFavoriteItem(item) }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FavoriteListCard(
    favorite: FavoriteListEntity,
    snackbarHostState: SnackbarHostState,
    scope: kotlinx.coroutines.CoroutineScope,
    viewModel: MainViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = favorite.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (!favorite.storeName.isNullOrBlank()) {
                    Text(
                        text = favorite.storeName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row {
                // Кнопка создания списка из избранного
                IconButton(onClick = {
                    viewModel.createListFromFavorite(favorite)
                    SnackbarHelper.show(snackbarHostState, scope, "Список «${favorite.name}» создан")
                }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Создать список",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                // Кнопка удаления из избранного
                IconButton(onClick = {
                    viewModel.removeFromFavoriteList(favorite)
                    SnackbarHelper.showWithUndo(
                        snackbarHostState, scope, "Список «${favorite.name}» удалён из избранного"
                    ) { viewModel.restoreFavoriteList(favorite) }
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
}

@Composable
fun FavoriteItemCard(
    item: FavoriteItemEntity,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${item.unit} • ${CategoryUtils.getEmoji(item.category)} ${item.category}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

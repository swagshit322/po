package com.example.shoppinghelper.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shoppinghelper.MapsLauncher
import com.example.shoppinghelper.ui.components.ScreenHeader
import com.example.shoppinghelper.ui.components.SectionHeader
import com.example.shoppinghelper.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapsScreen(viewModel: MainViewModel) {
    val storeNames by viewModel.storeNames.collectAsState(initial = emptyList())
    val context = LocalContext.current
    var searchText by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            ScreenHeader(title = "Карты", subtitle = "Магазины рядом и маршрут до них")
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Поиск любого магазина, даже если его нет в списках
            item(key = "search") {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = { searchText = it },
                            label = { Text("Название магазина или сети") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StoreActions(
                            enabled = searchText.isNotBlank(),
                            onShowNearby = { MapsLauncher.showStoresNearby(context, searchText) },
                            onRoute = { MapsLauncher.buildRoute(context, searchText) }
                        )
                    }
                }
            }

            if (storeNames.isEmpty()) {
                item(key = "empty") {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Укажите магазин при создании списка, и он появится здесь",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                item(key = "header") { SectionHeader("Магазины из ваших списков") }
                items(storeNames, key = { it }) { store ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.Store,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = store,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            StoreActions(
                                enabled = true,
                                onShowNearby = { MapsLauncher.showStoresNearby(context, store) },
                                onRoute = { MapsLauncher.buildRoute(context, store) }
                            )
                        }
                    }
                }
            }

            item(key = "hint") {
                Text(
                    text = "Карта и маршрут открываются в приложении карт на телефоне. Для них нужен интернет.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
    }
}

// Две кнопки: «Найти рядом» (F-05) и «Маршрут» (F-06)
@Composable
fun StoreActions(
    enabled: Boolean,
    onShowNearby: () -> Unit,
    onRoute: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onShowNearby,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Найти рядом")
        }
        Button(
            onClick = onRoute,
            enabled = enabled,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Маршрут")
        }
    }
}

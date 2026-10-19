package com.example.shoppinghelper.ui.navigation

import android.content.Context
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Map
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.shoppinghelper.ui.components.WelcomeDialog
import com.example.shoppinghelper.ui.screens.ArchiveScreen
import com.example.shoppinghelper.ui.screens.FavoritesScreen
import com.example.shoppinghelper.ui.screens.ListDetailScreen
import com.example.shoppinghelper.ui.screens.MainScreen
import com.example.shoppinghelper.ui.screens.MapsScreen
import com.example.shoppinghelper.viewmodel.MainViewModel

object Routes {
    const val MAIN = "main"
    // Передаём только ID: название, магазин и напоминание экран берёт из БД,
    // поэтому изменения списка сразу видны
    const val LIST_DETAIL = "list_detail/{listId}"
    const val ARCHIVE = "archive"
    const val FAVORITES = "favorites"
    const val MAPS = "maps"

    fun listDetail(listId: Long): String = "list_detail/$listId"
}

data class BottomNavItem(val route: String, val icon: ImageVector, val label: String)

val bottomNavItems = listOf(
    BottomNavItem(Routes.MAIN, Icons.AutoMirrored.Filled.List, "Списки"),
    BottomNavItem(Routes.FAVORITES, Icons.Default.Favorite, "Избранное"),
    BottomNavItem(Routes.MAPS, Icons.Default.Map, "Карты"),
    BottomNavItem(Routes.ARCHIVE, Icons.Default.Archive, "Архив")
)

private const val PREFS_NAME = "app_prefs"
private const val KEY_WELCOME_SHOWN = "welcome_shown"

@Composable
fun AppNavigation(
    viewModel: MainViewModel,
    listToOpen: Long? = null,
    onListOpened: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = bottomNavItems.any { it.route == currentRoute }

    // Открытие списка из уведомления
    LaunchedEffect(listToOpen) {
        if (listToOpen != null) {
            navController.navigate(Routes.listDetail(listToOpen)) { launchSingleTop = true }
            onListOpened()
        }
    }

    // Краткое руководство показывается один раз, при первом запуске
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var showWelcome by remember { mutableStateOf(!prefs.getBoolean(KEY_WELCOME_SHOWN, false)) }
    if (showWelcome) {
        WelcomeDialog(onDismiss = {
            prefs.edit().putBoolean(KEY_WELCOME_SHOWN, true).apply()
            showWelcome = false
        })
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = currentRoute == item.route,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Routes.MAIN,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Routes.MAIN) {
                MainScreen(
                    viewModel = viewModel,
                    onListClick = { list -> navController.navigate(Routes.listDetail(list.id)) }
                )
            }

            composable(
                route = Routes.LIST_DETAIL,
                arguments = listOf(navArgument("listId") { type = NavType.LongType })
            ) { backStackEntry ->
                ListDetailScreen(
                    listId = backStackEntry.arguments?.getLong("listId") ?: 0L,
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable(Routes.ARCHIVE) {
                ArchiveScreen(viewModel = viewModel)
            }

            composable(Routes.FAVORITES) {
                FavoritesScreen(viewModel = viewModel)
            }

            // Экран карт: магазины открываются во внешнем приложении карт
            composable(Routes.MAPS) {
                MapsScreen(viewModel = viewModel)
            }
        }
    }
}

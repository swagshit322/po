package com.example.shoppinghelper.ui.screens

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.shoppinghelper.CategoryUtils
import com.example.shoppinghelper.MapsLauncher
import com.example.shoppinghelper.ShoppingUtils
import com.example.shoppinghelper.SnackbarHelper
import com.example.shoppinghelper.data.local.dao.FrequentItem
import com.example.shoppinghelper.data.local.entity.FavoriteItemEntity
import com.example.shoppinghelper.data.local.entity.ItemEntity
import com.example.shoppinghelper.data.local.entity.ShoppingListEntity
import com.example.shoppinghelper.ui.components.EmptyState
import com.example.shoppinghelper.ui.components.ReminderPickerDialog
import com.example.shoppinghelper.ui.components.ScreenHeader
import com.example.shoppinghelper.ui.components.SectionHeader
import com.example.shoppinghelper.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun ListDetailScreen(
    listId: Long,
    viewModel: MainViewModel,
    onBackClick: () -> Unit
) {
    // remember: иначе на каждой перерисовке создаётся новый запрос к БД
    val listFlow = remember(listId) { viewModel.getList(listId) }
    val shoppingList by listFlow.collectAsState(initial = null)
    val list = shoppingList ?: return // список ещё загружается (или был удалён)

    val itemsFlow = remember(listId) { viewModel.getItemsForList(listId) }
    val items by itemsFlow.collectAsState(initial = emptyList())
    val categories by viewModel.categories.collectAsState(initial = CategoryUtils.defaultCategories)
    val favoriteItems by viewModel.favorites.collectAsState(initial = emptyList())

    var showAddItemDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<ItemEntity?>(null) }
    var showArchiveDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Включены ли уведомления (F-10). Проверяем заново при каждом возврате на экран:
    // пользователь мог включить их в настройках телефона
    var notificationsEnabled by remember { mutableStateOf(areNotificationsEnabled(context)) }
    var showNotificationsOffDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) notificationsEnabled = areNotificationsEnabled(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Разрешение на уведомления нужно на Android 13+. Если пользователь отказал
    // (или отказал раньше и система больше не спрашивает) — объясняем и ведём в настройки
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsEnabled = areNotificationsEnabled(context)
        if (!granted || !notificationsEnabled) showNotificationsOffDialog = true
    }

    // Умные подсказки (F-08): частые покупки в магазине этого списка.
    // Загружаются при открытии; то, что уже есть в списке, отфильтровывается.
    var frequentItems by remember(list.id, list.storeName) { mutableStateOf(emptyList<FrequentItem>()) }
    LaunchedEffect(list.id, list.storeName) {
        frequentItems = viewModel.getSmartSuggestions(list)
    }
    val suggestions = remember(frequentItems, items) { ShoppingUtils.filterSuggestions(frequentItems, items) }
    var suggestionsHidden by rememberSaveable { mutableStateOf(false) }

    // F-04: купленные товары уходят в отдельный раздел внизу списка
    val unboughtByCategory = remember(items) { items.filter { !it.isBought }.groupBy { it.category } }
    val boughtItems = remember(items) { items.filter { it.isBought } }

    val onToggle: (ItemEntity) -> Unit = { item -> viewModel.toggleItemBought(item.id, item.isBought) }
    val onDelete: (ItemEntity) -> Unit = { item ->
        viewModel.deleteItem(item)
        SnackbarHelper.showWithUndo(snackbarHostState, scope, "«${item.name}» удалён") { viewModel.addItem(item) }
    }

    Scaffold(
        topBar = {
            ScreenHeader(
                title = list.name,
                subtitle = list.storeName,
                onBack = onBackClick,
                actions = {
                    // Магазин списка на карте (F-05), маршрут строится уже в приложении карт
                    if (!list.storeName.isNullOrBlank()) {
                        IconButton(onClick = { MapsLauncher.showStoresNearby(context, list.storeName) }) {
                            Icon(Icons.Default.Place, contentDescription = "Показать магазин на карте")
                        }
                    }
                    IconButton(onClick = { showReminderDialog = true }) {
                        if (list.reminderTime != null) {
                            Icon(
                                Icons.Default.NotificationsActive,
                                contentDescription = "Изменить напоминание",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        } else {
                            Icon(Icons.Outlined.NotificationsNone, contentDescription = "Поставить напоминание")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddItemDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Товар") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (items.isNotEmpty() || list.reminderTime != null) {
                item(key = "summary") {
                    SummaryCard(
                        boughtCount = boughtItems.size,
                        totalCount = items.size,
                        totalPrice = ShoppingUtils.totalPrice(items),
                        reminderTime = list.reminderTime,
                        notificationsEnabled = notificationsEnabled,
                        onEnableNotifications = { openNotificationSettings(context) },
                        onReminderClick = { showReminderDialog = true },
                        onFinish = { showArchiveDialog = true }
                    )
                }
            }

            if (suggestions.isNotEmpty() && !suggestionsHidden) {
                item(key = "suggestions") {
                    SuggestionsCard(
                        storeName = list.storeName,
                        suggestions = suggestions,
                        onAdd = { suggestion ->
                            viewModel.addItem(
                                ItemEntity(
                                    listId = list.id,
                                    name = suggestion.name,
                                    unit = suggestion.unit,
                                    category = suggestion.category
                                )
                            )
                            SnackbarHelper.show(snackbarHostState, scope, "«${suggestion.name}» добавлен в список")
                        },
                        onHide = { suggestionsHidden = true }
                    )
                }
            }

            if (items.isEmpty()) {
                item(key = "empty") {
                    EmptyState(
                        emoji = "🧺",
                        title = "Список пуст",
                        text = "Нажмите «Товар», чтобы добавить первую покупку"
                    )
                }
            }

            unboughtByCategory.forEach { (category, categoryItems) ->
                item(key = "cat_$category") {
                    SectionHeader(
                        title = "${CategoryUtils.getEmoji(category)}  $category · ${categoryItems.size}",
                        modifier = Modifier.animateItem()
                    )
                }
                items(categoryItems, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        modifier = Modifier.animateItem(),
                        onClick = { itemToEdit = item },
                        onToggleBought = { onToggle(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }

            if (boughtItems.isNotEmpty()) {
                item(key = "bought_header") {
                    SectionHeader(
                        title = "✓  Куплено · ${boughtItems.size}",
                        modifier = Modifier.animateItem()
                    )
                }
                items(boughtItems, key = { it.id }) { item ->
                    ItemRow(
                        item = item,
                        modifier = Modifier.animateItem(),
                        onClick = { itemToEdit = item },
                        onToggleBought = { onToggle(item) },
                        onDelete = { onDelete(item) }
                    )
                }
            }
        }
    }

    if (showAddItemDialog || itemToEdit != null) {
        val editing = itemToEdit
        ItemDialog(
            initial = editing,
            categories = categories,
            favoriteItems = favoriteItems,
            guessCategory = { name -> viewModel.guessCategory(name) },
            onAddCategory = { viewModel.addCategory(it) },
            onAddToFavorites = { name, unit, category ->
                viewModel.addItemToFavorites(name, unit, category)
                SnackbarHelper.show(snackbarHostState, scope, "«$name» добавлен в избранное")
            },
            onDismiss = {
                showAddItemDialog = false
                itemToEdit = null
            },
            onConfirm = { name, quantity, unit, category, price ->
                if (editing == null) {
                    viewModel.addItem(
                        ItemEntity(
                            listId = list.id,
                            name = name,
                            quantity = quantity,
                            unit = unit,
                            category = category,
                            price = price
                        )
                    )
                    SnackbarHelper.show(snackbarHostState, scope, "«$name» добавлен в список")
                } else {
                    viewModel.updateItem(
                        editing.copy(name = name, quantity = quantity, unit = unit, category = category, price = price)
                    )
                }
                showAddItemDialog = false
                itemToEdit = null
            }
        )
    }

    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            title = { Text("Завершить покупки?") },
            text = { Text("Список переместится в архив. Потом его можно будет повторить одним нажатием.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.archiveList(list.id)
                        showArchiveDialog = false
                        onBackClick()
                    }
                ) {
                    Text("Завершить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showArchiveDialog = false }) {
                    Text("Отмена")
                }
            }
        )
    }

    if (showReminderDialog) {
        ReminderPickerDialog(
            initialTime = list.reminderTime,
            onDismiss = { showReminderDialog = false },
            onRemove = if (list.reminderTime != null) {
                {
                    viewModel.clearReminder(list)
                    showReminderDialog = false
                    SnackbarHelper.show(snackbarHostState, scope, "Напоминание убрано")
                }
            } else null,
            onConfirm = { time ->
                showReminderDialog = false
                if (time <= System.currentTimeMillis()) {
                    SnackbarHelper.show(snackbarHostState, scope, "Это время уже прошло, выберите другое")
                } else {
                    viewModel.setReminder(list, time)
                    SnackbarHelper.show(snackbarHostState, scope, "Напомним ${ShoppingUtils.formatReminder(time)}")
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED
                    ) {
                        notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else if (!areNotificationsEnabled(context)) {
                        // Разрешение есть, но уведомления выключены в настройках телефона
                        showNotificationsOffDialog = true
                    }
                }
            }
        )
    }

    if (showNotificationsOffDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsOffDialog = false },
            icon = { Icon(Icons.Default.NotificationsOff, contentDescription = null) },
            title = { Text("Уведомления выключены") },
            text = {
                Text(
                    "Напоминание сохранено, но телефон не покажет его, пока уведомления " +
                            "для приложения выключены. Включите их в настройках."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showNotificationsOffDialog = false
                    openNotificationSettings(context)
                }) { Text("Открыть настройки") }
            },
            dismissButton = {
                TextButton(onClick = { showNotificationsOffDialog = false }) { Text("Позже") }
            }
        )
    }
}

// Итоги списка: прогресс, примерная стоимость (F-09), напоминание (F-10) и кнопка завершения
@Composable
fun SummaryCard(
    boughtCount: Int,
    totalCount: Int,
    totalPrice: Double,
    reminderTime: Long?,
    notificationsEnabled: Boolean,
    onEnableNotifications: () -> Unit,
    onReminderClick: () -> Unit,
    onFinish: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("Куплено", style = MaterialTheme.typography.bodySmall)
                    Text("$boughtCount из $totalCount", style = MaterialTheme.typography.titleLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Примерно", style = MaterialTheme.typography.bodySmall)
                    Text(
                        if (totalPrice > 0) ShoppingUtils.formatPrice(totalPrice) else "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { ShoppingUtils.progress(boughtCount, totalCount) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.small),
                    trackColor = MaterialTheme.colorScheme.surface,
                    drawStopIndicator = {}
                )
            }

            if (reminderTime != null) {
                Spacer(modifier = Modifier.height(12.dp))
                AssistChip(
                    onClick = onReminderClick,
                    label = { Text("Напоминание: ${ShoppingUtils.formatReminder(reminderTime)}") },
                    leadingIcon = { Icon(Icons.Default.Notifications, contentDescription = null, Modifier.size(18.dp)) }
                )
                // Напоминание есть, но телефон его не покажет — предупреждаем прямо на карточке
                if (!notificationsEnabled) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "Уведомления выключены",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = onEnableNotifications) { Text("Включить") }
                    }
                }
            }

            if (totalCount > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                val allBought = boughtCount == totalCount
                if (allBought) {
                    Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Всё куплено, завершить")
                    }
                } else {
                    OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
                        Text("Завершить покупки")
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionsCard(
    storeName: String?,
    suggestions: List<FrequentItem>,
    onAdd: (FrequentItem) -> Unit,
    onHide: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (storeName.isNullOrBlank()) "💡 Вы часто покупаете"
                    else "💡 Часто покупаете в «$storeName»",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onHide) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Скрыть подсказки",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { onAdd(suggestion) },
                        label = { Text("${CategoryUtils.getEmoji(suggestion.category)} ${suggestion.name}") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(containerColor = MaterialTheme.colorScheme.surface)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
    }
}

@Composable
fun ItemRow(
    item: ItemEntity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onToggleBought: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isBought) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isBought) 0.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // F-04: отметка одним нажатием
            IconButton(onClick = onToggleBought) {
                Icon(
                    imageVector = if (item.isBought) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = if (item.isBought) "Отметить как некупленное" else "Отметить как купленное",
                    tint = if (item.isBought) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (item.isBought) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (item.isBought) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${ShoppingUtils.formatQuantity(item.quantity)} ${item.unit}" +
                            if (item.isBought) " · ${CategoryUtils.getEmoji(item.category)} ${item.category}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (item.price != null && item.price > 0) {
                Text(
                    text = ShoppingUtils.formatPrice(item.price),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (item.isBought) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Удалить",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                )
            }
        }
    }
}

/**
 * Добавление и редактирование товара (UC-02).
 * initial == null — новый товар, иначе — изменение существующего.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDialog(
    initial: ItemEntity?,
    categories: List<String>,
    favoriteItems: List<FavoriteItemEntity>,
    guessCategory: suspend (String) -> String?,
    onAddCategory: (String) -> Unit,
    onAddToFavorites: (name: String, unit: String, category: String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, String, String, Double?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var quantityText by remember { mutableStateOf(initial?.let { ShoppingUtils.formatQuantity(it.quantity) } ?: "1") }
    var unit by remember { mutableStateOf(initial?.unit ?: "шт") }
    var category by remember { mutableStateOf(initial?.category ?: CategoryUtils.DEFAULT_CATEGORY) }
    var priceText by remember { mutableStateOf(initial?.price?.let { ShoppingUtils.formatQuantity(it) }.orEmpty()) }

    // Категорию, выбранную вручную, автоподбор больше не трогает
    var categoryChosenByUser by remember { mutableStateOf(initial != null) }
    var categoryGuessed by remember { mutableStateOf(false) }

    var showAddCategory by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }

    // F-03: подбираем категорию, пока пользователь печатает название (с небольшой паузой)
    LaunchedEffect(name) {
        if (categoryChosenByUser) return@LaunchedEffect
        delay(300)
        val guess = guessCategory(name)
        if (guess != null) {
            category = guess
            categoryGuessed = true
        } else {
            category = CategoryUtils.DEFAULT_CATEGORY
            categoryGuessed = false
        }
    }

    // Категория могла прийти из истории, но её нет среди чипов — покажем и её
    val shownCategories = if (category in categories) categories else categories + category

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новый товар" else "Изменить товар") },
        text = {
            // Диалог — отдельное окно, поэтому фокус и клавиатуру берём именно здесь, внутри него
            val focusManager = LocalFocusManager.current
            val keyboard = LocalSoftwareKeyboardController.current
            Column(
                modifier = Modifier
                    .heightIn(max = 440.dp) // окно не уходит под клавиатуру целиком
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initial == null && favoriteItems.isNotEmpty()) {
                    Text("Из избранного:", style = MaterialTheme.typography.bodySmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        favoriteItems.forEach { fav ->
                            SuggestionChip(
                                onClick = {
                                    name = fav.name
                                    unit = fav.unit
                                    category = fav.category
                                    categoryChosenByUser = true
                                },
                                label = { Text(fav.name) }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название товара") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text("Количество") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text("Цена, ₽") },
                        placeholder = { Text("необяз.") },
                        singleLine = true,
                        // «Готово» прячет клавиатуру, и становятся видны кнопки окна
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            keyboard?.hide()
                            focusManager.clearFocus()
                        }),
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Единица измерения", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("шт", "кг", "л").forEach { u ->
                        FilterChip(
                            selected = unit == u,
                            onClick = { unit = u },
                            label = { Text(u) }
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Категория", style = MaterialTheme.typography.bodySmall)
                    if (categoryGuessed && !categoryChosenByUser) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "подобрана автоматически",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    shownCategories.forEach { c ->
                        FilterChip(
                            selected = category == c,
                            onClick = {
                                category = c
                                categoryChosenByUser = true
                            },
                            label = { Text("${CategoryUtils.getEmoji(c)} $c") }
                        )
                    }
                    AssistChip(
                        onClick = { showAddCategory = true },
                        label = { Text("Своя") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
            }
        },
        confirmButton = {
            Row {
                IconButton(
                    onClick = { if (name.isNotBlank()) onAddToFavorites(name.trim(), unit, category) },
                    enabled = name.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Добавить в избранное",
                        tint = if (name.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TextButton(
                    onClick = {
                        val qty = ShoppingUtils.parseNumber(quantityText)?.takeIf { it > 0 } ?: 1.0
                        val price = ShoppingUtils.parseNumber(priceText)?.takeIf { it >= 0 }
                        onConfirm(name.trim(), qty, unit, category, price)
                    },
                    enabled = name.isNotBlank()
                ) {
                    Text(if (initial == null) "Добавить" else "Сохранить")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    if (showAddCategory) {
        AlertDialog(
            onDismissRequest = { showAddCategory = false },
            title = { Text("Новая категория") },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text("Название категории") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newName = newCategoryName.trim()
                        if (newName.isNotBlank()) {
                            onAddCategory(newName)
                            category = newName
                            categoryChosenByUser = true
                            newCategoryName = ""
                            showAddCategory = false
                        }
                    },
                    enabled = newCategoryName.isNotBlank()
                ) {
                    Text("Добавить")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddCategory = false
                    newCategoryName = ""
                }) {
                    Text("Отмена")
                }
            }
        )
    }
}

// Уведомления разрешены и не выключены в настройках телефона
private fun areNotificationsEnabled(context: Context): Boolean =
    NotificationManagerCompat.from(context).areNotificationsEnabled()

// Экран настроек уведомлений этого приложения
private fun openNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Запасной вариант: общий экран «О приложении»
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

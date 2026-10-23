package dev.retrobit.bitlauncher.ui.drawer

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retrobit.bitlauncher.LauncherViewModel
import dev.retrobit.bitlauncher.data.AppLayout
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.ui.components.AlphabetRail
import dev.retrobit.bitlauncher.ui.components.AppActionsSheet
import dev.retrobit.bitlauncher.ui.components.AppIcon
import dev.retrobit.bitlauncher.util.AppSearch
import dev.retrobit.bitlauncher.util.DrawerItem
import dev.retrobit.bitlauncher.util.SystemIntents
import dev.retrobit.bitlauncher.util.buildDrawerItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerScreen(
    viewModel: LauncherViewModel,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val keyboard = LocalSoftwareKeyboardController.current

    var query by rememberSaveable { mutableStateOf("") }
    var selectedApp by remember { mutableStateOf<LaunchableApp?>(null) }
    val listState = rememberLazyListState()
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val filtered = remember(apps, query) { AppSearch.filter(apps, query) }
    val isGrid = settings.layout == AppLayout.GRID
    val items = remember(filtered, isGrid, settings.gridColumns) {
        buildDrawerItems(filtered, grid = isGrid, columns = settings.gridColumns)
    }
    val letters = remember(items) {
        items.filterIsInstance<DrawerItem.LetterHeader>().map { it.letter }
    }
    val letterIndices = remember(items) {
        items.mapIndexedNotNull { index, item ->
            (item as? DrawerItem.LetterHeader)?.let { it.letter to index }
        }.toMap()
    }

    val shortcuts by produceState(
        initialValue = emptyList<android.content.pm.ShortcutInfo>(),
        selectedApp?.key,
    ) {
        val app = selectedApp
        value = if (app == null) emptyList()
        else withContext(Dispatchers.IO) { viewModel.shortcuts(app) }
    }

    androidx.compose.runtime.LaunchedEffect(focusSearch, settings.autoFocusSearch) {
        if (focusSearch || settings.autoFocusSearch) {
            delay(180)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    androidx.compose.runtime.LaunchedEffect(query) {
        if (query.isNotEmpty()) listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clickable(onClick = onClose)
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total > 24f) onClose() },
                        onVerticalDrag = { _, delta -> total += delta },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .background(MaterialTheme.colorScheme.outline, CircleShape),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchField(
                query = query,
                onQueryChange = { query = it },
                focusRequester = focusRequester,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = {
                    viewModel.setLayout(if (isGrid) AppLayout.LIST else AppLayout.GRID)
                },
            ) {
                Icon(
                    imageVector = if (isGrid) Icons.Outlined.ViewAgenda else Icons.Outlined.Apps,
                    contentDescription = if (isGrid) "Tampilan daftar" else "Tampilan kisi",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Pengaturan",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 4.dp, end = 28.dp, top = 4.dp, bottom = 48.dp),
            ) {
                itemsIndexed(items) { _, item ->
                    when (item) {
                        is DrawerItem.LetterHeader -> Text(
                            text = item.letter.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 1.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 20.dp, top = 18.dp, bottom = 4.dp),
                        )

                        is DrawerItem.RowItem -> {
                            if (isGrid) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    item.apps.forEach { app ->
                                        AppGridCell(
                                            app = app,
                                            onClick = { viewModel.launch(app) },
                                            onLongClick = {
                                                if (settings.haptics) {
                                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                                }
                                                selectedApp = app
                                            },
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat(settings.gridColumns - item.apps.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            } else {
                                item.apps.forEach { app ->
                                    AppListRow(
                                        app = app,
                                        onClick = { viewModel.launch(app) },
                                        onLongClick = {
                                            if (settings.haptics) {
                                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                            selectedApp = app
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (filtered.isEmpty()) {
                Text(
                    text = if (loading) "Memuat aplikasi…" else "Tidak ada aplikasi yang cocok",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 48.dp),
                )
            }

            if (settings.showAlphabetRail && query.isBlank()) {
                AlphabetRail(
                    letters = letters,
                    onLetterClick = { letter ->
                        if (settings.haptics) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        val index = letterIndices[letter]
                        if (index != null) {
                            scope.launch { listState.scrollToItem(index) }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.92f)
                        .padding(end = 2.dp),
                )
            }
        }
    }

    selectedApp?.let { app ->
        AppActionsSheet(
            app = app,
            isFavorite = app.key in settings.favorites,
            shortcuts = shortcuts,
            onDismiss = { selectedApp = null },
            onOpen = {
                viewModel.launch(app)
                selectedApp = null
            },
            onShortcut = { shortcut ->
                viewModel.launchShortcut(app, shortcut)
                selectedApp = null
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(app)
                selectedApp = null
            },
            onHide = {
                viewModel.setHidden(app, true)
                selectedApp = null
            },
            onAppInfo = {
                SystemIntents.appInfo(context, app.packageName)
                selectedApp = null
            },
            onUninstall = {
                SystemIntents.uninstall(context, app.packageName)
                selectedApp = null
            },
        )
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.titleMedium.copy(
        fontSize = 17.sp,
        fontWeight = FontWeight.Normal,
        color = colors.onSurface,
    )
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.surfaceVariant.copy(alpha = 0.55f))
            .focusRequester(focusRequester)
            .padding(horizontal = 16.dp),
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(colors.primary),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari aplikasi",
                            style = textStyle,
                            color = colors.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Hapus pencarian",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppListRow(
    app: LaunchableApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, size = 34.dp)
        Spacer(Modifier.width(16.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (app.isWorkProfile) {
            WorkBadge()
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGridCell(
    app: LaunchableApp,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(vertical = 10.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app = app, size = 46.dp)
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WorkBadge() {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = "kerja",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
        )
    }
}

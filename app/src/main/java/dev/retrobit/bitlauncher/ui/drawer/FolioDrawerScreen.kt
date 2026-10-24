package dev.retrobit.bitlauncher.ui.drawer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dev.retrobit.bitlauncher.ui.home.DottedLeader
import dev.retrobit.bitlauncher.ui.home.DoubleRule
import dev.retrobit.bitlauncher.ui.home.Hairline
import dev.retrobit.bitlauncher.ui.theme.folioAccent
import dev.retrobit.bitlauncher.util.AppSearch
import dev.retrobit.bitlauncher.util.DrawerItem
import dev.retrobit.bitlauncher.util.SystemIntents
import dev.retrobit.bitlauncher.util.buildDrawerItems
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The archive: every installed app presented as a printed index, with page-style numbering,
 * dotted leaders and section rules.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolioDrawerScreen(
    viewModel: LauncherViewModel,
    focusSearch: Boolean,
    onClose: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val notificationCounts by viewModel.notificationCounts.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
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
    val entryNumber = remember(filtered) {
        filtered.mapIndexed { index, app -> app.key to index + 1 }.toMap()
    }

    val shortcuts by produceState(
        initialValue = emptyList<android.content.pm.ShortcutInfo>(),
        selectedApp?.key,
    ) {
        val app = selectedApp
        value = if (app == null) emptyList()
        else withContext(Dispatchers.IO) { viewModel.shortcuts(app) }
    }

    LaunchedEffect(focusSearch, settings.autoFocusSearch) {
        if (focusSearch || settings.autoFocusSearch) {
            delay(180)
            runCatching { focusRequester.requestFocus() }
            keyboard?.show()
        }
    }

    LaunchedEffect(query) {
        if (query.isNotEmpty()) listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        // ------------------------------------------------------------ masthead
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
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
                        .size(width = 40.dp, height = 3.dp)
                        .background(colors.outline),
                )
            }
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "ARSIP",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.onBackground,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "${filtered.size} JUDUL",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "TUTUP",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .padding(bottom = 10.dp)
                        .clickable(onClick = onClose),
                )
            }
            Spacer(Modifier.height(8.dp))
            DoubleRule()
            Spacer(Modifier.height(12.dp))
        }

        // -------------------------------------------------------------- search
        FolioSearchField(
            query = query,
            onQueryChange = { query = it },
            focusRequester = focusRequester,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isGrid) "TAMPILAN: KISI" else "TAMPILAN: DAFTAR",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier
                    .clickable { viewModel.setLayout(if (isGrid) AppLayout.LIST else AppLayout.GRID) }
                    .padding(vertical = 8.dp),
            )
            Spacer(Modifier.weight(1f))
            IconButton(
                onClick = { viewModel.setLayout(if (isGrid) AppLayout.LIST else AppLayout.GRID) },
            ) {
                Icon(
                    imageVector = if (isGrid) Icons.Outlined.ViewAgenda else Icons.Outlined.Apps,
                    contentDescription = "Ubah tata letak",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Pengaturan",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Hairline()

        // ---------------------------------------------------------------- list
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 0.dp, end = 26.dp, top = 0.dp, bottom = 40.dp),
            ) {
                itemsIndexed(items) { _, item ->
                    when (item) {
                        is DrawerItem.LetterHeader -> SectionHeading(letter = item.letter)

                        is DrawerItem.RowItem -> {
                            if (isGrid) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 2.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    item.apps.forEach { app ->
                                        FolioGridCell(
                                            app = app,
                                            unread = if (settings.showNotificationBadges) {
                                                notificationCounts[app.packageName] ?: 0
                                            } else {
                                                0
                                            },
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
                                    val number = entryNumber[app.key] ?: 0
                                    FolioArchiveRow(
                                        app = app,
                                        unread = if (settings.showNotificationBadges) {
                                            notificationCounts[app.packageName] ?: 0
                                        } else {
                                            0
                                        },
                                        number = number,
                                        accent = folioAccent(number),
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
                    text = if (loading) "Memuat arsip…" else "Tidak ada judul yang cocok",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurfaceVariant,
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
                        letterIndices[letter]?.let { index ->
                            scope.launch { listState.scrollToItem(index) }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight(0.9f)
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
private fun SectionHeading(letter: Char) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = letter.toString(),
            style = MaterialTheme.typography.displaySmall.copy(fontSize = 28.sp),
            color = colors.onBackground,
        )
        Spacer(Modifier.width(10.dp))
        Hairline(color = colors.outline, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun FolioSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
) {
    val colors = MaterialTheme.colorScheme
    val textStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 16.sp,
        color = colors.onBackground,
    )
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .height(46.dp)
            .border(1.dp, colors.outline, RoundedCornerShape(2.dp))
            .focusRequester(focusRequester)
            .padding(horizontal = 14.dp),
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
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Cari di arsip",
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
                            .size(16.dp)
                            .clickable { onQueryChange("") },
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolioArchiveRow(
    app: LaunchableApp,
    unread: Int,
    number: Int,
    accent: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(start = 20.dp, end = 6.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = number.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = accent,
            modifier = Modifier.width(22.dp),
        )
        AppIcon(app = app, size = 28.dp, badgeCount = unread)
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        if (app.isWorkProfile) {
            Spacer(Modifier.width(6.dp))
            Text(
                text = "KERJA",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = colors.onSurfaceVariant,
            )
        }
        DottedLeader(
            color = colors.outlineVariant,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
        )
        Text(
            text = "›",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolioGridCell(
    app: LaunchableApp,
    unread: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .border(BorderStroke(1.dp, colors.outlineVariant), RoundedCornerShape(2.dp))
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AppIcon(app = app, size = 40.dp, badgeCount = unread)
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

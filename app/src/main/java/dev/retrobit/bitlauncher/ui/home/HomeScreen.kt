package dev.retrobit.bitlauncher.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retrobit.bitlauncher.LauncherViewModel
import dev.retrobit.bitlauncher.data.GestureAction
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.ui.components.AppActionsSheet
import dev.retrobit.bitlauncher.ui.components.AppIcon
import dev.retrobit.bitlauncher.ui.components.ClockHeader
import dev.retrobit.bitlauncher.ui.components.HomeMenuSheet
import dev.retrobit.bitlauncher.util.SystemIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val HomeTextShadow = Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = Offset(0f, 2f),
    blurRadius = 12f,
)

private val HomeBarColor = Color.Black.copy(alpha = 0.42f)

/**
 * Niagara-style vertical home: a huge clock, a single scrollable column of pinned apps and a
 * minimal bottom bar that opens the drawer. Long-press on empty space opens the Nova-style menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    viewModel: LauncherViewModel,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val notificationCounts by viewModel.notificationCounts.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var showHomeMenu by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<LaunchableApp?>(null) }
    var isDefaultLauncher by remember { mutableStateOf(SystemIntents.isDefaultLauncher(context)) }

    val shortcuts by produceState(
        initialValue = emptyList<android.content.pm.ShortcutInfo>(),
        selectedApp?.key,
    ) {
        val app = selectedApp
        value = if (app == null) emptyList()
        else withContext(Dispatchers.IO) { viewModel.shortcuts(app) }
    }

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefaultLauncher = SystemIntents.isDefaultLauncher(context) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultLauncher = SystemIntents.isDefaultLauncher(context)
    }

    val notesApp = remember(allApps, settings.notesPackage) {
        findNotesApp(allApps, settings.notesPackage)
    }
    fun openNotes() {
        val app = notesApp
        if (app != null) {
            viewModel.launch(app)
        } else {
            Toast.makeText(context, "Pilih aplikasi catatan di Pengaturan", Toast.LENGTH_SHORT).show()
            onOpenSettings()
        }
    }
    val performGesture = rememberHomeGestureHandler(
        onOpenDrawer = { onOpenDrawer(false) },
        onOpenSearch = { onOpenDrawer(true) },
        onOpenSettings = onOpenSettings,
        onOpenHomeMenu = { showHomeMenu = true },
        onOpenNotes = ::openNotes,
    )

    Box(modifier = Modifier.fillMaxSize()) {
        // Scrim keeps white text readable on any wallpaper.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.38f),
                        0.30f to Color.Black.copy(alpha = 0.10f),
                        0.68f to Color.Transparent,
                        1f to Color.Black.copy(alpha = 0.58f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .homeHorizontalGestures(
                    leftAction = settings.swipeLeftAction,
                    rightAction = settings.swipeRightAction,
                    onAction = performGesture,
                )
                .homeTwoFingerGestures(
                    upAction = settings.twoFingerSwipeUpAction,
                    downAction = settings.twoFingerSwipeDownAction,
                    onAction = performGesture,
                )
                .pointerInput(settings.doubleTapAction, settings.longPressAction) {
                    detectTapGestures(
                        onLongPress = { performGesture(settings.longPressAction) },
                        onDoubleTap = { performGesture(settings.doubleTapAction) },
                    )
                },
        ) {
            if (!isDefaultLauncher) {
                DefaultLauncherBanner(
                    onClick = {
                        runCatching {
                            roleLauncher.launch(SystemIntents.defaultLauncherIntent(context))
                        }
                    },
                )
            }

            if (settings.showClock) {
                ClockHeader(
                    use24Hour = settings.use24Hour,
                    showDate = settings.showDate,
                    color = Color.White,
                    onClick = { SystemIntents.openAlarms(context) },
                )
            }

            Spacer(Modifier.height(8.dp))

            if (favorites.isEmpty()) {
                EmptyFavorites(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onOpenDrawer = { onOpenDrawer(false) },
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    items(favorites, key = { it.key }) { app ->
                        FavoriteRow(
                            app = app,
                            compact = settings.compactRows,
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
                        )
                    }
                }
            }

            HomeBottomBar(
                onSearch = { onOpenDrawer(true) },
                swipeUpAction = settings.swipeUpAction,
                onGesture = performGesture,
                onOpenSettings = onOpenSettings,
            )
        }
    }

    if (showHomeMenu) {
        HomeMenuSheet(
            onDismiss = { showHomeMenu = false },
            onOpenSettings = {
                showHomeMenu = false
                onOpenSettings()
            },
            onWallpaper = {
                showHomeMenu = false
                SystemIntents.openWallpaperPicker(context)
            },
            onDefaultLauncher = {
                showHomeMenu = false
                runCatching { roleLauncher.launch(SystemIntents.defaultLauncherIntent(context)) }
            },
            onRefresh = {
                showHomeMenu = false
                viewModel.refresh()
            },
        )
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
private fun DefaultLauncherBanner(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = HomeBarColor,
        contentColor = Color.White,
        modifier = Modifier.padding(start = 24.dp, top = 8.dp),
    ) {
        Text(
            text = "Jadikan BitLauncher launcher default  ›",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun EmptyFavorites(modifier: Modifier, onOpenDrawer: () -> Unit) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Beranda masih kosong",
            style = MaterialTheme.typography.headlineSmall,
            color = Color.White,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Buka laci aplikasi, lalu tekan lama sebuah aplikasi untuk mem-pin ke beranda.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.72f),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "Buka laci aplikasi  ›",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            modifier = Modifier.clickable(onClick = onOpenDrawer),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FavoriteRow(
    app: LaunchableApp,
    compact: Boolean,
    unread: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 24.dp, vertical = if (compact) 7.dp else 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, size = if (compact) 24.dp else 30.dp, badgeCount = unread)
        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = app.label,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontSize = if (compact) 21.sp else 26.sp,
                    fontWeight = FontWeight.Light,
                    shadow = HomeTextShadow,
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.isWorkProfile) {
                Text(
                    text = "Profil kerja",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.62f),
                )
            }
        }
    }
}

@Composable
private fun HomeBottomBar(
    onSearch: () -> Unit,
    swipeUpAction: GestureAction,
    onGesture: (GestureAction) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().navigationBarsPadding()) {
        // Swipe up anywhere on the bottom bar to open the drawer (Niagara gesture).
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(26.dp)
                .pointerInput(swipeUpAction) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total < -24f) onGesture(swipeUpAction) },
                        onVerticalDrag = { _, delta -> total += delta },
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = 40.dp, height = 4.dp)
                    .background(color = Color.White.copy(alpha = 0.45f), shape = CircleShape),
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                onClick = onSearch,
                shape = RoundedCornerShape(28.dp),
                color = HomeBarColor,
                contentColor = Color.White,
                modifier = Modifier.weight(1f),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Cari aplikasi",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowUp,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            CircleButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Outlined.Tune,
                    contentDescription = "Pengaturan launcher",
                    modifier = Modifier.size(20.dp),
                    tint = Color.White,
                )
            }
        }
    }
}

@Composable
private fun CircleButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = HomeBarColor,
        contentColor = Color.White,
    ) {
        Box(
            modifier = Modifier.size(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

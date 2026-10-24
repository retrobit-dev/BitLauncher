package dev.retrobit.bitlauncher.ui.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retrobit.bitlauncher.LauncherViewModel
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.ui.components.AppActionsSheet
import dev.retrobit.bitlauncher.ui.components.AppIcon
import dev.retrobit.bitlauncher.ui.components.HomeMenuSheet
import dev.retrobit.bitlauncher.ui.components.rememberMinuteTick
import dev.retrobit.bitlauncher.ui.theme.folioAccent
import dev.retrobit.bitlauncher.util.SystemIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val EditorialLocale = Locale("id", "ID")

/**
 * "Folio" home: the front page of a newspaper printed on e-paper.
 *
 * Masthead, double rule, dateline, serif clock and a dotted-leader index of pinned apps.
 * Everything is ink on paper — no wallpaper, no glass, no shadow.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolioHomeScreen(
    viewModel: LauncherViewModel,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val colors = MaterialTheme.colorScheme

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

    val now = rememberMinuteTick()
    val timeFormatter = remember(settings.use24Hour) {
        SimpleDateFormat(if (settings.use24Hour) "HH:mm" else "h:mm", Locale.getDefault())
    }
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", EditorialLocale) }
    val edition = remember(now) { now.get(Calendar.DAY_OF_YEAR) }
    val timeText = timeFormatter.format(now.time)
    val dateline = dateFormatter.format(now.time).uppercase(EditorialLocale)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showHomeMenu = true },
                    onDoubleTap = { onOpenDrawer(true) },
                )
            },
    ) {
        Masthead(edition = edition, dateline = dateline)

        if (!isDefaultLauncher) {
            DefaultLauncherNotice(
                onClick = {
                    runCatching { roleLauncher.launch(SystemIntents.defaultLauncherIntent(context)) }
                },
            )
        }

        // ------------------------------------------------------------ clock
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "WAKTU SETEMPAT",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (settings.use24Hour) "FORMAT 24 JAM" else "FORMAT 12 JAM",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            Text(
                text = timeText,
                style = MaterialTheme.typography.displayLarge,
                color = colors.onBackground,
                maxLines = 1,
            )
        }

        Spacer(Modifier.height(16.dp))
        Hairline()
        SectionLabel(
            title = "INDEKS APLIKASI",
            trailing = "${favorites.size.toString().padStart(2, '0')} ENTRI",
        )
        Hairline()

        if (favorites.isEmpty()) {
            EmptyFolio(
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
                contentPadding = PaddingValues(vertical = 6.dp),
            ) {
                itemsIndexed(favorites, key = { _, app -> app.key }) { index, app ->
                    FolioAppRow(
                        app = app,
                        index = index + 1,
                        accent = folioAccent(index),
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

        FolioFooter(
            onSearch = { onOpenDrawer(true) },
            onOpenDrawer = { onOpenDrawer(false) },
            onOpenSettings = onOpenSettings,
        )
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

// ------------------------------------------------------------------ primitives

@Composable
fun Hairline(
    color: Color = MaterialTheme.colorScheme.outlineVariant,
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = color)
}

/** Thick-thin rule used under the masthead, the classic newspaper double line. */
@Composable
fun DoubleRule(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier.fillMaxWidth()) {
        HorizontalDivider(thickness = 2.5.dp, color = colors.onBackground)
        Spacer(Modifier.height(2.dp))
        HorizontalDivider(thickness = 0.8.dp, color = colors.onBackground)
    }
}

@Composable
fun SectionLabel(title: String, trailing: String? = null) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onBackground,
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Masthead(edition: Int, dateline: String) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BITLAUNCHER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.4.sp,
                    fontSize = 11.sp,
                ),
                color = colors.onBackground,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "EDISI ${edition.toString().padStart(3, '0')}",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(7.dp))
        DoubleRule()
        Text(
            text = dateline,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.2.sp),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
        )
        Hairline()
    }
}

@Composable
private fun DefaultLauncherNotice(onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "BITLAUNCHER BELUM MENJADI LAUNCHER DEFAULT",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = colors.secondary,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "ATUR →",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = colors.secondary,
        )
    }
    Hairline()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolioAppRow(
    app: LaunchableApp,
    index: Int,
    accent: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = accent,
            modifier = Modifier.width(24.dp),
        )
        AppIcon(app = app, size = 26.dp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = app.label,
                style = MaterialTheme.typography.headlineSmall.copy(fontSize = 23.sp),
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (app.isWorkProfile) {
                Text(
                    text = "PROFIL KERJA",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = colors.onSurfaceVariant,
                )
            }
        }
        DottedLeader(
            color = colors.outline,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        )
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
    }
}

@Composable
fun DottedLeader(
    color: Color,
    modifier: Modifier = Modifier,
) {
    val dash = with(LocalDensity.current) {
        floatArrayOf(1.6.dp.toPx(), 5.dp.toPx())
    }
    Box(
        modifier = modifier
            .height(10.dp)
            .drawBehind {
                drawLine(
                    color = color,
                    start = androidx.compose.ui.geometry.Offset(0f, size.height / 2f),
                    end = androidx.compose.ui.geometry.Offset(size.width, size.height / 2f),
                    strokeWidth = 1.dp.toPx() * 0.8f,
                    pathEffect = PathEffect.dashPathEffect(dash),
                )
            },
    )
}

@Composable
private fun EmptyFolio(modifier: Modifier, onOpenDrawer: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier.padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Belum ada entri hari ini",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.onBackground,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Buka arsip aplikasi, lalu tekan lama satu aplikasi untuk menaruhnya di halaman depan.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = "BUKA ARSIP →",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.primary,
            modifier = Modifier.clickable(onClick = onOpenDrawer),
        )
    }
}

@Composable
private fun FolioFooter(
    onSearch: () -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.navigationBarsPadding()) {
        // Swipe up to open the archive, same gesture as the minimal home.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .pointerInput(Unit) {
                    var total = 0f
                    detectVerticalDragGestures(
                        onDragStart = { total = 0f },
                        onDragEnd = { if (total < -24f) onOpenDrawer() },
                        onVerticalDrag = { _, delta -> total += delta },
                    )
                },
        )
        Hairline(color = colors.onBackground)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                onClick = onSearch,
                color = Color.Transparent,
                contentColor = colors.onBackground,
                shape = RoundedCornerShape(2.dp),
                border = BorderStroke(1.dp, colors.onBackground),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Outlined.Search, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "CARI DI ARSIP",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                }
            }
            FolioSquareButton(icon = Icons.Outlined.Apps, label = "Buka arsip", onClick = onOpenDrawer)
            FolioSquareButton(icon = Icons.Outlined.Tune, label = "Pengaturan", onClick = onOpenSettings)
        }
        Text(
            text = "BITLAUNCHER · LAUNCHER EDITORIAL · HLM. 1",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.2.sp),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
        )
    }
}

@Composable
private fun FolioSquareButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        contentColor = colors.onBackground,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, colors.onBackground),
        modifier = Modifier.size(46.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp))
        }
    }
}

/** Kept for callers that need a plain newspaper rule inside other screens. */
@Composable
fun EditorialRule(color: Color = MaterialTheme.colorScheme.onBackground) {
    Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
        drawRect(color = color)
    }
}

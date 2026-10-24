package dev.retrobit.bitlauncher.ui.home

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retrobit.bitlauncher.LauncherViewModel
import dev.retrobit.bitlauncher.R
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.ui.components.AppIcon
import dev.retrobit.bitlauncher.ui.components.HomeMenuSheet
import dev.retrobit.bitlauncher.ui.components.rememberMinuteTick
import dev.retrobit.bitlauncher.util.AppSearch
import dev.retrobit.bitlauncher.util.SystemIntents
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val EditorialLocale = Locale("id", "ID")
private const val APP_LINK_TAG = "news-app"
private const val CALENDAR_LINK_TAG = "news-calendar"
private const val NOTES_LINK_TAG = "news-notes"

private val NOTE_PACKAGES = setOf(
    "com.google.android.keep",
    "com.samsung.android.app.notes",
    "com.miui.notes",
    "com.oppo.notes",
    "com.coloros.note",
    "com.vivo.notes",
    "com.evernote",
    "com.notion.id",
)

private data class NewsEntry(val app: LaunchableApp, val count: Int)

/** A pure newspaper front page: masthead search, live article, and editorial widgets. */
@Composable
fun FolioHomeScreen(
    viewModel: LauncherViewModel,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val notificationCounts by viewModel.notificationCounts.collectAsStateWithLifecycle()
    val notificationAccess by viewModel.notificationAccess.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    var showHomeMenu by remember { mutableStateOf(false) }
    var isDefaultLauncher by remember { mutableStateOf(SystemIntents.isDefaultLauncher(context)) }
    var searchExpanded by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val roleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefaultLauncher = SystemIntents.isDefaultLauncher(context) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultLauncher = SystemIntents.isDefaultLauncher(context)
        viewModel.refreshNotificationAccess()
    }

    LaunchedEffect(searchExpanded) {
        if (searchExpanded) {
            searchFocus.requestFocus()
            keyboard?.show()
        } else {
            keyboard?.hide()
        }
    }

    val now = rememberMinuteTick()
    val timeFormatter = remember(settings.use24Hour) {
        SimpleDateFormat(if (settings.use24Hour) "HH.mm" else "h.mm", Locale.getDefault())
    }
    val dateFormatter = remember { SimpleDateFormat("EEEE, d MMMM yyyy", EditorialLocale) }
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", EditorialLocale) }
    val edition = remember(now) { now.get(Calendar.DAY_OF_YEAR) }
    val timeText = timeFormatter.format(now.time)
    val dateline = dateFormatter.format(now.time)
    val location = settings.newsLocation.trim().ifBlank { "Rembang" }.take(40)

    val newsEntries = remember(apps, notificationCounts) {
        apps.distinctBy { it.packageName }.mapNotNull { app ->
            notificationCounts[app.packageName]?.takeIf { it > 0 }?.let { NewsEntry(app, it) }
        }.sortedByDescending { it.count }.take(3)
    }
    val shortcutApps = remember(favorites, apps) {
        (if (favorites.isNotEmpty()) favorites else apps).distinctBy { it.packageName }.take(3)
    }
    val notesApp = remember(allApps, settings.notesPackage) {
        allApps.firstOrNull { it.packageName == settings.notesPackage }
            ?: allApps.firstOrNull { it.packageName in NOTE_PACKAGES }
            ?: allApps.firstOrNull { app ->
                val label = app.label.lowercase()
                "catatan" in label || "notes" in label || "keep" in label
            }
    }
    val searchResults = remember(apps, searchQuery) {
        if (searchQuery.isBlank()) emptyList() else AppSearch.filter(apps, searchQuery).take(6)
    }
    val headline = when {
        newsEntries.isNotEmpty() -> {
            val lead = newsEntries.first()
            "${lead.count} ${newsNoun(lead.app)} menanti di ${lead.app.label}"
        }
        notificationAccess -> "Tidak ada kabar baru hari ini"
        else -> "Kabar dari aplikasi Anda"
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .pointerInput(Unit) {
                var distance = 0f
                detectHorizontalDragGestures(
                    onDragStart = { distance = 0f },
                    onDragEnd = {
                        when {
                            distance < -70f -> onOpenSettings()
                            distance > 70f -> SystemIntents.expandNotifications(context)
                        }
                    },
                    onHorizontalDrag = { _, delta -> distance += delta },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { showHomeMenu = true },
                    onDoubleTap = { searchExpanded = true },
                )
            },
    ) {
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 12.dp),
        ) {
            item {
                Masthead(
                    edition = edition,
                    dateline = if (settings.showDate) dateline.uppercase(EditorialLocale) else "LAUNCHER EDITORIAL",
                    searchExpanded = searchExpanded,
                    searchQuery = searchQuery,
                    searchFocus = searchFocus,
                    onQueryChange = { searchQuery = it },
                    onToggleSearch = {
                        searchExpanded = !searchExpanded
                        if (!searchExpanded) searchQuery = ""
                    },
                    onSubmit = {
                        searchResults.firstOrNull()?.let { app ->
                            viewModel.launch(app)
                            searchQuery = ""
                            searchExpanded = false
                        }
                    },
                )
            }

            if (!isDefaultLauncher) {
                item {
                    DefaultLauncherNotice(
                        onClick = {
                            runCatching { roleLauncher.launch(SystemIntents.defaultLauncherIntent(context)) }
                        },
                    )
                }
            }

            if (searchQuery.isNotBlank()) {
                item {
                    SectionLabel(title = "HASIL PENCARIAN", trailing = "${searchResults.size} APLIKASI")
                }
                if (searchResults.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada aplikasi yang cocok",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    items(searchResults, key = { it.key }) { app ->
                        SearchResultRow(
                            app = app,
                            unread = notificationCounts[app.packageName] ?: 0,
                            onClick = {
                                viewModel.launch(app)
                                searchQuery = ""
                                searchExpanded = false
                            },
                        )
                    }
                }
            } else {
                item {
                    SectionLabel(title = "KABAR UTAMA", trailing = "HLM. 1")
                    Text(
                        text = headline,
                        style = MaterialTheme.typography.displaySmall,
                        color = colors.onBackground,
                        modifier = Modifier.padding(horizontal = 20.dp),
                    )
                    Spacer(Modifier.height(10.dp))
                }

                if (settings.showNewsPhoto) {
                    item { NewsPhoto() }
                }

                item {
                    NewsArticle(
                        location = location,
                        date = if (settings.showDate) dateline else "",
                        time = if (settings.showClock) timeText else "",
                        notificationAccess = notificationAccess,
                        entries = newsEntries,
                        shortcutApps = shortcutApps,
                        onOpenApp = { app -> viewModel.launch(app) },
                        onOpenCalendar = { SystemIntents.openCalendar(context) },
                        onOpenNotes = ::openNotes,
                    )
                }

                if (!notificationAccess) {
                    item { NotificationAccessNotice(onEnable = { SystemIntents.openNotificationAccess(context) }) }
                }

                if (settings.showNewsWidgets) {
                    item {
                        NewsWidgets(
                            day = now.get(Calendar.DAY_OF_MONTH),
                            monthYear = monthFormatter.format(now.time),
                            notesLabel = notesApp?.label ?: "Pilih aplikasi",
                            onOpenCalendar = { SystemIntents.openCalendar(context) },
                            onOpenNotes = ::openNotes,
                        )
                    }
                }
            }
        }

        FolioGestureFooter(onOpenDrawer = { onOpenDrawer(false) })
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
}

// ---------------------------------------------------------------------- masthead

@Composable
private fun Masthead(
    edition: Int,
    dateline: String,
    searchExpanded: Boolean,
    searchQuery: String,
    searchFocus: FocusRequester,
    onQueryChange: (String) -> Unit,
    onToggleSearch: () -> Unit,
    onSubmit: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "BITLAUNCHER",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = if (searchExpanded) 1.4.sp else 3.4.sp,
                    fontSize = if (searchExpanded) 9.sp else 11.sp,
                ),
                color = colors.onBackground,
                maxLines = 1,
            )
            Spacer(Modifier.width(8.dp))
            if (searchExpanded) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                        .border(1.dp, colors.outline, RoundedCornerShape(2.dp))
                        .focusRequester(searchFocus)
                        .padding(horizontal = 8.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(
                        color = colors.onBackground,
                        fontSize = 14.sp,
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                    decorationBox = { innerTextField ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Cari aplikasi",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        }
                    },
                )
            } else {
                Spacer(Modifier.weight(1f))
                Text(
                    text = "EDISI ${edition.toString().padStart(3, '0')}",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.onSurfaceVariant,
                )
            }
            IconButton(onClick = onToggleSearch, modifier = Modifier.size(34.dp)) {
                Icon(
                    imageVector = if (searchExpanded) Icons.Outlined.Close else Icons.Outlined.Search,
                    contentDescription = if (searchExpanded) "Tutup pencarian" else "Cari aplikasi",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        DoubleRule()
        Text(
            text = dateline,
            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 2.2.sp),
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 7.dp),
        )
        Hairline()
    }
}

@Composable
private fun SearchResultRow(app: LaunchableApp, unread: Int, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppIcon(app = app, size = 28.dp)
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (unread > 0) {
            Text(
                text = "$unread ${newsNoun(app).uppercase(EditorialLocale)}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                color = colors.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------- article

@Composable
private fun NewsPhoto() {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.padding(horizontal = 20.dp)) {
        Image(
            painter = painterResource(R.drawable.news_hero),
            contentDescription = "Ilustrasi suasana jalan di pagi hari",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth().height(176.dp),
        )
        Hairline(color = colors.onBackground)
        Text(
            text = "ILUSTRASI · SUASANA PAGI",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(vertical = 5.dp),
        )
    }
}

@Composable
private fun NewsArticle(
    location: String,
    date: String,
    time: String,
    notificationAccess: Boolean,
    entries: List<NewsEntry>,
    shortcutApps: List<LaunchableApp>,
    onOpenApp: (LaunchableApp) -> Unit,
    onOpenCalendar: () -> Unit,
    onOpenNotes: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val linkStyle = SpanStyle(
        color = colors.primary,
        fontWeight = FontWeight.Bold,
        textDecoration = TextDecoration.Underline,
    )
    val linkedApps = (entries.map { it.app } + shortcutApps).associateBy { it.key }
    val article = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(location) }
        if (date.isNotBlank()) {
            append(", ")
            val start = length
            withStyle(linkStyle) { append(date) }
            addStringAnnotation(CALENDAR_LINK_TAG, "calendar", start, length)
        }
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(" — ") }
        val timeClause = if (time.isBlank()) "Pada edisi ini" else "Pada pukul $time"

        when {
            !notificationAccess -> {
                append("$timeClause, halaman ini belum dapat membaca jumlah pesan. ")
                append("Aktifkan akses notifikasi untuk kabar terbaru. ")
                if (shortcutApps.isEmpty()) {
                    append("Arsip aplikasi dapat dibuka dengan gestur geser ke atas.")
                } else {
                    append("Sementara itu, ")
                    appendAppLinks(shortcutApps, linkStyle)
                    append(" siap dibuka dari artikel ini.")
                }
            }
            entries.isEmpty() -> {
                append("$timeClause, tidak ada pesan baru yang menanti. Suasana redaksi tenang. ")
                if (shortcutApps.isEmpty()) {
                    append("Arsip aplikasi dapat dibuka dengan gestur geser ke atas.")
                } else {
                    append("Namun ")
                    appendAppLinks(shortcutApps, linkStyle)
                    append(" tetap dapat dibuka dengan satu ketukan.")
                }
            }
            else -> {
                val lead = entries.first()
                append("$timeClause terjadi sebuah fenomena aneh: ada ")
                appendAppLink(
                    text = "${lead.count} ${newsNoun(lead.app)} belum dibaca di ${lead.app.label}",
                    app = lead.app,
                    style = linkStyle,
                )
                append(".")
                entries.drop(1).forEach { entry ->
                    append(" Kabar lain datang dari ")
                    appendAppLink(
                        text = "${entry.app.label}, dengan ${entry.count} ${newsNoun(entry.app)} yang menanti",
                        app = entry.app,
                        style = linkStyle,
                    )
                    append(".")
                }
                val additional = shortcutApps.filter { app -> entries.none { it.app.key == app.key } }
                if (additional.isNotEmpty()) {
                    append(" Untuk kabar berikutnya, buka ")
                    appendAppLinks(additional, linkStyle)
                    append(".")
                }
            }
        }
        append(" Untuk mencatat kabar ini, buka ")
        val notesStart = length
        withStyle(linkStyle) { append("Catatan") }
        addStringAnnotation(NOTES_LINK_TAG, "notes", notesStart, length)
        append(".")
    }

    ClickableText(
        text = article,
        style = MaterialTheme.typography.bodyMedium.copy(
            color = colors.onBackground,
            fontSize = 17.sp,
            lineHeight = 26.sp,
            textAlign = TextAlign.Justify,
        ),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
        onClick = { offset ->
            article.getStringAnnotations(APP_LINK_TAG, offset, offset).firstOrNull()?.let { link ->
                linkedApps[link.item]?.let(onOpenApp)
            }
            if (article.getStringAnnotations(CALENDAR_LINK_TAG, offset, offset).isNotEmpty()) {
                onOpenCalendar()
            }
            if (article.getStringAnnotations(NOTES_LINK_TAG, offset, offset).isNotEmpty()) {
                onOpenNotes()
            }
        },
    )
}

private fun AnnotatedString.Builder.appendAppLink(text: String, app: LaunchableApp, style: SpanStyle) {
    val start = length
    withStyle(style) { append(text) }
    addStringAnnotation(APP_LINK_TAG, app.key, start, length)
}

private fun AnnotatedString.Builder.appendAppLinks(apps: List<LaunchableApp>, style: SpanStyle) {
    apps.forEachIndexed { index, app ->
        when (index) {
            0 -> Unit
            apps.lastIndex -> append(" dan ")
            else -> append(", ")
        }
        appendAppLink(app.label, app, style)
    }
}

private fun newsNoun(app: LaunchableApp): String {
    val messagingPackages = setOf(
        "com.whatsapp", "com.whatsapp.w4b", "org.telegram.messenger",
        "com.facebook.orca", "jp.naver.line.android", "com.google.android.gm",
        "com.google.android.apps.messaging", "com.android.messaging",
        "com.microsoft.office.outlook",
    )
    return if (app.packageName in messagingPackages) "pesan" else "notifikasi"
}

@Composable
private fun NewsWidgets(
    day: Int,
    monthYear: String,
    notesLabel: String,
    onOpenCalendar: () -> Unit,
    onOpenNotes: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Surface(
            onClick = onOpenCalendar,
            color = Color.Transparent,
            contentColor = colors.onBackground,
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, colors.outline),
            modifier = Modifier.weight(1f).height(118.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "KALENDER", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(text = day.toString(), style = MaterialTheme.typography.displaySmall)
                Text(
                    text = monthYear.uppercase(EditorialLocale),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = colors.onSurfaceVariant,
                )
            }
        }
        Surface(
            onClick = onOpenNotes,
            color = Color.Transparent,
            contentColor = colors.onBackground,
            shape = RoundedCornerShape(2.dp),
            border = BorderStroke(1.dp, colors.outline),
            modifier = Modifier.weight(1f).height(118.dp),
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "CATATAN", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                Text(
                    text = notesLabel,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "KETUK UNTUK MEMBUKA",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun NotificationAccessNotice(onEnable: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        onClick = onEnable,
        color = Color.Transparent,
        contentColor = colors.onBackground,
        shape = RoundedCornerShape(2.dp),
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Notifications, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = "AKTIFKAN AKSES NOTIFIKASI",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = "Hanya jumlah notifikasi aktif yang digunakan; isi pesan tidak disimpan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
    }
}

// ------------------------------------------------------------------ gestures/footer

@Composable
private fun FolioGestureFooter(onOpenDrawer: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .clickable(onClick = onOpenDrawer)
            .pointerInput(Unit) {
                var distance = 0f
                detectVerticalDragGestures(
                    onDragStart = { distance = 0f },
                    onDragEnd = { if (distance < -28f) onOpenDrawer() },
                    onVerticalDrag = { _, delta -> distance += delta },
                )
            }
            .padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = Modifier.width(40.dp).height(3.dp).background(colors.outline))
        Spacer(Modifier.height(5.dp))
        Text(
            text = "GESER ATAS UNTUK ARSIP · HLM. 1",
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, letterSpacing = 1.2.sp),
            color = colors.onSurfaceVariant,
        )
    }
}

// ------------------------------------------------------------------ shared primitives

@Composable
fun Hairline(color: Color = MaterialTheme.colorScheme.outlineVariant, modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, thickness = 1.dp, color = color)
}

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
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = colors.onBackground,
        )
        Spacer(Modifier.weight(1f))
        if (trailing != null) {
            Text(text = trailing, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
        }
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
        Text(text = "ATUR →", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = colors.secondary)
    }
    Hairline()
}

@Composable
fun DottedLeader(color: Color, modifier: Modifier = Modifier) {
    val dash = with(LocalDensity.current) { floatArrayOf(1.6.dp.toPx(), 5.dp.toPx()) }
    Box(
        modifier = modifier.height(10.dp).drawBehind {
            drawLine(
                color = color,
                start = Offset(0f, size.height / 2f),
                end = Offset(size.width, size.height / 2f),
                strokeWidth = 1.dp.toPx() * 0.8f,
                pathEffect = PathEffect.dashPathEffect(dash),
            )
        },
    )
}

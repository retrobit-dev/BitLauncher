package dev.retrobit.bitlauncher.ui.settings

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.retrobit.bitlauncher.LauncherViewModel
import dev.retrobit.bitlauncher.data.AppLayout
import dev.retrobit.bitlauncher.data.automaticArticleApps
import dev.retrobit.bitlauncher.data.resolveArticleApps
import dev.retrobit.bitlauncher.data.GestureAction
import dev.retrobit.bitlauncher.data.IconPackInfo
import dev.retrobit.bitlauncher.data.IconPackRepository
import dev.retrobit.bitlauncher.data.IconPackSelection
import dev.retrobit.bitlauncher.data.LauncherFont
import dev.retrobit.bitlauncher.data.LauncherStyle
import dev.retrobit.bitlauncher.data.SettingsRepository
import dev.retrobit.bitlauncher.data.ThemeMode
import dev.retrobit.bitlauncher.ui.components.GestureActionSheet
import dev.retrobit.bitlauncher.ui.components.HiddenAppsSheet
import dev.retrobit.bitlauncher.ui.components.IconPackSheet
import dev.retrobit.bitlauncher.ui.components.NewsAppsSheet
import dev.retrobit.bitlauncher.ui.components.NewsArticleEditorSheet
import dev.retrobit.bitlauncher.ui.components.NotesAppSheet
import dev.retrobit.bitlauncher.ui.components.label
import dev.retrobit.bitlauncher.util.SystemIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class GestureSlot(val title: String) {
    DOUBLE_TAP("Ketuk dua kali"),
    LONG_PRESS("Tekan lama"),
    SWIPE_LEFT("Geser ke kiri"),
    SWIPE_RIGHT("Geser ke kanan"),
    SWIPE_UP("Geser ke atas"),
    TWO_FINGER_UP("Geser dua jari ke atas"),
    TWO_FINGER_DOWN("Geser dua jari ke bawah"),
}

@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val notificationAccess by viewModel.notificationAccess.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showHiddenApps by remember { mutableStateOf(false) }
    var showNotesPicker by remember { mutableStateOf(false) }
    var showNewsAppsPicker by remember { mutableStateOf(false) }
    var showNewsArticleEditor by remember { mutableStateOf(false) }
    var showIconPackPicker by remember { mutableStateOf(false) }
    var iconPackRefresh by remember { mutableStateOf(0) }
    var gestureSlot by remember { mutableStateOf<GestureSlot?>(null) }
    var isDefaultLauncher by remember { mutableStateOf(SystemIntents.isDefaultLauncher(context)) }
    var gestureServiceEnabled by remember {
        mutableStateOf(SystemIntents.isGestureServiceEnabled(context))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultLauncher = SystemIntents.isDefaultLauncher(context)
        gestureServiceEnabled = SystemIntents.isGestureServiceEnabled(context)
        iconPackRefresh += 1
    }

    val discoveredIconPacks by produceState<List<IconPackInfo>?>(
        initialValue = null,
        allApps,
        iconPackRefresh,
    ) {
        value = withContext(Dispatchers.IO) {
            IconPackRepository.get(context).availableIconPacks()
        }
    }
    val iconPacks = discoveredIconPacks ?: IconPackRepository.builtInIconPacks()
    val automaticNewsApps = remember(apps, favorites) { automaticArticleApps(apps, favorites) }
    val articleApps = remember(apps, allApps, favorites, settings.newsApps) {
        resolveArticleApps(apps, allApps, favorites, settings.newsApps)
    }
    LaunchedEffect(discoveredIconPacks, settings.iconPack) {
        val packs = discoveredIconPacks
        if (
            packs != null &&
            IconPackSelection.isExternal(settings.iconPack) &&
            packs.none { it.id == settings.iconPack }
        ) {
            viewModel.updateSettings { it.copy(iconPack = IconPackSelection.THEME) }
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val success = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(viewModel.exportBackup().toByteArray())
                        } ?: error("Tidak bisa membuka file")
                    }.isSuccess
                }
                Toast.makeText(
                    context,
                    if (success) "Backup tersimpan" else "Gagal menyimpan backup",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val raw = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    }.getOrNull()
                }
                val success = raw != null && viewModel.importBackup(raw)
                Toast.makeText(
                    context,
                    if (success) "Pengaturan dipulihkan" else "File backup tidak valid",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val imageLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val previousUri = settings.newsImageUri
            if (previousUri.isNotBlank() && previousUri != uri.toString()) {
                runCatching {
                    context.contentResolver.releasePersistableUriPermission(
                        Uri.parse(previousUri),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                }
            }
            viewModel.updateSettings { it.copy(newsImageUri = uri.toString()) }
        }
    }

    val defaultLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { isDefaultLauncher = SystemIntents.isDefaultLauncher(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Tutup pengaturan",
                )
            }
            Spacer(Modifier.width(4.dp))
            Text(
                text = "Pengaturan",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium),
            )
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item {
                SettingsCard("Tampilan") {
                    SettingsSubtitle("Gaya")
                    ChoiceRow(
                        options = listOf(
                            LauncherStyle.FOLIO to "Folio koran",
                            LauncherStyle.MINIMAL to "Minimalis",
                        ),
                        selected = settings.style,
                        onSelect = { viewModel.setStyle(it) },
                    )
                    SettingsSubtitle(
                        if (settings.style == LauncherStyle.FOLIO) {
                            "Halaman depan bergaya koran di atas kertas Kaleido: masthead, artikel, dan kartu editorial."
                        } else {
                            "Beranda vertikal minimalis di atas wallpaper, gaya Niagara + Minima."
                        },
                    )
                    SettingsSubtitle("Jenis font")
                    ChoiceRow(
                        options = listOf(
                            LauncherFont.STYLE_DEFAULT to "Bawaan",
                            LauncherFont.LORA to "Lora",
                            LauncherFont.ROBOTO to "Roboto",
                            LauncherFont.TYPEWRITER to "Typewriter",
                            LauncherFont.PIXEL to "Pixel",
                            LauncherFont.OPERATOR to "Operator",
                        ),
                        selected = settings.fontChoice,
                        onSelect = { choice ->
                            viewModel.updateSettings { it.copy(fontChoice = choice) }
                        },
                    )
                    SettingsSubtitle(
                        when (settings.fontChoice) {
                            LauncherFont.STYLE_DEFAULT -> "Mengikuti gaya Folio atau Minimal."
                            LauncherFont.LORA -> "Serif editorial Lora untuk seluruh tampilan."
                            LauncherFont.ROBOTO -> "Sans Roboto untuk seluruh tampilan."
                            LauncherFont.TYPEWRITER -> "Courier Prime, gaya mesin ketik untuk seluruh tampilan."
                            LauncherFont.PIXEL -> "Silkscreen, gaya bitmap/pixel untuk seluruh tampilan."
                            LauncherFont.OPERATOR -> "Space Mono, alternatif open-source bergaya Operator/terminal."
                        },
                    )
                    ActionRow(
                        title = "Paket ikon",
                        value = iconPacks.firstOrNull { it.id == settings.iconPack }?.label ?: "Tidak ditemukan",
                        description = "Ikon tema bawaan atau icon pack pihak ketiga yang terpasang",
                        onClick = { showIconPackPicker = true },
                    )
                    SettingsSubtitle("Tema")
                    ChoiceRow(
                        options = listOf(
                            ThemeMode.KALEIDO to "Kaleido",
                            ThemeMode.KALEIDO_NIGHT to "Kaleido malam",
                            ThemeMode.SYSTEM to "Sistem",
                            ThemeMode.LIGHT to "Terang",
                            ThemeMode.DARK to "Gelap",
                            ThemeMode.AMOLED to "AMOLED",
                        ),
                        selected = settings.themeMode,
                        onSelect = { viewModel.setTheme(it) },
                    )
                    Spacer(Modifier.padding(top = 4.dp))
                    SwitchRow(
                        title = "Warna dinamis",
                        description = "Ikuti warna wallpaper (Android 12+)",
                        checked = settings.dynamicColor,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(dynamicColor = enabled) }
                        },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    SwitchRow(
                        title = "Jam besar",
                        checked = settings.showClock,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showClock = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Tampilkan tanggal",
                        checked = settings.showDate,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showDate = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Format 24 jam",
                        checked = settings.use24Hour,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(use24Hour = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Baris ringkas",
                        description = "Ikon dan teks lebih kecil di beranda",
                        checked = settings.compactRows,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(compactRows = enabled) }
                        },
                    )
                }
            }

            item {
                SettingsCard("Laci aplikasi") {
                    SettingsSubtitle("Tata letak")
                    ChoiceRow(
                        options = listOf(
                            AppLayout.LIST to "Daftar",
                            AppLayout.GRID to "Kisi",
                        ),
                        selected = settings.layout,
                        onSelect = { viewModel.setLayout(it) },
                    )
                    if (settings.layout == AppLayout.GRID) {
                        SettingsSubtitle("Jumlah kolom")
                        ChoiceRow(
                            options = (SettingsRepository.MIN_COLUMNS..SettingsRepository.MAX_COLUMNS)
                                .map { it to it.toString() },
                            selected = settings.gridColumns,
                            onSelect = { viewModel.setColumns(it) },
                        )
                    }
                    Spacer(Modifier.padding(top = 4.dp))
                    SwitchRow(
                        title = "Rel alfabet",
                        description = "Lompat cepat antar huruf",
                        checked = settings.showAlphabetRail,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showAlphabetRail = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Fokus pencarian otomatis",
                        description = "Keyboard langsung muncul saat laci dibuka",
                        checked = settings.autoFocusSearch,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(autoFocusSearch = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Getaran",
                        checked = settings.haptics,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(haptics = enabled) }
                        },
                    )
                }
            }

            item {
                SettingsCard("Gestur beranda") {
                    ActionRow(
                        title = "Ketuk dua kali",
                        value = settings.doubleTapAction.label(),
                        onClick = { gestureSlot = GestureSlot.DOUBLE_TAP },
                    )
                    ActionRow(
                        title = "Tekan lama",
                        value = settings.longPressAction.label(),
                        onClick = { gestureSlot = GestureSlot.LONG_PRESS },
                    )
                    ActionRow(
                        title = "Geser ke kiri",
                        value = settings.swipeLeftAction.label(),
                        onClick = { gestureSlot = GestureSlot.SWIPE_LEFT },
                    )
                    ActionRow(
                        title = "Geser ke kanan",
                        value = settings.swipeRightAction.label(),
                        onClick = { gestureSlot = GestureSlot.SWIPE_RIGHT },
                    )
                    ActionRow(
                        title = "Geser ke atas",
                        value = settings.swipeUpAction.label(),
                        onClick = { gestureSlot = GestureSlot.SWIPE_UP },
                    )
                    ActionRow(
                        title = "Geser dua jari ke atas",
                        value = settings.twoFingerSwipeUpAction.label(),
                        onClick = { gestureSlot = GestureSlot.TWO_FINGER_UP },
                    )
                    ActionRow(
                        title = "Geser dua jari ke bawah",
                        value = settings.twoFingerSwipeDownAction.label(),
                        onClick = { gestureSlot = GestureSlot.TWO_FINGER_DOWN },
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ActionRow(
                        title = "Gestur sistem",
                        value = if (gestureServiceEnabled) "Aktif" else "Aktifkan",
                        description = "Untuk kunci layar, panel, recents, pengaturan cepat, dan screenshot; konten layar tidak dibaca",
                        onClick = { SystemIntents.openGestureSettings(context) },
                    )
                    SettingsSubtitle("Setiap gestur dapat dimatikan dengan memilih Tidak ada.")
                }
            }

            item {
                SettingsCard("Halaman berita") {
                    OutlinedTextField(
                        value = settings.newsLocation,
                        onValueChange = { value ->
                            viewModel.updateSettings { it.copy(newsLocation = value.take(40)) }
                        },
                        label = { Text("Kota dateline") },
                        placeholder = { Text("Rembang") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    SwitchRow(
                        title = "Artikel berita",
                        checked = settings.showNewsArticle,
                        description = "Tampilkan artikel otomatis atau manual di halaman depan",
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showNewsArticle = enabled) }
                        },
                    )
                    ActionRow(
                        title = "Edit artikel manual",
                        value = when {
                            settings.newsManualEnabled -> "Manual aktif"
                            settings.newsManualHeadline.isNotBlank() || settings.newsManualBody.isNotBlank() -> "Draf tersimpan"
                            else -> "Otomatis"
                        },
                        description = "Tulis judul dan isi sendiri tanpa ditimpa notifikasi",
                        onClick = { showNewsArticleEditor = true },
                    )
                    ActionRow(
                        title = "Aplikasi di artikel",
                        value = if (settings.newsApps == null) {
                            "Otomatis · ${articleApps.size}"
                        } else {
                            "${articleApps.size} dipilih"
                        },
                        description = "Hanya aplikasi ini yang menjadi shortcut dan sumber notifikasi artikel",
                        onClick = { showNewsAppsPicker = true },
                    )
                    SwitchRow(
                        title = "Daftar aplikasi di artikel",
                        checked = settings.showNewsAppShelf,
                        description = "Tampilkan atau sembunyikan chip daftar aplikasi tanpa mengubah artikel dan filter notifikasi",
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showNewsAppShelf = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Shortcut aplikasi di artikel",
                        checked = settings.showNewsAppLinks,
                        description = "Nama aplikasi dan hyperlink Manual dapat diketuk untuk membukanya",
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showNewsAppLinks = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Ringkasan notifikasi di artikel",
                        checked = settings.showNewsUnread,
                        description = "Rangkai jumlah notifikasi aktif menjadi berita",
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showNewsUnread = enabled) }
                        },
                    )
                    SwitchRow(
                        title = "Foto utama",
                        checked = settings.showNewsPhoto,
                        description = "Tampilkan foto monokrom di atas artikel",
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showNewsPhoto = enabled) }
                        },
                    )
                    ActionRow(
                        title = "Gambar artikel",
                        value = if (settings.newsImageUri.isBlank()) "Bawaan" else "Kustom",
                        description = "Pilih gambar dari galeri; izin baca disimpan untuk pemakaian berikutnya",
                        onClick = { imageLauncher.launch(arrayOf("image/*")) },
                    )
                    if (settings.newsImageUri.isNotBlank()) {
                        ActionRow(
                            title = "Gunakan gambar bawaan",
                            description = "Hapus gambar kustom dan pulihkan ilustrasi koran",
                            onClick = {
                                runCatching {
                                    context.contentResolver.releasePersistableUriPermission(
                                        Uri.parse(settings.newsImageUri),
                                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                                    )
                                }
                                viewModel.updateSettings { it.copy(newsImageUri = "") }
                            },
                        )
                    }
                    SwitchRow(
                        title = "Widget kalender & catatan",
                        checked = settings.showNewsWidgets,
                        description = "Tampilkan dua kartu interaktif setelah artikel",
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showNewsWidgets = enabled) }
                        },
                    )
                    ActionRow(
                        title = "Aplikasi catatan",
                        value = allApps.firstOrNull { it.packageName == settings.notesPackage }?.label
                            ?: "Otomatis",
                        description = "Dipakai oleh tautan dan widget Catatan",
                        onClick = { showNotesPicker = true },
                    )
                    ActionRow(
                        title = "Akses notifikasi",
                        value = if (notificationAccess) "Aktif" else "Belum aktif",
                        description = "Buka pengaturan khusus Android untuk membaca jumlah notifikasi aktif",
                        onClick = { SystemIntents.openNotificationAccess(context) },
                    )
                    SettingsSubtitle(
                        "Artikel hanya memakai jumlah dan nama aplikasi. Isi pesan tidak dibaca " +
                            "atau disimpan. Jumlah pesan adalah estimasi dari notifikasi aktif.",
                    )
                }
            }

            item {
                SettingsCard("Aplikasi") {
                    SwitchRow(
                        title = "Badge notifikasi",
                        checked = settings.showNotificationBadges,
                        description = "Jumlah notifikasi aktif pada ikon beranda dan laci",
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { it.copy(showNotificationBadges = enabled) }
                        },
                    )
                    ActionRow(
                        title = "Aplikasi tersembunyi",
                        value = if (settings.hidden.isEmpty()) "Tidak ada" else "${settings.hidden.size}",
                        onClick = { showHiddenApps = true },
                    )
                    SettingsSubtitle(
                        "Tekan lama aplikasi di laci (atau beranda gaya minimal) untuk pin, " +
                            "membuka shortcut, menyembunyikan, atau melihat info aplikasi.",
                    )
                }
            }

            item {
                SettingsCard("Sistem") {
                    ActionRow(
                        title = "Launcher default",
                        value = if (isDefaultLauncher) "Aktif" else "Belum aktif",
                        onClick = {
                            runCatching {
                                defaultLauncher.launch(SystemIntents.defaultLauncherIntent(context))
                            }
                        },
                    )
                    ActionRow(
                        title = "Wallpaper",
                        description = "Buka pemilih wallpaper sistem",
                        onClick = { SystemIntents.openWallpaperPicker(context) },
                    )
                    ActionRow(
                        title = "Muat ulang aplikasi",
                        description = "Sinkronkan daftar aplikasi terpasang",
                        onClick = { viewModel.refresh() },
                    )
                }
            }

            item {
                SettingsCard("Cadangan") {
                    ActionRow(
                        title = "Ekspor pengaturan",
                        description = "Simpan tema, favorit, dan aplikasi tersembunyi ke file JSON",
                        onClick = { exportLauncher.launch("bitlauncher-backup.json") },
                    )
                    ActionRow(
                        title = "Impor pengaturan",
                        description = "Pulihkan dari file backup BitLauncher",
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) },
                    )
                }
            }

            item {
                SettingsCard("Tentang") {
                    Box(Modifier.padding(vertical = 8.dp)) {
                        Column {
                            Text(
                                text = "BitLauncher 0.5.0",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(
                                text = "Gaya folio koran di atas kertas Kaleido, atau beranda minimalis " +
                                    "ala Niagara + Minima. Dibangun dengan Jetpack Compose.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    gestureSlot?.let { slot ->
        val selected = when (slot) {
            GestureSlot.DOUBLE_TAP -> settings.doubleTapAction
            GestureSlot.LONG_PRESS -> settings.longPressAction
            GestureSlot.SWIPE_LEFT -> settings.swipeLeftAction
            GestureSlot.SWIPE_RIGHT -> settings.swipeRightAction
            GestureSlot.SWIPE_UP -> settings.swipeUpAction
            GestureSlot.TWO_FINGER_UP -> settings.twoFingerSwipeUpAction
            GestureSlot.TWO_FINGER_DOWN -> settings.twoFingerSwipeDownAction
        }
        GestureActionSheet(
            title = slot.title,
            selectedAction = selected,
            onSelect = { action: GestureAction ->
                viewModel.updateSettings { current ->
                    when (slot) {
                        GestureSlot.DOUBLE_TAP -> current.copy(doubleTapAction = action)
                        GestureSlot.LONG_PRESS -> current.copy(longPressAction = action)
                        GestureSlot.SWIPE_LEFT -> current.copy(swipeLeftAction = action)
                        GestureSlot.SWIPE_RIGHT -> current.copy(swipeRightAction = action)
                        GestureSlot.SWIPE_UP -> current.copy(swipeUpAction = action)
                        GestureSlot.TWO_FINGER_UP -> current.copy(twoFingerSwipeUpAction = action)
                        GestureSlot.TWO_FINGER_DOWN -> current.copy(twoFingerSwipeDownAction = action)
                    }
                }
                gestureSlot = null
            },
            onDismiss = { gestureSlot = null },
        )
    }

    if (showNewsArticleEditor) {
        NewsArticleEditorSheet(
            initialHeadline = settings.newsManualHeadline,
            initialBody = settings.newsManualBody,
            apps = allApps,
            manualEnabled = settings.newsManualEnabled,
            onSave = { manual, headline, body ->
                viewModel.updateSettings {
                    it.copy(
                        showNewsArticle = if (manual) true else it.showNewsArticle,
                        newsManualEnabled = manual,
                        newsManualHeadline = headline,
                        newsManualBody = body,
                    )
                }
                showNewsArticleEditor = false
            },
            onDismiss = { showNewsArticleEditor = false },
        )
    }

    if (showNewsAppsPicker) {
        NewsAppsSheet(
            apps = allApps,
            selectedKeys = settings.newsApps,
            automaticKeys = automaticNewsApps.map { it.key },
            onSelectionChange = { selection ->
                viewModel.updateSettings { it.copy(newsApps = selection) }
            },
            onDismiss = { showNewsAppsPicker = false },
        )
    }

    if (showIconPackPicker) {
        IconPackSheet(
            packs = iconPacks,
            selectedPack = settings.iconPack,
            onSelect = { pack ->
                viewModel.updateSettings { it.copy(iconPack = pack) }
                showIconPackPicker = false
            },
            onDismiss = { showIconPackPicker = false },
        )
    }

    if (showNotesPicker) {
        NotesAppSheet(
            apps = allApps,
            selectedPackage = settings.notesPackage,
            onSelect = { packageName ->
                viewModel.updateSettings { it.copy(notesPackage = packageName) }
                showNotesPicker = false
            },
            onDismiss = { showNotesPicker = false },
        )
    }

    if (showHiddenApps) {
        HiddenAppsSheet(
            allApps = allApps,
            settings = settings,
            onToggle = { app, hidden -> viewModel.setHidden(app, hidden) },
            onDismiss = { showHiddenApps = false },
        )
    }
}

package dev.retrobit.bitlauncher.ui.settings

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import dev.retrobit.bitlauncher.data.LauncherStyle
import dev.retrobit.bitlauncher.data.SettingsRepository
import dev.retrobit.bitlauncher.data.ThemeMode
import dev.retrobit.bitlauncher.ui.components.HiddenAppsSheet
import dev.retrobit.bitlauncher.util.SystemIntents
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    viewModel: LauncherViewModel,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val allApps by viewModel.allApps.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    var showHiddenApps by remember { mutableStateOf(false) }
    var isDefaultLauncher by remember { mutableStateOf(SystemIntents.isDefaultLauncher(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        isDefaultLauncher = SystemIntents.isDefaultLauncher(context)
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
                            "Halaman depan bergaya koran di atas kertas Kaleido: masthead, garis ganda, dan indeks bertitik."
                        } else {
                            "Beranda vertikal minimalis di atas wallpaper, gaya Niagara + Minima."
                        },
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
                SettingsCard("Aplikasi") {
                    ActionRow(
                        title = "Aplikasi tersembunyi",
                        value = if (settings.hidden.isEmpty()) "Tidak ada" else "${settings.hidden.size}",
                        onClick = { showHiddenApps = true },
                    )
                    SettingsSubtitle(
                        "Tekan lama aplikasi di beranda atau laci untuk pin ke beranda, " +
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
                                text = "BitLauncher 0.1.0",
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

    if (showHiddenApps) {
        HiddenAppsSheet(
            allApps = allApps,
            settings = settings,
            onToggle = { app, hidden -> viewModel.setHidden(app, hidden) },
            onDismiss = { showHiddenApps = false },
        )
    }
}

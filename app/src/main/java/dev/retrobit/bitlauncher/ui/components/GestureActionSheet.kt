package dev.retrobit.bitlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.retrobit.bitlauncher.data.GestureAction

fun GestureAction.label(): String = when (this) {
    GestureAction.NONE -> "Tidak ada"
    GestureAction.OPEN_DRAWER -> "Buka arsip aplikasi"
    GestureAction.OPEN_SEARCH -> "Cari aplikasi"
    GestureAction.OPEN_SETTINGS -> "Buka pengaturan"
    GestureAction.OPEN_HOME_MENU -> "Menu launcher"
    GestureAction.OPEN_NOTIFICATIONS -> "Panel notifikasi"
    GestureAction.OPEN_QUICK_SETTINGS -> "Pengaturan cepat"
    GestureAction.OPEN_RECENTS -> "Aplikasi terbaru"
    GestureAction.LOCK_SCREEN -> "Kunci layar"
    GestureAction.TAKE_SCREENSHOT -> "Ambil screenshot"
    GestureAction.OPEN_CALENDAR -> "Buka kalender"
    GestureAction.OPEN_NOTES -> "Buka catatan"
}

fun GestureAction.needsGestureService(): Boolean = this in setOf(
    GestureAction.OPEN_NOTIFICATIONS,
    GestureAction.OPEN_QUICK_SETTINGS,
    GestureAction.OPEN_RECENTS,
    GestureAction.LOCK_SCREEN,
    GestureAction.TAKE_SCREENSHOT,
)

/** Reusable action picker for every single- and two-finger home gesture. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureActionSheet(
    title: String,
    selectedAction: GestureAction,
    onSelect: (GestureAction) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.82f)
                .padding(horizontal = 20.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Pilih Tidak ada untuk mematikan gestur. Aksi sistem memerlukan BitLauncher — gestur sistem di Aksesibilitas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                items(GestureAction.values().toList(), key = { it.name }) { action ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(action) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(text = action.label(), style = MaterialTheme.typography.titleMedium)
                            if (action.needsGestureService()) {
                                Text(
                                    text = "Aksi sistem via Aksesibilitas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        if (action == selectedAction) {
                            Icon(Icons.Outlined.Check, contentDescription = "Dipilih")
                        }
                    }
                }
                item { Spacer(Modifier.padding(bottom = 24.dp)) }
            }
        }
    }
}

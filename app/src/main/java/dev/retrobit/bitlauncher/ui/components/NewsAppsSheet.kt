package dev.retrobit.bitlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.retrobit.bitlauncher.data.LaunchableApp

/** Multi-select editor for the apps allowed to appear in and contribute notifications to the article. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsAppsSheet(
    apps: List<LaunchableApp>,
    selectedKeys: List<String>?,
    automaticKeys: List<String>,
    onSelectionChange: (List<String>?) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val choices = apps.distinctBy { it.packageName }.sortedBy { it.sortKey }
    val filtered = choices.filter { app ->
        query.isBlank() || app.label.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
    }
    val effectiveSelected = selectedKeys ?: automaticKeys

    fun toggle(app: LaunchableApp) {
        val validKeys = choices.map { it.key }.toSet()
        val updated = effectiveSelected.filter { it in validKeys }.toMutableList()
        if (app.key in updated) updated.removeAll { it == app.key } else updated += app.key
        onSelectionChange(updated)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.86f)
                .padding(horizontal = 20.dp),
        ) {
            Text(text = "Aplikasi di artikel", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Hanya aplikasi terpilih yang menjadi shortcut dan sumber notifikasi artikel. Pilih kosong untuk mematikan keduanya.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Cari aplikasi") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectionChange(null) }
                            .padding(vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(text = "Otomatis", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "Favorit atau tiga aplikasi pertama, seperti perilaku sebelumnya",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (selectedKeys == null) Icon(Icons.Outlined.Check, contentDescription = "Dipilih")
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                if (filtered.isEmpty()) {
                    item {
                        Text(
                            text = "Tidak ada aplikasi yang cocok",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 18.dp),
                        )
                    }
                }
                items(filtered, key = { it.key }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { toggle(app) }
                            .padding(vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app = app, size = 32.dp)
                        Spacer(Modifier.width(13.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Checkbox(
                            checked = app.key in effectiveSelected,
                            onCheckedChange = null,
                        )
                    }
                }
                item { Spacer(Modifier.padding(bottom = 24.dp)) }
            }
        }
    }
}

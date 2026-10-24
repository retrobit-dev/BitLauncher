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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.retrobit.bitlauncher.data.LaunchableApp

/** Chooses which installed app the article's "Catatan" link and widget should open. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesAppSheet(
    apps: List<LaunchableApp>,
    selectedPackage: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val choices = apps.distinctBy { it.packageName }.sortedBy { it.sortKey }

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
            Text(text = "Aplikasi catatan", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Dipakai oleh tautan dan widget Catatan di artikel. Pilih Otomatis untuk deteksi aplikasi catatan yang umum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f)) {
                item {
                    ChoiceRow(
                        label = "Otomatis",
                        selected = selectedPackage.isBlank(),
                        onClick = { onSelect("") },
                    )
                }
                items(choices, key = { it.key }) { app ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(app.packageName) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        AppIcon(app = app, size = 32.dp)
                        Spacer(Modifier.width(14.dp))
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
                        if (app.packageName == selectedPackage) {
                            Icon(Icons.Outlined.Check, contentDescription = "Dipilih")
                        }
                    }
                }
                item { Spacer(Modifier.padding(bottom = 24.dp)) }
            }
        }
    }
}

@Composable
private fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Outlined.Check, contentDescription = "Dipilih")
    }
}

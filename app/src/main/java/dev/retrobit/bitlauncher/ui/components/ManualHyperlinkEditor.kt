package dev.retrobit.bitlauncher.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.data.SettingsRepository
import dev.retrobit.bitlauncher.data.insertManualArticleLink
import dev.retrobit.bitlauncher.data.parseManualArticleLinks
import dev.retrobit.bitlauncher.data.removeManualArticleLink
import dev.retrobit.bitlauncher.data.replaceManualArticleLink

/** Controls to insert, retarget, rename, or remove app hyperlinks in a manual article draft. */
@Composable
fun ManualHyperlinkEditor(
    apps: List<LaunchableApp>,
    body: String,
    onBodyChange: (String) -> Unit,
) {
    val sortedApps = remember(apps) { apps.distinctBy { it.key }.sortedBy { it.sortKey } }
    var label by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedKey by rememberSaveable { mutableStateOf("") }
    var editingStart by rememberSaveable { mutableStateOf<Int?>(null) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val links = parseManualArticleLinks(body)
    val editingLink = links.firstOrNull { it.start == editingStart }
    val selectedApp = sortedApps.firstOrNull { it.key == selectedKey } ?: sortedApps.firstOrNull()
    val filteredApps = sortedApps.filter { app ->
        query.isBlank() || app.label.contains(query, ignoreCase = true) ||
            app.packageName.contains(query, ignoreCase = true)
    }.take(3)
    val visibleApps = selectedApp?.let { selected ->
        if (query.isBlank() && filteredApps.none { it.key == selected.key }) {
            listOf(selected) + filteredApps.take(2)
        } else {
            filteredApps
        }
    } ?: filteredApps

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
        Text(text = "Hyperlink aplikasi", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Pilih aplikasi dan teks tautan. Teks boleh berbeda dari nama aplikasi; pada artikel hanya teks tautan yang terlihat.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 3.dp, bottom = 8.dp),
        )

        if (links.isEmpty()) {
            Text(
                text = "Belum ada hyperlink manual.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            links.forEach { link ->
                val target = sortedApps.firstOrNull { it.key == link.appKey }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = link.label,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = target?.label ?: "Aplikasi tidak ditemukan",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    TextButton(onClick = {
                        editingStart = link.start
                        label = link.label
                        selectedKey = link.appKey
                        error = null
                    }) {
                        Text("Edit")
                    }
                    TextButton(onClick = {
                        onBodyChange(removeManualArticleLink(body, link))
                        if (editingStart == link.start) {
                            editingStart = null
                            label = ""
                        }
                    }) {
                        Text("Hapus")
                    }
                }
            }
        }

        OutlinedTextField(
            value = label,
            onValueChange = { label = it.replace('\n', ' ').take(120) },
            label = { Text("Teks hyperlink") },
            placeholder = { Text("Contoh: obrolan keluarga") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        )
        OutlinedTextField(
            value = query,
            onValueChange = { query = it.take(80) },
            label = { Text("Cari aplikasi tujuan") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )
        visibleApps.forEach { app ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedKey = app.key
                        if (label.isBlank()) label = app.label
                        error = null
                    }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, size = 28.dp)
                Spacer(Modifier.width(11.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
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
                if (selectedApp?.key == app.key) {
                    Icon(Icons.Outlined.Check, contentDescription = "Dipilih")
                }
            }
        }
        if (visibleApps.isEmpty()) {
            Text(
                text = "Tidak ada aplikasi yang cocok",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
        error?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Button(
            onClick = {
                val cleanLabel = label.trim()
                val app = selectedApp
                when {
                    cleanLabel.isEmpty() || cleanLabel.contains(']') ->
                        error = "Teks hyperlink wajib diisi dan tidak boleh mengandung ]"
                    app == null -> error = "Pilih aplikasi tujuan"
                    else -> {
                        val updated = editingLink?.let { link ->
                            replaceManualArticleLink(body, link, cleanLabel, app.key)
                        } ?: insertManualArticleLink(body, cleanLabel, app.key)
                        if (updated.length > SettingsRepository.MAX_MANUAL_BODY) {
                            error = "Artikel terlalu panjang untuk menambahkan hyperlink"
                        } else {
                            onBodyChange(updated)
                            label = ""
                            editingStart = null
                            error = null
                        }
                    }
                }
            },
            enabled = selectedApp != null,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        ) {
            Text(if (editingLink != null) "Simpan perubahan hyperlink" else "Sisipkan hyperlink")
        }
        if (editingStart != null) {
            TextButton(
                onClick = {
                    editingStart = null
                    label = ""
                    error = null
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Batal edit hyperlink")
            }
        }
        Text(
            text = "Setelah menyusun hyperlink, simpan artikel manual di bawah.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
    }
}

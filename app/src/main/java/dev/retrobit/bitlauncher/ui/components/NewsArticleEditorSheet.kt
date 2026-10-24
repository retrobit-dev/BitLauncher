package dev.retrobit.bitlauncher.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.data.SettingsRepository

/** Editor for a user-written headline and article body, with an automatic-mode escape hatch. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsArticleEditorSheet(
    initialHeadline: String,
    initialBody: String,
    apps: List<LaunchableApp>,
    manualEnabled: Boolean,
    onSave: (manualEnabled: Boolean, headline: String, body: String) -> Unit,
    onDismiss: () -> Unit,
) {
    var headline by rememberSaveable { mutableStateOf(initialHeadline) }
    var body by rememberSaveable { mutableStateOf(initialBody) }
    var attempted by rememberSaveable { mutableStateOf(false) }
    val headlineBlank = headline.isBlank()
    val bodyBlank = body.isBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.92f)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp),
        ) {
            Text(text = "Edit artikel manual", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Tulis judul, isi, dan hyperlink aplikasi sendiri. Mode manual tidak ditimpa notifikasi; lokasi, tanggal, Catatan, dan badge tetap dipertahankan, sedangkan chip aplikasi mengikuti pengaturan.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
            Text(
                text = if (manualEnabled) "Saat ini: artikel manual" else "Saat ini: artikel otomatis",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 12.dp),
            )
            OutlinedTextField(
                value = headline,
                onValueChange = { headline = it.take(SettingsRepository.MAX_MANUAL_HEADLINE) },
                label = { Text("Judul artikel") },
                placeholder = { Text("Tulis headline utama") },
                singleLine = true,
                isError = attempted && headlineBlank,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (attempted && headlineBlank) {
                    "Judul wajib diisi untuk mode manual"
                } else {
                    "${headline.length}/${SettingsRepository.MAX_MANUAL_HEADLINE}"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (attempted && headlineBlank) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 10.dp),
            )
            OutlinedTextField(
                value = body,
                onValueChange = { body = it.take(SettingsRepository.MAX_MANUAL_BODY) },
                label = { Text("Isi artikel") },
                placeholder = { Text("Tulis kabar Anda. Gunakan kontrol Hyperlink aplikasi di bawah.") },
                minLines = 8,
                maxLines = 14,
                isError = attempted && bodyBlank,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (attempted && bodyBlank) {
                    "Isi wajib diisi untuk mode manual"
                } else {
                    "${body.length}/${SettingsRepository.MAX_MANUAL_BODY} · Baris baru dipertahankan"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (attempted && bodyBlank) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.padding(bottom = 14.dp),
            )
            ManualHyperlinkEditor(
                apps = apps,
                body = body,
                onBodyChange = { body = it },
            )
            Button(
                onClick = {
                    attempted = true
                    if (!headlineBlank && !bodyBlank) {
                        onSave(true, headline.trim(), body.trim())
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simpan & gunakan artikel manual")
            }
            TextButton(
                onClick = { onSave(false, headline.trim(), body.trim()) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Simpan draf & gunakan otomatis")
            }
            Text(
                text = "Hyperlink Manual membuka aplikasi yang dipilih di editor. Nama aplikasi biasa mengikuti daftar aplikasi artikel; keduanya mengikuti saklar shortcut. Jumlah notifikasi tidak disisipkan ke teks manual.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

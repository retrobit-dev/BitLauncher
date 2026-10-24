package dev.retrobit.bitlauncher.ui.components

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.retrobit.bitlauncher.data.LaunchableApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsSheet(
    app: LaunchableApp,
    isFavorite: Boolean,
    shortcuts: List<ShortcutInfo>,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onShortcut: (ShortcutInfo) -> Unit,
    onToggleFavorite: () -> Unit,
    onHide: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpen)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app = app, size = 44.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = if (app.isWorkProfile) "Profil kerja · Ketuk untuk buka" else "Ketuk untuk buka",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (shortcuts.isNotEmpty()) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(shortcuts, key = { it.id }) { shortcut ->
                        ShortcutChip(shortcut = shortcut, onClick = { onShortcut(shortcut) })
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            SheetAction(
                icon = { tint -> Icon(if (isFavorite) Icons.Outlined.Star else Icons.Outlined.StarBorder, null, tint = tint) },
                label = if (isFavorite) "Lepas dari favorit" else "Pin ke beranda",
                onClick = onToggleFavorite,
            )
            SheetAction(
                icon = { tint -> Icon(Icons.Outlined.VisibilityOff, null, tint = tint) },
                label = "Sembunyikan aplikasi",
                onClick = onHide,
            )
            SheetAction(
                icon = { tint -> Icon(Icons.Outlined.Info, null, tint = tint) },
                label = "Info aplikasi",
                onClick = onAppInfo,
            )
            SheetAction(
                icon = { tint -> Icon(Icons.Outlined.DeleteOutline, null, tint = tint) },
                label = "Copot pemasangan",
                tint = MaterialTheme.colorScheme.error,
                onClick = onUninstall,
            )
        }
    }
}

@Composable
private fun ShortcutChip(
    shortcut: ShortcutInfo,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier
                .width(76.dp)
                .padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ShortcutIcon(shortcut = shortcut, size = 32.dp)
            Spacer(Modifier.height(8.dp))
            Text(
                text = (shortcut.shortLabel ?: shortcut.longLabel ?: "").toString(),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun SheetAction(
    icon: @Composable (androidx.compose.ui.graphics.Color) -> Unit,
    label: String,
    onClick: () -> Unit,
    tint: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(tint)
        Spacer(Modifier.width(20.dp))
        Text(text = label, style = MaterialTheme.typography.titleMedium, color = tint)
    }
}

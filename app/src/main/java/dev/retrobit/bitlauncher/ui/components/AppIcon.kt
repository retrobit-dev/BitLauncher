package dev.retrobit.bitlauncher.ui.components

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dev.retrobit.bitlauncher.data.IconPackRepository
import dev.retrobit.bitlauncher.data.IconPackSelection
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.data.LauncherStyle
import dev.retrobit.bitlauncher.ui.theme.LocalEpaper
import dev.retrobit.bitlauncher.ui.theme.LocalIconPack
import dev.retrobit.bitlauncher.ui.theme.LocalLauncherStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class LoadedIcon(
    val bitmap: ImageBitmap,
    val themed: Boolean = false,
    val fromIconPack: Boolean = false,
)

/** Shared, bounded cache. Selection-specific keys prevent stale icons after a pack change. */
private object IconCache {
    private val cache = LruCache<String, LoadedIcon>(320)

    fun get(key: String): LoadedIcon? = cache.get(key)

    fun put(key: String, icon: LoadedIcon) {
        cache.put(key, icon)
    }

    fun clear() = cache.evictAll()
}

/** Leave only a trace of chroma, similar to photos printed by an e-paper newspaper. */
internal val EpaperColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.275592f, 0.657984f, 0.066424f, 0f, 0f,
            0.195592f, 0.737984f, 0.066424f, 0f, 0f,
            0.195592f, 0.657984f, 0.146424f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

private fun originalAppIcon(app: LaunchableApp, densityDpi: Int, sizePx: Int): LoadedIcon? {
    IconCache.get(app.iconKey)?.let { return it }
    val drawable = runCatching { app.info.getBadgedIcon(densityDpi) }.getOrNull() ?: return null
    return drawable.toLoadedIcon(sizePx)?.also { IconCache.put(app.iconKey, it) }
}

private fun themedAppIcon(app: LaunchableApp, densityDpi: Int, sizePx: Int): LoadedIcon? {
    val key = "theme:${app.iconKey}:$sizePx"
    IconCache.get(key)?.let { return it }
    val drawable = runCatching { app.info.getIcon(densityDpi) }.getOrNull() ?: return null
    return drawable.toThemedIcon(sizePx)?.also { IconCache.put(key, it) }
}

private suspend fun loadAppIcon(
    context: Context,
    app: LaunchableApp,
    densityDpi: Int,
    sizePx: Int,
    selection: String,
): LoadedIcon? = withContext(Dispatchers.IO) {
    if (IconPackSelection.isExternal(selection)) {
        IconPackRepository.get(context).loadIcon(selection, app.component, sizePx)?.let { bitmap ->
            return@withContext LoadedIcon(bitmap = bitmap, fromIconPack = true)
        }
    }
    if (IconPackSelection.isTheme(selection)) {
        themedAppIcon(app, densityDpi, sizePx)?.let { return@withContext it }
    }
    originalAppIcon(app, densityDpi, sizePx)
}

private fun shortcutDrawable(
    context: Context,
    shortcut: ShortcutInfo,
    densityDpi: Int,
    badged: Boolean,
): Drawable? {
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    return runCatching {
        if (badged) {
            launcherApps?.getShortcutBadgedIconDrawable(shortcut, densityDpi)
        } else {
            launcherApps?.getShortcutIconDrawable(shortcut, densityDpi)
        }
    }.getOrNull()
}

private fun originalShortcutIcon(
    context: Context,
    shortcut: ShortcutInfo,
    densityDpi: Int,
    sizePx: Int,
    key: String,
): LoadedIcon? {
    IconCache.get(key)?.let { return it }
    val drawable = shortcutDrawable(context, shortcut, densityDpi, badged = true) ?: return null
    return drawable.toLoadedIcon(sizePx)?.also { IconCache.put(key, it) }
}

private fun themedShortcutIcon(
    context: Context,
    shortcut: ShortcutInfo,
    densityDpi: Int,
    sizePx: Int,
    key: String,
): LoadedIcon? {
    val themedKey = "theme:$key"
    IconCache.get(themedKey)?.let { return it }
    val drawable = shortcutDrawable(context, shortcut, densityDpi, badged = false) ?: return null
    return drawable.toThemedIcon(sizePx)?.also { IconCache.put(themedKey, it) }
}

private suspend fun loadShortcutIcon(
    context: Context,
    shortcut: ShortcutInfo,
    densityDpi: Int,
    sizePx: Int,
    selection: String,
    key: String,
): LoadedIcon? = withContext(Dispatchers.IO) {
    if (IconPackSelection.isExternal(selection)) {
        IconPackRepository.get(context).loadIcon(selection, shortcut.activity, sizePx)?.let { bitmap ->
            return@withContext LoadedIcon(bitmap = bitmap, fromIconPack = true)
        }
    }
    if (IconPackSelection.isTheme(selection)) {
        themedShortcutIcon(context, shortcut, densityDpi, sizePx, key)?.let { return@withContext it }
    }
    originalShortcutIcon(context, shortcut, densityDpi, sizePx, key)
}

private fun Drawable.toLoadedIcon(sizePx: Int): LoadedIcon? = runCatching {
    val side = sizePx.coerceAtLeast(1)
    LoadedIcon(bitmap = toBitmap(side, side).asImageBitmap())
}.getOrNull()

/** Adaptive foreground/monochrome provides an alpha glyph that is safe to tint. */
private fun Drawable.toThemedIcon(sizePx: Int): LoadedIcon? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O || this !is AdaptiveIconDrawable) return null
    val glyph = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        monochrome ?: foreground
    } else {
        foreground
    } ?: return null
    return runCatching {
        val side = sizePx.coerceAtLeast(1)
        LoadedIcon(bitmap = glyph.toBitmap(side, side).asImageBitmap(), themed = true)
    }.getOrNull()
}

@Composable
private fun IconImage(icon: LoadedIcon, size: Dp, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    if (icon.themed) {
        val shape = if (LocalLauncherStyle.current == LauncherStyle.FOLIO) {
            RoundedCornerShape(2.dp)
        } else {
            CircleShape
        }
        Surface(
            modifier = modifier,
            shape = shape,
            color = colors.surfaceVariant,
            border = BorderStroke(0.7.dp, colors.outline),
        ) {
            Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().padding(size * 0.12f),
                contentScale = ContentScale.Fit,
                colorFilter = ColorFilter.tint(colors.primary, BlendMode.SrcIn),
            )
        }
    } else {
        Image(
            bitmap = icon.bitmap,
            contentDescription = null,
            modifier = modifier,
            contentScale = ContentScale.Fit,
            colorFilter = if (!icon.fromIconPack && LocalEpaper.current) EpaperColorFilter else null,
        )
    }
}

@Composable
fun AppIcon(
    app: LaunchableApp,
    size: Dp,
    modifier: Modifier = Modifier,
    badgeCount: Int = 0,
) {
    val context = LocalContext.current
    val selection = LocalIconPack.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val initial = when {
        IconPackSelection.isTheme(selection) -> IconCache.get("theme:${app.iconKey}:$sizePx")
        IconPackSelection.isExternal(selection) -> null
        else -> IconCache.get(app.iconKey)
    }
    val icon by produceState<LoadedIcon?>(initialValue = initial, app.iconKey, sizePx, selection) {
        value = null
        value = loadAppIcon(context, app, densityDpi, sizePx, selection)
    }
    Box(modifier = modifier.size(size)) {
        icon?.let { IconImage(icon = it, size = size, modifier = Modifier.fillMaxSize()) }
        if (badgeCount > 0) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .defaultMinSize(minWidth = 16.dp, minHeight = 16.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.background),
            ) {
                Text(
                    text = if (badgeCount > 99) "99+" else badgeCount.toString(),
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, lineHeight = 10.sp),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 3.dp),
                )
            }
        }
    }
}

@Composable
fun ShortcutIcon(
    shortcut: ShortcutInfo,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val selection = LocalIconPack.current
    val densityDpi = context.resources.displayMetrics.densityDpi
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val key = "shortcut:${shortcut.getPackage()}:${shortcut.id}:$sizePx"
    val icon by produceState<LoadedIcon?>(initialValue = null, key, sizePx, selection) {
        value = null
        value = loadShortcutIcon(context, shortcut, densityDpi, sizePx, selection, key)
    }
    Box(modifier = modifier.size(size)) {
        icon?.let { IconImage(icon = it, size = size, modifier = Modifier.fillMaxSize()) }
    }
}

fun clearIconCache() = IconCache.clear()

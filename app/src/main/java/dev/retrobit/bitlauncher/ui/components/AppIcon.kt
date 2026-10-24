package dev.retrobit.bitlauncher.ui.components

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.drawable.toBitmap
import dev.retrobit.bitlauncher.data.LaunchableApp
import dev.retrobit.bitlauncher.ui.theme.LocalEpaper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Bitmap cache shared by every icon in the process. Launchers redraw icons constantly; decoding
 * a drawable for every recomposition would be fatal for scroll performance.
 */
private object IconCache {
    private val cache = LruCache<String, ImageBitmap>(256)

    fun get(key: String): ImageBitmap? = cache.get(key)

    fun put(key: String, bitmap: ImageBitmap) {
        cache.put(key, bitmap)
    }

    fun clear() = cache.evictAll()
}

/**
 * Leave only a trace of chroma, similar to colour photos printed by an e-paper newspaper.
 * The cached bitmap remains untouched; this filter is applied only while drawing.
 */
private val EpaperColorFilter = ColorFilter.colorMatrix(
    ColorMatrix(
        floatArrayOf(
            0.275592f, 0.657984f, 0.066424f, 0f, 0f,
            0.195592f, 0.737984f, 0.066424f, 0f, 0f,
            0.195592f, 0.657984f, 0.146424f, 0f, 0f,
            0f, 0f, 0f, 1f, 0f,
        ),
    ),
)

private suspend fun loadAppIcon(app: LaunchableApp, sizePx: Int): ImageBitmap? =
    withContext(Dispatchers.IO) {
        IconCache.get(app.iconKey)?.let { return@withContext it }
        val drawable = runCatching { app.info.getBadgedIcon(0) }.getOrNull()
            ?: return@withContext null
        runCatching {
            val side = sizePx.coerceAtLeast(1)
            drawable.toBitmap(side, side).asImageBitmap().also { IconCache.put(app.iconKey, it) }
        }.getOrNull()
    }

private suspend fun loadShortcutIcon(
    context: Context,
    shortcut: ShortcutInfo,
    densityDpi: Int,
    sizePx: Int,
): ImageBitmap? = withContext(Dispatchers.IO) {
    val key = "shortcut:${shortcut.getPackage()}:${shortcut.id}:$sizePx"
    IconCache.get(key)?.let { return@withContext it }
    val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
    val drawable = runCatching {
        launcherApps?.getShortcutBadgedIconDrawable(shortcut, densityDpi)
    }.getOrNull() ?: return@withContext null
    runCatching {
        val side = sizePx.coerceAtLeast(1)
        drawable.toBitmap(side, side).asImageBitmap().also { IconCache.put(key, it) }
    }.getOrNull()
}

@Composable
fun AppIcon(
    app: LaunchableApp,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(initialValue = IconCache.get(app.iconKey), app.iconKey, sizePx) {
        if (value == null) value = loadAppIcon(app, sizePx)
    }
    Box(modifier = modifier.size(size)) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (LocalEpaper.current) EpaperColorFilter else null,
            )
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
    val densityDpi = context.resources.displayMetrics.densityDpi
    val sizePx = with(LocalDensity.current) { size.roundToPx() }
    val key = "shortcut:${shortcut.getPackage()}:${shortcut.id}:$sizePx"
    val bitmap by produceState<ImageBitmap?>(initialValue = IconCache.get(key), key, sizePx) {
        if (value == null) value = loadShortcutIcon(context, shortcut, densityDpi, sizePx)
    }
    Box(modifier = modifier.size(size)) {
        bitmap?.let {
            Image(
                bitmap = it,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
                colorFilter = if (LocalEpaper.current) EpaperColorFilter else null,
            )
        }
    }
}

fun clearIconCache() = IconCache.clear()

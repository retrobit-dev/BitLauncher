package dev.retrobit.bitlauncher.data

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.util.Calendar

/** Built-in choices use a namespace that cannot collide with an installed package name. */
object IconPackSelection {
    const val THEME = "builtin:theme"
    const val SYSTEM = "builtin:system"

    fun isTheme(id: String): Boolean = id == THEME
    fun isExternal(id: String): Boolean = id != THEME && id != SYSTEM && id.isNotBlank()
}

data class IconPackInfo(
    val id: String,
    val label: String,
    val description: String,
    val isThirdParty: Boolean = false,
)

private data class ParsedIconPack(
    val resources: Resources,
    val version: Long,
    val icons: Map<String, String>,
    val calendars: Map<String, String>,
)

/**
 * Discovers and reads Nova/ADW/Apex/GO/Lawnchair-compatible icon packs.
 *
 * Only public PackageManager/Resources APIs are used. Explicit appfilter mappings are preferred;
 * apps without a mapping return null so the caller can retain their original icon.
 */
class IconPackRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val packageManager = appContext.packageManager
    private val parsedPacks = mutableMapOf<String, ParsedIconPack>()
    private val bitmapCache = LruCache<String, ImageBitmap>(384)

    fun availableIconPacks(): List<IconPackInfo> {
        val discovered = mutableMapOf<String, String>()
        ICON_PACK_ACTIONS.forEach { action ->
            val activities = runCatching {
                packageManager.queryIntentActivities(Intent(action), PackageManager.MATCH_ALL)
            }.getOrNull().orEmpty()
            activities.forEach { info ->
                val packageName = info.activityInfo?.packageName ?: return@forEach
                if (packageName == appContext.packageName || packageName in discovered) return@forEach
                val label = runCatching {
                    packageManager.getApplicationInfo(packageName, 0).loadLabel(packageManager).toString()
                }.getOrNull()
                    ?: runCatching { info.loadLabel(packageManager).toString() }.getOrNull()
                    ?: packageName
                discovered[packageName] = label
            }
        }
        val thirdParty = discovered.entries
            .sortedBy { it.value.lowercase() }
            .map { (packageName, label) ->
                IconPackInfo(
                    id = packageName,
                    label = label,
                    description = packageName,
                    isThirdParty = true,
                )
            }
        return builtInIconPacks() + thirdParty
    }

    /** Loads a mapped icon, or null when the app/pack/resource cannot be resolved. */
    fun loadIcon(
        packPackage: String,
        component: ComponentName?,
        sizePx: Int,
    ): ImageBitmap? {
        if (component == null || !IconPackSelection.isExternal(packPackage)) return null
        val pack = pack(packPackage) ?: return null
        val componentKey = component.flattenToString()
        val drawableName = pack.icons[componentKey]
            ?: pack.calendars[componentKey]?.let { prefix ->
                "$prefix${Calendar.getInstance().get(Calendar.DAY_OF_MONTH)}"
            }
            ?: return null
        val side = sizePx.coerceAtLeast(1)
        val cacheKey = "$packPackage@${pack.version}:$componentKey:$drawableName:$side"
        bitmapCache.get(cacheKey)?.let { return it }

        return runCatching {
            val resourceId = drawableResource(pack.resources, packPackage, drawableName)
            if (resourceId == 0) return null
            val drawable = ResourcesCompat.getDrawable(pack.resources, resourceId, null)
                ?: return null
            drawable.toBitmap(side, side).asImageBitmap().also { bitmapCache.put(cacheKey, it) }
        }.getOrNull()
    }

    @Synchronized
    private fun pack(packageName: String): ParsedIconPack? {
        val applicationInfo = runCatching {
            packageManager.getApplicationInfo(packageName, 0)
        }.getOrNull() ?: return null
        val version = runCatching { File(applicationInfo.sourceDir).lastModified() }.getOrDefault(0L)
        parsedPacks[packageName]?.let { cached ->
            if (cached.version == version) return cached
        }
        val parsed = runCatching { parsePack(packageName, version) }.getOrNull()
        if (parsed != null) {
            parsedPacks[packageName] = parsed
        } else {
            parsedPacks.remove(packageName)
        }
        return parsed
    }

    private fun parsePack(packageName: String, version: Long): ParsedIconPack {
        val resources = packageManager.getResourcesForApplication(packageName)
        val icons = linkedMapOf<String, String>()
        val calendars = linkedMapOf<String, String>()

        val xmlId = resources.getIdentifier("appfilter", "xml", packageName)
        val rawId = resources.getIdentifier("appfilter", "raw", packageName)
        when {
            xmlId != 0 -> {
                val parser = resources.getXml(xmlId)
                try {
                    parseAppFilter(parser, icons, calendars)
                } finally {
                    parser.close()
                }
            }
            else -> {
                val input = if (rawId != 0) {
                    runCatching { resources.openRawResource(rawId) }.getOrNull()
                } else {
                    runCatching { resources.assets.open("appfilter.xml") }.getOrNull()
                }
                input?.use { stream ->
                    val parser = XmlPullParserFactory.newInstance().apply {
                        isNamespaceAware = true
                    }.newPullParser()
                    parser.setInput(stream, "UTF-8")
                    parseAppFilter(parser, icons, calendars)
                }
            }
        }
        return ParsedIconPack(resources, version, icons, calendars)
    }

    private fun parseAppFilter(
        parser: XmlPullParser,
        icons: MutableMap<String, String>,
        calendars: MutableMap<String, String>,
    ) {
        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            if (event == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "item" -> {
                        val component = componentKey(
                            parser.getAttributeValue(null, "component"),
                        )
                        val drawable = parser.getAttributeValue(null, "drawable")
                        if (component != null && !drawable.isNullOrBlank()) {
                            icons.putIfAbsent(component, drawable)
                        }
                    }
                    "calendar" -> {
                        val component = componentKey(
                            parser.getAttributeValue(null, "component"),
                        )
                        val prefix = parser.getAttributeValue(null, "prefix")
                            ?: parser.getAttributeValue(null, "drawable")
                        if (component != null && !prefix.isNullOrBlank()) {
                            calendars.putIfAbsent(component, prefix)
                        }
                    }
                }
            }
            event = parser.next()
        }
    }

    private fun componentKey(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val trimmed = raw.trim()
        val flattened = if (trimmed.startsWith("ComponentInfo{") && trimmed.endsWith("}")) {
            trimmed.substring(14, trimmed.lastIndex)
        } else {
            trimmed
        }
        return runCatching {
            ComponentName.unflattenFromString(flattened)?.flattenToString()
        }.getOrNull()
    }

    private fun drawableResource(
        resources: Resources,
        packageName: String,
        rawName: String,
    ): Int {
        val name = rawName.substringAfterLast('/').substringAfterLast(':')
        val drawable = resources.getIdentifier(name, "drawable", packageName)
        return if (drawable != 0) drawable else resources.getIdentifier(name, "mipmap", packageName)
    }

    companion object {
        private val ICON_PACK_ACTIONS = listOf(
            "app.lawnchair.icons.THEMED_ICON",
            "com.novalauncher.THEME",
            "com.teslacoilsw.launcher.THEME",
            "org.adw.launcher.THEMES",
            "org.adw.ActivityStarter.THEMES",
            "com.anddoes.launcher.THEME",
            "com.gau.go.launcherex.theme",
            "com.fede.launcher.THEME_ICONPACK",
            "com.dlto.atom.launcher.THEME",
        )

        @Volatile
        private var instance: IconPackRepository? = null

        fun get(context: Context): IconPackRepository =
            instance ?: synchronized(this) {
                instance ?: IconPackRepository(context).also { instance = it }
            }

        fun builtInIconPacks(): List<IconPackInfo> = listOf(
            IconPackInfo(
                id = IconPackSelection.THEME,
                label = "Ikon tema",
                description = "Glyph adaptif dengan tinta dan bentuk yang mengikuti tema",
            ),
            IconPackInfo(
                id = IconPackSelection.SYSTEM,
                label = "Ikon sistem",
                description = "Ikon asli aplikasi dengan penyesuaian e-paper bila aktif",
            ),
        )
    }
}

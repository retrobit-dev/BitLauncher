package dev.retrobit.bitlauncher.data

import android.content.Context
import android.text.format.DateFormat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED, KALEIDO, KALEIDO_NIGHT }

/**
 * Visual identity of the launcher.
 * MINIMAL is the original Niagara/Minima inspired look, FOLIO turns the home screen into a
 * newspaper front page printed on e-paper (Kaleido palette).
 */
enum class LauncherStyle { MINIMAL, FOLIO }

enum class AppLayout { LIST, GRID }

data class LauncherSettings(
    val style: LauncherStyle = LauncherStyle.FOLIO,
    val themeMode: ThemeMode = ThemeMode.KALEIDO,
    val dynamicColor: Boolean = true,
    val showClock: Boolean = true,
    val showDate: Boolean = true,
    val use24Hour: Boolean = false,
    val layout: AppLayout = AppLayout.LIST,
    val gridColumns: Int = 4,
    val autoFocusSearch: Boolean = false,
    val showAlphabetRail: Boolean = true,
    val haptics: Boolean = true,
    val compactRows: Boolean = false,
    val newsLocation: String = "Rembang",
    val showNewsPhoto: Boolean = true,
    val showNewsWidgets: Boolean = true,
    val notesPackage: String = "",
    val favorites: List<String> = emptyList(),
    val hidden: Set<String> = emptySet(),
)

/**
 * Tiny SharedPreferences-backed store. Launchers must start instantly, so we deliberately avoid
 * DataStore/Room startup cost for a handful of preferences.
 */
class SettingsRepository(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val systemUses24Hour = DateFormat.is24HourFormat(context)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private fun load(): LauncherSettings {
        val theme = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.KALEIDO.name)!!)
        }.getOrDefault(ThemeMode.KALEIDO)
        val style = runCatching {
            LauncherStyle.valueOf(prefs.getString(KEY_STYLE, LauncherStyle.FOLIO.name)!!)
        }.getOrDefault(LauncherStyle.FOLIO)
        val layout = runCatching {
            AppLayout.valueOf(prefs.getString(KEY_LAYOUT, AppLayout.LIST.name)!!)
        }.getOrDefault(AppLayout.LIST)
        val favorites = prefs.getString(KEY_FAVORITES, null)
            ?.split(SEPARATOR)
            ?.filter { it.isNotBlank() }
            ?: emptyList()
        return LauncherSettings(
            style = style,
            themeMode = theme,
            dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
            showClock = prefs.getBoolean(KEY_SHOW_CLOCK, true),
            showDate = prefs.getBoolean(KEY_SHOW_DATE, true),
            use24Hour = prefs.getBoolean(KEY_24H, systemUses24Hour),
            layout = layout,
            gridColumns = prefs.getInt(KEY_COLUMNS, 4).coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            autoFocusSearch = prefs.getBoolean(KEY_AUTOFOCUS, false),
            showAlphabetRail = prefs.getBoolean(KEY_ALPHABET_RAIL, true),
            haptics = prefs.getBoolean(KEY_HAPTICS, true),
            compactRows = prefs.getBoolean(KEY_COMPACT, false),
            newsLocation = prefs.getString(KEY_NEWS_LOCATION, "Rembang") ?: "Rembang",
            showNewsPhoto = prefs.getBoolean(KEY_NEWS_PHOTO, true),
            showNewsWidgets = prefs.getBoolean(KEY_NEWS_WIDGETS, true),
            notesPackage = prefs.getString(KEY_NOTES_PACKAGE, "") ?: "",
            favorites = favorites,
            hidden = prefs.getStringSet(KEY_HIDDEN, emptySet()) ?: emptySet(),
        )
    }

    private fun persist(settings: LauncherSettings) {
        prefs.edit()
            .putString(KEY_STYLE, settings.style.name)
            .putString(KEY_THEME, settings.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            .putBoolean(KEY_SHOW_CLOCK, settings.showClock)
            .putBoolean(KEY_SHOW_DATE, settings.showDate)
            .putBoolean(KEY_24H, settings.use24Hour)
            .putString(KEY_LAYOUT, settings.layout.name)
            .putInt(KEY_COLUMNS, settings.gridColumns)
            .putBoolean(KEY_AUTOFOCUS, settings.autoFocusSearch)
            .putBoolean(KEY_ALPHABET_RAIL, settings.showAlphabetRail)
            .putBoolean(KEY_HAPTICS, settings.haptics)
            .putBoolean(KEY_COMPACT, settings.compactRows)
            .putString(KEY_NEWS_LOCATION, settings.newsLocation)
            .putBoolean(KEY_NEWS_PHOTO, settings.showNewsPhoto)
            .putBoolean(KEY_NEWS_WIDGETS, settings.showNewsWidgets)
            .putString(KEY_NOTES_PACKAGE, settings.notesPackage)
            .putString(KEY_FAVORITES, settings.favorites.joinToString(SEPARATOR))
            .putStringSet(KEY_HIDDEN, settings.hidden)
            .apply()
    }

    fun update(transform: (LauncherSettings) -> LauncherSettings) {
        val updated = transform(_settings.value)
        _settings.value = updated
        persist(updated)
    }

    fun toggleFavorite(key: String) = update { settings ->
        if (key in settings.favorites) {
            settings.copy(favorites = settings.favorites - key)
        } else {
            settings.copy(favorites = settings.favorites + key)
        }
    }

    fun setHidden(key: String, hidden: Boolean) = update { settings ->
        if (hidden) {
            settings.copy(hidden = settings.hidden + key, favorites = settings.favorites - key)
        } else {
            settings.copy(hidden = settings.hidden - key)
        }
    }

    // ---------------------------------------------------------------- backup

    fun exportJson(): String {
        val settings = _settings.value
        val root = JSONObject()
        root.put("schema", SCHEMA)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("style", settings.style.name)
        root.put("themeMode", settings.themeMode.name)
        root.put("dynamicColor", settings.dynamicColor)
        root.put("showClock", settings.showClock)
        root.put("showDate", settings.showDate)
        root.put("use24Hour", settings.use24Hour)
        root.put("layout", settings.layout.name)
        root.put("gridColumns", settings.gridColumns)
        root.put("autoFocusSearch", settings.autoFocusSearch)
        root.put("showAlphabetRail", settings.showAlphabetRail)
        root.put("haptics", settings.haptics)
        root.put("compactRows", settings.compactRows)
        root.put("newsLocation", settings.newsLocation)
        root.put("showNewsPhoto", settings.showNewsPhoto)
        root.put("showNewsWidgets", settings.showNewsWidgets)
        root.put("notesPackage", settings.notesPackage)
        root.put("favorites", JSONArray(settings.favorites))
        root.put("hidden", JSONArray(settings.hidden.toList()))
        return root.toString(2)
    }

    /** @return true when the file was a valid BitLauncher backup. */
    fun importJson(raw: String): Boolean = runCatching {
        val root = JSONObject(raw)
        if (root.optInt("schema", 0) != SCHEMA) return false
        val current = _settings.value
        val favorites = root.optJSONArray("favorites")?.let { array ->
            (0 until array.length()).mapNotNull { array.optString(it, null) }
        } ?: current.favorites
        val hidden = root.optJSONArray("hidden")?.let { array ->
            (0 until array.length()).mapNotNull { array.optString(it, null) }.toSet()
        } ?: current.hidden
        val restored = current.copy(
            style = runCatching {
                LauncherStyle.valueOf(root.optString("style", current.style.name))
            }.getOrDefault(current.style),
            themeMode = runCatching {
                ThemeMode.valueOf(root.optString("themeMode", current.themeMode.name))
            }.getOrDefault(current.themeMode),
            dynamicColor = root.optBoolean("dynamicColor", current.dynamicColor),
            showClock = root.optBoolean("showClock", current.showClock),
            showDate = root.optBoolean("showDate", current.showDate),
            use24Hour = root.optBoolean("use24Hour", current.use24Hour),
            layout = runCatching {
                AppLayout.valueOf(root.optString("layout", current.layout.name))
            }.getOrDefault(current.layout),
            gridColumns = root.optInt("gridColumns", current.gridColumns)
                .coerceIn(MIN_COLUMNS, MAX_COLUMNS),
            autoFocusSearch = root.optBoolean("autoFocusSearch", current.autoFocusSearch),
            showAlphabetRail = root.optBoolean("showAlphabetRail", current.showAlphabetRail),
            haptics = root.optBoolean("haptics", current.haptics),
            compactRows = root.optBoolean("compactRows", current.compactRows),
            newsLocation = root.optString("newsLocation", current.newsLocation),
            showNewsPhoto = root.optBoolean("showNewsPhoto", current.showNewsPhoto),
            showNewsWidgets = root.optBoolean("showNewsWidgets", current.showNewsWidgets),
            notesPackage = root.optString("notesPackage", current.notesPackage),
            favorites = favorites,
            hidden = hidden,
        )
        _settings.value = restored
        persist(restored)
        true
    }.getOrDefault(false)

    companion object {
        const val SCHEMA = 1
        const val MIN_COLUMNS = 3
        const val MAX_COLUMNS = 6

        private const val PREFS = "bitlauncher_settings"
        private const val SEPARATOR = "\u001f"

        private const val KEY_STYLE = "style"
        private const val KEY_THEME = "theme"
        private const val KEY_DYNAMIC_COLOR = "dynamic_color"
        private const val KEY_SHOW_CLOCK = "show_clock"
        private const val KEY_SHOW_DATE = "show_date"
        private const val KEY_24H = "use_24h"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_COLUMNS = "columns"
        private const val KEY_AUTOFOCUS = "autofocus"
        private const val KEY_ALPHABET_RAIL = "alphabet_rail"
        private const val KEY_HAPTICS = "haptics"
        private const val KEY_COMPACT = "compact"
        private const val KEY_NEWS_LOCATION = "news_location"
        private const val KEY_NEWS_PHOTO = "news_photo"
        private const val KEY_NEWS_WIDGETS = "news_widgets"
        private const val KEY_NOTES_PACKAGE = "notes_package"
        private const val KEY_FAVORITES = "favorites"
        private const val KEY_HIDDEN = "hidden"
    }
}
